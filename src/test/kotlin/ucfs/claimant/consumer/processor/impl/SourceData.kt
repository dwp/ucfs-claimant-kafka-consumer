package ucfs.claimant.consumer.processor.impl

import com.google.gson.JsonObject
import ucfs.claimant.consumer.domain.DatabaseAction
import ucfs.claimant.consumer.domain.JsonProcessingExtract

object SourceData {
    const val claimantTopic = "db.core.claimant"
    private const val contractTopic = "db.core.contract"
    private const val statementTopic = "db.core.statement"

    const val claimantIdSourceField = "citizenId"
    private const val contractIdSourceField = "contractId"
    private const val statementIdSourceField = "statementId"

    val idSourceFields =
        mapOf(claimantTopic to claimantIdSourceField,
            contractTopic to contractIdSourceField,
            statementTopic to statementIdSourceField)

    fun jsonProcessingExtract(json: JsonObject = JsonObject(), id: String = "id") =
        JsonProcessingExtract(json, id, DatabaseAction.MONGO_UPDATE, Pair("2020-12-12", "_lastModifiedDateTime"))

}
