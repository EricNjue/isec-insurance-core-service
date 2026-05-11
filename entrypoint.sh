#!/bin/bash
set -e

# Run Liquibase Migrations
echo "Running database migrations..."
# Update properties with environment variables if they exist
if [ -n "$DB_URL" ]; then sed -i "s|url=.*|url=$DB_URL|g" app-bootstrap/liquibase.properties || sed -i "" "s|url=.*|url=$DB_URL|g" app-bootstrap/liquibase.properties; fi
if [ -n "$DB_USERNAME" ]; then sed -i "s|username=.*|username=$DB_USERNAME|g" app-bootstrap/liquibase.properties || sed -i "" "s|username=.*|username=$DB_USERNAME|g" app-bootstrap/liquibase.properties; fi
if [ -n "$DB_PASSWORD" ]; then sed -i "s|password=.*|password=$DB_PASSWORD|g" app-bootstrap/liquibase.properties || sed -i "" "s|password=.*|password=$DB_PASSWORD|g" app-bootstrap/liquibase.properties; fi

./mvnw liquibase:update -pl app-bootstrap -Dliquibase.propertyFile=app-bootstrap/liquibase.properties

# Start the Application
echo "Starting the application..."
java -jar app.jar
