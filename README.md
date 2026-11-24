# AirCheck Server 🌤️

[오늘공기](https://github.com/choidev2026/aircheck) 앱의 백엔드 서버

> **Spring Boot 3.4** + **Kotlin** + **헥사고날 아키텍처**

---

## 역할

```
앱 ──▶ aircheck-server ──▶ 에어코리아 API (대기질)
                       ──▶ 기상청 API (날씨)
```

- **API 프록시**: 앱에서 직접 호출하지 않고 서버 경유
- **캐싱**: 동일 요청 1시간 캐시 → API 할당량 절약
- **데이터 통합**: 날씨 + 대기질 한 번에 응답

---

## 아키텍처

### 멀티모듈 + 헥사고날 (Port-Adapter)

```
┌─────────────────────────────────────────────────────────┐
│                        :app                              │
│              (부트스트랩, DI 조립, 설정)                  │
└─────────────────────────┬───────────────────────────────┘
                          │
      ┌─────────────┬─────┴───────┬─────────────┐
      ▼             ▼             ▼             ▼
┌────────────┐┌────────────┐┌────────────┐┌────────────┐
│  :weather  ││   :push    ││:app-version││   :admin   │
│  날씨 API  ││ 푸시 구독  ││ 버전 확인  ││  관리 API  │
└─────┬──────┘└─────┬──────┘└─────┬──────┘└─────┬──────┘
      └─────────────┴──────┬──────┴─────────────┘
                           ▼
┌─────────────────────────────────────────────────────────┐
│                    :core:service                         │
│              (비즈니스 로직, UseCase)                    │
└─────────────────────────┬───────────────────────────────┘
                          │
┌─────────────────────────┴───────────────────────────────┐
│                    :core:domain                          │
│              (Port 인터페이스, 도메인 모델)              │
└─────────────────────────┬───────────────────────────────┘
                          │
        ┌─────────┬───────┴──┬──────────┐
        ▼         ▼          ▼          ▼
    ┌────────┐┌────────┐┌────────┐┌────────────┐
    │  kma   ││airkor- ││  fcm   ││persistence │
    │adapter ││ea-adap ││adapter ││  -adapter  │
    ├────────┤├────────┤├────────┤├────────────┤
    │ 기상청 ││에어코리││Firebase││   MariaDB  │
    │  API   ││아 API  ││  FCM   ││            │
    └────────┘└────────┘└────────┘└────────────┘
```

### 모듈 구성

| 모듈 | 역할 |
|------|------|
| `:app` | Spring Boot 진입점, 설정 |
| `:feature:weather` | 날씨/대기질 REST API |
| `:feature:push` | 푸시 구독 REST API |
| `:feature:app-version` | 앱 버전 확인 REST API |
| `:feature:admin` | 관리자 API (사용량, 버전) |
| `:core:service` | 비즈니스 로직 |
| `:core:domain` | Port 인터페이스, 도메인 모델 |
| `:core:kma-adapter` | 기상청 API (초단기/단기예보) |
| `:core:airkorea-adapter` | 에어코리아 API |
| `:core:fcm-adapter` | Firebase FCM 푸시 |
| `:core:persistence-adapter` | JPA + MariaDB |

---

## API 엔드포인트

### 날씨/대기질

| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/api/v1/weather/combined?lat=&lng=` | 통합 (날씨+대기질) |
| `POST` | `/api/v1/weather/batch` | 다중 위치 일괄 조회 |

### 앱 버전

| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/api/v1/app/version?platform=&versionCode=` | 버전 체크 |

### Admin (인증 필요)

> **헤더**: `X-Admin-Key: {ADMIN_API_KEY}`

| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/admin/api-usage/today` | 오늘 API 사용량 |
| `GET` | `/admin/api-usage/remaining` | 잔여 호출 수 |
| `POST` | `/admin/app-version` | 앱 버전 설정 |

### Admin 웹 페이지

| URL | 설명 |
|-----|------|
| `/admin` | React 기반 관리 페이지 |

---

## 캐싱 전략

| 캐시 | TTL | 설명 |
|------|-----|------|
| `combined` | 1시간 | 날씨+대기질 통합 |
| `stations` | 24시간 | 측정소 목록 |

- **Caffeine Cache** 사용 (인메모리)
- 좌표별 캐싱 (`lat,lng` 키)

---

## 환경 변수

| 변수 | 설명 | 필수 |
|------|------|------|
| `AIRKOREA_API_KEY` | 에어코리아 API 키 | ✅ |
| `KMA_API_KEY` | 기상청 API 키 | ✅ |
| `ADMIN_API_KEY` | 관리자 API 인증 키 | ✅ |
| `FIREBASE_CREDENTIALS_JSON` | Firebase 서비스 계정 | |
| `DB_HOST`, `DB_USER`, `DB_PASSWORD` | MariaDB 연결 | |

---

## 배포

### 현재 구성

```
AWS Lightsail ($3.50/월)
├── nginx (리버스 프록시)
└── systemd (Spring Boot JAR)
```

### 수동 배포

```bash
# 빌드
./gradlew :app:bootJar

# 서버 전송
scp app/build/libs/app-*.jar ec2-user@서버:/opt/aircheck/app.jar

# 재시작
ssh ec2-user@서버 "sudo systemctl restart aircheck"
```

### CI/CD

- **Push to main** → GitHub Actions → 자동 빌드 & 배포

---

## 로컬 실행

### 1. 환경변수: `local.properties`

프로젝트 루트의 `local.properties`(`.gitignore`에 포함, 커밋하지 않는다)에 적으면 `bootRun`이 환경변수로 넘겨준다.
(`app/build.gradle.kts`의 `bootRun` 설정)

```properties
DB_USER=aircheck
DB_PASSWORD=aircheck
AIRKOREA_API_KEY=your-key
ADMIN_API_KEY=your-key
```

DB 계정은 `docker-compose.yml`의 `db` 서비스와 같은 값(`aircheck` / `aircheck`)을 쓴다.

### 2. DB 실행

`bootRun`은 DB를 띄워주지 않는다. MariaDB를 도커로 먼저 실행한다.

```bash
docker compose up db -d
```

### 3. 서버 실행

```bash
./gradlew :app:bootRun
```

로그에 `Started AircheckServerApplication`이 나오면 정상이다.

---

## 테스트

```bash
# 단위 테스트 (Docker 불필요)
./gradlew test

# 통합 테스트 (Docker 필요, 느림)
./gradlew :app:integrationTest
```

- 통합 테스트는 `app/src/integrationTest`에 있다. MariaDB 컨테이너(Testcontainers)와 기상청/에어코리아 가짜 서버(WireMock)로 서버 전체를 띄워 API를 호출한다.

---

## 라이선스

MIT License

---

**개발:** seriouschoi
