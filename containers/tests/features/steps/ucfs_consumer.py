import base64
import base64
import binascii
import json
import requests
import time
import parse

from Crypto import Random
from Crypto.Cipher import AES
from Crypto.Util import Counter
from behave import given, then, step, register_type
from kafka import KafkaProducer, KafkaConsumer, TopicPartition
import mysql.connector


@parse.with_pattern(r"\d+")
def parse_number(text):
    return int(text)


register_type(Number=parse_number)


@given("a data key has been acquired")
def step_impl(context):
    response = requests.get("https://dks:8443/datakey",
                            cert=("ucfs-claimant-kafka-consumer-tests-crt.pem",
                                  "ucfs-claimant-kafka-consumer-tests-key.pem"),
                            verify="dks-crt.pem")
    context.dks = response.json()


@step("{count:Number} records exist on {table}")
def step_impl(context, count, table):
    with (mysql.connector.connect(host="rds", user="claimantapi", password="password",
                                  database="ucfs-claimant")) as connection:
        data = [(extant_record(table, index),) for index in range(0, count * 2, 2)]
        statement = f"INSERT INTO {table} (data) VALUES (%s) ON DUPLICATE KEY UPDATE data = VALUES (data)"
        cursor = connection.cursor()
        cursor.executemany(statement, data)
        cursor.close()
        connection.commit()


@given("{count:Number} {state} messages have been posted to {topic}")
def step_impl(context, count, state, topic):
    data_key = context.dks["plaintextDataKey"]
    producer = KafkaProducer(bootstrap_servers=bootstrap_server())

    for i in range(count):

        db_object = topic_db_object(i, topic)

        if state == "mixed" and i % 2 == 1:
            db_object.pop("_id", None)

        (initialisation_vector, encrypted_db_object) = encrypt(data_key, json.dumps(db_object))

        payload = {
            "traceId": "string",
            "unitOfWorkId": "string",
            "@type": "string",
            "message": {
                "@type": "MONGO_INSERT",
                "_id": {id_field(topic): i},
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
        producer.send(topic=topic, value=value.encode(), key=f"{i}".encode())

    producer.close()


@then("{topic} offset will be committed at {offset:Number}")
def step_impl(context, topic, offset):
    consumer = KafkaConsumer(bootstrap_servers=bootstrap_server(), group_id="ucfs-claimant-consumers",
                             enable_auto_commit=False)
    topic_partition = TopicPartition(topic=topic, partition=0)
    assert_committed_offset(consumer, topic_partition, offset)
    consumer.close()


@then("{count:Number} messages will be sent to {topic} starting from offset {offset:Number}")
def step_impl(context, count, topic, offset):
    consumer = subscribed_consumer(topic, offset)
    assert_messages_on_queue(consumer, count)


@step("{expected:Number} decrypted records will be on {table}")
def step_impl(context, expected, table):
    with (mysql.connector.connect(host="rds", user="claimantapi", password="password",
                                  database="ucfs-claimant")) as connection:
        count_cursor = connection.cursor()
        count_cursor.execute(f"SELECT count(*) FROM {table}")
        actual = count_cursor.fetchone()[0]
        assert expected == actual
        count_cursor.close()

        contents_cursor = connection.cursor()
        contents_cursor.execute(f"SELECT {overwritten_field(table)} FROM {table}")
        for data in contents_cursor:
            assert data[0] == overwritten_value(table)


def extant_record(table: str, index: int) -> str:
    if table == "claimant":
        return json.dumps({"_id": {"citizenId": index}, "nino": "phoney"})
    elif table == "contract":
        return json.dumps({"_id": {"contractId": index}, "people": ["phoney"]})
    else:
        return json.dumps({"_id": {"statementId": index}, "assessmentPeriod": {"contractId": "phoney"}})


def overwritten_field(table: str) -> str:
    if table == "claimant":
        return "nino"
    elif table == "contract":
        return "citizen_a"
    else:
        return "contract_id"


def overwritten_value(table: str) -> str:
    if table == "claimant":
        return "xFJrf8lbU4G-LB3gx6uF0z531bs0DIVYQ5o5514Y5OrrlxEriQ_W-jEum6bgveIL9gFwwRswDXz8lgqmTQCgFg=="
    elif table == "contract":
        return "abc"
    else:
        return "6e2b4428-2da5-4f77-9904-e0c2fc850c4f"


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


def subscribed_consumer(topic: str, offset: int = 0):
    consumer = KafkaConsumer(auto_offset_reset='earliest', bootstrap_servers=bootstrap_server(),
                             group_id="integration-tests")
    print(f"Subscribing to {topic}")
    consumer.subscribe([topic, ])
    print(f"Initial poll")
    consumer.poll()
    print(f"Seeking to {offset} for {topic}")
    consumer.seek(TopicPartition(topic=topic, partition=0), offset)
    print(f"Sought to {offset} for {topic}")
    return consumer


def bootstrap_server():
    return "kafka:9092"


def topic_db_object(i, topic):
    if topic == "db.core.claimant":
        return claimant_db_object(i)
    return contract_db_object(i) if topic == "db.core.contract" else statement_db_object(i)


def claimant_db_object(record_number: int):
    return {
        "_id": {
            "citizenId": f"{record_number}"
        },
        "nino": "AA123456A"
    }


def contract_db_object(record_number: int):
    return {
        "_id": {
            "contractId": f"{record_number}"
        },
        "assessmentPeriods": [],
        "people": ["abc", "def"],
        "declaredDate": 20200123,
        "startDate": 20200224,
        "entitlementDate": 20200325,
        "closedDate": 20200426,
        "annualVerificationEligibilityDate": None,
        "annualVerificationCompletionDate": None,
        "paymentDayOfMonth": 7,
        "flags": [],
        "claimClosureReason": "FraudIntervention",
        "claimSuspension": {"suspensionDate": None},
        "_version": 12,
        "createdDateTime": "2020-12-12T10:37:45.000",
        "coupleContract": False,
        "claimantsExemptFromWaitingDays": [],
        "contractTypes": None,
        "_entityVersion": 2,
        "_lastModifiedDateTime": "2020-03-04T06:37:45.000",
        "stillSingle": True,
        "contractType": "INITIAL"
    }


def statement_db_object(record_number: int):
    return {
        "_id": {
            "statementId": f"{record_number}"
        },
        "_version": 1,
        "people": ["person1", "person2"],
        "assessmentPeriod": {
            "endDate": 20280131,
            "startDate": 20280101,
            "contractId": "6e2b4428-2da5-4f77-9904-e0c2fc850c4f",
            "paymentDate": 20280123,
            "processDate": None,
            "createdDateTime": "2027-12-01T00:00:00.000000Z",
            "assessmentPeriodId": "5395ec9d-d054-4bf4-b027-cc11d1ec473d"
        },
        "standardAllowanceElement": "317.82",
        "housingElement": "0.00",
        "housingElementRent": "0.00",
        "housingElementServiceCharges": "0.00",
        "childElement": "0.00",
        "numberEligibleChildren": 0,
        "disabledChildElement": "0.00",
        "numberEligibleDisabledChildren": 0,
        "childcareElement": "0.00",
        "numberEligibleChildrenInChildCare": 0,
        "carerElement": "0.00",
        "numberPeopleCaredFor": 0,
        "takeHomePay": "$takeHomePay",
        "takeHomeBreakdown": {
            "rte": "0.00",
            "selfReported": "0.00",
            "selfEmployed": "0.00",
            "selfEmployedWithMif": "0.00"
        },
        "unaffectedPayElement": "0.00",
        "totalReducedForHomePay": "0.00",
        "otherIncomeAdjustment": "0.00",
        "capitalAdjustment": "0.00",
        "totalAdjustments": "0.00",
        "fraudPenalties": "0.00",
        "sanctions": "317.82",
        "advances": "0.00",
        "deductions": "0.00",
        "totalPayment": "0.00",
        "createdDateTime": "2020-12-11T10:12:34.000",
        "earningsSource": None,
        "otherBenefitAwards": [],
        "overlappingBenefits": [],
        "totalOtherBenefitsAdjustment": "0",
        "capApplied": None,
        "type": "CALCULATED",
        "preAdjustmentTotal": "317.82",
        "_entityVersion": 4,
        "_lastModifiedDateTime": "2020-12-11T10:12:34.000",
        "workCapabilityElement": None,
        "benefitCapThreshold": None,
        "benefitCapAdjustment": None,
        "gracePeriodEndDate": None,
        "landlordPayment": "0"
    }


def id_field(topic: str) -> str:
    id_fields = {"db.core.claimant": "citizenId", "db.core.contract": "contractId", "db.core.statement": "statementId"}
    return id_fields[topic]
