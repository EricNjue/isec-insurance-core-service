FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests -Dskip.migrations=true -Dpostgresql.version=42.7.3

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Install dependencies for Liquibase
RUN apt-get update && apt-get install -y curl unzip && rm -rf /var/lib/apt/lists/*

# Install Liquibase CLI
ENV LIQUIBASE_VERSION=4.27.0
RUN curl -L https://github.com/liquibase/liquibase/releases/download/v${LIQUIBASE_VERSION}/liquibase-${LIQUIBASE_VERSION}.zip -o liquibase.zip && \
    unzip liquibase.zip -d /opt/liquibase && \
    ln -s /opt/liquibase/liquibase /usr/local/bin/liquibase && \
    rm liquibase.zip

# Download PostgreSQL JDBC driver for Liquibase
ENV POSTGRES_VERSION=42.7.3
RUN curl -L https://repo1.maven.org/maven2/org/postgresql/postgresql/${POSTGRES_VERSION}/postgresql-${POSTGRES_VERSION}.jar -o /opt/liquibase/lib/postgresql.jar

# Copy the built jar
COPY --from=build /app/app-bootstrap/target/*.jar app.jar

# Copy necessary files for Liquibase migrations
# We only need the changelogs and properties file
COPY --from=build /app/app-bootstrap/src/main/resources/db/changelog db/changelog
COPY --from=build /app/app-bootstrap/liquibase.properties .

# Update liquibase.properties to point to the correct changelog path in the container
RUN sed -i 's|changeLogFile=src/main/resources/|changeLogFile=|g' liquibase.properties

COPY --from=build /app/entrypoint.sh .
RUN chmod +x entrypoint.sh

ENTRYPOINT ["./entrypoint.sh"]
