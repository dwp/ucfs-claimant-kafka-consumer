FROM python:3-buster

RUN curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
RUN unzip -qq awscliv2.zip
RUN ./aws/install

ENV SERVICE_USER=dataworks
ENV SERVICE_USER_HOME=/home/$SERVICE_USER
ENV INSTALL_DIR=/opt/aws

RUN mkdir -p $SERVICE_USER_HOME $INSTALL_DIR
RUN useradd -d $SERVICE_USER_HOME $SERVICE_USER

WORKDIR $INSTALL_DIR
COPY environment.sh ./
COPY run.sh ./

RUN chown -R $SERVICE_USER.$SERVICE_USER $SERVICE_USER_HOME
RUN chown -R $SERVICE_USER.$SERVICE_USER $INSTALL_DIR

USER $SERVICE_USER

ENTRYPOINT ["./run.sh"]
