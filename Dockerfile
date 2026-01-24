# Build stage - Liberica JDK 25
FROM bellsoft/liberica-openjdk-alpine:25 AS build
RUN apk add --no-cache maven
# Note: Using Temurin for build is fine as long as it handles Java 25 syntax, 
# but for consistency and CRaC support we'll use Liberica for the runtime.
WORKDIR /app
COPY pom.xml .
COPY modulith-service ./modulith-service
COPY automation-tests ./automation-tests
RUN mvn clean package -pl modulith-service -am -DskipTests

# Run stage - Liberica JDK 25 (Standard) for CRaC and Performance
FROM bellsoft/liberica-openjdk-alpine:25
WORKDIR /app
COPY --from=build /app/modulith-service/target/*.jar app.jar

EXPOSE 8080

# Java 25 Production Optimized Entrypoint
# - Generational ZGC: Sub-millisecond GC pauses
# - String Deduplication: Reduce memory for repeated strings
ENTRYPOINT ["java", \
    "-Duser.timezone=Asia/Kolkata", \
    "-XX:+UseZGC", \
    "-XX:+ZGenerational", \
    "-XX:+UseStringDeduplication", \
    "-Xms512m", \
    "-Xmx2g", \
    "-jar", "app.jar"]
