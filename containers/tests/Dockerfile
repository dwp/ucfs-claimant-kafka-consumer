FROM python:3.8

RUN mkdir /tests
WORKDIR tests
COPY ./features .
COPY requirements.txt .
RUN pip install -r requirements.txt
ENTRYPOINT ["behave"]
