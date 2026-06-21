# syntax=docker/dockerfile:1
#
# Multi-stage build shared by every backend service. Build context is the repo
# root; the target module is selected with --build-arg SERVICE=<name>. The jar is
# built inside the image, so running the stack needs only Docker — no host JDK.

# ---- build stage -------------------------------------------------------------
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY shared ./shared
COPY services ./services

ARG SERVICE
# sharing=locked serializes access to the shared Gradle cache so the four service
# images can build concurrently without colliding on Gradle's cache lock.
# bootJar emits both an executable jar and a `-plain` library jar; keep the former.
RUN --mount=type=cache,target=/root/.gradle,sharing=locked \
    ./gradlew --no-daemon :services:${SERVICE}:bootJar \
    && find services/${SERVICE}/build/libs -name "${SERVICE}-*.jar" ! -name "*-plain.jar" \
       -exec cp {} /workspace/app.jar \;

# ---- runtime stage -----------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

RUN useradd --system --uid 10001 gsswec
USER gsswec

COPY --from=build /workspace/app.jar app.jar

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
