FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre-jammy
RUN groupadd --system homely && useradd --system --gid homely homely
WORKDIR /app
COPY --from=build /workspace/target/rental-0.0.1-SNAPSHOT.jar app.jar
USER homely
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-jar", "/app/app.jar"]
