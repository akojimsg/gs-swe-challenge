# syntax=docker/dockerfile:1
#
# Shared multi-stage build for every backend service. Build context is the REPO
# ROOT (so the Gradle wrapper, build scripts, the :shared module, and the target
# service are all visible); the service is selected with --build-arg SERVICE=<name>.
#
# Docker-only: the jar is built inside the image, so a reviewer needs nothing but
# Docker — no host JDK or Gradle. The BuildKit cache mount on the Gradle home is
# shared across all six service builds, so dependencies and the :shared module are
# resolved/built once, not six times.

# ---- build stage -------------------------------------------------------------
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY shared ./shared
COPY services ./services

ARG SERVICE
# The Gradle home cache is shared across the per-service builds. `sharing=locked`
# serializes access so concurrent service builds (compose builds all four at once)
# don't collide on Gradle's journal/VFS lock — otherwise one wins the lock and the
# rest fail with "Timeout waiting to lock journal cache". Deps + :shared are still
# resolved once and reused, just not simultaneously.
#
# bootJar emits two artifacts: the executable boot jar and a `-plain` library jar.
# Pick the executable one (exclude `-plain`).
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

# Container-friendly JVM defaults; override via JAVA_OPTS.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
