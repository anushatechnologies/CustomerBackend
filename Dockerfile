# Stage: Production Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Run as non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy pre-built JAR from Maven build
COPY --chown=appuser:appgroup target/app.jar app.jar

USER appuser

# Configure port 9000
ENV SERVER_PORT=9000
EXPOSE 9000

# Execute application with container-aware JVM flags
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-XX:+ExitOnOutOfMemoryError", "-Duser.timezone=UTC", "-jar", "app.jar"]
