FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy the built jar
COPY --from=build /app/app-bootstrap/target/*.jar app.jar

# Copy necessary files for Liquibase migrations
COPY --from=build /app/mvnw .
COPY --from=build /app/.mvn .mvn
COPY --from=build /app/pom.xml .
COPY --from=build /app/app-bootstrap/pom.xml app-bootstrap/
COPY --from=build /app/app-bootstrap/src app-bootstrap/src
COPY --from=build /app/app-bootstrap/liquibase.properties app-bootstrap/
COPY --from=build /app/entrypoint.sh .

RUN chmod +x entrypoint.sh mvnw

ENTRYPOINT ["./entrypoint.sh"]
