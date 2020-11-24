FROM openjdk:8-jre

EXPOSE 8080
EXPOSE 8443

RUN apt-get update && apt-get install -y jq

RUN mkdir -p /opt/data-key-service/data
RUN mkdir -p /opt/data-key-service/logs
WORKDIR /opt/data-key-service

RUN curl --silent --insecure "https://api.github.com/repos/dwp/data-key-service/releases/latest" \
             | jq -r '.assets[0].browser_download_url' > dks_version
RUN cat dks_version
RUN wget --input-file dks_version --no-verbose --no-check-certificate --output-document dks-latest.jar
RUN ls -la *.jar

COPY application.properties ./
COPY dks-keystore.jks ./keystore.jks
COPY dks-truststore.jks ./truststore.jks

RUN chmod a+rwx -R /opt/data-key-service

ENTRYPOINT ["sh", "-c", "java -Dlogging.level.root=INFO -Ddks.log.directory=/opt/data-key-service/logs -jar dks-latest.jar --server.environment_name=local --s3.service.endpoint=http://localstack:4566 \"$@\"", "--"]
