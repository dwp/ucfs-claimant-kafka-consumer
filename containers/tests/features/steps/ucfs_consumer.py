from behave import *
from kafka import KafkaProducer, KafkaConsumer, TopicPartition
import time


@given("messages are posted to a subscribed queue")
def step_impl(context):
    producer = KafkaProducer(bootstrap_servers="kafka:9092")
    for i in range(200):
        producer.send(topic="db.database.collection", value=f"message-{i}".encode(), key=f"{i}".encode())
        print(f"Sent {i}.")
    producer.close()


@then("the messages are consumed")
def step_impl(context):
    consumer = KafkaConsumer(bootstrap_servers="kafka:9092", group_id="ucfs-claimant-consumers",
                             enable_auto_commit=False)
    topic_partition = TopicPartition(topic="db.database.collection", partition=0)
    assert_committed_offset(consumer, topic_partition)
    consumer.close()


def assert_committed_offset(consumer, topic_partition):
    committed = consumer.committed(topic_partition)
    if committed is not None:
        assert committed == 200
        return
    print("Did not find committed offset, trying again.")
    time.sleep(1)
    return assert_committed_offset(consumer, topic_partition)
