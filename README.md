VolunMe

Reviewer note: 심사/검증에 필요한 환경변수(API Key, DB 계정 등)는 비공개로 제공됩니다.
공개 저장소 특성상 전화번호·비밀값은 README에 직접 기재하지 않습니다.

담당: 팀 밥팅이 – 팀장 심호용
이메일: shimhy0704@naver.com

자원봉사 매칭/게시/관리 웹앱 (Spring Boot).
JDK 21 + Spring Boot 3.5 + MySQL (환경변수 분리, 비밀 커밋 금지)

🚀 빠른 실행(Quickstart)
1) 필수 설치

JDK 21

MySQL (또는 AWS RDS/MySQL 호환)

(선택) IntelliJ IDEA 2024+

2) .env 준비

레포 루트의 .env.example를 복사해 .env를 만들고 값 채우기:

# macOS/Linux
cp .env.example .env
# Windows PowerShell
copy .env.example .env


.env 예시:

DB_URL=jdbc:mysql://<HOST>:<PORT>/<DBNAME>?useUnicode=true&characterEncoding=utf8&connectionCollation=utf8mb4_general_ci&serverTimezone=Asia/Seoul&useSSL=false&allowPublicKeyRetrieval=true
DB_USERNAME=your_db_user
DB_PASSWORD=your_db_password

APP_BASE_URL=http://localhost:8080

# (선택) 카카오 OAuth 사용 시
KAKAO_CLIENT_ID=your_kakao_client_id
KAKAO_REDIRECT_URI=http://localhost:8080/OAuth2/kakao/callback


⚠️ .env는 절대 커밋하지 마세요. 이미 .gitignore에 제외되어 있습니다.

3) 실행

CLI

./gradlew bootRun
# Windows: .\gradlew bootRun
# 브라우저: http://localhost:8080


IntelliJ

Run/Debug Configuration의 Working directory를 프로젝트 루트로 설정
(예: C:\Users\...\VolunMe)

Run ▶︎

⚙️ 환경 설정

src/main/resources/application.properties는 모든 비밀을 환경변수로 받습니다.

# .env 자동 로드 (여러 경로 대응)
spring.config.import=optional:file:./.env[.properties],optional:file:.env[.properties],optional:file:../.env[.properties]

spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect

환경변수 목록
변수	필수	설명
DB_URL	✅	JDBC URL
DB_USERNAME	✅	DB 사용자
DB_PASSWORD	✅	DB 비밀번호
APP_BASE_URL	▫️	기본 http://localhost:8080
KAKAO_CLIENT_ID	▫️	카카오 로그인 사용 시
KAKAO_REDIRECT_URI	▫️	카카오 로그인 사용 시
🧪 빌드/테스트/패키징
# 빌드
./gradlew clean build

# 테스트
./gradlew test

# 실행 Jar (build/libs/*.jar 생성 후)
java -jar build/libs/*-SNAPSHOT.jar

🧰 트러블슈팅
1) ${DB_URL} 관련 에러(Hikari/Driver not accept ${DB_URL}…)

원인: .env 미로딩 또는 Working directory가 루트가 아님.
해결:

.env가 프로젝트 루트에 있는지 확인

IntelliJ Working directory = 프로젝트 루트

완전 재시작(DevTools 핫리로드 말고 Run 중지 → 다시 실행)

필요 시 Run Config Environment variables에 DB_URL/DB_USERNAME/DB_PASSWORD 직접 등록

2) 포트/인코딩

기본 server.port=8080, UTF-8 강제 설정.

3) 스키마 생성/마이그레이션

심사/개발: spring.jpa.hibernate.ddl-auto=update

초기 더미 데이터가 필요하면 src/main/resources/data.sql 사용

🔒 보안 가이드

비밀(.env, application-*.properties) 은 절대 커밋하지 않습니다.

이미 커밋했다면: 즉시 비밀번호/키 변경(회전) → Git 히스토리에서 제거(BFG/git filter-repo) → 강제 푸시.

Git 확인 스니펫 (PowerShell):

$hits = git ls-files | Select-String -Pattern '\.env$|application-(dev|prod)\.properties'
if ($hits) { "🚨 tracked secrets: "; $hits } else { "✅ secrets not tracked" }

🧱 프로젝트 구조(요약)
VolunMe/
├─ .env.example
├─ .gitignore
├─ build.gradle
├─ settings.gradle
├─ gradlew / gradlew.bat / gradle/
└─ src/
   ├─ main/
   │  ├─ java/com/example/logindemo/...
   │  └─ resources/
   │     └─ application.properties
   └─ test/...

🧱 기술 스택

Java 21, Spring Boot 3.5 (Web, Thymeleaf, Data JPA, Validation)

Hibernate 6, HikariCP

MySQL (Connector/J)

Gradle

🤝 기여(선택)

이슈 등록 또는 포크

브랜치: feat/xxx, fix/yyy

커밋: feat: ..., fix: ..., chore: ...

PR 생성

📄 라이선스

TBD (추후 명시)

📬 Contact (비공개)

심사/검증을 위한 환경변수 값(API Key, DB 계정 등)이 필요하시면 연락해주세요
공개 저장소 특성상 전화번호·비밀값은 README에 직접 기재하지 않습니다.
담당: 팀 밥팅이 – 팀장 심호용 / 이메일: shimhy0704@naver.com
