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