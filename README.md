# 🎮 EZ.GG - 롤 듀오 매칭 플랫폼

<p align="center">
  <img width="500" alt="logo" src="https://github.com/user-attachments/assets/1d9a3d77-44ac-43df-af54-5ab5708e1c7a" />
</p>

<p align="center">
  <strong>리그 오브 레전드 듀오 파트너 매칭 서비스</strong><br>
  플레이 스타일과 성향에 맞는 최적의 듀오 파트너를 찾아보세요!
</p>

## 🚀 프로젝트 소개

EZ.GG는 리그 오브 레전드를 즐기는 유저들을 위한 **듀오 매칭 플랫폼**입니다.  
혼자가 아닌 **듀오로 랭크를 올리고 싶은 유저들**에게 자신의 플레이 스타일과 성향에 맞는 파트너를 찾을 수 있는 서비스를 제공합니다.

### ✨ 주요 기능
- 🔍 **스마트 검색**: 플레이 스타일 기반 듀오 파트너 검색
<p align="center">
<img width="800" alt="스마트 검색" src="https://github.com/user-attachments/assets/9a4931e2-1bbf-453c-aaab-6e6d182788f5" />
</p>

- 🎯 **실시간 매칭**: WebSocket을 활용한 즉시 매칭
<p align="center">
<img width="800" alt="스마트 검색" src="https://github.com/user-attachments/assets/3f0e6ee0-cae7-415c-9d77-44633dfc929f" />
</p>

- 📊 **통계 분석**: 라이엇 API 연동으로 정확한 게임 데이터 제공
<p align="center">
<img width="809" alt="타임라인" src="https://github.com/user-attachments/assets/28b3eaab-f2db-49e2-8aea-bcc06d8fddbf" />
</p>
 
- 💬 **커뮤니케이션**: 매칭 후 원활한 소통 지원
<p align="center">
<img width="800" alt="채팅화면" src="https://github.com/user-attachments/assets/1a449ccf-64a3-4428-8710-8aedf3f3414a" />
</p>

## 🏗 서비스 아키텍처

<p align="center">
  <img width="800" alt="아키텍처" src="https://github.com/user-attachments/assets/129b0b13-3a47-42af-90fd-92587cad1988" />
</p>

## 🛠 기술 스택

### Backend
- **Java 21** & **Spring Boot 3.4.4**
- **WebSocket** - 실시간 매칭 시스템
- **ElasticSearch** - 검색 및 매칭 알고리즘
- **Redis** - 캐싱 및 세션 관리
- **Docker** - 컨테이너화
- **Swagger** - API 문서화

### Frontend
- **React 19.1** + **Vite**
- **Node.js 23.11.0** / **npm 11.3.0**

### External APIs
- **Riot Games API** - 게임 데이터 연동
- **OpenAI API** - 자연어 처리 매칭


## 🚀 시작하기

### 환경 설정

프로젝트 루트에 `.env` 파일을 생성하고 다음 정보를 입력하세요:

```bash
# OpenAI API (자연어 처리 매칭)
OPENAI_API_KEY=your_openai_api_key

# 보안 키
SECRET_KEY=your_secret_key

# Riot Games API (게임 데이터)
RIOT_API_KEY=your_riot_api_key
```

> ⚠️ **주의사항**: Riot API 키는 **매일 갱신**이 필요하며, **사용 횟수 제한**이 있습니다.

## 📋 개발 규칙

- **코딩 컨벤션**: [네이버 코딩 컨벤션](https://github.com/naver/hackday-conventions-java) 준수
- **API 문서**: Swagger를 통한 API 명세 관리
- **테스트**: 단위 테스트 작성으로 안정성 확보

