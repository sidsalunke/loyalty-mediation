FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /workspace

# Build context is loyalty-mediation/ (the repo root)

# Parent POM — required by Maven reactor
COPY pom.xml ./pom.xml

# All module POMs — Maven reads every <module> entry even with -pl
COPY mediation-service/pom.xml ./mediation-service/pom.xml
COPY vendor-mock/pom.xml       ./vendor-mock/pom.xml

# Only mediation-service sources are needed for this image
COPY mediation-service/src ./mediation-service/src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -pl mediation-service package -DskipTests -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/mediation-service/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
