import base64
import binascii
import hashlib
import json
import re
import time

import boto3 as boto3
import mysql.connector
import parse
import requests
from Crypto import Random
from Crypto.Cipher import AES
from Crypto.Util import Counter
from assertpy import assert_that
from behave import given, then, step, register_type
from kafka import KafkaProducer, KafkaConsumer, TopicPartition


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
    with (database_connection()) as connection:
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

        db_object = topic_db_object(i, topic, state != "nonino")

        if state == "mixed" and i % 2 == 1:
            db_object.pop("_id", None)

        (initialisation_vector, encrypted_db_object) = encrypt(data_key, json.dumps(db_object))

        payload = {
            "traceId": "string",
            "unitOfWorkId": "string",
            "@type": "string",
            "message": {
                "@type": "MONGO_DELETE" if state == "delete" else "MONGO_INSERT",
                "_id": {id_field(topic): f"{i}"},
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


@step("{expected:Number} {record_type} records will be on {table}")
def step_impl(context, expected, record_type, table):
    with (database_connection()) as connection:
        count_cursor = connection.cursor()
        count_cursor.execute(f"SELECT count(*) FROM {table}")
        actual = count_cursor.fetchone()[0]

        assert expected == actual
        count_cursor.close()

        contents_cursor = connection.cursor()
        contents_cursor.execute(f"SELECT {overwritten_field(table)} FROM {table}")
        for data in contents_cursor:
            assert data[0] == ("phoney" if record_type == "original" else overwritten_value(table))


@step("the records on {table} can be deciphered")
def step_impl(context, table):
    kms_client = aws_client("kms")
    ssm_client = aws_client("ssm")
    parameter = ssm_client.get_parameter(Name='/ucfs/claimant-api/nino/salt')
    salt = parameter['Parameter']['Value']
    with (database_connection()) as connection:
        cursor = connection.cursor()
        cursor.execute(f"SELECT data FROM {table}")
        for data in cursor:
            obj = json.loads(data[0])
            if 'encryptedTakeHomePay' in obj:
                encryption_object = obj['encryptedTakeHomePay']
                encrypted_take_home_pay = encryption_object['takeHomePay']
                encrypted_key = encryption_object['cipherTextBlob']
                cipher = base64.b64decode(encrypted_key)
                decrypted_key_response = kms_client.decrypt(CiphertextBlob=cipher)
                plaintext_key = decrypted_key_response['Plaintext']
                iv = encrypted_take_home_pay[:16]
                encrypted = encrypted_take_home_pay[16:]
                aes = AES.new(plaintext_key, AES.MODE_GCM, nonce=base64.b64decode(iv))
                raw = base64.b64decode(encrypted)
                decrypted = aes.decrypt_and_verify(raw[:-16], raw[-16:])
                assert decrypted == b"1234.56"
            elif 'nino' in obj:
                sha = hashlib.sha512()
                sha.update("AA123456A".encode())
                sha.update(salt.encode())
                digest_b64 = base64.b64encode(sha.digest()).decode().replace('+', '-').replace('/', '_')
                assert digest_b64 == obj['nino']


@given("the expected metrics have been pushed and scraped")
def step_impl(context):
    response = requests.get("http://prometheus:9090/api/v1/targets/metadata").json()["data"]
    custom_metrics = []
    expected = ['logback_appender_created',
                'logback_appender_total',
                'uckc_decrypt_datakey',
                'uckc_decrypt_datakey_created',
                'uckc_delete',
                'uckc_delete_created',
                'uckc_deleted_records_created',
                'uckc_deleted_records_total',
                'uckc_dks_decrypt_failures_created',
                'uckc_dks_decrypt_failures_total',
                'uckc_dks_decrypt_retries_created',
                'uckc_dks_decrypt_retries_total',
                'uckc_dlq',
                'uckc_dlq_created',
                'uckc_encrypted_datakey',
                'uckc_encrypted_datakey_created',
                'uckc_failed_records_created',
                'uckc_failed_records_total',
                'uckc_inserted_records_created',
                'uckc_inserted_records_total',
                'uckc_kms_failures_created',
                'uckc_kms_failures_total',
                'uckc_kms_retries_created',
                'uckc_kms_retries_total',
                'uckc_salt',
                'uckc_salt_created',
                'uckc_salt_failures_created',
                'uckc_salt_failures_total',
                'uckc_salt_retries_created',
                'uckc_salt_retries_total',
                'uckc_secret_failures_created',
                'uckc_secret_failures_total',
                'uckc_secret_retries_created',
                'uckc_secret_retries_total',
                'uckc_secrets',
                'uckc_secrets_created',
                'uckc_topic_partition_lags',
                'uckc_updated_records_created',
                'uckc_updated_records_total',
                'uckc_upsert',
                'uckc_upsert_created']

    while not all(item in custom_metrics for item in expected):
        time.sleep(2)
        custom_metrics = [f['metric'] for f in
                          [m for m in response if not re.compile(r"^(go|process|push(gateway)?)_").match(m['metric'])]]


@then("metric query {query} should give the result {expected}")
def step_impl(context, query, expected):
    count = 0
    while count < 1:
        response = requests.get(f"http://prometheus:9090/api/v1/query?query={query}").json()
        results = response['data']['result']
        count = len(results)
        if count < 1:
            time.sleep(1)

    actual = results[0]['value'][1]
    assert_that(actual).is_equal_to(expected)


def database_connection():
    return mysql.connector.connect(host="rds", user="claimantapi", password="password",
                                   database="ucfs-claimant")


def extant_record(table: str, index: int) -> str:
    if table == "claimant":
        return json.dumps({"_id": {"citizenId": f"{index}"}, "nino": "phoney"})
    elif table == "contract":
        return json.dumps({"_id": {"contractId": f"{index}"}, "people": ["phoney"]})
    else:
        return json.dumps({"_id": {"statementId": f"{index}"}, "assessmentPeriod": {"contractId": "phoney"}})


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


def topic_db_object(i, topic, include_nino: bool):
    if topic == "db.core.claimant":
        return claimant_db_object(i, include_nino)
    return contract_db_object(i) if topic == "db.core.contract" else statement_db_object(i)


def claimant_db_object(record_number: int, include_nino: bool):
    return {
        "_id": {
            "citizenId": f"{record_number}"
        },
        "nino": "AA123456A" if include_nino else ""
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
        "takeHomePay": "1234.56",
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


def aws_client(service_name: str):
    return boto3.client(service_name=service_name,
                        endpoint_url="http://localstack:4566",
                        use_ssl=False,
                        region_name='eu-west-2',
                        aws_access_key_id="accessKeyId",
                        aws_secret_access_key="secretAccessKey")
