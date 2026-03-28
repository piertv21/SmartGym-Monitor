# -----------------------------
# BUILD
# -----------------------------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
ARG SERVICE

COPY gradle/ gradle/
COPY gradlew .
COPY settings.gradle.kts build.gradle.kts ./

COPY analytics-service/build.gradle.kts analytics-service/build.gradle.kts
COPY auth-service/build.gradle.kts auth-service/build.gradle.kts
COPY gateway/build.gradle.kts gateway/build.gradle.kts
COPY service-discovery/build.gradle.kts service-discovery/build.gradle.kts
COPY area-service/build.gradle.kts area-service/build.gradle.kts
COPY machine-service/build.gradle.kts machine-service/build.gradle.kts
COPY tracking-service/build.gradle.kts tracking-service/build.gradle.kts
COPY embedded-service/build.gradle.kts embedded-service/build.gradle.kts
COPY e2e/build.gradle.kts e2e/build.gradle.kts


RUN chmod +x gradlew
RUN ./gradlew --no-daemon :${SERVICE}:dependencies

COPY ${SERVICE}/src ${SERVICE}/src
RUN ./gradlew --no-daemon :${SERVICE}:bootJar -x test

# -----------------------------
# RUNTIME
# -----------------------------
FROM eclipse-temurin:21-jre
WORKDIR /app
ARG SERVICE
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/${SERVICE}/build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]