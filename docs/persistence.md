# Persistence

## Requirements

* Transformed messages are inserted into the relevant database table
* Existing records in the database are updated when updates are received through Kafka
* Deletes are ignored

## Implementation

Kafka messages have a `@type` [see example here](https://github.ucds.io/dip/aws-ingestion/blob/master/docs/interfaces.md#ucfs-business-data-event-interface). 

For each type do the following:

### MONGO_INSERT or MONGO_UPDATE

Insert/update the record , matching on the citizen_id, contract_id or statement_id
using something like (note the only field you update is data, other fields are virtual

```sql
    INSERT INTO claimant (data) VALUES (data_json_object)
    ON DUPLICATE KEY UPDATE data=data_json_object WHERE citizen_id = the_citizen_id
```

see: https://dev.mysql.com/doc/refman/5.7/en/insert-on-duplicate.html for more details

### MONGO_DELETE

Do nothing (DW-5169)

Extend local integration test to cover inserts and updates
*  include insert-update, update-insert, just insert, and just update - all should be happy cases



### Docs

Update readme docs of application to state what it does

### Testing
In dev post messages to Kafka producer.

* new messages should be inserted
* messages with same id should be updated in DB
* query DB using cloud9 or Lambda to confirm
