FROM maven:3.5.0-jdk-8
# RUN mvn org.apache.maven.plugins:maven-dependency-plugin:2.6:get -Dartifact=org.springframework.boot:spring-boot-maven-plugin:LATEST
# RUN mkdir /opt/app
RUN mkdir -p /opt/app
ADD target/quantalex-users-0.0.1-SNAPSHOT.jar /opt/app
ADD docker/wait-for-it.sh /wait-for-it.sh
RUN chmod 777 /wait-for-it.sh
CMD ["java", "-jar", "/opt/app/quantalex-users-0.0.1-SNAPSHOT.jar"]
# CMD ["echo", "$DB_HOST"]
# EXPOSE 8080