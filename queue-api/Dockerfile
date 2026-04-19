FROM gradle:8.10-jdk17 AS builder
WORKDIR /workspace

COPY . .
RUN gradle :queue-api:bootJar --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=builder /workspace/queue-api/build/libs/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
