# Distributed Lock <sub>분산락</sub>

Lock 은 여러 Thread 간 데이터에 동시 접근할때, 데이터의 무결성과 일관성을 지켜준다. Java 의 synchronized 나 mutex 등.. 을 활용할 수 있겠지만 범위가 하나의 프로세스가 아닌 여러개의 프로세스가 된다면 외부에서 Lock 을 관리해주는 방법이 필요하다. 이를 <code>분산락 <sub>Distributed Lock</sub> </code> 이라 한다.

분산락은 대표적으로 `Redis`, `RDB Lock`, `Zookeeper` 을 활용한 방법이 있다.  
이 글에서는 대표적으로 많이 쓰이는 Redis 의 분산락에 대해서 다루겠다.

## Redis SET NX

Redis 에서 `SET` 명령에 `NX` 옵션을 붙여 분산락을 구현할 수 있다. 
NX 옵션을 붙일 경우, Key 가 존재하지 않을 경우에만 성공한다. 
Redis 는 Single Thread 로 동작하기에 여러 프로세스가 접근 시, 동시성 문제를 해결할 수 있다.
이와 더불어 `EX` 옵션을 추가로 활용하여 Lock 의 Timeout 을 설정하여 
Lock 을 계속 선점할 수 없도록 방지하여 Deadlock 을 예방할 수 있다.

위와 같은 메커니즘은 아래 도식과 같이 간단하게 표현할 수 있다.
``` mermaid
flowchart TD
    TRY((Try)) --> LOCK{SET key NX}
    LOCK -->|Success| PROC[Work Processor]
    PROC --> DEL(DEL key) --> END((End))
    LOCK -->|Fail| RETRY[Retry after random time] --> TRY
```

Go언어를 통해 위 도식과 같이 구현한다면 아래와 같이 할 수 있다.
``` go
type RedisLock struct {
	client *redis.Client
	key    string
	value  string
	ttl    time.Duration
}

func (l *RedisLock) Lock(ctx context.Context) (bool, error) {
	result, err := l.client.SetNX(ctx, l.key, l.value, l.ttl).Result()
	if err != nil {
		return false, err
	}

	return result, nil
}

func (l *RedisLock) Unlock(ctx context.Context) error {
	delResult, err := l.client.Del(ctx, l.key).Result()
	if err != nil {
		return err
	}

	if delResult == 0 {
		return fmt.Errorf("failed to release lock, key does not exist")
	}

	return nil
}

func (l *RedisLock) Wait(ctx context.Context, ch chan<- bool) error {
	for {
		locked, err := l.Lock(ctx)
		if err != nil {
			ch <- false
			return err
		}

		if locked {
			ch <- true
			return nil
		}

		randomSleep()
	}
}

func randomSleep() {
	now := uint64(time.Now().UnixNano())
	rand.Seed(now)
	sleepDuration := time.Duration(rand.Intn(26)) * time.Millisecond
	time.Sleep(sleepDuration)
}

...

lock := RedisLock {
	client: rdb, 
	key: "some-key", 
	value: "", 
	ttl: 10*time.Second
}

ch := make(chan bool)
go lock.WaitForLock(ch)

locked := <-ch
if !locked {
	fmt.Println("Failed")
	return
}

fmt.Println("Success")

err := lock.Unlock()
if err != nil {
	fmt.Println("Failed")
}

...

```

# 문제점

위에서 간단하게 분산락을 구현해보았다. 다만 위와 같이 구현하였을때 문제점이 있다.
아래서는 어떤 문제가 있는지 알아보겠다.

## Redis 의 부하

위에서 사용한 `Spin Lock` 방식은 Redis 에게 엄청난 부담을 주게된다.
지속적으로 락을 흭득하기 시도하면서 보내는 요청들은 Redis 에서 트래픽 처리로 인한 부담으로 다가온다.
또한 Lock 점유 시간이 늘어날수록 Redis 에게 전달되는 부담은 커지게된다.

위와 같은 이유에서 Redis 로 분산락을 구현할때 스핀락 대신, Redis 의 `Pub/Sub` 기능을 사용한다.
Lock 을 점유하고 해제할때, 구독중인 프로세서에게 메시지를 발행하여 Lock 을 점유, 해지한다는 메시지를 전달한다.
따라서 지속적인 Lock 점유 여부를 질의하는 요청을 보내지 않아도 되기 때문에 부하가 줄어들게 된다.

위와 같이 Pub/Sub 을 통해 Spin Lock 의 문제를 해결하였다. 그런데 아직 문제가 남아있다.
Pub/Sub 을 사용하면 `1. Lock 획득`, `2. 메시지 발행` 2가지 일을 처리해야한다. 
Subcriber 를 통해 흭득가능 메시지를 받았을 때, 흭득 시도를 하였지만 그 사이 다른 프로세서가 락을 흭득해버리는 경우가 발생할 수 있다.
위와 같이 2가지 명령을 따로 보내게 되면 `Atomic` 하지 않기 때문에 실행 순서가 섞여 문제가 발생할 수 있다.

이는 `Lua Script` 를 활용하여 해결할 수 있다. Redis 에게 Lua Script 를 전달하여 여러 명령을 Atomic 하게 수행할 수 있도록 함으로써 이를 해결할 수 있다.

## Cluster 에서 문제

위에서는 모두 Redis 가 단일 서버로 동작한다는 가정에서 하였다면 이는 `단일 장애 지점`<sub>SPOF</sub> 이 될 수 있지만, Cluster 구조에서 아래와 같이 문제가 발생할 수 있다.

Slave 가 Master 로 승격되었을 때, 
클라이언트가 Lock 을 흭득 후 Master 가 다운될 경우 Slave 가 Master 로 승격하게된다.
이때, 만약 Master 로 승격된 Slave 에게 쓰기 데이터가 전송되지 않았다면 
다른 클라이언트에게 Lock 을 허용하게 되는 것이다.

이러한 문제를 해결하기위해 Redis에서는 `Redlock` 알고리즘을 제공한다. 이는 아래와 같이 작동한다.

### Redlock

1. 현재 시간을 ms 단위로 흭득.
2. 숙차적으로 n 개의 Redis 에 전부 Lock 을 흭득.
3. Lock 자체의 Timeout 대비 짧은 시간 내에 과반수의 Redis 에서 잠금이 흭득되었다면 Lock 흭득.
4. 과반수가 흭득하지 못한면 전부 잠금을 모두 해제.

다만 위 방식은 단일 서버 대비 성능 손해가 심하며, 이러한 알고리즘 또한 여러 문제가 발생하게된다. 

## Redlock 에서 발생할 수 있는 문제

Redlock 또한 완벽하지 않다. Redlock 알고리즘 또한 문제가 발생할 수 있으며, 
아래서는 Redlock 에서 발생할 수 있는 문제에 대해 알아보겠다.

### Clock Drift

현실에서 Redis 노드들의 시간은 크지는 않지만 약간의 오차가 존재한다. 
이 오차 때문에 발생하는 문제를 Clock Drift 라고 부른다.

만약 클라이언트가 5개의 노드 중, 1번, 2번, 3번 노드를 통해 잠금을 흭득하였다고 가정하겠다.
3번 노드의 시간이 약간 빨라 잠금이 만료되었고, 
이 사이 다른 클라이언트가 3번, 4번, 5번 노드를 통해 잠금을 얻는다면 
split brain condition 문제가 발생할 수 있다.

### App 중단으로 인한 이슈

클라이언트가 Lock 을 흭득 후, 어떠한 이유로 `Stop The World` 가 발생한다고 가정하겠다.
설정된 TTL 이 지나고서야 stop-the-world 가 해제되고 클라이언트는 공유자원을 변경하다.
이때, 동시에 다른 클라이언트가 TTL 이 해제된 Lock 점유후 공유자원을 건들게되면 공유자원은 원자성을 보장받지 못하게된다.

이러한 문제는 공유자원에 `Version` 등의 필드를 추가하여 CAS 와 같은 추가적인 기능으로 해결할 수 있다.

### 그럼에도..

그럼에도 Redlock 좋은 알고리즘이고 Redis 에서도 분산락으로 레드락의 사용을 권장한다.
만약, 가용성이 중요하다면 Zookeepr Cluster 를 사용을 고려해볼만하다.