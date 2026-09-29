FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY target/fish-sso-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 9000

# Supply environment variables or mount /app/config/application.yml; persist /app/keys.
ENTRYPOINT ["java", "-jar", "app.jar"]
