# syntax=docker/dockerfile:1

# ---- Build ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY gradlew ./
COPY gradle gradle
COPY settings.gradle.kts build.gradle.kts ./
RUN ./gradlew --no-daemon --version

COPY src src
RUN ./gradlew --no-daemon bootJar

# ---- Runtime ----
FROM eclipse-temurin:21-jre AS runtime

RUN useradd --system --create-home --shell /usr/sbin/nologin gymbuddy
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
RUN chown gymbuddy:gymbuddy app.jar

USER gymbuddy
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
