FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src src
ENV MAVEN_OPTS="-Xmx512m -XX:ActiveProcessorCount=2"
RUN --mount=type=cache,target=/root/.m2,sharing=locked mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre-jammy
RUN groupadd --system homely && useradd --system --gid homely homely
WORKDIR /app
COPY --from=build /workspace/target/rental-0.0.1-SNAPSHOT.jar app.jar
USER homely
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-jar", "/app/app.jar"]
