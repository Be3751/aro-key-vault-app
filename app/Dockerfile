
# Multi-stage build for fat jar
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
# Cache dependencies first
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle* settings.gradle* gradle.properties* ./
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies || true
COPY . .
RUN ./gradlew --no-daemon shadowJar

FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=build /app/build/libs/app.jar app.jar
COPY .env .env
COPY entrypoint.sh entrypoint.sh
RUN chmod +x entrypoint.sh
ENTRYPOINT ["/app/entrypoint.sh", "java", "-jar", "app.jar"]
