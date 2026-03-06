# ---------- FASE 1: BUILD ----------
FROM gradle:8.14-jdk21 AS build
WORKDIR /app

# -- 1. Copia solo i file di configurazione Gradle (cambiano raramente) --
COPY gradle/                  gradle/
COPY gradlew                  gradlew
COPY settings.gradle          settings.gradle
COPY build.gradle             build.gradle
COPY analytics-service/build.gradle analytics-service/build.gradle
COPY auth-service/build.gradle       auth-service/build.gradle
COPY gateway/build.gradle            gateway/build.gradle
COPY service-discovery/build.gradle  service-discovery/build.gradle

# -- 2. Scarica le dipendenze (layer cacheable: si riesegue solo se cambiano i build.gradle) --
RUN sh gradlew dependencies --no-daemon

# -- 3. Copia i sorgenti e compila --
COPY . .
RUN sh gradlew :auth-service:bootJar -x test --no-daemon

# ---------- FASE 2: RUNTIME ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/auth-service/build/libs/auth-service-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]