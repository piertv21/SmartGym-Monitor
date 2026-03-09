# -----------------------------
# BUILD
# -----------------------------
FROM gradle:8.14-jdk21 AS build
WORKDIR /app
ARG SERVICE

COPY gradle gradle
COPY gradlew .
COPY settings.gradle.kts .
COPY build.gradle.kts .

COPY analytics-service/build.gradle.kts analytics-service/build.gradle.kts
COPY auth-service/build.gradle.kts auth-service/build.gradle.kts
COPY gateway/build.gradle.kts gateway/build.gradle.kts
COPY service-discovery/build.gradle.kts service-discovery/build.gradle.kts

RUN gradle dependencies --no-daemon
COPY ${SERVICE}/src ${SERVICE}/src

RUN gradle :${SERVICE}:bootJar -x test --no-daemon

# -----------------------------
# RUNTIME
# -----------------------------
FROM eclipse-temurin:21-jre
WORKDIR /app
ARG SERVICE
COPY --from=build /app/${SERVICE}/build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]