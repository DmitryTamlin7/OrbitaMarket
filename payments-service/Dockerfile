FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY build/libs/*.jar app.jar
EXPOSE 8082
RUN chown -R guest:guest /app
USER guest
ENTRYPOINT ["java", "-jar", "app.jar"]
