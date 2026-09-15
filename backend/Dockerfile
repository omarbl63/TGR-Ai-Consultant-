# =============================================================================
# Multi-stage build for the AdminAI Spring Boot backend.
#   Stage 1 (build)   : Maven + JDK 21 compiles and packages the app.
#   Stage 2 (runtime) : slim JRE 21 runs the resulting jar as a non-root user.
# =============================================================================

# ---- Stage 1: build --------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Warm the dependency cache first (better layer caching on code-only changes).
# The cache mount persists ~/.m2 across builds so dependencies aren't re-downloaded.
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline

# Compile & package.
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q clean package -DskipTests

# ---- Stage 2: runtime ------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

# Run as a non-root user.
RUN groupadd --system spring && useradd --system --gid spring spring \
    && mkdir -p /app/storage && chown -R spring:spring /app

COPY --from=build --chown=spring:spring /workspace/target/*.jar /app/app.jar

USER spring
EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
