#!/bash
# Script to run E2E smoke tests
mvn test -pl automation-tests -Dtest=SmokeTest
