FROM maven:3.9.10-eclipse-temurin-25 AS build

WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:25-jre

WORKDIR /app
COPY --from=build /app/target/smartbus-1.0.0.jar app.jar

ENV PORT=8080
EXPOSE 8080
CMD sh -c "java -Dserver.port=${PORT} -jar app.jar"