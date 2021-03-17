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
SSL_DIR="$(mktemp -d)"
if [ "${KAFKA_USE_SSL}" == "true" ]
then

    export SECURITY_KEY_PASSWORD="$(uuidgen)"

    export SECURITY_KEYSTORE="${SSL_DIR}/kafka.keystore"
    export SECURITY_KEYSTORE_PASSWORD="$(uuidgen)"

    export SECURITY_TRUSTSTORE="${SSL_DIR}/kafka.truststore"
    export SECURITY_TRUSTSTORE_PASSWORD="$(uuidgen)"

    if [ "${KAFKA_CERT_MODE}" = "CERTGEN" ]; then
        export SECURITY_KEYSTORE_ALIAS="${CERTGEN_PRIVATE_KEY_ALIAS}"

        echo "Generating cert for host ${HOSTNAME}"

        acm-pca-cert-generator \
            --log-level "${LOG_LEVEL}" \
            --subject-cn "${HOSTNAME}" \
            --keystore-path "${SECURITY_KEYSTORE}" \
            --keystore-password "${SECURITY_KEYSTORE_PASSWORD}" \
            --private-key-password "${SECURITY_KEY_PASSWORD}" \
            --truststore-path "${SECURITY_TRUSTSTORE}" \
            --truststore-password "${SECURITY_TRUSTSTORE_PASSWORD}"

        echo "Cert generation result is $? for ${HOSTNAME}"

    elif [ "${KAFKA_CERT_MODE}" = "RETRIEVE" ]; then
        export SECURITY_KEYSTORE_ALIAS="${RETRIEVER_PRIVATE_KEY_ALIAS}"

        export RETRIEVER_ACM_KEY_PASSPHRASE="$(uuidgen)"

        echo "Finding acm-cert-retriever"
        which acm-cert-retriever

        echo "Retrieving cert from ${RETRIEVER_ACM_CERT_ARN}"

        acm-cert-retriever \
            --log-level "${LOG_LEVEL}" \
            --acm-key-passphrase "${RETRIEVER_ACM_KEY_PASSPHRASE}" \
            --keystore-path "${SECURITY_KEYSTORE}" \
            --keystore-password "${SECURITY_KEYSTORE_PASSWORD}" \
            --private-key-password "${SECURITY_KEY_PASSWORD}" \
            --truststore-path "${SECURITY_TRUSTSTORE}" \
            --truststore-password "${SECURITY_TRUSTSTORE_PASSWORD}"

        echo "Cert retrieve result is $? for ${RETRIEVER_ACM_CERT_ARN}"

    else
        echo "KAFKA_CERT_MODE must be one of 'CERTGEN,RETRIEVE' but was ${KAFKA_CERT_MODE}"
        exit 1
    fi
else
    echo "Skipping Kafka cert generation for host ${HOSTNAME}"
fi

if [ "${RDS_USE_SSL}" == "true" ]; then
  if [ -f "${RDS_CA_CERT_PATH}" ]; then
    export RDS_TRUSTSTORE="${SSL_DIR}/rds.truststore"
    export RDS_TRUSTSTORE_PASSWORD="$(uuidgen)"

    keytool -v \
        -genkeypair \
        -keyalg RSA \
        -alias cid \
        -keystore "${RDS_TRUSTSTORE}" \
        -storepass "${RDS_TRUSTSTORE_PASSWORD}" \
        -validity 365 \
        -keysize 2048 \
        -keypass "${RDS_TRUSTSTORE_PASSWORD}" \
        -dname "CN=${HOSTNAME},OU=DataWorks,O=DWP,L=Leeds,ST=West Yorkshire,C=UK"

      keytool -importcert \
        -noprompt \
        -v \
        -trustcacerts \
        -file "${RDS_CA_CERT_PATH}" \
        -keystore "${RDS_TRUSTSTORE}" \
        -storepass "${RDS_TRUSTSTORE_PASSWORD}"
  else
    echo "CA certificate file ${RDS_CA_CERT_PATH} does not exist. Unset RDS_USE_SSL environment variable to disable SSL."
    exit 1
  fi
else
  echo "Not using SSL for RDS"
fi

exec "${@}"
