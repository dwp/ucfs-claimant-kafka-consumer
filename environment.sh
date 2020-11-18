#!/usr/bin/env bash

main() {
    make_keystore dks-keystore.jks
    extract_public_certificate dks-keystore.jks dks.crt
    make_truststore dks.crt

    make_keystore ucfs-claimant-kafka-consumer-keystore.jks
    extract_public_certificate ucfs-claimant-kafka-consumer-keystore.jks
    make_truststore ucfs-claimant-kafka-consumer.crt

    make_keystore ucfs-claimant-kafka-consumer-tests-keystore.jks
    extract_public_certificate ucfs-claimant-kafka-consumer-tests-keystore.jks

    import_into_truststore dks-truststore.jks ucfs-claimant-kafka-consumer.crt
    import_into_truststore dks-truststore.jks \
                           ucfs-claimant-kafka-consumer-tests.crt

    import_into_truststore ucfs-claimant-kafka-consumer-truststore.jks dks.crt

    extract_pems ./ucfs-claimant-kafka-consumer-tests-keystore.jks
    extract_pems ./dks-keystore.jks

    cp -v dks-crt.pem ucfs-claimant-kafka-consumer-tests-key.pem \
       ucfs-claimant-kafka-consumer-tests-crt.pem ./containers/tests

    mv -v dks-truststore.jks containers/dks
    mv -v dks-keystore.jks containers/dks
}

make_keystore() {
    local keystore=${1:?Usage: $FUNCNAME keystore [common-name]}
    local common_name=${2:-${keystore%-keystore.jks}}

    [[ -f $keystore ]] && rm -v $keystore

    keytool -v \
            -genkeypair \
            -keyalg RSA \
            -alias cid \
            -keystore $keystore \
            -storepass $(password) \
            -validity 365 \
            -keysize 2048 \
            -keypass $(password) \
            -dname "CN=$common_name,OU=DataWorks,O=DWP,L=Leeds,ST=West Yorkshire,C=UK"
}

make_truststore() {
    local certificate=${1:?Usage: $FUNCNAME certificate [truststore]}
    local truststore=${2:-${certificate%.crt}-truststore.jks}
    [[ -f $truststore ]] && rm -v $truststore
    import_into_truststore $truststore $certificate self
}

extract_public_certificate() {
    local keystore=${1:?Usage: $FUNCNAME keystore [certificate]}
    local certificate=${2:-${keystore%-keystore.jks}.crt}

    [[ -f $certificate ]] && rm -v $certificate

    keytool -v \
            -exportcert \
            -keystore $keystore \
            -storepass $(password) \
            -alias cid \
            -file $certificate
}


import_into_truststore() {
    local truststore=${1:?Usage: $FUNCNAME truststore certificate}
    local certificate=${2:?Usage: $FUNCNAME truststore certificate}
    local alias=${3:-${certificate%.crt}}

    keytool -importcert \
            -noprompt \
            -v \
            -trustcacerts \
            -alias $alias \
            -file $certificate \
            -keystore $truststore \
            -storepass $(password)
}

extract_pems() {
    local keystore=${1:-keystore.jks}
    local key=${2:-${keystore%-keystore.jks}-key.pem}
    local certificate=${3:-${keystore%-keystore.jks}-crt.pem}

    local intermediate_store=${keystore/jks/p12}

    local filename=$(basename $keystore)
    local alias=cid

    [[ -f $intermediate_store ]] && rm -v $intermediate_store
    [[ -f $key ]] && rm -v $key

    if keytool -importkeystore \
               -srckeystore $keystore \
               -srcstorepass $(password) \
               -srckeypass $(password) \
               -srcalias $alias \
               -destalias $alias \
               -destkeystore $intermediate_store \
               -deststoretype PKCS12 \
               -deststorepass $(password) \
               -destkeypass $(password); then
        local pwd=$(password)
        export pwd

        openssl pkcs12 \
                -in $intermediate_store \
                -nodes \
                -nocerts \
                -password env:pwd \
                -out $key

        openssl pkcs12 \
                -in $intermediate_store \
                -nokeys \
                -out $certificate \
                -password env:pwd

        unset pwd
    else
        echo Failed to generate intermediate keystore $intermediate_store >&2
    fi
}

password() {
    echo changeit
}
