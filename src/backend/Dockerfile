# -----------------------------
# FASE 1 — BUILD
# -----------------------------
FROM gradle:8.14-jdk21 AS build

WORKDIR /app

# Nome del microservizio da buildare
ARG SERVICE

# --- Copia solo file Gradle (layer cacheabile) ---
COPY gradle gradle
COPY gradlew .
COPY settings.gradle .
COPY build.gradle .

COPY analytics-service/build.gradle analytics-service/build.gradle
COPY auth-service/build.gradle auth-service/build.gradle
COPY gateway/build.gradle gateway/build.gradle
COPY service-discovery/build.gradle service-discovery/build.gradle

# --- Scarica le dipendenze (cache Docker) ---
RUN gradle dependencies --no-daemon

# --- Copia solo il codice del servizio richiesto ---
COPY ${SERVICE}/src ${SERVICE}/src

# --- Build del microservizio ---
RUN gradle :${SERVICE}:bootJar -x test --no-daemon

# -----------------------------
# FASE 2 — RUNTIME
# -----------------------------
FROM eclipse-temurin:21-jre

WORKDIR /app

ARG SERVICE

COPY --from=build /app/${SERVICE}/build/libs/*.jar app.jar

ENTRYPOINT ["java","-jar","app.jar"]