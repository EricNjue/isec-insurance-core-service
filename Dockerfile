FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Install Tesseract and OpenCV dependencies
RUN apt-get update && apt-get install -y \
    tesseract-ocr \
    libtesseract-dev \
    && rm -rf /var/lib/apt/lists/*

COPY app-bootstrap/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
