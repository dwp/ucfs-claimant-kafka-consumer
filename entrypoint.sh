#!/bin/sh

set -e

# If a proxy is requested, set it up

if [ "${INTERNET_PROXY}" ]; then
    export http_proxy="http://${INTERNET_PROXY}:3128"
    export HTTP_PROXY="$http_proxy"
    export https_proxy="$http_proxy"
    export HTTPS_PROXY="$https_proxy"
    export no_proxy="${NON_PROXIED_ENDPOINTS}"
    export NO_PROXY="$no_proxy"
    echo "Using proxy ${INTERNET_PROXY}"
fi

# Generate a cert for Kafka mutual auth
HOSTNAME=$(hostname)
if [ "${KAFKA_INSECURE}" != "true" ]
then

    SSL_DIR="$(mktemp -d)"
    export KAFKA_PRIVATE_KEY_PASSWORD="$(uuidgen)"

    export KAFKA_KEYSTORE_PATH="${SSL_DIR}/kafka.keystore"
    export KAFKA_KEYSTORE_PASSWORD="$(uuidgen)"

    export KAFKA_TRUSTSTORE_PATH="${SSL_DIR}/kafka.truststore"
    export KAFKA_TRUSTSTORE_PASSWORD="$(uuidgen)"

    if [ "${KAFKA_CERT_MODE}" = "CERTGEN" ]; then

        echo "Generating cert for host ${HOSTNAME}"

        acm-pca-cert-generator \
            --subject-cn "${HOSTNAME}" \
            --keystore-path "${KAFKA_KEYSTORE_PATH}" \
            --keystore-password "${KAFKA_KEYSTORE_PASSWORD}" \
            --private-key-password "${KAFKA_PRIVATE_KEY_PASSWORD}" \
            --truststore-path "${KAFKA_TRUSTSTORE_PATH}" \
            --truststore-password "${KAFKA_TRUSTSTORE_PASSWORD}" \
            --truststore-aliases "${KAFKA_CONSUMER_TRUSTSTORE_ALIASES}" \
            --truststore-certs "${KAFKA_CONSUMER_TRUSTSTORE_CERTS}" >> /var/log/acm-cert-retriever.log 2>&1 \
            --region "${AWS_REGION}"

        echo "Cert generation result is $? for ${HOSTNAME}"

    elif [ "${KAFKA_CERT_MODE}" = "RETRIEVE" ]; then

        export RETRIEVER_ACM_KEY_PASSPHRASE="$(uuidgen)"

        echo "Finding acm-cert-retriever"
        which acm-cert-retriever

        echo "Retrieving cert from ${RETRIEVER_ACM_CERT_ARN}"

        acm-cert-retriever \
            --region "${AWS_REGION}" \
            --acm-key-passphrase "${RETRIEVER_ACM_KEY_PASSPHRASE}" \
            --keystore-path "${KAFKA_KEYSTORE_PATH}" \
            --keystore-password "${KAFKA_KEYSTORE_PASSWORD}" \
            --private-key-password "${KAFKA_PRIVATE_KEY_PASSWORD}" \
            --truststore-path "${KAFKA_TRUSTSTORE_PATH}" \
            --truststore-password "${KAFKA_TRUSTSTORE_PASSWORD}" \
            --truststore-aliases "${KAFKA_CONSUMER_TRUSTSTORE_ALIASES}" \
            --truststore-certs "${KAFKA_CONSUMER_TRUSTSTORE_CERTS}" >> /var/log/acm-cert-retriever.log 2>&1

        echo "Cert retrieve result is $? for ${RETRIEVER_ACM_CERT_ARN}"

        export SECURITY_KEY_PASSWORD="${KAFKA_PRIVATE_KEY_PASSWORD}"
        export SECURITY_KEYSTORE="${KAFKA_KEYSTORE_PATH}"
        export SECURITY_KEYSTORE_PASSWORD="${KAFKA_KEYSTORE_PASSWORD}"
        export SECURITY_TRUSTSTORE="${KAFKA_TRUSTSTORE_PATH}"
        export SECURITY_TRUSTSTORE_PASSWORD="${KAFKA_TRUSTSTORE_PASSWORD}"

    else
        echo "KAFKA_CERT_MODE must be one of 'CERTGEN,RETRIEVE' but was ${KAFKA_CERT_MODE}"
        exit 1
    fi
else
    echo "Skipping cert generation for host ${HOSTNAME}"
fi

exec "${@}"
