심사/검증을 위한 환경변수 값(API Key, DB 계정 등)이 필요하시면 연락주세요
공개 저장소 특성상 전화번호·비밀값은 README에 직접 기재하지 않습니다.

담당: 팀 밥팅이 – 팀장 심호용  
이메일: shimhy0704@naver.com

VolunMe

자원봉사 매칭/게시/관리 웹앱(Spring Boot).
JDK 21 + Spring Boot 3.5 + MySQL (환경변수 분리, 비밀 커밋 금지).

🚀 빠른 실행(Quickstart)
1) 필수 설치

JDK 21

MySQL (또는 AWS RDS/MySQL 호환)

(선택) IntelliJ IDEA 2024+

2) .env 준비

레포 루트에 있는 .env.example를 복사해서 .env를 만들고 값 채우기:

# 프로젝트 루트(gradlew가 있는 폴더)에서
cp .env.example .env
# Windows PowerShell:
# copy .env.example .env


.env 예시 값:

DB_URL=jdbc:mysql://<HOST>:<PORT>/<DBNAME>?useUnicode=true&characterEncoding=utf8&connectionCollation=utf8mb4_general_ci&serverTimezone=Asia/Seoul&useSSL=false&allowPublicKeyRetrieval=true
DB_USERNAME=your_db_user
DB_PASSWORD=your_db_password

APP_BASE_URL=http://localhost:8080

# (선택) 카카오 OAuth를 사용할 경우만
KAKAO_CLIENT_ID=your_kakao_client_id
KAKAO_REDIRECT_URI=http://localhost:8080/OAuth2/kakao/callback


⚠️ .env는 절대 커밋하지 마세요. 이미 .gitignore에 제외되어 있습니다.

3) 실행
CLI
./gradlew bootRun
# Windows PowerShell: .\gradlew bootRun
# 브라우저: http://localhost:8080

IntelliJ

Run/Debug Configuration의 Working directory를 프로젝트 루트로 설정
(예: C:\Users\...\VolunMe)

그냥 Run ▶︎

⚙️ 환경 설정
Spring Boot 설정

src/main/resources/application.properties는 모든 비밀을 환경변수로 받습니다.

# .env 자동 로드 (여러 경로 대응)
spring.config.import=optional:file:./.env[.properties],optional:file:.env[.properties],optional:file:../.env[.properties]

spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect

필요한 환경변수 목록

DB_URL (필수) : JDBC URL

DB_USERNAME (필수)

DB_PASSWORD (필수)

APP_BASE_URL (옵션) : 기본 http://localhost:8080

KAKAO_CLIENT_ID, KAKAO_REDIRECT_URI (옵션) : 카카오 로그인 사용 시

🧪 빌드/테스트/패키징
# 빌드
./gradlew clean build

# (있다면) 테스트만
./gradlew test

# 실행 Jar (build/libs/*.jar 생성 후)
java -jar build/libs/*-SNAPSHOT.jar

🧰 트러블슈팅
1) ${DB_URL} 관련 에러(Hikari/Driver not accept ${DB_URL}…)

원인: .env가 로드되지 않았거나 Working directory가 루트가 아님.
해결:

.env가 프로젝트 루트에 있는지 확인

IntelliJ Working directory = 프로젝트 루트로 지정

완전 재시작(DevTools 핫리로드 말고 Run 중지 후 다시 실행)

필요 시 IntelliJ Run Config의 Environment variables에 DB_URL/DB_USERNAME/DB_PASSWORD 직접 등록

2) 포트/인코딩

server.port=8080, UTF-8 강제 설정이 기본입니다.

3) 스키마 생성/마이그레이션

심사/개발 환경은 spring.jpa.hibernate.ddl-auto=update

초기 더미 데이터가 필요하면 src/main/resources/data.sql 사용 가능

🔒 보안 가이드

**비밀(.env, application-*.properties)**은 절대 커밋하지 않습니다.

이미 커밋했다면: 즉시 비밀번호/키 변경(회전) → Git 히스토리에서 제거(BFG/git filter-repo) → 강제 푸시.

Git 확인 스니펫:

# 추적 중인 비밀 여부 검사 (PowerShell)
$hits = git ls-files | Select-String -Pattern '\.env$|application-(dev|prod)\.properties'
if ($hits) { "🚨 tracked secrets: "; $hits } else { "✅ secrets not tracked" }

🧱 프로젝트 구조(요약)
VolunMe/
├─ .env.example            # 예시(형식만, 커밋 O)
├─ .gitignore
├─ build.gradle
├─ settings.gradle
├─ gradlew / gradlew.bat / gradle/
└─ src/
   ├─ main/
   │  ├─ java/com/example/logindemo/...   # 소스
   │  └─ resources/
   │     └─ application.properties        # 환경변수 참조 전용
   └─ test/...

🧱 기술 스택

Java 21, Spring Boot 3.5 (Web, Thymeleaf, Data JPA, Validation)

Hibernate 6, HikariCP

MySQL (Connector/J)

Gradle

🤝 기여(선택)

이슈 등록 또는 포크

브랜치 생성: feat/xxx or fix/yyy

커밋 메시지 컨벤션 예: feat: ..., fix: ..., chore: ...

PR 생성

📄 라이선스

TBD (추후 명시)

📞 문의

이슈 트래커에 등록해 주세요.
필요하면 재현(step-by-step), 로그, 환경정보(JDK/OS)를 함께 올려주세요.
