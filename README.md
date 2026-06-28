# 🌉 Bridgify — 미국 주식 투자 시뮬레이션 시스템

> 세금, 환율, 물가 상승률을 반영한 실질 자산 가치를 시뮬레이션하세요.

<br>

## 📌 프로젝트 소개

**Bridgify**는 개인 투자자가 미국 주식 투자 시 고려해야 할 세금, 환율, 물가 상승률을 통합적으로 반영하여 실제 구매력 기준의 투자 성과를 분석할 수 있도록 설계된 투자 시뮬레이션 시스템입니다.

단일 인터페이스를 통해 투자 정보를 입력하고, 실질 가치가 반영된 결과를 리포트 형태로 제공받을 수 있습니다.

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
![MyBatis](https://img.shields.io/badge/MyBatis-4.0.1-000000?style=flat)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat&logo=mysql)
![Lombok](https://img.shields.io/badge/Lombok-red?style=flat)

### Frontend
![React](https://img.shields.io/badge/React-19-61DAFB?style=flat&logo=react&logoColor=black)
![TypeScript](https://img.shields.io/badge/TypeScript-6.0-3178C6?style=flat&logo=typescript&logoColor=white)
![Zustand](https://img.shields.io/badge/Zustand-5.0-orange?style=flat)
![Vite](https://img.shields.io/badge/Vite-8.0-646CFF?style=flat&logo=vite&logoColor=white)
![React Router](https://img.shields.io/badge/React_Router-7.0-CA4245?style=flat&logo=reactrouter&logoColor=white)

<br>

## ✨ 주요 기능

### 📈 투자 시뮬레이션 실행
- 사용자가 입력한 정보를 기반으로 실질 가치 계산 및 리포트 제공
- 현재가치(PV) → 미래가치(FV) 계산: `PV = FV / (1 + r)^n`

### 🔍 매수 데이터 입력 및 종목 검색
- 티커 검색, 매수일자, 평단가, 환율 등 과거 매수 데이터 입력

### 💸 세금 및 수수료 설정
- 양도소득세, 배당세, 수수료 등 세금 및 수수료 비율 설정

### 📉 물가 상승률 적용
- 예상 연간 물가 상승률(KRW/USD) 적용으로 실질 가치 환산

### 📊 자산 구매력 환산 리포트
- 명목 vs 실질 비교 ("20년 뒤 돈 10억은, 현재의 4.5억 원의 가치")
- 실물 가치 체감 지표: 치킨 / 자동차 / 부동산 기준으로 시각화

### 📉 자산 추이 시각화
- 명목 잔고 vs 실질 구매력 그래프
- 세금 요약 (양도소득세 / 배당세 / 수수료 누적)

### 💾 결과 리포트 조회 및 저장
- 명목/실질 비교, 실물 가치 체감 차트 및 그래프 리포트 저장

<br>

## 🗄 DB 설계

```
simulation_config (시뮬레이션 설정)
├── asset (종목 정보)
├── purchase (매수 데이터)
├── tax_fee (세금 및 수수료)
└── price_item (실물 가격 데이터)
```

- `config_id` 기준으로 모든 데이터 연결
- 1:N 관계로 여러 종목 유연하게 관리
- `item_name` + `price`로 구성된 일반화 구조

<br>

## 🏗 프로젝트 구조

```
Bridgify/
├── BridgifyProject/          # Spring Boot 백엔드
│   └── src/main/java/
│       ├── controller/       # REST API 컨트롤러
│       ├── service/          # 비즈니스 로직
│       ├── dao/              # MyBatis DAO
│       └── model/            # VO / DTO 클래스
│
└── bridgify-ui/              # React 프론트엔드
    └── src/
        ├── api/              # API 호출
        ├── components/       # 공통 컴포넌트
        │   ├── layout/       # 레이아웃
        │   ├── simulation/   # 시뮬레이션 컴포넌트
        │   └── ui/           # UI 컴포넌트
        ├── pages/            # 페이지
        ├── store/            # Zustand 전역 상태
        ├── types/            # TypeScript 타입
        └── utils/            # 유틸리티
```

<br>

## 🔑 아키텍처 특징

- **RESTful API** 설계 (noun 기반 URI)
- **생성자 주입** 방식 (`@RequiredArgsConstructor`)
- **Zustand**로 전역 상태 관리
- **TypeScript**로 타입 안정성 확보
- 백엔드(Spring Boot) + 프론트엔드(React) 완전 분리 구조
- 사용자 입력 데이터를 내부적으로 Asset 객체로 변환, ResultDTO 형태로 출력
