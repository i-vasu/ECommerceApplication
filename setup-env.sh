#!/bin/bash

# Configuration
JDK_DIR="$HOME/tools/jdk-linux"
MAVEN_DIR="$HOME/tools/maven-linux"

# Export JAVA_HOME and PATH
export JAVA_HOME="$JDK_DIR"
export PATH="$MAVEN_DIR/bin:$JAVA_HOME/bin:$PATH"

echo "Environment Setup Complete:"
echo "JAVA_HOME: $JAVA_HOME"
echo "Java Version: $(java -version 2>&1 | head -n 1)"
echo "Maven Version: $(mvn -version | head -n 1)"
