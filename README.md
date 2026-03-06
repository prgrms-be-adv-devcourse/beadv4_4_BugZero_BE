# beadv4_4_bugzero_BE
[2026 프로그래머스 백엔드 심화 4기] 버그제로 팀의 RareGo 레포지토리입니다.

## 프로젝트 개요

**RareGo**는 희귀 레고 거래를 기반으로 한 **검수 + 실시간 경매 + 예치금 기반 결제**를 결합한 서비스입니다.<br>
검증된 상품만 경매에 참여할 수 있도록 설계하여, 누구나 신뢰할 수 있는 환경에서 공정한 가격으로 레고를 거래할 수 있습니다.

### 설계 원칙 및 기술적 도전
RareGo는 대규모 트래픽 환경에서도 데이터 정합성을 유지하고, 유연하게 확장 가능한 구조를 설계하는 데 집중했습니다.

* 동시성 제어 및 데이터 정합성 보장
  * 비관적 락: 데이터베이스 수준에서 정합성이 반드시 보장되어야 하는 핵심 비즈니스 로직에 적용하여 충돌을 방지했습니다.
  * Redis 분산락: 경매 입찰과 같이 찰나의 순간에 발생하는 초고빈도 트래픽 환경에서 성능 저하를 최소화하면서도 정확한 순차 처리를 구현했습니다.
* Kafka를 활용한 비동기 이벤트 기반 아키텍처
  * 서비스 간의 강한 결합도를 제거하기 위해 Kafka를 도입했습니다.
  * 입찰 성공, 낙찰, 알림 전송 등의 프로세스를 비동기 이벤트로 처리하여 시스템의 응답 속도를 개선하고, 특정 서비스의 장애가 전체로 확산되지 않도록 설계했습니다.
* MSA(Microservices Architecture) 및 DDD 기반 설계
  * 도메인 주도 설계(DDD) 원칙에 따라 각 서비스의 경계를 명확히 정의했습니다.
  * 독립적인 배포와 확장이 가능한 MSA 구조를 채택하여, 향후 특정 도메인의 트래픽 급증 시 해당 서비스만 개별적으로 스케일 아웃할 수 있는 유연성을 확보했습니다.
---

## 프로젝트 링크
| 프로젝트 문서 및 발표 | 시연 및 서비스 |
| :--- | :--- |
| [팀 노션 페이지](https://www.notion.so/TEAM3-RareGo-318aab84681c80bdb65ef7ebdb3463da) | [서비스 시연 영상](https://www.youtube.com/watch?v=kI5zAsByftY) |
| [세미 발표 자료](https://www.canva.com/design/DAG_MeqTsSQ/i_uVHnmCmiVRX8YLOYn6yA/edit) | [서비스 접속 링크](https://rarego.duckdns.org/) |
| [최종 발표 자료](https://www.canva.com/design/DAHCHin51ek/n_-nnGhVSeuMuvm1hdJKmw/edit) | [프론트엔드 저장소](https://github.com/prgrms-be-adv-devcourse/beadv4_4_BugZero_FE) |

---

## 팀원 소개

| 프로필 | 이름 | 역할 및 담당 도메인 |
| :---: | :--- | :--- |
| <img src="https://github.com/gawoooon.png" width="100"> | **[이가원](https://github.com/gawoooon)** | • **PO**<br>• 회원 및 인증 도메인 |
| <img src="https://github.com/Kkimdoyeon.png" width="100"> | **[김도연](https://github.com/Kkimdoyeon)** | • 경매 도메인 |
| <img src="https://github.com/jin214930.png" width="100"> | **[김진명](https://github.com/jin214930)** | • 예치금 결제 및 정산 프로세스<br>• 알림 도메인 |
| <img src="https://github.com/P-Taeyoung.png" width="100"> | **[박태영](https://github.com/P-Taeyoung)** | • 상품 도메인<br>• AI 기능<br>• Kafka (Inbox, Outbox, DLQ) |
| <img src="https://github.com/geolyun.png" width="100"> | **[신동걸](https://github.com/geolyun)** | • 경매 도메인<br>• Redis 분산락<br>• AI 기능<br>• 부하 테스트 |
| <img src="https://github.com/JEONELIJAH.png" width="100"> | **[전우진](https://github.com/JEONELIJAH)** | • AWS 배포 및 인프라<br>• 보증금 프로세스 (Saga 패턴)<br>• 모니터링 |

---

## 기술 스택

| 분류 | 기술 기술 (Stack) |
| :--- | :--- |
| **Backend** | ![Java](https://img.shields.io/badge/Java_21-ED8B00?style=flat-square&logo=openjdk&logoColor=white) ![Spring Boot](https://img.shields.io/badge/Spring_Boot_4.0-6DB33F?style=flat-square&logo=springboot&logoColor=white) ![JPA](https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=flat-square) <br> ![Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=flat-square&logo=springsecurity&logoColor=white) ![OAuth2](https://img.shields.io/badge/OAuth2-000000?style=flat-square) ![Batch](https://img.shields.io/badge/Spring_Batch-6DB33F?style=flat-square) |
| **DB & Cache** | ![MySQL](https://img.shields.io/badge/MySQL_8.0-4479A1?style=flat-square&logo=mysql&logoColor=white) ![Redis](https://img.shields.io/badge/Redis-DC382D?style=flat-square&logo=redis&logoColor=white) ![Elasticsearch](https://img.shields.io/badge/Elasticsearch-005571?style=flat-square&logo=elasticsearch&logoColor=white) |
| **Messaging** | ![Apache Kafka](https://img.shields.io/badge/Apache_Kafka-231F20?style=flat-square&logo=apachekafka&logoColor=white) |
| **Infrastructure** | ![AWS](https://img.shields.io/badge/AWS-232F3E?style=flat-square&logo=amazonwebservices&logoColor=white) ![Terraform](https://img.shields.io/badge/Terraform-7B42BC?style=flat-square&logo=terraform&logoColor=white) ![Kubernetes](https://img.shields.io/badge/Kubernetes-326CE5?style=flat-square&logo=kubernetes&logoColor=white) <br> ![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white) ![Github Actions](https://img.shields.io/badge/Github_Actions-2088FF?style=flat-square&logo=githubactions&logoColor=white) ![Nginx](https://img.shields.io/badge/Nginx-009639?style=flat-square&logo=nginx&logoColor=white) |
| **Monitoring** | ![Grafana](https://img.shields.io/badge/Grafana-F46800?style=flat-square&logo=grafana&logoColor=white) ![Prometheus](https://img.shields.io/badge/Prometheus-E6522C?style=flat-square&logo=prometheus&logoColor=white) |
| **Testing** | ![JUnit5](https://img.shields.io/badge/JUnit5-25A162?style=flat-square&logo=junit5&logoColor=white) ![Mockito](https://img.shields.io/badge/Mockito-78A1BB?style=flat-square) ![k6](https://img.shields.io/badge/k6-7D64FF?style=flat-square&logo=k6&logoColor=white) |
| **Frontend** | ![NextJS](https://img.shields.io/badge/Next.js-000000?style=flat-square&logo=nextdotjs&logoColor=white) ![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?style=flat-square&logo=typescript&logoColor=white) ![TailwindCSS](https://img.shields.io/badge/Tailwind_CSS-06B6D4?style=flat-square&logo=tailwindcss&logoColor=white) |
| **Communication** | ![Notion](https://img.shields.io/badge/Notion-000000?style=flat-square&logo=notion&logoColor=white) ![Slack](https://img.shields.io/badge/Slack-4A154B?style=flat-square&logo=slack&logoColor=white) ![Discord](https://img.shields.io/badge/Discord-5865F2?style=flat-square&logo=discord&logoColor=white) <br> ![Figma](https://img.shields.io/badge/Figma-F24E1E?style=flat-square&logo=figma&logoColor=white) ![Swagger](https://img.shields.io/badge/Swagger-85EA2D?style=flat-square&logo=swagger&logoColor=black) |

---

## 아키텍처
<img width="1024" height="565" alt="Image" src="https://github.com/user-attachments/assets/5d95dd48-59c9-498f-a99b-61f7b1d6dcbf" />

---

## 서비스 흐름도
<img width="500" height="1500" alt="mermaid-diagram-2026-03-05-042524" src="https://github.com/user-attachments/assets/c5654596-98c1-409f-9e3d-e4a7cc225c55" />

## 주요 기능

### 회원 및 인증
* **소셜 로그인**: OAuth2를 이용한 간편 로그인 지원
* **역할 기반 시스템**: 구매자와 판매자, 관리자 역할을 구분하여 도메인 별 접근 권한 관리

### 상품 및 검수
* **AI 가격 추천**: RAG 및 외부 API를 활용한 최적의 경매 시작가 제안
* **신뢰 기반 검수**: 관리자의 직접 검수 프로세스를 거친 검증된 레고 상품만 경매로 등록

### 실시간 경매
* **실시간 입찰**: SSE를 활용한 실시간 가격 업데이트 및 경쟁 입찰
* **동시성 제어**: Redis 분산락을 활용하여 찰나의 순간에 발생하는 대량의 입찰 요청을 안정적으로 처리

### 결제 및 정산
* **예치금 기반 시스템**: 토스페이먼츠 연동을 통한 자유로운 충전 및 출금
* **보증금 프로세스**: 입찰 시 일정 금액을 보증금으로 설정하여 무분별한 입찰 방지 및 노쇼 시 보증금 몰수 로직 적용
* **정산**: 배치를 이용한 대용량 정산 처리
* **분산 트랜잭션 관리**: '경매 낙찰 → 최종 결제 → 정산 데이터 생성'에 이르는 일련의 과정을 Saga 패턴으로 구현하여 서비스 간 데이터 일관성 보장

### 알림
* **이벤트 기반 알림**: Kafka를 활용한 비동기 메시징 처리를 통해 입찰 현황, 경매 낙찰, 정산 완료 등을 실시간으로 사용자에게 알림

___

## 개발 환경 설정

### 1. 환경 변수 설정
프로젝트 루트 디렉토리에 `.env` 파일을 생성하고 아래 내용을 복사하여 필요한 값을 채워주세요.

```env
# Database Configuration
DB_HOST=localhost
DB_PORT=3306
DB_ROOT_PASSWORD=root_password
DB_NAME=rarego
DB_USERNAME=rarego_user
DB_PASSWORD=rarego_password
TZ=Asia/Seoul

# AWS Configuration (S3)
AWS_ACCESS_KEY=<YOUR_AWS_ACCESS_KEY>
AWS_SECRET_KEY=<YOUR_AWS_SECRET_KEY>

# Social Login (OAuth2)
GOOGLE_CLIENT=<YOUR_GOOGLE_CLIENT_ID>
GOOGLE_SECRET=<YOUR_GOOGLE_CLIENT_SECRET>
KAKAO_CLIENT=<YOUR_KAKAO_CLIENT_ID>
NAVER_CLIENT=<YOUR_NAVER_CLIENT_ID>
NAVER_SECRET=<YOUR_NAVER_CLIENT_SECRET>

# Security
JWT_SECRET=<YOUR_JWT_SECRET_KEY_MIN_32_CHARS>

# Infrastructure & Proxy
NGINX_BASIC_AUTH_USER=admin
NGINX_BASIC_AUTH_PASS=admin_password
INTERNAL_BACK_URL=http://backend:8080

# External APIs
TOSS_PAYMENTS_SECRET_KEY=<YOUR_TOSS_SECRET_KEY>
OPENAI_API_KEY=<YOUR_OPENAI_API_KEY>
SLACK_WEBHOOK_URL=<YOUR_SLACK_WEBHOOK_URL>
SLACK_ENABLED=true
```

### 2. 실행 방법
RareGo는 MSA 구조로 설계되어 있어, Docker Compose를 이용한 실행을 권장합니다.

```bash
# 1. 저장소 클론
git clone https://github.com/prgrms-be-adv-devcourse/beadv4_4_bugzero_BE.git
cd beadv4_4_bugzero_BE

# 2. .env 파일 작성 (위의 예시 참고)

# 3. 인프라 및 서비스 실행
docker-compose up -d
```
