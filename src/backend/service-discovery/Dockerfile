# ---------- FASE 1: BUILD ----------
FROM gradle:8.14-jdk21 AS build
WORKDIR /app
# Copio tutto il contenuto del microservizio dentro il container
COPY . .
# Compilo e creo il JAR (senza test per velocizzare)
RUN gradle clean build -x test

# ---------- FASE 2: RUNTIME ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
# Copio il JAR costruito nella fase precedente
COPY --from=build /app/build/libs/*.jar app.jar
# Avvio l'applicazione
ENTRYPOINT ["java", "-jar", "app.jar"]