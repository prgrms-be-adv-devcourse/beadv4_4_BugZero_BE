# Build stage
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Gradle 전체 프로젝트를 복사하여 빌드 컨텍스트를 맞춥니다.
COPY . .

# 실행 권한을 부여하고 'payment-service' 모듈만 bootJar로 빌드합니다.
RUN sed -i 's/\r$//' gradlew && chmod +x ./gradlew

RUN ./gradlew :payment-service:bootJar --no-daemon

# Run stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# build 스테이지에서 빌드된 payment-service의 JAR 파일만 복사합니다.
COPY --from=build /app/payment-service/build/libs/payment-service-*.jar app.jar

# 애플리케이션 포트 노출 (docker-compose에서 설정할 포트와 맞춤)
EXPOSE 8082

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
