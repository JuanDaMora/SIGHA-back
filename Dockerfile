
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY target/*.jar app.jar
# Crear usuario no root
RUN addgroup -S spring && adduser -S spring -G spring
USER spring
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]