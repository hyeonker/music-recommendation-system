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

/**
 * ---- Version variables (하드코딩 금지 경고 해결) ----
 * Spring Boot가 관리하는 스타터/테스트컨테이너 등은 버전 생략.
 * 외부 라이브러리만 변수로 버전 관리.
 */
val springdocVersion = "2.8.9"
val flywayVersion = "11.7.2"          // core와 database-postgresql은 동일 버전
val caffeineVersion = "3.1.8"
val resilience4jVersion = "2.2.0"
val commonsMathVersion = "3.6.1"

// val jwtVersion = "0.12.3"           // 보안 붙일 때 사용

dependencies {

    // ---------- implementation (런타임/컴파일 공통) ----------
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // 캐시
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine:$caffeineVersion")

    // 재시도(일시적 오류 대응)
    implementation("io.github.resilience4j:resilience4j-spring-boot3:$resilience4jVersion")

    // WebSocket
    implementation("org.springframework.boot:spring-boot-starter-websocket")

    // OpenAPI(Swagger UI)
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocVersion")

    // 마이그레이션
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("org.flywaydb:flyway-database-postgresql:$flywayVersion")

    // 추천/유틸
    implementation("org.apache.commons:commons-math3:$commonsMathVersion")

    // (선택) WebClient 등 리액티브가 필요할 때만 사용
    // implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // ---------- runtimeOnly ----------
    runtimeOnly("org.postgresql:postgresql")

    // ---------- testImplementation ----------
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")

    // ---------- testRuntimeOnly ----------
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // 보안
     implementation("org.springframework.boot:spring-boot-starter-security")
     implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    // implementation("io.jsonwebtoken:jjwt-api:$jwtVersion")
    // runtimeOnly("io.jsonwebtoken:jjwt-impl:$jwtVersion")
    // runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jwtVersion")
}

tasks.test {
    useJUnitPlatform()
}
