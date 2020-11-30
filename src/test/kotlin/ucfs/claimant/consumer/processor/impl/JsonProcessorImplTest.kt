package ucfs.claimant.consumer.processor.impl

import com.google.gson.Gson
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.assertions.json.shouldMatchJson
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.types.shouldBeSameInstanceAs
import ucfs.claimant.consumer.domain.SourceRecord

class JsonProcessorImplTest : StringSpec() {
    init {
        "Returns right if well-formed json" {
            val json = """
                {
                    "message": {
                        "dbObject": "ENCRYPTED_OBJECT"
                    }
                }
            """.trimIndent()

            val queueRecord = mock<SourceRecord>()
            val result = JsonProcessorImpl().process(Pair(queueRecord, json))
            result shouldBeRight { (record, result) ->
                Gson().toJson(result) shouldMatchJson json
                record shouldBeSameInstanceAs queueRecord
            }
        }

        "Returns left if malformed json" {
            val json = """
                    "message": {
                        "dbObject": "ENCRYPTED_OBJECT"
                    }
            """.trimIndent()

            val record = mock<SourceRecord> {
                on { key() } doReturn "key".toByteArray()
            }
            val result = JsonProcessorImpl().process(Pair(record, json))
            result shouldBeLeft {
                it shouldBeSameInstanceAs record
            }
        }
    }
}
