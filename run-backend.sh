#!/bin/bash

# Configuration pointing to global tools
JDK_DIR="$HOME/tools/jdk-linux"
MAVEN_DIR="$HOME/tools/maven-linux"

# Export JAVA_HOME and PATH (in case not in env yet)
export JAVA_HOME="$JDK_DIR"
export PATH="$MAVEN_DIR/bin:$JAVA_HOME/bin:$PATH"

echo "Starting Modulith Service..."
echo "Using Java: $(java --version | head -n 1)"
echo "Using Maven: $(mvn -version | head -n 1)"

# Run the application
mvn spring-boot:run -pl modulith-service -DskipTests
