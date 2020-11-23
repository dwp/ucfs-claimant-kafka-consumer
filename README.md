# UCFS Claimant Kafka Consumer

Subscribes to the configured topics, logs the records on the console, discards
the records.

## Makefile

The Makefile wraps some of the docker-compose commands to give a more unified
basic set of operations. These can be checked by running `make help`

To bring up the app and run the integration tests run `make tests`.

## Configuration

### Application configuration

The application can be configured with spring properties (specified on the
command line or in an application properties file) or with environment
variables or a mixture of the two.

| Spring property              | Environment variable            | Purpose |
|------------------------------|---------------------------------|---------|
| kafka.bootstrapServers       | KAFKA_BOOTSTRAP_SERVERS         | kafka server hosts and ports |
| kafka.consumerGroup          | KAFKA_CONSUMER_GROUP            | The consumer group to join |
| kafka.fetchMaxBytes          | KAFKA_FETCH_MAX_BYTES           | The max volume of data to get on each poll loop |
| kafka.keyPassword            | KAFKA_KEY_PASSWORD              | Private key password |
| kafka.keystore               | KAFKA_KEYSTORE                  | Path to keystore containing app certificates |
| kafka.keystorePassword       | KAFKA_KEYSTORE_PASSWORD         | Keystore password |
| kafka.maxPartitionFetchBytes | KAFKA_MAX_PARTITION_FETCH_BYTES | The max volume of data in an assigned partition that can be fetched before the poll returns |
| kafka.maxPollIntervalMs      | KAFKA_MAX_POLL_INTERVAL_MS      | How long to wait inbetween polls before consumer is dropped from the group |
| kafka.maxPollRecords         | KAFKA_MAX_POLL_RECORDS          | How many records to collect on each poll before returning |
| kafka.pollDurationSeconds    | KAFKA_POLL_DURATION_SECONDS     | How long to poll before returning |
| kafka.topicRegex             | KAFKA_TOPIC_REGEX               | Topics matching this regex will be subscribed to |
| kafka.truststore             | KAFKA_TRUSTSTORE                | Path to keystore containing trusted certificates |
| kafka.truststorePassword     | KAFKA_TRUSTSTORE_PASSWORD       | Truststore password |
| kafka.useSsl                 | KAFKA_USE_SSL                   | Whether to enable a mutually authenticated connection |

### SSL Mutual Authentication (CERTGEN mode)

By default the SSL is enabled but has no defaults. These must either be
configured in full or disabled entirely via `K2HB_KAFKA_INSECURE=FALSE`
and `K2HB_KAFKA_CERT_MODE=CERTGEN`.

For an authoritative full list of arguments see the tool help; Arguments not listed here are
defaulted in the `entrypoint.sh` script.

* **CERTGEN_CA_ARN**
    The AWS CA ARN to use to generate the cert
* **CERTGEN_KEY_TYPE**
    The type of private key (`RSA` or `DSA`)
* **CERTGEN_KEY_LENGTH**
    The key length in bits (`1024`, `2048` or `4096`)
* **CERTGEN_KEY_DIGEST**
    The key digest algorithm (`sha256`, `sha384`, `sha512`)
* **CERTGEN_SUBJECT_C**
    The subject country
* **CERTGEN_SUBJECT_ST**
    The subject state/province/county
* **CERTGEN_SUBJECT_L**
    The subject locality
* **CERTGEN_SUBJECT_O**
    The subject organisation
* **CERTGEN_SUBJECT_OU**
    The subject organisational unit
* **CERTGEN_SUBJECT_EMAILADDRESS**
    The subject email address
* **CERTGEN_SIGNING_ALGORITHM**
    The certificate signing algorithm used by ACM PCA
    (`SHA256WITHECDSA`, `SHA384WITHECDSA`, `SHA512WITHECDSA`, `SHA256WITHRSA`, `SHA384WITHRSA`, `SHA512WITHRSA`)
* **CERTGEN_VALIDITY_PERIOD**
    The certificate validity period in Go style duration (e.g. `1y2m6d`)
* **CERTGEN_PRIVATE_KEY_ALIAS**
    Alias for the private key
* **CERTGEN_TRUSTSTORE_CERTS**
    Comma delimited list of S3 URIs pointing to certificates to be included in the trust store
* **CERTGEN_TRUSTSTORE_ALIASES**
    Comma delimited list of aliases for the certificate
* **CERTGEN_LOG_LEVEL**
    The log level of the certificate generator (`CRITICAL`, `ERROR`, `WARNING`, `INFO`, `DEBUG`)


### SSL Mutual Authentication (RETRIEVE mode)

By default the SSL is enabled but has no defaults. These must either be
configured in full or disabled entirely via `KAFKA_INSECURE=FALSE`
and `KAFKA_CERT_MODE=RETRIEVE`.

For an authoritative full list of arguments see the tool help; Arguments not listed here are
defaulted in the `entrypoint.sh` script. These env vars will be overwritten by flags given in `entrypoint.sh`.

* **RETRIEVER_ACM_CERT_ARN**
    ARN in AWS ACM to use to fetch the required cert, cert chain, and key
* **RETRIEVER_ADD_DOWNLOADED_CHAIN**
    Whether or not to add the downloaded cert chain from the ARN to the trust store
    Allowed missing, `true`, `false`, `yes`, `no`, `1` or `0`
    If missing defaults to false
* **RETRIEVE_TRUSTSTORE_CERTS**
    Comma delimited list of S3 URIs pointing to certificates to be included in the trust store
* **RETRIEVE_TRUSTSTORE_ALIASES**
    Comma delimited list of aliases for the certificate
* **RETRIEVER_PRIVATE_KEY_ALIAS**
    Name of application
* **RETRIEVE_LOG_LEVEL**
    The log level of the certificate generator (`CRITICAL`, `ERROR`, `WARNING`, `INFO`, `DEBUG`)

### Releases

Container images are available on [DockerHub](https://hub.docker.com/repository/docker/dwpdigital/ucfs-claimant-kafka-consumer)
