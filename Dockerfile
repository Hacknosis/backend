ARG JAVA_BASE_IMAGE=adoptopenjdk/openjdk13:jre-13.0.2_8-alpine
FROM ${JAVA_BASE_IMAGE}
ARG JAR_LOCATION=build/libs
ARG JAR_FILE=accountservice-0.0.1-SNAPSHOT.jar
COPY ${JAR_LOCATION}/${JAR_FILE} app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]