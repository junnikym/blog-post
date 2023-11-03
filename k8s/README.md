# K8s <sub>Kubernetes</sub> Hands-On

컨테이너 환경에서 서비스를 운영하다보면 컨테이너 수가 많아지면서 몇가지 고민이 생기기 마련이다. 대표적으로 배포 관리, 제어, 모니터링, 스케일링, 네트워킹 등.. 을 하기위해 별도의 구성을 하기위해 고민이 생긴다. 이를 관리하는 채계가 없을 때, 이를 해결하기 위해 수동적으로 찾아 해결을 하여야하지만 규모가 커질수록 관리가 어려워진다.

위와 같은 문제를 해결하기위해 `컨테이너 오케스트레이션` 을 사용하여 관리의 복잡성을 줄이고 자동화할 수 있다. 이러한 오케스트레이션 중 대표적인것이 `K8s` 이다.

K8s 는 서비스 디스커버리와 스케일링, 스토리지 오케스트레이션, 자동화된 롤아웃과 롤백, 자동화된 빈 패킹, 자동화된 복구, 시크릿과 구성 관리 등.. 의 다양한 기능을 제공해준다.

위 기능에 대한 자세한 설명은 [K8s Docs](https://kubernetes.io/ko/docs/concepts/overview/) 에서 확인할 수 있다.

<sup>* ref: [nhn cloud 컨테이너 기술 중 ..](https://youtu.be/fivJy6fYmRo?si=4j_XNurgoQcbO2Vl&t=562) </sup>

#### 여담

>  Kubernetes 는 '조타수', '파일럿' 을 의미하는 그리스어에서 유래하였으며, K8s 라는 표기는 Kubernetes 의 맨앞과 맨뒷글자 'K', 's' 그리고 그 사이의 8글자를 의미한다.

> K8s 은 Google 내부에서 사용하기위한 통합 컨테이너 관리를 위해 Borg 라는 시스템을 개발한 것으로 시작되었다. 이후 Google 내부에서 Omega 라는 시스탬을 한차례 더 개발 한 후, 2015년 논문과 함께 Kubernetes를 공식적으로 발표하였다. 현재는 리눅스 재단 산하의 CNCF 재단에 기증되었다.

## K8s 구조

K8s 는 기본적으로 클러스터 단위로 동작을 하고있으며, 용도에 따라 Worker Node 와 Master Node로 구분된다.

![img](./images/components-of-kubernetes.png)

> 클러스터 <sub>Cluster</sub>  
> 각기 다른 서버들을 하나로 묶은 집합으로 하나의 시스템 같이 동작하게 하는 것이다.
> 
> 따라서 K8s 에서의 Cluster 는 컨테이너 형태의 어플리케이션을 호스팅하는 물리/가상 노드들의 집합이라고 볼 수 있다.

> 워커 노드 <sub>Worker Node</sub>  
> 컨테이너가 실제 배치되는 노드이다

> 마스터 노드 <sub>Master Node</sub> ( Control Plane )  
> 클러스터의 전체를 관리하는 노드이다.

### Control Plane Components

Control Plane 은 Master Node 에 해당하며, Control Plane 은 클러스터 전체의 워크로드 리소스 등 주요 구성 요소를 배포하고 제어하는 역할을 한다. 아래는 Control Plane 에 해당하는 기능이다.

<sup>* 워크로드 <sub>Workload</sub>: 쿠버네티스 상에서 동작되는 어플리케이션을 의미</sup>

 - etcd:   
 Key-Value 형태의 스토리지이며, 클러스터 안의 각 구성요소들에 대한 상태나 설정 정보가 저장된다.

 - API Server:  
 쿠버네티스의 명령과 통신을 위한 API 서버; Restful API 지원.

 - Controller Manager:  
 컨트롤러를 생성하고 이를 각 노드에 배포하여 관리하는 역할; 컨트롤러는 노드, 레플리케이션, 엔드포인트, 서비스 어카운트와 토큰을 관리하는 컨트롤러로 구성.

 - Kube-Scheduler:  
 Pod, 서비스 등.. 각 리소스들을 적절한 노드에 할당하는 역할.

 - Cloud Controller Manager:  
 K8s 와 Cloud Provider API 와 연동하기 위한 컨트롤러를 생성하고 이를 각 노드에 배포하며 관리하는 역할

### Node Components

Node Components 는 각 노드에서 Pod 와 컨테이너를 구동하고 관리하기 위해 필요한 요소를 말한다. Cluster 제어에 필요한 Control Plane Components 역시 개별 Pod 로 구성되기 때문에 이를 관리할 수 있는 도구가 필요하다. 따라서 Worker Node 뿐 아니라 Master Node 에도 존재한다.

 - Container Runtime Engine:  
 Cluster 내부에 컨테이너 이미지를 가져오고 구동. pod가 노드안에서 동작할 수 있도록 도와준다.
 - Kube-Proxy:  
 Cluster 의 각 Node 에서 구동되는 쿠버네티스 네트워크 프록시. 
 - Kubelet:  
 Cluster 의 각 Node 에서 실행되는 에이전트; Pod 의 spec 정보 관리/운용, Pod 를 Node 에 할당 등.. 의 역할을 수행함.

<sup>* ref: [nhn cloud Kubernetes 이해하기 중 ..](https://www.youtube.com/watch?v=9zwHZ6Xi8CA) </sup>

<sup>* ref: https://seongjin.me/kubernetes-cluster-components </sup>
