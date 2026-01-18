FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="kafka-rest-bridge"
LABEL description="Spring Boot microservice bridging Kafka to REST API"

# Create app user
RUN addgroup -g 1001 appuser && \
    adduser -D -u 1001 -G appuser appuser

# Set working directory
WORKDIR /app

# Copy the JAR file
COPY target/*.jar app.jar

# Change ownership
RUN chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", \
    "app.jar"]
