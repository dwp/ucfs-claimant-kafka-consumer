Feature: Does what it is supposed to
  The UCFS consumer consumes

  Scenario: Performs its task
    Given messages are posted to a subscribed queue
    Then the messages are consumed
