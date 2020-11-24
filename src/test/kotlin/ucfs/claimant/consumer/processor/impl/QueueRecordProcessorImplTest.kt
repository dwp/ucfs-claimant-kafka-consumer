package ucfs.claimant.consumer.processor.impl

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import ucfs.claimant.consumer.domain.SourceRecord

class QueueRecordProcessorImplTest : StringSpec() {
    init {
        "Returns right if body present" {
            val value = ByteArray(1)
            val queueRecordProcessor = SourceRecordProcessorImpl()
            val queueRecord = mock<SourceRecord> {
                on { value() } doReturn value
            }
            val result = queueRecordProcessor.process(queueRecord)
            result shouldBeRight { (record, result) ->
                record shouldBeSameInstanceAs queueRecord
                result shouldBe value
            }
        }

        "Returns left if body not present" {
            val queueRecordProcessor = SourceRecordProcessorImpl()
            val queueRecord = mock<SourceRecord> {
                on { value() } doReturn null
            }
            val result = queueRecordProcessor.process(queueRecord)
            result shouldBeLeft queueRecord
        }
    }
}
