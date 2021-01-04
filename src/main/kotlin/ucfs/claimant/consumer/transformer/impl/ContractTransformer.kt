package ucfs.claimant.consumer.transformer.impl

import arrow.core.Either
import arrow.core.extensions.either.applicative.applicative
import arrow.core.fix
import com.google.gson.JsonObject
import org.springframework.stereotype.Component
import ucfs.claimant.consumer.transformer.Transformer
import ucfs.claimant.consumer.utility.GsonExtensions.boolean
import ucfs.claimant.consumer.utility.GsonExtensions.getObject
import ucfs.claimant.consumer.utility.GsonExtensions.json
import ucfs.claimant.consumer.utility.GsonExtensions.list
import ucfs.claimant.consumer.utility.GsonExtensions.nullableInteger
import ucfs.claimant.consumer.utility.GsonExtensions.nullableObject
import ucfs.claimant.consumer.utility.GsonExtensions.string

@Component
class ContractTransformer: Transformer {

    override fun transform(dbObject: JsonObject): Either<Any, String> =
            Either.applicative<Any>().tupledN(
                dbObject.getObject("_id"),
                dbObject.list<String>("people"),
                dbObject.string("contractType"),
                dbObject.boolean("coupleContract"),
                dbObject.string("createdDateTime")).fix()
            .map { (id, people, contractType, coupleContract, createdDateTime) ->
                val dateKey = "\$date"

                """{
                        "_id": $id,
                        "people": ${people.json()},
                        "startDate": ${dbObject.nullableInteger("startDate")},
                        "closedDate": ${dbObject.nullableInteger("closedDate")},
                        "contractType": "$contractType",
                        "declaredDate": ${dbObject.nullableInteger("declaredDate")},
                        "coupleContract": $coupleContract,
                        "claimSuspension": ${dbObject.nullableObject("claimSuspension")},
                        "createdDateTime": {"$dateKey": "$createdDateTime"},
                        "entitlementDate": ${dbObject.nullableInteger("entitlementDate")}
                    }"""
            }
}
