package ucfs.claimant.consumer.transformer.impl

import arrow.core.Either
import arrow.core.extensions.either.applicative.applicative
import arrow.core.fix
import arrow.core.flatMap
import org.springframework.stereotype.Component
import ucfs.claimant.consumer.transformer.Transformer
import ucfs.claimant.consumer.utility.GsonExtensions.boolean
import ucfs.claimant.consumer.utility.GsonExtensions.getObject
import ucfs.claimant.consumer.utility.GsonExtensions.json
import ucfs.claimant.consumer.utility.GsonExtensions.jsonObject
import ucfs.claimant.consumer.utility.GsonExtensions.list
import ucfs.claimant.consumer.utility.GsonExtensions.nullableInteger
import ucfs.claimant.consumer.utility.GsonExtensions.nullableObject
import ucfs.claimant.consumer.utility.GsonExtensions.string

@Component
class ContractTransformer: Transformer {

    override fun transform(dbObject: String): Either<Any, String> =
            dbObject.jsonObject().flatMap {
                Either.applicative<Any>().tupledN(
                    Either.Right(it),
                    it.getObject("_id"),
                    it.list<String>("people"),
                    it.string("contractType"),
                    it.boolean("coupleContract"),
                    it.string("createdDateTime")).fix()
            }.map { (jsonObject, id, people, contractType, coupleContract, createdDateTime) ->
                val dateKey = "\$date"

                """{
                        "_id": $id,
                        "people": ${people.json()},
                        "startDate": ${jsonObject.nullableInteger("startDate")},
                        "closedDate": ${jsonObject.nullableInteger("closedDate")},
                        "contractType": "$contractType",
                        "declaredDate": ${jsonObject.nullableInteger("declaredDate")},
                        "coupleContract": $coupleContract,
                        "claimSuspension": ${jsonObject.nullableObject("claimSuspension")},
                        "createdDateTime": {"$dateKey": "$createdDateTime"},
                        "entitlementDate": ${jsonObject.nullableInteger("entitlementDate")}
                    }"""
            }
}
