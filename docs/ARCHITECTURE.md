# 아키텍처 상세

## 캐시 전략

### Cache-Aside 패턴

요청이 올 때만 데이터를 가져오고, 캐시에 저장하는 방식.

```
┌──────────┐      ┌───────────┐      ┌──────────────┐
│  Client  │ ──── │   Server  │ ──── │  공공 API    │
└──────────┘      └─────┬─────┘      └──────────────┘
                        │
                   ┌────▼────┐
                   │  Cache  │
                   │ (10분)  │
                   └─────────┘
```

### 흐름

```
1. 요청 도착
2. 캐시 확인
   ├─ 캐시 있음 (HIT)  → 바로 반환 (빠름)
   └─ 캐시 없음 (MISS) → API 호출 → 캐시 저장 → 반환
3. 30분 후 캐시 만료 → 다음 요청 시 다시 API 호출
```

### 왜 이 방식인가?

**프로액티브 방식 (사용 안 함)**
```
스케줄러가 10분마다 API 호출
→ 사용자 없어도 호출
→ 서버 자원 낭비
→ API 호출 제한 빨리 소진
```

**리액티브 방식 (현재 사용)**
```
요청 올 때만 API 호출
→ 사용자 없으면 호출 없음
→ 서버 자원 절약
→ API 호출 최소화
```

### 호출 수 계산

```
하루 = 1440분
캐시 유효시간 = 30분
위치당 최대 호출 = 1440 ÷ 30 = 48회/일
```

| 사용자 수 | 같은 위치 요청 | API 호출 |
|-----------|---------------|----------|
| 0명 | - | 0회 |
| 10명 | 모두 서울 | 최대 144회 |
| 1000명 | 모두 서울 | 최대 144회 |
| 1000명 | 서울/부산/대구 | 최대 432회 |

**사용자 수와 무관하게 "위치 수 × 144"로 제한됨!**

---

## 요청 흐름

### 날씨 조회 (`GET /api/v1/weather`)

```
Client
  │
  ▼
WeatherController (adapter-in)
  │ @GetMapping("/api/v1/weather")
  │
  ▼
GetWeatherUseCase (domain port)
  │ interface
  │
  ▼
WeatherService (application)
  │ @Cacheable("weather")
  │
  ├──────────────────┬────────────────────┐
  ▼                  ▼                    ▼
WeatherPort      AirQualityPort       (Cache)
  │                  │                    │
  ▼                  ▼                    │
KmaWeatherAdapter AirKoreaAdapter        │
  │                  │                    │
  ▼                  ▼                    │
기상청 API       에어코리아 API          │
  │                  │                    │
  └──────────────────┴────────────────────┘
                     │
                     ▼
                  Response
```

### 푸시 알림 발송 (스케줄러)

```
@Scheduled(cron = "0 0 * * * *")  // 매 정시
PushScheduler (adapter-in)
  │
  ▼
PushSubscriptionUseCase (domain port)
  │
  ▼
PushSubscriptionService (application)
  │
  ├─────────────────────────────────┐
  ▼                                 ▼
PushSubscriptionPort           PushNotificationPort
  │                                 │
  ▼                                 ▼
PushSubscriptionRepository      FcmAdapter
  │                                 │
  ▼                                 ▼
MariaDB                         Firebase FCM
```

---

## 스케줄러

| 스케줄러 | 주기 | 역할 |
|----------|------|------|
| `PushScheduler` | 매 정시 | 해당 시간 구독자에게 푸시 발송 |
| `CacheRefreshScheduler` | 6시간 | 측정소 목록 갱신 |

**날씨 데이터는 스케줄러로 갱신하지 않음** → 요청 시 캐시 만료되면 자동 갱신

---

## 캐시 설정

```kotlin
// CacheConfig.kt
Caffeine.newBuilder()
    .expireAfterWrite(30, TimeUnit.MINUTES)  // 30분 후 만료
    .maximumSize(1000)                        // 최대 1000개 항목
```

| 설정 | 값 | 설명 |
|------|---|------|
| `expireAfterWrite` | 30분 | 저장 후 30분 지나면 만료 |
| `maximumSize` | 1000 | LRU로 오래된 항목 제거 |
