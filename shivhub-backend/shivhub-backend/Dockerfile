FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /app
COPY . .
RUN chmod +x mvnw && ./mvnw -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app
COPY --from=build /app/.shivhub-build/shivhub-backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
CMD ["sh", "-c", "java -jar app.jar"]
