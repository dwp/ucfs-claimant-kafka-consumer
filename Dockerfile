FROM gradle:latest as build

RUN mkdir -p /build
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY src/ ./src
RUN gradle build
RUN cp build/libs/ucfs-claimant-kafka-consumer-*.jar /build/ucfs-claimant-kafka-consumer.jar
RUN ls -la /build/

FROM openjdk:14-alpine

ARG http_proxy_full=""

# Set environment variables for apk
ENV http_proxy=${http_proxy_full}
ENV https_proxy=${http_proxy_full}
ENV HTTP_PROXY=${http_proxy_full}
ENV HTTPS_PROXY=${http_proxy_full}

RUN echo "ENV http: ${http_proxy}" \
    && echo "ENV https: ${https_proxy}" \
    && echo "ENV HTTP: ${HTTP_PROXY}" \
    && echo "ENV HTTPS: ${HTTPS_PROXY}" \
    && echo "ARG full: ${http_proxy_full}"

ENV acm_cert_helper_version 0.32.0
RUN echo "===> Installing Dependencies ..." \
    && echo "===> Updating base packages ..." \
    && apk update \
    && apk upgrade \
    && echo "==Update done==" \
    && apk add --no-cache util-linux \
    && echo "===> Installing acm_pca_cert_generator ..." \
    && apk add --no-cache g++ python3 python3-dev libffi-dev openssl-dev gcc py3-pip \
    && pip3 install --upgrade pip setuptools \
    && pip3 install https://github.com/dwp/acm-pca-cert-generator/releases/download/${acm_cert_helper_version}/acm_cert_helper-${acm_cert_helper_version}.tar.gz \
    && echo "==Dependencies done=="

# Set user to run the process as in the docker contianer
ENV USER_NAME=uckc
ENV GROUP_NAME=uckc

RUN mkdir /ucfs-claimant-kafka-consumer
WORKDIR /ucfs-claimant-kafka-consumer

COPY --from=build /build/ucfs-claimant-kafka-consumer.jar .
RUN addgroup $GROUP_NAME
RUN adduser --system --ingroup $GROUP_NAME $USER_NAME
COPY ./entrypoint.sh /
COPY ./ucfs-claimant-kafka-consumer-keystore.jks ./development-keystore.jks
COPY ./ucfs-claimant-kafka-consumer-truststore.jks ./development-truststore.jks
RUN chown -R $USER_NAME.$GROUP_NAME /ucfs-claimant-kafka-consumer
RUN chown -R $USER_NAME.$GROUP_NAME -R /var
RUN chmod a+rw /var/log
USER $USER_NAME
RUN pwd && ls
ENTRYPOINT ["/entrypoint.sh"]
CMD ["./bin/ucfs-claimant-kafka-consumer"]
