# Сборка JAR с встроенным Tomcat (профиль dockerBuild) и запуск в контейнере.
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle ./gradle
COPY bitrix-jca-ra ./bitrix-jca-ra
COPY src ./src

RUN chmod +x gradlew \
    && ./gradlew bootJar -PdockerBuild=true --no-daemon -x test

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=build /app/build/libs/*.jar /app/app.jar

EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=docker

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
