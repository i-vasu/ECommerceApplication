#!/bin/bash
# Spring Boot CDS Training Script

APP_JAR="modulith-service/target/modulith-service-0.0.2-SNAPSHOT-exec.jar"

if [ ! -f "$APP_JAR" ]; then
    echo "Error: JAR file not found. Run 'mvn package' first."
    exit 1
fi

echo "Starting CDS Training Run..."
# Perform a training run to generate the application.jsa archive
java -Dspring.context.exit=onRefresh \
     -XX:ArchiveClassesAtExit=application.jsa \
     -Dspring.main.lazy-initialization=false \
     -jar "$APP_JAR"

if [ -f "application.jsa" ]; then
    echo "CDS Training Complete. application.jsa generated."
else
    echo "Error: Failed to generate application.jsa."
    exit 1
fi
