# board-gateway

회원, 게시글, 댓글, 공지, 이미지 업로드를 제공하는 게시판 서비스의 API 게이트웨이입니다. 클라이언트 요청을 한곳에서 받아 인증을 확인하고 board-api로 전달합니다.

## 전체 구성

```
board-front (3000) → board-gateway (8888) → board-api (8000) → PostgreSQL / Redis / LocalStack(S3)
```

| 저장소 | 역할 |
|---|---|
| [board-front](https://github.com/jaeryeol2/board-front) | 화면 (Next.js) |
| board-gateway (이 저장소) | 단일 진입점, 인증 확인, CORS, 라우팅 |
| [board-api](https://github.com/jaeryeol2/board-api) | 비즈니스 로직, DB/캐시/파일 저장 |

## 게이트웨이를 따로 둔 이유

클라이언트 진입점을 하나로 두고, 모든 요청에 공통으로 필요한 처리를 게이트웨이에서 맡아 뒤쪽 서비스가 비즈니스 로직에 집중하게 했습니다.

- **인증 확인** (`zconfig/filters/GatewayFilter.java`): `Authorization: Bearer` 토큰이 없거나 만료되었고 `X-Guest-Id`도 없으면 401로 막습니다. 로그인, 회원가입, 토큰 재발급, 홈 등은 검사하지 않습니다.
- **CORS** (`zconfig/SecurityConfig.java`): `FRONT_URL`로 지정한 origin만 허용합니다.

## 서비스 분리를 염두에 둔 라우트

라우트를 도메인 단위로 나눠 두었습니다 (`src/main/resources/config/application-local.yaml`).

| 라우트 id | 경로 | 현재 대상 |
|---|---|---|
| `common` | `/api/{menus,user,auth,home}/**` | `http://localhost:8000` |
| `board` | `/api/{board,notice}/**` | `http://localhost:8000` |

지금은 두 라우트 모두 board-api 하나로 갑니다. 회원과 게시판을 별도 서비스로 나누면 프론트 변경 없이 라우트의 `uri`만 바꾸면 됩니다. 실제 서비스 분리는 아직 하지 않았습니다.

gateway(Java 25, Spring Boot 4.0.7)와 api(Java 21, Spring Boot 3.5.4)는 저장소와 런타임 버전이 별개라 따로 배포하고 업그레이드할 수 있습니다.

## 기술 스택

Java 25, Spring Boot 4.0.7, Spring Cloud 2025.1.2 (Gateway Server WebMVC), Spring Security, jjwt 0.12.6

## 로컬 실행

board-api가 8000 포트에서 실행 중이어야 합니다. `JWT_SECRET_KEY`는 board-api, board-front와 같은 값이어야 합니다.

```bash
export JWT_SECRET_KEY=...                  # JWT 서명 키
export FRONT_URL=https://localhost:3000    # CORS 허용 origin
./gradlew bootRun                          # http://localhost:8888/api
```

확인: `curl http://localhost:8888/api/home` (인증 없이 허용되는 경로)
