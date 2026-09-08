# syntax=docker/dockerfile:1

# ---- build ------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /build

# Copy the wrapper and POM first so dependency resolution is cached independently
# of source changes - editing a class should not re-download the internet.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -q clean package -DskipTests

# Unpack the layered jar so Docker can cache dependencies separately from app code.
RUN java -Djarmode=tools -jar target/*.jar extract --layers --launcher --destination extracted

# ---- runtime ----------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

RUN apk add --no-cache curl \
    && addgroup --system --gid 1001 spring \
    && adduser --system --uid 1001 --ingroup spring spring

WORKDIR /app
USER spring:spring

# Ordered least- to most-frequently changing, so a code edit invalidates one small layer.
COPY --from=build --chown=spring:spring /build/extracted/dependencies/ ./
COPY --from=build --chown=spring:spring /build/extracted/spring-boot-loader/ ./
COPY --from=build --chown=spring:spring /build/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=spring:spring /build/extracted/application/ ./

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=5 \
    CMD curl -fsS http://localhost:8080/actuator/health/readiness || exit 1

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseContainerSupport"

# `extract --launcher` explodes the jar into layer directories rather than producing an
# app.jar, so the app is started through the Boot launcher instead of `java -jar`.
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
