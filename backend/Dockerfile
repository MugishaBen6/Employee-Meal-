# ==============================================================================
# Multi-Stage Dockerfile for Spring Boot Backend on Render
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build Application with Eclipse Temurin JDK 17 & Maven Wrapper
# ------------------------------------------------------------------------------
FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /app

# Copy Maven wrapper configuration and POM for Docker layer caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Ensure Maven Wrapper has executable permissions
RUN chmod +x mvnw

# Copy application source code
COPY src ./src

# Build production executable JAR
RUN ./mvnw clean package -DskipTests

# ------------------------------------------------------------------------------
# Stage 2: Production Runtime with Eclipse Temurin JRE 17
# ------------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Create a dedicated non-root application user for container security
RUN groupadd -r emeal && useradd -r -g emeal emeal

# Copy the standalone Spring Boot executable JAR from the builder stage
COPY --from=builder /app/target/employee-meal-management-system-1.0.0.jar app.jar

# Set ownership
RUN chown -R emeal:emeal /app

# Run as non-root user
USER emeal

# Default fallback port (Render injects $PORT dynamically)
ENV PORT=8080
EXPOSE 8080

# Run Spring Boot binding dynamically to Render's $PORT
ENTRYPOINT ["sh", "-c", "java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Dserver.port=${PORT:-8080} -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
