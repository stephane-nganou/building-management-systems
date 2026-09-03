FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build
# Dependencies first, so a source-only change does not re-download the world.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
RUN addgroup -S bms && adduser -S bms -G bms
COPY --from=build /build/target/*.jar app.jar
USER bms
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
