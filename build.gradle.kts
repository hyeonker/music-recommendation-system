plugins {
    java
    id("org.springframework.boot") version "3.5.4"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

// 외부 라이브러리 버전(변수로 관리)
val springdocVersion = "2.8.9"
val flywayVersion = "11.7.2"   // ★ core와 database-postgresql을 반드시 같은 버전으로
// val jwtVersion = "0.12.3"      // 🔥 나중에 Security 활성화할 때 주석 해제

dependencies {
    // Boot가 관리하므로 버전 ❌
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")

    // 🚀 WebSocket 의존성 추가 (실시간 매칭 및 채팅용)
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework:spring-messaging")
    implementation("org.springframework:spring-websocket")

    // 🔥 Security 관련 임시 주석처리 (테스트 통과 후 다시 활성화)
    // implementation("org.springframework.boot:spring-boot-starter-security")
    // implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

    // 🔥 JWT 토큰 처리도 임시 주석처리./
    // implementation("io.jsonwebtoken:jjwt-api:$jwtVersion")
    // runtimeOnly("io.jsonwebtoken:jjwt-impl:$jwtVersion")
    // runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jwtVersion")

    // OpenAPI(Swagger UI)
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocVersion")

    // Flyway (PostgreSQL 지원 모듈까지 함께)
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("org.flywaydb:flyway-database-postgresql:$flywayVersion")

    // DB 드라이버
    runtimeOnly("org.postgresql:postgresql")

    // 🎵 추천 시스템 의존성들
    implementation("org.apache.commons:commons-math3:3.6.1")
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // 🔥 Security 테스트도 임시 주석처리
    // testImplementation("org.springframework.security:spring-security-test")
}

tasks.test {
    useJUnitPlatform()
}
