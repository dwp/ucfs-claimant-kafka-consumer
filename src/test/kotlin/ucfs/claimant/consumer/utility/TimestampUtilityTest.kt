package ucfs.claimant.consumer.utility

import com.google.gson.Gson
import com.google.gson.JsonObject
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.assertions.json.shouldMatchJson
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class TimestampUtilityTest: StringSpec() {
    init {
        "Left when no action" {
            forAll(*sourceFields()) { sourceField ->
                val x = jsonObject("""{ 
                        "message": { "$sourceField": "2020-01-02" },
                        "timestamp": "2020-05-06"
                    }""".trimIndent())
                TimestampUtility.timestamp(x).shouldBeLeft {
                    it.shouldBeInstanceOf<Pair<JsonObject, String>>()
                    val (jsonObject, field) = it
                    gson.toJson(jsonObject) shouldMatchJson """{ "$sourceField": "2020-01-02" }"""
                    field shouldBe "@type"
                }
            }
        }

        "Left when unknown action" {
            forAll(*sourceFields()) { sourceField ->
                val x = jsonObject("""{ 
                    "message": { 
                        "$sourceField": "2020-01-02",
                         "@type": "MONGO_JERRY"
                    },
                    "timestamp": "2020-05-06"
                }""".trimIndent())
                TimestampUtility.timestamp(x).shouldBeLeft {
                    it.shouldBeInstanceOf<Exception>()
                }
            }
        }

        "Extracts _lastModifiedDateTime when insert or update and field present" {
            forAll(*actions()) { action ->
                val x = jsonObject("""{
                    "message": {
                        "$LAST_MODIFIED_TIMESTAMP_FIELD": "2020-01-02",
                        "@type": "$action"
                    }
                }""".trimIndent())
                TimestampUtility.timestamp(x).shouldBeRight { (date, source) ->
                    date shouldBe "2020-01-02"
                    source shouldBe "_lastModifiedDateTime"
                }
            }
        }

        "Epoch when insert or update and no fields present" {
            forAll(*actions()) { action ->
                val x = jsonObject("""{
                    "message": {
                        "@type": "$action"
                    }
                }""".trimIndent())
                validateEpoch(x)
            }
        }

        "Epoch when insert or update and enqueued date present" {
            forAll(*actions()) { action ->
                val x = jsonObject("""{
                    "message": {
                        "@type": "$action"
                    },
                    "timestamp": "2020-05-06"
                }""".trimIndent())
                validateEpoch(x)
            }
        }

        "createdDateTime when insert or update and lastModifiedDateTime not present" {
            forAll(*actions()) { action ->
                val x = jsonObject("""{
                    "message": {
                        "$CREATED_TIMESTAMP_FIELD": "2020-01-02",
                        "@type": "$action"
                    }
                }""".trimIndent())

                TimestampUtility.timestamp(x).shouldBeRight { (date, source) ->
                    date shouldBe "2020-01-02"
                    source shouldBe "createdDateTime"
                }
            }
        }

        "_lastModifiedDateTime when insert or update and all timestamp fields present" {
            forAll(*actions()) { action ->
                val x = jsonObject("""{
                    "message": {
                        "$LAST_MODIFIED_TIMESTAMP_FIELD": "2020-01-02",
                        "$CREATED_TIMESTAMP_FIELD": "2020-01-03",
                        "@type": "$action"
                    },
                    "$ENQUEUED_TIMESTAMP_FIELD": "2020-03-04" 
                }""".trimIndent())

                TimestampUtility.timestamp(x).shouldBeRight { (date, source) ->
                    date shouldBe "2020-01-02"
                    source shouldBe "_lastModifiedDateTime"
                }
            }
        }


        "Enqueued date when delete and enqueued date present" {
            val x = jsonObject("""{
                "message": {
                    "$LAST_MODIFIED_TIMESTAMP_FIELD": "2020-01-02",
                    "$CREATED_TIMESTAMP_FIELD": "2020-01-03",
                    "@type": "$DELETE_ACTION"
                },
                "$ENQUEUED_TIMESTAMP_FIELD": "2020-03-04" 
            }""".trimIndent())

            TimestampUtility.timestamp(x).shouldBeRight { (date, source) ->
                date shouldBe "2020-03-04"
                source shouldBe ENQUEUED_TIMESTAMP_FIELD
            }
        }

        "Epoch when delete and enqueued date not present" {
            val x = jsonObject("""{
                "message": {
                    "$LAST_MODIFIED_TIMESTAMP_FIELD": "2020-01-02",
                    "$CREATED_TIMESTAMP_FIELD": "2020-01-03",
                    "@type": "$DELETE_ACTION"
                } 
            }""".trimIndent())

            validateEpoch(x)
        }

    }

    private fun validateEpoch(x: JsonObject) {
        TimestampUtility.timestamp(x).shouldBeRight { (date, source) ->
            date shouldBe EPOCH
            source shouldBe EPOCH_INDICATOR
        }
    }


    companion object {
        private const val EPOCH = "1980-01-01T00:00:00.000+0000"
        private const val INSERT_ACTION = "MONGO_INSERT"
        private const val UPDATE_ACTION = "MONGO_UPDATE"
        private const val DELETE_ACTION = "MONGO_DELETE"
        private const val LAST_MODIFIED_TIMESTAMP_FIELD = "_lastModifiedDateTime"
        private const val CREATED_TIMESTAMP_FIELD = "createdDateTime"
        private const val ENQUEUED_TIMESTAMP_FIELD = "timestamp"
        private const val EPOCH_INDICATOR = "epoch"

        private fun actions() = listOf(INSERT_ACTION, UPDATE_ACTION).map(::row).toTypedArray()
        private fun sourceFields() = listOf(LAST_MODIFIED_TIMESTAMP_FIELD, CREATED_TIMESTAMP_FIELD).map(::row).toTypedArray()

        private fun jsonObject(x: String) = gson.fromJson(x, JsonObject::class.java)
        private val gson = Gson()
    }

}
