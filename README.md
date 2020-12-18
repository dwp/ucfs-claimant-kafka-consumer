# UCFS Claimant Kafka Consumer

Subscribes to the configured topics, decrypts them, logs them to the console
or writes them to a "success" topic derived from the source topic.

## Makefile

The Makefile wraps some of the docker-compose commands to give a more unified
basic set of operations. These can be checked by running `make help`

To bring up the app and run the integration tests run `make tests`.

### To push a container from local dev into management-dev

You can run the following, to reduce our development cycle time in AWS;

```
make push-local-to-ecr aws_mgmt_dev_account="12345678" aws_default_region="eu-west-X"
```

## High level view

### Orchestration

The application is composed of a source (the kafka consumer class), a processor
and a target. Batches of records are read from the source, put through the
processor and then sent to the target, this is done by the orchestrator

### The source records

Batches of records are read from the kafka broker and then split up such that
each sub-batch only contains records from a single same topic and partition.
Each of these sub-batches are then passed to the processor.

### The processor

The orchestrator deals with the top-level compound processor which delegates
the processing steps to a sequence of delegate processors, each of these
performs one task.

#### The delegate processors

The delegate processors process the output of the previous processor in the
chain.

Using a functional programming pattern a processor returns an object called an
`Either`, which either contains the result of a successful transformation
(a `Right` or the result of an unsuccessful transformation (a `Left`).

The following processor only processes the output of the previous one if the
output is a `Right`, `Left` values pass through unchanged. At the end of the
steps the results are split into `Lefts` and `Rights` - the `Rights` go to the
success target (MySql or whatever it will be) and the `Lefts` go to the dead
letter queue.

To enable the dead letter queue processing to take place the `Left` value must
always be the source record from kafka. And to allow each processor to return
the source record as its `Left`, the `Right` value must always be a pair of
values one of which is the source record and the other being the result of the
successful processing step.

#### Current processors

* `SourceRecordProcessor` - simply reads and returns the value of the kafka
  consumer record.
* `ValidationProcessor` - filters out messages that are not valid against the message schema.
* `JsonProcessor` - parse the source record value as a Json object.
* `ExtractionProcessor` - extracts the encryption metadata from the Json
  object (the encrypted key, the encrypting key id, and the initialisation
      vector generated when encrypting the payload).
* `DatakeyProcessor` - calls DKS to get the encrypted datakey decrypted.
* `DecryptionProcessor` - decrypts the `dbObject`
* `TransformationProcessor` - transforms the decrypted dbObject in line with
    the current batch process, the actual transformation is delegated to an inferior
  transformer of which there are 3, one for each source topic (as each has a unique transformation 
  applied)


### The targets

At present there are two, neither of which will make it into production. One
simply prints the processing output onto the console (or rather to the log), the
other posts successfully processed records to a kafka queue. This is to
facilitate integration testing of the processor.

Records that fail processing are sent to a dead letter queue and are considered
dealt with.

## Configuration

### Application configuration

The application can be configured with spring properties (specified on the
command line or in an application properties file) or with environment
variables or a mixture of the two.

| Property                        | Environment variable              | Purpose | Default | Needs override |
|---------------------------------|-----------------------------------|---------|---------|----------------|
| aws.cmkAlias                    | AWS_CMK_ALIAS                     | The alias of the master key in KMS |  | Yes |
| aws.saltParameterName           | AWS_SALT_PARAMETER_NAME           | The name of the parameter in the parameter store which houses the salt value | | Yes |
| cipher.dataKeySpec              | CIPHER_DATA_KEY_SPEC              | The specification of the datakeys that are generated | "AES_256" | |
| cipher.decryptingAlgorithm      | CIPHER_DECRYPTING_ALGORITHM       | The cipher algorithm used to decrypt the data keys that encrypted the message dbObjects | "AES" | |
| cipher.decryptingProvider       | CIPHER_DECRYPTING_PROVIDER        | The provider used to perform the data key decryption | "BC" | |
| cipher.decryptingTransformation | CIPHER_DECRYPTING_TRANSFORMATION  | The cipher transformation to use when decrypting the incoming data keys | "AES/CTR/NoPadding" | |
| cipher.encryptingAlgorithm      | CIPHER_ENCRYPTING_ALGORITHM       | The algorithm to use when encrypting the take home pay field | "AES" | |
| cipher.encryptingProvider       | CIPHER_ENCRYPTING_PROVIDER        | The crypto provider used to perform the encryption | "BC" | |
| cipher.encryptingTransformation | CIPHER_ENCRYPTING_TRANSFORMATION  | The algorithm to use when encrypting the take home pay field | "AES/GCM/NoPadding" | |
| cipher.initialisationVectorSize | CIPHER_INITIALISATION_VECTOR_SIZE | The size of the initialisation vector to use when encrypting the take home pay field | 12 | |
| cipher.maxKeyUsage              | CIPHER_MAX_KEY_USAGE              | How many times a datakey should be used before a new one is requested | 10 000 | |
| dks.url                         | DKS_URL                           | The data key service url | "https://dks:8443" | Yes |
| kafka.bootstrapServers          | KAFKA_BOOTSTRAP_SERVERS           | kafka server hosts and ports | "kafka:9092" | Yes |
| kafka.consumerGroup             | KAFKA_CONSUMER_GROUP              | The consumer group to join | "ucfs-claimant-consumers" | |
| kafka.dlqTopic                  | KAFKA_DLQ_TOPIC                   | The queue to which records that could not be processed are sent | "dead.letter.queue" | Yes|
| kafka.fetchMaxBytes             | KAFKA_FETCH_MAX_BYTES             | The max volume of data to get on each poll loop | 1024 * 1024 | |
| kafka.maxPartitionFetchBytes    | KAFKA_MAX_PARTITION_FETCH_BYTES   | The max volume of data in an assigned partition that can be fetched before the poll returns | 1024 * 1024 | |
| kafka.maxPollIntervalMs         | KAFKA_MAX_POLL_INTERVAL_MS        | How long to wait in between polls before consumer is dropped from the group | 5.minutes.inMilliseconds.toInt() | |
| kafka.maxPollRecords            | KAFKA_MAX_POLL_RECORDS            | How many records to collect on each poll before returning | 5 000 | |
| kafka.pollDurationSeconds       | KAFKA_POLL_DURATION_SECONDS       | How long to poll before returning | 10 | Possibly |
| kafka.topicRegex                | KAFKA_TOPIC_REGEX                 | Topics matching this regex will be subscribed to |  | Yes |
| kafka.useSsl                    | KAFKA_USE_SSL                     | Whether to enable a mutually authenticated connection | false | Yes |
| security.keyPassword            | SECURITY_KEY_PASSWORD             | Private key password | | Yes |
| security.keystore               | SECURITY_KEYSTORE                 | Path to keystore containing app certificates | "" | Yes |
| security.keystoreAlias          | SECURITY_KEYSTORE_ALIAS           | The private key alias | | Yes |
| security.keystorePassword       | SECURITY_KEYSTORE_PASSWORD        | The keystore password | | Yes |
| security.truststore             | SECURITY_TRUSTSTORE               | The path to the truststore | | Yes |
| security.truststorePassword     | SECURITY_TRUSTSTORE_PASSWORD      | The truststore password | | Yes |
| topic.claimant                  | TOPIC_CLAIMANT                    | The name of the claimant topic | "db.core.claimant" | |
| topic.contract                  | TOPIC_CONTRACT                    | The name of the contract topic | "db.core.contract" | |
| topic.statement                 | TOPIC_STATEMENT                   | The name of the statement topic | "db.core.statement" | |
| validation.schemaLocation       | VALIDATION_SCHEMA_LOCATION        | The location of the message json schema | "/message.schema.json" | |



### SSL Mutual Authentication (CERTGEN mode)

By default the SSL is enabled but has no defaults. These must either be
configured in full using `KAFKA_CERT_MODE=CERTGEN`, or disabled entirely using `KAFKA_USE_SSL=FALSE`.

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
configured in full using `KAFKA_CERT_MODE=RETRIEVE`, or disabled entirely with `KAFKA_USE_SSL=FALSE`.

For an authoritative full list of arguments see the tool help; Arguments not listed here are
defaulted in the `entrypoint.sh` script. These env vars will be overwritten by flags given in `entrypoint.sh`.

* **RETRIEVER_ACM_CERT_ARN**
    ARN in AWS ACM to use to fetch the required cert, cert chain, and key
* **RETRIEVER_ADD_DOWNLOADED_CHAIN**
    Whether or not to add the downloaded cert chain from the ARN to the trust store
    Allowed missing, `true`, `false`, `yes`, `no`, `1` or `0`
    If missing defaults to false
* **RETRIEVER_TRUSTSTORE_CERTS**
    Comma delimited list of S3 URIs pointing to certificates to be included in the trust store
* **RETRIEVER_TRUSTSTORE_ALIASES**
    Comma delimited list of aliases for the certificate
* **RETRIEVER_PRIVATE_KEY_ALIAS**
    Name of application
* **RETRIEVE_LOG_LEVEL**
    The log level of the certificate generator (`CRITICAL`, `ERROR`, `WARNING`, `INFO`, `DEBUG`)

### Releases

Container images are available on [DockerHub](https://hub.docker.com/repository/docker/dwpdigital/ucfs-claimant-kafka-consumer)
