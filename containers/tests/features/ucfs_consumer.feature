Feature: Consumes and decrypts
  The UCFS consumer consumes messages from the kafka queue, decrypts them and commits the offset. Invalid messages are
  sent to the dead letter queue.

  Scenario: Commits the offset
    Given a data key has been acquired
    And 200 valid messages have been posted to db.database.collection1
    Then the db.database.collection1 offset will be committed at 200

  Scenario: Messages are decrypted
    Given a data key has been acquired
    And 200 valid messages have been posted to db.database.collection2
    Then 200 db.database.collection2 messages have been decrypted

  Scenario: Messages are posted to the dead letter queue
    Given a data key has been acquired
    And 200 mixed messages have been posted to db.database.collection3
    Then 100 messages will be sent to dead.letter.queue
    And 100 messages will be sent to db.database.collection3.success
    And the db.database.collection3 offset will be committed at 200
