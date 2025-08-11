# ---------- 1) Build stage ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Gradle 캐시 최적화: 먼저 설정파일만 복사
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./
# CRLF → LF 로 변환(윈도우 방지) + 실행권한
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# 나머지 소스 복사
COPY src src

# JAR 빌드 (테스트까지 돌리고 싶으면 bootJar 대신 build)
RUN ./gradlew --no-daemon clean bootJar

# ---------- 2) Runtime stage ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# 빌드 산출물만 복사
COPY --from=build /app/build/libs/*SNAPSHOT.jar /app/app.jar

# 옵션: 메모리/GC 등
ENV JAVA_OPTS="-Xms256m -Xmx512m"

EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
