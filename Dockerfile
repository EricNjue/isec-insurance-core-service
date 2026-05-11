FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests -Dskip.migrations=true -Dpostgresql.version=42.7.3

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

# Copy all other module poms to satisfy Maven project structure at runtime
# This includes common, messaging, and all subdirectories under modules/
COPY --from=build /app/common/pom.xml common/
COPY --from=build /app/messaging/pom.xml messaging/
COPY --from=build /app/modules modules/

# Clean up: keep only pom.xml files in the dependency modules to keep image small
RUN find common messaging modules -type f ! -name "pom.xml" -delete
# Also remove empty directories to keep things tidy
RUN find common messaging modules -type d -empty -delete

COPY --from=build /app/entrypoint.sh .

RUN chmod +x entrypoint.sh mvnw

ENTRYPOINT ["./entrypoint.sh"]
