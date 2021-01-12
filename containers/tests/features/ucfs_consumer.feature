Feature: Consumes, decrypts, persists.
  The UCFS consumer consumes messages from the kafka queue, decrypts them and commits the offset. Valid messages are
  persisted, invalid messages are sent to the dead letter queue.

  Scenario Outline: Messages are transformed and sent to the appropriate target
    Given 50 records exist on <table>
    And a data key has been acquired
    And 200 mixed messages have been posted to <topic>
    Then <topic> offset will be committed at 200
    And 100 updated records will be on <table>
    And the records on <table> can be deciphered
    And 100 messages will be sent to dead.letter.queue starting from offset <dlq_offset>
    Examples:
      | topic             | dlq_offset | table     |
      | db.core.claimant  |          0 | claimant  |
      | db.core.statement |        100 | statement |


  Scenario Outline: Deletes are removed from the correct table
    Given 50 records exist on <table>
    And a data key has been acquired
    And 50 delete messages have been posted to <topic>
    Then <topic> offset will be committed at 50
    And 25 original records will be on <table>
    Examples:
      | topic             | table    |
      | db.core.contract  | contract |
