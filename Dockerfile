# Multi-stage build for optimized Docker image
# Stage 1: Build
FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /workspace

# Copy Gradle wrapper and build files
COPY gradle gradle
COPY gradlew .
COPY settings.gradle .
COPY build.gradle .

# Fix Windows line endings and grant execution permission
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# Download dependencies for better layer caching
RUN ./gradlew dependencies --no-daemon || true

# Copy source code
COPY src src

# Build the application
RUN ./gradlew bootJar -x test --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-jammy

# Set working directory
WORKDIR /app

# Create a non-root user
RUN groupadd -r branchdown && useradd -r -g branchdown branchdown

# Copy the jar file from builder stage
COPY --from=builder /workspace/build/libs/*.jar app.jar

# Change ownership to branchdown user
RUN chown -R branchdown:branchdown /app

# Create log directory
RUN mkdir -p /var/log/branchdown && chown -R branchdown:branchdown /var/log/branchdown

# Switch to non-root user
USER branchdown:branchdown

# Expose the application port
EXPOSE 8083

# Expose the management port (Actuator)
EXPOSE 8084

# Set JVM options for containerized environment
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
