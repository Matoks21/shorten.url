#Dockerfile
FROM openjdk:17-jdk-slim

ENV TZ=Europe/Kyiv

ENV SPRING_PROFILES_ACTIVE=default

COPY build/libs/urlshorten-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]
