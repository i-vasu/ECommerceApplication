FROM eclipse-temurin:25-jdk-alpine
RUN apk add --no-cache vips
WORKDIR /app
COPY target/product-service-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "--enable-preview", "-jar", "app.jar"]
