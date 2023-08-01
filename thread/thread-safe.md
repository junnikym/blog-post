# Thread Safe

Multi Thread 환경에서 여러 공유 자원에 여러 스레드가 동시에 접근해도 문제가 되지 않도록 Thread-Safe 를 고려해야한다.
Thread Safety를 지키기 위해서는 아래와 같은 4가지 방법을 고려해야한다.

1. 상호 배제 <sub>Mutual Exclusion</sub>
2. 원자 연산 <sub>Atomic Operation</sub>
3. 쓰레드 지역 저장소 <sub>Thread-Local Storage</sub>
4. 재진입성 <sub>Re-Entrancy</sub>

## 1. 상호 배제 <sub>Mutual Exclusion</sub>

둘 이상의 프로세스가 동시에 임계 영역<sub>CS: Critical Session</sub> 에 진입하는것을 방지하기 위해 사용되는 알고리즘이다.

상호 배제에는 `enterCS()`, `exitCS()` 두가지의 Primitive <sub>기본 연산</sub> 이 존제한다.
이는 각각 진입 전, 후에 실시해야할 연산을 실행하게된다.

- enterCS() Primitive : CS 진입 전 다른 프로세스가 있는지 검사; 다른 프로세스가 있다면 대기.
- exitCS() Primitive : CS 를 빠져나올 때, 후처리를 실행; 프로세스가 CS 벗어남을 표시 

위 두가지 기본 연산을 위해 `Mutual Exclusion`, `Progress`, `Bounded Waiting`과 같은 3가지 요구사항을 만족시켜야한다.

- Mutual Exclusion <sub>상호 배제</sub> : CS 에 프로세스가 있으면 다른프로세스의 진입을 금한다.
- Progress <sub>진행</sub> : CS 에 프로세스가 없다면 CS 에 진일 할 수 있어야한다.
- Bounded Waiting <sub>유한한 대기</sub> : 프로세스의 CS 진임은 유한한 시간 내에 허용되어야 하며 언젠간 들어갈 수 있게 하여 기아상태를 방지해야한다.

