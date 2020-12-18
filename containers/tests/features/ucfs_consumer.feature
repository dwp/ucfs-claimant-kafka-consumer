Feature: Consumes and decrypts
  The UCFS consumer consumes messages from the kafka queue, decrypts them and commits the offset. Invalid messages are
  sent to the dead letter queue.

  Scenario Outline: Messages are transformed and sent to the appropriate target
    Given a data key has been acquired
    And 200 mixed messages have been posted to <topic>
    Then 100 messages will be sent to dead.letter.queue starting from offset <dlq_offset>
    And 100 <topic> messages have been decrypted
    And <topic> offset will be committed at 200
    Examples:
      | topic             | dlq_offset |
      | db.core.claimant  |          0 |
      | db.core.contract  |        100 |
      | db.core.statement |        200 |
