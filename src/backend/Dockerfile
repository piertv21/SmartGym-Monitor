# -----------------------------
# BUILD
# -----------------------------
FROM gradle:8.14-jdk21 AS build
WORKDIR /app
ARG SERVICE

COPY gradle gradle
COPY gradlew .
COPY settings.gradle .
COPY build.gradle .

COPY analytics-service/build.gradle analytics-service/build.gradle
COPY auth-service/build.gradle auth-service/build.gradle
COPY gateway/build.gradle gateway/build.gradle
COPY service-discovery/build.gradle service-discovery/build.gradle

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