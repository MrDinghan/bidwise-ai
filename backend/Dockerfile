# --- build stage ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Cache dependencies first.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
COPY checkstyle.xml spotbugs-exclude.xml ./
RUN mvn -q -B package -DskipTests

# --- runtime stage ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# wget (busybox) is available in alpine for the compose healthcheck.
COPY --from=build /app/target/bidwise-backend.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
