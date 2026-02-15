@echo off
setlocal
echo Starting Modulith Service with Podman...

REM Ensure current directory is project root
cd /d "%~dp0"

REM Set environment variables for Spring Boot
set SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/dev
set JAVA_DB_USERNAME=postgres
set JAVA_DB_PASSWORD=123Vasu456
set REDIS_HOST=127.0.0.1
set SPRING_FLYWAY_URL=jdbc:postgresql://127.0.0.1:5432/dev

REM Run Spring Boot with debug logging for database initialization
mvn spring-boot:run -pl modulith-service -Dmaven.test.skip=true -Dspring-boot.run.arguments="--logging.level.org.flywaydb=DEBUG --logging.level.org.hibernate.SQL=DEBUG"
