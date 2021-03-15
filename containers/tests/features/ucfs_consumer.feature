Feature: Consumes, decrypts, persists.
  The UCFS consumer consumes messages from the kafka queue, decrypts them and commits the offset. Valid messages are
  persisted, invalid messages are sent to the dead letter queue.

  Scenario Outline: Messages are transformed and sent to the appropriate target
    Given 50 records exist on <table>
    And a data key has been acquired
    And 200 mixed messages have been posted to <topic>
    And 100 nonino messages have been posted to db.core.claimant
    Then <topic> offset will be committed at <topic_offset>
    And 100 updated records will be on <table>
    And the records on <table> can be deciphered
    And 100 messages will be sent to dead.letter.queue starting from offset <dlq_offset>
    Examples:
      | topic             | dlq_offset | table     | topic_offset |
      | db.core.claimant  |          0 | claimant  |         300  |
      | db.core.statement |        100 | statement |         200  |


  Scenario Outline: Deletes are removed from the correct table
    Given 50 records exist on <table>
    And a data key has been acquired
    And 50 delete messages have been posted to <topic>
    Then <topic> offset will be committed at 50
    And 25 original records will be on <table>
    Examples:
      | topic             | table    |
      | db.core.contract  | contract |

  Scenario Outline: Counter metrics have the correct values
    Given the expected metrics have been pushed and scraped
    Then metric query <query> should give the result <expected>
    Examples:
      | query                                                  | expected |
      | uckc_deleted_records_total{topic="db.core.contract"}   |       50 |
      | uckc_deleted_records_total{topic="db.core.claimant"}   |        0 |
      | uckc_deleted_records_total{topic="db.core.statement"}  |        0 |
      | uckc_updated_records_total{topic="db.core.contract"}   |        0 |
      | uckc_updated_records_total{topic="db.core.claimant"}   |       50 |
      | uckc_updated_records_total{topic="db.core.statement"}  |       50 |
      | uckc_failed_records_total{topic="db.core.claimant"}    |      100 |
      | uckc_failed_records_total{topic="db.core.statement"}   |      100 |
      | uckc_inserted_records_total{topic="db.core.claimant"}  |       50 |
      | sum(uckc_topic_partition_lags)                         |        0 |
