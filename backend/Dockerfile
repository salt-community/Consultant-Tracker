FROM maven:3.8.8-eclipse-temurin-21-alpine AS build

WORKDIR /app

ENV HOST 0.0.0.0
COPY . .
RUN mvn clean package -DskipTests

FROM amazoncorretto:21.0.3-alpine3.19

COPY --from=build /app/target/api-0.0.1-SNAPSHOT.jar consultant-tracker.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","consultant-tracker.jar"]