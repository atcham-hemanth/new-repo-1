FROM eclipse-temurin:25-jre

WORKDIR /app

COPY target/ecr-java-app-1.0.0.jar app.jar

EXPOSE 8085

CMD ["java", "-jar", "app.jar"]
