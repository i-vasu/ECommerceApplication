# Build stage
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# SkyWalking Agent Stage
FROM alpine:latest AS agent
WORKDIR /agent
RUN apk add --no-cache wget tar
# Download SkyWalking Java Agent
RUN wget https://archive.apache.org/dist/skywalking/java-agent/9.0.0/apache-skywalking-java-agent-9.0.0.tgz && \
    tar -zxvf apache-skywalking-java-agent-9.0.0.tgz && \
    mv skywalking-agent agent

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Copy agent from agent stage
COPY --from=agent /agent/agent /app/skywalking-agent

EXPOSE 8080

# Production Entrypoint with SkyWalking Agent
ENTRYPOINT ["java", "-Duser.timezone=Asia/Kolkata", "-javaagent:/app/skywalking-agent/skywalking-agent.jar", "-Dskywalking.agent.service_name=ecommerce-backend", "-Dskywalking.collector.backend_service=skywalking-oap:11800", "-jar", "app.jar"]
