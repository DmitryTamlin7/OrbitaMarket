FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY build/libs/*.jar app.jar
EXPOSE 8081
RUN chown -R guest:guest /app
USER guest
ENTRYPOINT ["java", "-jar", "app.jar"]
