FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY build/libs/*.jar app.jar
EXPOSE 8081
RUN chown -R appuser:appgroup /app
USER appuser
ENTRYPOINT ["java", "-jar", "app.jar"]