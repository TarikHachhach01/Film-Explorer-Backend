FROM eclipse-temurin:21-alpine
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "target/film-explorer-0.0.1-SNAPSHOT.jar"]
