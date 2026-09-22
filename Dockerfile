FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -q -DskipTests dependency:go-offline
COPY src src
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 verifipay
COPY --from=build /app/target/*.jar app.jar
USER verifipay
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
