AOP Proxy를 생성할 때 Target의 Interface 유무를 판단하여 다음과 같은 Proxy를 사용한다.

1. Interface가 존재 시, `JDK Dynamic Proxy` 를 통해 생성
2. Interface가 존재하지 않을 시, `CGLib` 를 통해 생성

## JDK Dynamic Proxy 와 CGLib 

`JDK Dynamic Proxy` 는 Java reflection package에 존재하는 Proxy 클래스를 통해 생성된 객체이며, 
<code>CGLib<sub> Code Generator Library</sub></code> 는 Class의 Byte Code 를 조작하여 Proxy 객체를 생성해주는 라이브러리이다.

### JDK Dynamic Proxy 
   
JDK Dynamic Proxy는 자체적인 검증로직을 통해 Target의 Interface를 검증 후 Proxy Factory를 통해 인터페이스를 상속한 Proxy 객체를 생성한다.
이때, Proxy 객체에 Invocation Handler를 통해 하나의 객체로 반환된다.

```mermaid
flowchart LR
    Interface([Interface])

    style Proxy stroke-dasharray: 5 5
    style Interface stroke-dasharray: 5 5

    subgraph Proxying
        ProxyFactory(Interface)
        Proxy((Proxy))
    end
    Interface-.->ProxyFactory
    ProxyFactory-.->Proxy
    Proxy-.->|Implements| Interface

    subgraph Result
        subgraph ProxyInResult[Proxy]
            subgraph HandlerInResult[Handler]
                TargetInResult[Target]
            end
        end
    end

    TargetInResult-.->|Implements| Interface
    ProxyInResult-.->|Implements| Interface
```

### CGLib <sub> Code Generator Library</sub>

Spring은 CGLib를 사용사용하여 인터페이스가 아닌 Target의 Class에 대해서도 Enhancer라는 Class를 통해 Proxy를 생성한다.
따라서 생성 시 JDK Dynamic Proxy와 다른점은 아래와 같이 2가지가 있다.

- Interface가 아닌 Class에 대한 Proxy를 생성한다
- ProxyFactory가 아닌 Enhancer를 사용한다.

CGLib는 바이트 코드로 조작하여 Proxy를 생성하기 때문에 JDK Dynamic Proxy 보다 성능이 좋다.
하지만 Final Method 또는 Class에 대해 재정의가 불가하므로 Proxy 생성이 불가하다는 단점이 있다.

```mermaid
flowchart LR
    Class([Class])

    style Proxy stroke-dasharray: 5 5
    style Class stroke-dasharray: 5 5

    subgraph Proxying
        Enhancer(Enhancer)
        Proxy((Proxy))
    end
    Class-.->Enhancer
    Enhancer-.->Proxy
    Proxy-.->|Implements| Class

    subgraph Result
        subgraph ProxyInResult[Proxy]
            subgraph HandlerInResult[Handler]
                TargetInResult[Target]
            end
        end
    end

    TargetInResult-.->|Implements| Class
    ProxyInResult-.->|Implements| Class
```

## Proxy 생성해보기

