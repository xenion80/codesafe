# ===========================================================================
# CyberTotal backend — multi-stage build
#
#   Stage 1 (build):   JDK 25 + Maven wrapper -> packaged Spring Boot jar
#   Stage 2 (runtime): JRE only, non-root, container-aware JVM settings
#
# Build:  docker build -t cybertotal-backend .
# Run:    docker run --rm -p 8080:8080 --env-file .env cybertotal-backend
#
# Required env at runtime (same as local dev):
#   DB_USERNAME, DB_PASSWORD, JWT_SECRET
#   Optional: GITHUB_CLIENT_ID, GITHUB_CLIENT_SECRET, FRONTEND_URL
# ===========================================================================

# ---------- Stage 1: build ----------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# Copy the Maven wrapper + descriptor first so Docker layer caching skips
# dependency re-download on every code change.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline

# Then copy sources and build the jar (skipping tests; CI runs them).
COPY src/ src/
RUN ./mvnw -q -B clean package -DskipTests

# ---------- Stage 2: runtime ----------
FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app

# Run as non-root user.
RUN useradd --system --create-home --shell /usr/sbin/nologin appuser
USER appuser

# Layered jar support: copy exploratory execution if present; plain jar copy
# works for both layered and non-layered layouts.
COPY --from=build /workspace/target/*.jar app.jar

# Container-aware JVM: heap limits follow cgroup limits; JAVA_OPTS can be
# used to add tuning flags at runtime without rebuilding the image.
ENV JAVA_OPTS=""

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
