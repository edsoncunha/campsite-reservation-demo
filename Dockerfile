FROM openjdk:19
MAINTAINER edsoncunha.github.io
COPY build/libs/takehome-0.0.1-SNAPSHOT.jar takehome-0.0.1-SNAPSHOT.jar
ENV SPRING_PROFILES_ACTIVE production
ENTRYPOINT ["java","-Duser.timezone=UTC","-jar","/takehome-0.0.1-SNAPSHOT.jar"]