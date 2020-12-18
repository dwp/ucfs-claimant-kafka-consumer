package ucfs.claimant.consumer.transformer.impl

import arrow.core.Either
import arrow.core.extensions.either.applicative.applicative
import arrow.core.fix
import arrow.core.flatMap
import org.springframework.stereotype.Component
import ucfs.claimant.consumer.service.EncryptionService
import ucfs.claimant.consumer.transformer.Transformer
import ucfs.claimant.consumer.utility.GsonExtensions.getObject
import ucfs.claimant.consumer.utility.GsonExtensions.json
import ucfs.claimant.consumer.utility.GsonExtensions.jsonObject
import ucfs.claimant.consumer.utility.GsonExtensions.list
import ucfs.claimant.consumer.utility.GsonExtensions.string

@Component
class StatementTransformer(private val encryptionService: EncryptionService): Transformer {

    override fun transform(dbObject: String): Either<Any, String> {
        return dbObject.jsonObject().flatMap {
            Either.applicative<Any>().tupledN(Either.Right(it), it.string("takeHomePay")).fix()
        }.flatMap { (jsonObject, takeHomePay) ->
            Either.applicative<Any>().tupledN(
                jsonObject.getObject("_id"),
                jsonObject.list<String>("people"),
                jsonObject.string("createdDateTime"),
                jsonObject.getObject("assessmentPeriod"),
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
}
