# --- Build stage -------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Cache dependencies separately from source so `docker build` doesn't
# re-download the world on every code change.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B package -DskipTests

# --- Runtime stage -------------------------------------------------------
# Debian's ffmpeg package is built with libass/libfreetype (subtitle support)
# by default, unlike Homebrew's stripped-down formula on macOS — verified
# against this image (see TASKS.md for the macOS gotcha this avoids).
FROM eclipse-temurin:21-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        ffmpeg \
        python3 \
        python3-venv \
        python3-pip \
        curl \
    && rm -rf /var/lib/apt/lists/*

# piper-tts (Python package) provides Vietnamese narration synthesis; kept in
# its own venv so it doesn't fight the base image's system Python packages.
RUN python3 -m venv /opt/piper-venv \
    && /opt/piper-venv/bin/pip install --no-cache-dir piper-tts \
    && ln -s /opt/piper-venv/bin/piper /usr/local/bin/piper

WORKDIR /app
COPY --from=build /build/target/videogen-0.0.1-SNAPSHOT.jar app.jar

# ./models (piper voice model) and ./data (job outputs) are expected to be
# mounted as volumes — see docker-compose.yml. The image intentionally does
# not bundle the ~63MB Vietnamese voice model so it can be swapped/updated
# without rebuilding.
VOLUME ["/app/models", "/app/data"]

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=3s --start-period=20s \
    CMD curl -f http://localhost:8080/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
