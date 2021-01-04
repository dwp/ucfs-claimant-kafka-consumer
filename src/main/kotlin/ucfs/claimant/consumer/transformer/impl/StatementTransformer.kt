package ucfs.claimant.consumer.transformer.impl

import arrow.core.Either
import arrow.core.extensions.either.applicative.applicative
import arrow.core.fix
import arrow.core.flatMap
import com.google.gson.JsonObject
import org.springframework.stereotype.Component
import ucfs.claimant.consumer.service.EncryptionService
import ucfs.claimant.consumer.transformer.Transformer
import ucfs.claimant.consumer.utility.GsonExtensions.getObject
import ucfs.claimant.consumer.utility.GsonExtensions.json
import ucfs.claimant.consumer.utility.GsonExtensions.list
import ucfs.claimant.consumer.utility.GsonExtensions.string

@Component
class StatementTransformer(private val encryptionService: EncryptionService): Transformer {

    override fun transform(dbObject: JsonObject): Either<Any, String> =
            dbObject.string("takeHomePay")
                .flatMap { takeHomePay ->
                    Either.applicative<Any>().tupledN(
                        dbObject.getObject("_id"),
                        dbObject.list<String>("people"),
                        dbObject.string("createdDateTime"),
                        dbObject.getObject("assessmentPeriod"),
                        encryptionService.encrypt(takeHomePay)).fix()
                }.map { (id, people, createdDateTime, assessmentPeriod, encryptedTakeHomePay) ->
                    val dateKey = "\$date"
                    """
                        {
                            "_id": $id,
                            "people": ${people.json()},
                            "createdDateTime": {"$dateKey": "$createdDateTime"},
                            "assessmentPeriod": $assessmentPeriod,
                            "takeHomePay": "${encryptedTakeHomePay.initialisationVector}${encryptedTakeHomePay.cipherText}",
                            "encryptedTakeHomePay": {
                                "keyId": "${encryptedTakeHomePay.encryptingKeyId}",
                                "takeHomePay": "${encryptedTakeHomePay.initialisationVector}${encryptedTakeHomePay.cipherText}",
                                "cipherTextBlob": "${encryptedTakeHomePay.encryptedDataKey}"
                            }
                        }
                    """
                }
}
