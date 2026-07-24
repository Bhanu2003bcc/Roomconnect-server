# ── Stage 1: Build ─────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies first (only re-downloaded when pom.xml changes)
COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn clean package -DskipTests -q

# ── Stage 2: Run ───────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/target/roomconnect-server-0.0.1-SNAPSHOT.jar app.jar

# Render injects PORT; default to 8080 locally
EXPOSE 8080

ENTRYPOINT ["java", "-XX:+TieredCompilation", "-XX:TieredStopAtLevel=1", "-Xms256m", "-Xmx384m", "-jar", "app.jar", "--spring.profiles.active=prod"]
