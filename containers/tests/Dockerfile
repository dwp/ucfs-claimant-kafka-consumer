FROM python:3.8

RUN mkdir /tests
WORKDIR tests
COPY ./features .
COPY ./ucfs-claimant-kafka-consumer-tests-crt.pem .
COPY ./ucfs-claimant-kafka-consumer-tests-key.pem .
COPY ./dks-crt.pem .
COPY requirements.txt .
RUN pip install -r requirements.txt
ENTRYPOINT ["behave", "--no-capture"]
