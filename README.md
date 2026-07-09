# 🌉 Bridgify — 미국 주식 실질가치 투자 시뮬레이터

> 세금, 환율, 물가, 배당 재투자까지 반영한 **실질 구매력** 기준 투자 성과 분석

<br>

## 📌 프로젝트 소개

**Bridgify**는 미국 주식에 원화로 투자할 때 실제로 손에 남는 돈이 얼마인지를 계산하는 시뮬레이터입니다.

명목 수익률은 높아 보여도, 세금과 물가를 반영하면 실제 구매력은 크게 달라집니다. Bridgify는 이 차이를 시각화하고, 사용자가 직접 입력해야 하는 값(물가·환율·현재가·배당)을 **모두 외부 데이터로 자동 조회**해 채웁니다.

**핵심 지향점: "종목과 매수 정보만 입력하면 나머지는 전부 자동."**

<br>

## 👤 개발자

| 이름 | 역할 |
|------|------|
| 강지희 | 개인 프로젝트 / 풀스택 개발 |

<br>

## 🛠 기술 스택

### Backend
![Java](https://img.shields.io/badge/Java-17-007396?style=flat&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.6-6DB33F?style=flat&logo=springboot)
![MyBatis](https://img.shields.io/badge/MyBatis-000000?style=flat)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat&logo=mysql)
![Lombok](https://img.shields.io/badge/Lombok-red?style=flat)

### Frontend
![React](https://img.shields.io/badge/React-19-61DAFB?style=flat&logo=react&logoColor=black)
![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?style=flat&logo=typescript&logoColor=white)
![Zustand](https://img.shields.io/badge/Zustand-5.0-orange?style=flat)
![Vite](https://img.shields.io/badge/Vite-8.0-646CFF?style=flat&logo=vite&logoColor=white)
![Recharts](https://img.shields.io/badge/Recharts-22B5BF?style=flat)

### External APIs
| API | 용도 |
|-----|------|
| Finnhub | 실시간 주가 |
| 한국수출입은행 | 원/달러 환율 (매수일 기준 과거 환율 포함) |
| Alpha Vantage | 과거 배당금 + 연도별 주가 |
| FRED | 미국 소비자물가지수(CPI) |
| 한국은행 ECOS | 한국 소비자물가지수(CPI) |
| Google Gemini | 시뮬레이션 결과 AI 해설 |

<br>

## ✨ 주요 기능

### 📈 미래 자산 시뮬레이션
- 초기 투자금 + 월 적립금 기반 복리 계산
- 실질가치 환산: `PV = FV / (1 + r)^n`
- 명목 자산 vs 실질 구매력 비교, 연도별 성장 그래프

### 💰 실현손익 정산 (배당 재투자)
- 과거 매수 정보로 **지금 팔면 실제 순수익이 얼마인지** 계산
- **DRIP(배당 재투자)** — 매년 받은 배당으로 그 해 주가에 주식을 추가 매수
- 재투자 ON/OFF 토글로 복리 효과를 직접 비교 가능
- 양도소득세(250만 원 공제) + 배당소득세(15.4%) 반영

### 📉 실시간 물가 자동 반영
- 화면 진입 시 한국 CPI를 자동 조회해 물가 상승률에 반영
- 미국 CPI는 참고 표시 (계산 반영 경로는 아래 [물가 계산 설계](#2-미국-물가는-왜-따로-빼지-않는가) 참고)

### 🤖 AI 결과 해설 (Gemini)
- 시뮬레이션 결과를 자연어로 해설
- 명목과 실질의 차이가 왜 생기는지, 세금·물가가 각각 얼마나 영향을 줬는지 설명

### 📊 실물 가치 체감 지표
- 치킨 / 자동차 / 서울 아파트 기준으로 실질 가치를 환산해 체감도 제공

<br>

## 🔑 기술적 의사결정

### 1. 무료 API 호출 제한을 캐싱으로 극복

Alpha Vantage 무료 등급은 **하루 25회**로 제한됩니다. 사용자마다 다른 종목을 조회하는 서비스에서는 금방 한도에 걸립니다.

**해결:** 배당·과거주가는 한 번 확정되면 바뀌지 않는 데이터라는 점에 착안해, **온디맨드 조회 + DB 영구 캐싱** 구조를 설계했습니다.

```
배당 데이터 요청
  ├─ DB에 있음  → 즉시 반환 (API 호출 0회)
  └─ DB에 없음  → Alpha Vantage 1회 호출 → 월별 데이터를 연 단위로 가공 → DB 저장
                   (이후 같은 종목은 영구히 DB에서 처리)
```

- `TIME_SERIES_MONTHLY_ADJUSTED` 한 번의 호출로 **주가와 배당을 동시에** 수집해 호출 수를 절반으로 줄였습니다.
- 종목당 평생 1회만 호출하므로, 하루 25회 제한은 "하루에 새로 등장하는 종목 25개"를 의미하게 됩니다.
- 데이터 소스 교체가 필요하면 `DividendDataService` 한 클래스만 수정하면 되도록 분리했습니다.

### 2. 미국 물가는 왜 따로 빼지 않는가

미국 주식을 원화로 투자하면 물가가 두 번 작용하는 것처럼 보입니다. 그러나 두 물가를 모두 차감하면 **이중 차감**이 됩니다.

| 요소 | 반영되는 곳 |
|------|-------------|
| 미국 물가 | 기업 실적 → **주가(수익률)에 이미 포함** |
| 미·한 물가 차이 | 장기적으로 **환율에 반영** |
| **한국 물가** | **최종 원화의 구매력 차감 ← 직접 나누는 지점** |

따라서 계산은 한국 물가만 사용하되, 사용자가 "미국 물가는 왜 안 쓰이지?"라고 오해하지 않도록 **반영 경로를 UI에 명시**했습니다.

### 3. AI 프롬프트는 백엔드에서 조립

프론트에서 프롬프트 문자열을 만들어 보내면, 사용자가 요청을 조작해 AI에게 임의의 지시를 주입할 수 있습니다(프롬프트 인젝션).

**해결:** 프론트는 **숫자 데이터만** 전송하고, 프롬프트 문장은 서버가 조립합니다. API 키 역시 서버 환경변수에만 존재해 브라우저에 노출되지 않습니다. 프롬프트에는 특정 종목의 매수·매도 추천을 금지하는 규칙을 포함했습니다.

### 4. 렌더링 성능

- Zustand는 `useStore((s) => s.field)` **선택 구독**으로 불필요한 리렌더를 차단
- 차트 컴포넌트는 `React.memo`로 감싸, 부모 리렌더 시에도 props가 같으면 재렌더하지 않도록 처리

<br>

## 🗄 DB 설계

```
simulation_config (시뮬레이션 설정)
├── asset_item          (종목 정보)
├── asset_transaction   (매수 데이터)
└── yearly_result       (연도별 계산 결과)

standard_market_price   (실물 가격 기준 데이터)

── 외부 API 캐시 ──
dividend_history        (종목·연도별 주당배당금 DPS)
price_history           (종목·연도별 주가, DRIP 재투자 계산용)
```

- `config_id` 기준으로 시뮬레이션 데이터 연결 (1:N)
- `dividend_history` / `price_history`는 `(ticker, year)` 유니크 키 + `ON DUPLICATE KEY UPDATE` 로 upsert 처리

<br>

## 🏗 프로젝트 구조

```
Bridgify/
├── BridgifyProject/                # Spring Boot 백엔드
│   └── src/main/java/org/cloud/
│       ├── controller/             # REST API
│       ├── service/                # 비즈니스 로직 + 외부 API 연동
│       │   ├── DividendDataService     # 배당 온디맨드 조회 + 캐싱
│       │   ├── InflationDataService    # FRED / ECOS 물가 조회
│       │   ├── RealizedProfitService   # 실현손익 + DRIP
│       │   └── AiSummaryService        # Gemini 해설
│       ├── domain/
│       │   ├── calculator/         # 복리 / 물가 / 세금 / 배당 계산
│       │   └── entity/             # DB 엔티티
│       ├── dto/                    # 요청·응답 DTO
│       └── mapper/                 # MyBatis 매퍼
│
└── bridgify-ui/                    # React 프론트엔드
    └── src/
        ├── api/                    # API 호출
        ├── components/
        │   ├── layout/
        │   ├── simulation/         # 시뮬레이션 UI
        │   └── ui/                 # 공용 컴포넌트
        ├── pages/
        ├── store/                  # Zustand 전역 상태
        ├── types/
        └── utils/
```

<br>

## 🔌 주요 API

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/simulation/run` | 미래 자산 시뮬레이션 |
| POST | `/api/realized-profit` | 실현손익 정산 (배당 재투자 포함) |
| GET | `/api/inflation/all` | 미국·한국 물가상승률 조회 |
| POST | `/api/ai/summary` | 시뮬레이션 결과 AI 해설 |

<br>

## ⚙️ 실행 방법

### 1. 환경변수 설정
외부 API 키는 코드에 포함하지 않고 환경변수로 주입합니다.

```
FINNHUB_API_KEY=...
KOREAEXIM_API_KEY=...
ALPHAVANTAGE_API_KEY=...
FRED_API_KEY=...
ECOS_API_KEY=...
GEMINI_API_KEY=...
```

### 2. 백엔드
```bash
cd BridgifyProject
./gradlew bootRun      # http://localhost:8080
```

### 3. 프론트엔드
```bash
cd bridgify-ui
npm install
npm run dev            # http://localhost:5173
```

<br>

## 📐 아키텍처 특징

- **RESTful API** 설계, 백엔드 / 프론트엔드 완전 분리
- **생성자 주입** (`@RequiredArgsConstructor`)
- 외부 API 연동을 서비스 단위로 분리해 **데이터 소스 교체 비용 최소화**
- API 키는 전부 환경변수로 관리 (저장소에 노출 없음)
- **TypeScript**로 프론트 타입 안정성 확보
