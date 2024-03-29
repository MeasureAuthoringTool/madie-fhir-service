## Use AWS AL2 + Corretto base image
FROM amazoncorretto:17-al2-jdk

## add complied jar
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

## Update Packages
RUN yum update -y --security

## add NR jar
RUN curl -O https://download.newrelic.com/newrelic/java-agent/newrelic-agent/current/newrelic.jar \
    && curl -O https://download.newrelic.com/newrelic/java-agent/newrelic-agent/current/newrelic.yml

## Add the wait script to the image
ADD https://github.com/ufoscout/docker-compose-wait/releases/download/2.7.3/wait /wait
RUN chmod +x /wait

## Launch the wait tool and then your application
ENTRYPOINT ["java","-Dspring.profiles.active=it","-javaagent:newrelic.jar","-jar","app.jar"]
