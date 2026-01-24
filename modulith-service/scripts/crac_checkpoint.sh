#!/bin/bash
# Project CRaC Checkpoint Script

APP_JAR="modulith-service/target/modulith-service-0.0.2-SNAPSHOT-exec.jar"
CHECKPOINT_DIR="crac-files"

mkdir -p "$CHECKPOINT_DIR"

if [ ! -f "$APP_JAR" ]; then
    echo "Error: JAR file not found. Run 'mvn package' first."
    exit 1
fi

echo "Starting Application for Checkpoint..."
# Start with CRaC enabled
java -XX:CRaCCheckpointTo="$CHECKPOINT_DIR" \
     -XX:+UseCompactObjectHeaders --enable-preview \
     -jar "$APP_JAR" &

APP_PID=$!
echo "Application started with PID: $APP_PID"

# Wait for application to be ready (approximate)
sleep 15

echo "Taking Checkpoint..."
jcmd "$APP_PID" JDK.checkpoint

echo "Checkpoint taken. Application terminated."
