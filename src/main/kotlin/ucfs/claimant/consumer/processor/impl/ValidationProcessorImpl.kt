package ucfs.claimant.consumer.processor.impl

import org.everit.json.schema.loader.SchemaLoader
import org.json.JSONObject
import org.json.JSONTokener
import org.springframework.stereotype.Component
import ucfs.claimant.consumer.domain.SourceRecordProcessingResult
import ucfs.claimant.consumer.domain.ValidationProcessingOutput
import ucfs.claimant.consumer.processor.ValidationProcessor
import ucfs.claimant.consumer.utility.FunctionalUtility.encase
import ucfs.claimant.consumer.utility.FunctionalUtility.processingFailure

@Component
class ValidationProcessorImpl(private val schemaLocation: String): ValidationProcessor {

    override fun process(record: SourceRecordProcessingResult): ValidationProcessingOutput =
            encase {
                schema().validate(JSONObject(record.second))
                record
            }.mapLeft { processingFailure(record.first, it, "Message failed validation") }


    private fun schema() = schemaLoader.load().build()

    private val schemaLoader: SchemaLoader by lazy {
        SchemaLoader.builder().run {
            schemaJson(schemaObject())
            draftV7Support()
            build()
        }
    }

    private fun schemaObject() =
        javaClass.getResourceAsStream(schemaLocation).use { inputStream ->
            JSONObject(JSONTokener(inputStream))
        }
}
