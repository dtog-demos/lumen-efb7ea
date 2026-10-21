FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY . .
RUN mvn -q -pl service-query -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/service-query/target/*.jar /app/app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
