FROM maven:3.5.0-jdk-8
# RUN mvn org.apache.maven.plugins:maven-dependency-plugin:2.6:get -Dartifact=org.springframework.boot:spring-boot-maven-plugin:LATEST
# RUN mkdir /opt/app
RUN mkdir -p /opt/app
ADD target/quantalex-users-0.0.1-SNAPSHOT.jar /opt/app
ADD docker/wait-for-it.sh /wait-for-it.sh
RUN curl -O -X GET https://artifacts.elastic.co/downloads/beats/filebeat/filebeat-5.4.0-linux-x86_64.tar.gz
RUN mkdir /filebeat
RUN tar -xvf filebeat-5.4.0-linux-x86_64.tar.gz -C /filebeat
RUN mv  /filebeat/filebeat-5.4.0-linux-x86_64/filebeat.yml /filebeat/filebeat-5.4.0-linux-x86_64/filebeat-orig.yml
ADD docker/filebeat.yml /filebeat/filebeat-5.4.0-linux-x86_64/filebeat.yml

ADD docker/filebeat.yml /etc/filebeat/filebeat.yml

RUN curl -O -X GET https://raw.githubusercontent.com/logzio/public-certificates/master/COMODORSADomainValidationSecureServerCA.crt
RUN mkdir -p /filebeat/filebeat-5.4.0-linux-x86_64/certs
RUN mkdir -p /etc/pki/tls/certs
RUN cp COMODORSADomainValidationSecureServerCA.crt /filebeat/filebeat-5.4.0-linux-x86_64/certs

RUN cp COMODORSADomainValidationSecureServerCA.crt /etc/pki/tls/certs/

RUN chmod 777 /wait-for-it.sh
CMD ["java", "-jar", "/opt/app/quantalex-users-0.0.1-SNAPSHOT.jar"]
# CMD ["echo", "$DB_HOST"]
# EXPOSE 8080