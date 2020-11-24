import base64
import binascii
import json
import time

import requests
from Crypto import Random
from Crypto.Cipher import AES
from Crypto.Util import Counter
from behave import given, then
from kafka import KafkaProducer, KafkaConsumer, TopicPartition


@given("a data key has been acquired")
def step_impl(context):
    response = requests.get("https://dks:8443/datakey",
                            cert=("ucfs-claimant-kafka-consumer-tests-crt.pem",
                                  "ucfs-claimant-kafka-consumer-tests-key.pem"),
                            verify="dks-crt.pem")
    context.dks = response.json()


@given("{count} {state} messages have been posted to {topic}")
def step_impl(context, count, state, topic):
    data_key = context.dks["plaintextDataKey"]
    producer = KafkaProducer(bootstrap_servers=bootstrap_server())

    for i in range(int(count)):
        (initialisation_vector, encrypted_db_object) = encrypt(data_key, json.dumps({"data": f"data {i}"}))
        payload = {
            "traceId": "string",
            "unitOfWorkId": "string",
            "@type": "string",
            "message": {
                "@type": "string",
                "_id": {"id": i},
                "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                "db": "database",
                "collection": "collection",
                "dbObject": encrypted_db_object,
                "encryption": {
                    "encryptedEncryptionKey": context.dks['ciphertextDataKey'],
                    "keyEncryptionKeyId": "cloudhsm:1,2",
                    "initialisationVector": initialisation_vector
                }
            },
            "version": "v1",
            "timestamp": "2019-07-04T07:27:35.104+0000"
        }

        value = json.dumps(payload)
        payload.pop("timestamp", None)
        invalid_value = json.dumps(payload)

        if state == "valid":
            producer.send(topic=topic, value=value.encode(), key=f"{i}".encode())
        elif state == "mixed":
            mixed_value = value if i % 2 == 0 else invalid_value
            producer.send(topic=topic, value=mixed_value.encode(), key=f"{i}".encode())

        print(f"Sent to {topic}: {value}")
    producer.close()


@then("the {topic} offset will be committed at {offset}")
def step_impl(context, topic, offset):
    consumer = KafkaConsumer(bootstrap_servers=bootstrap_server(), group_id="ucfs-claimant-consumers",
                             enable_auto_commit=False)
    topic_partition = TopicPartition(topic=topic, partition=0)
    assert_committed_offset(consumer, topic_partition, int(offset))
    consumer.close()


@then("{count} {topic} messages have been decrypted")
def step_impl(context, count, topic):
    consumer = subscribed_consumer(f"{topic}.success")
    assert_encrypted_messages(consumer, int(count))
    consumer.close()


@then("{count} messages will be sent to {topic}")
def step_impl(context, count, topic):
    consumer = subscribed_consumer(topic)
    assert_messages_on_queue(consumer, int(count))


def assert_encrypted_messages(consumer, count, accumulation=0):
    records = consumer.poll(timeout_ms=20_000, max_records=10_000)
    if len(records) > 0:
        record_values = [x.value.decode() for x in [value for value in list(records.values())][0]]
        for index, actual in enumerate(record_values):
            actual = json.loads(actual)
            assert 'data' in actual
            actual = actual['data']
            expected = f"data {index + accumulation}"
            assert actual == expected
        accumulation += len(record_values)
        if accumulation >= count:
            assert accumulation == count
            return

    print("Not all records returned, recursing")
    time.sleep(1)
    return assert_encrypted_messages(consumer, count, accumulation)


def assert_messages_on_queue(consumer, count, accumulation=0):
    records = consumer.poll(timeout_ms=20_000, max_records=10_000)
    if len(records) > 0:
        record_values = [x.value.decode() for x in [value for value in list(records.values())][0]]
        accumulation += len(record_values)
        if accumulation >= count:
            assert accumulation == count
            return

    print("Not all records returned, recursing")
    time.sleep(1)
    return assert_messages_on_queue(consumer, count, accumulation)


def assert_committed_offset(consumer, topic_partition, offset):
    committed = consumer.committed(topic_partition)
    if committed is not None:
        print(f"Committed: {committed}")
        assert committed == offset
        return
    print("Did not find committed offset, trying again.")
    time.sleep(1)
    return assert_committed_offset(consumer, topic_partition, offset)


def encrypt(key, plaintext):
    initialisation_vector = Random.new().read(AES.block_size)
    iv_int = int(binascii.hexlify(initialisation_vector), 16)
    counter = Counter.new(AES.block_size * 8, initial_value=iv_int)
    aes = AES.new(base64.b64decode(key), AES.MODE_CTR, counter=counter)
    ciphertext = aes.encrypt(plaintext.encode("utf8"))
    return (base64.b64encode(initialisation_vector).decode("ASCII"),
            base64.b64encode(ciphertext).decode("ASCII"))


def subscribed_consumer(topic):
    consumer = KafkaConsumer(auto_offset_reset='earliest', bootstrap_servers=bootstrap_server(), group_id="integration-tests")
    print(f"Subscribing to {topic}")
    consumer.subscribe([topic, ])
    consumer.poll()
    consumer.seek_to_beginning()
    return consumer


def bootstrap_server():
    return "kafka:9092"
