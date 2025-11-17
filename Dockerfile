# Multi-stage build for optimal image size
FROM sbtscala/scala-sbt:eclipse-temurin-jammy-17.0.10_7_1.10.0_3.4.2 AS builder

WORKDIR /app

# Copy build files
COPY build.sbt .
COPY project ./project

# Download dependencies (cached layer)
RUN sbt update

# Copy source code
COPY src ./src

# Build fat JAR
RUN sbt assembly

# Runtime stage - smaller image
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copy JAR from builder
COPY --from=builder /app/target/scala-2.13/*assembly*.jar app.jar

# Cloud Run expects port 8080
ENV PORT=8080
ENV INTERFACE=0.0.0.0

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s \
  CMD curl -f http://localhost:8080/admin/health || exit 1

# Create non-root user
RUN useradd -m -u 1000 voiceai
USER voiceai

# Run application
ENTRYPOINT ["java", "-Xmx512m", "-XX:+UseG1GC", "-jar", "app.jar"]
