# ---- build ----
FROM maven:3.9-eclipse-temurin-26 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q verify

# ---- run ----
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd --system nord && useradd --system --gid nord nord
COPY --from=build --chown=nord:nord /app/target/*.jar app.jar
USER nord:nord
# Em produção o perfil é "prod": exige JWT_SECRET, DB_URL, DB_USER e DB_PASSWORD
ENV SPRING_PROFILES_ACTIVE=prod \
    TZ=America/Sao_Paulo \
    JAVA_TOOL_OPTIONS="-Duser.timezone=America/Sao_Paulo"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
