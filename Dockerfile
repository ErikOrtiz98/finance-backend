FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Optimizaciones JVM para bajo consumo
ENTRYPOINT ["java", \
    "-Xmx256m", \
    "-Xms128m", \
    "-XX:+UseG1GC", \
    "-XX:MaxGCPauseMillis=100", \
    "-XX:+DisableExplicitGC", \
    "-XX:+UseStringDeduplication", \
    "-jar", \
    "app.jar"]

EXPOSE 8080