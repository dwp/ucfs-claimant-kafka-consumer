package ucfs.claimant.consumer.transformer.impl

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
import ucfs.claimant.consumer.transformer.impl.GsonTestUtility.gson
import ucfs.claimant.consumer.transformer.impl.GsonTestUtility.withNullFields
import ucfs.claimant.consumer.transformer.impl.GsonTestUtility.withoutFields

class ContractTransformerTest: StringSpec() {

    init {

        "Returns left if required field missing" {
            forAll(*requiredFields) { field ->
                validateLeft(inputWithAllFields.withoutFields(field), field)
            }
        }

        "Returns left if required field null" {
            forAll(*requiredFields) { field ->
                validateLeft(inputWithAllFields.withNullFields(field), field)
            }
        }

        "Returns right if optional field missing" {
            forAll(*optionalFields) { field ->
                validateRight(inputWithAllFields.withoutFields(field), field)
            }
        }

        "Returns right if optional field null" {
            forAll(*optionalFields) { field ->
                validateRight(inputWithAllFields.withNullFields(field), field)
            }
        }
    }

    companion object {

        private fun transform(json: String) = ContractTransformer().transform(Gson().fromJson(json, JsonObject::class.java))

        private fun validateLeft(json: String, field: String) {
            transform(json) shouldBeLeft {
                it.shouldBeInstanceOf<Pair<JsonObject, String>>()
                gson.toJson(it.first) shouldMatchJson json
                it.second shouldBe field
            }
        }

        private fun validateRight(input: String, field: String) {
            transform(input) shouldBeRight {
                it shouldMatchJson outputWithAllFields.withNullFields(field)
            }
        }

        private const val dateKey = "\$date"
        private const val inputWithAllFields = """
                    {
                        "_id": {
                            "contractId": "abcde"
                        },
                        "assessmentPeriods": [],
                        "people": ["abc", "def"],
                        "declaredDate": 20200123,
                        "startDate": 20200224,
                        "entitlementDate": 20200325,
                        "closedDate": 20200426,
                        "annualVerificationEligibilityDate": null,
                        "annualVerificationCompletionDate": null,
                        "paymentDayOfMonth": 7,
                        "flags": [],
                        "claimClosureReason": "FraudIntervention",
                        "claimSuspension": { "suspensionDate": null },
                        "_version": 12,
                        "createdDateTime": "2020-12-12T10:37:45.000", 
                        "coupleContract": false,
                        "claimantsExemptFromWaitingDays": [],
                        "contractTypes": null,
                        "_entityVersion": 2,
                        "_lastModifiedDateTime": "2020-03-04T06:37:45.000",
                        "stillSingle": true,
                        "contractType": "INITIAL"
                    }
            """

        private const val outputWithAllFields = """
                        {
                            "_id": {
                                "contractId": "abcde"
                            },
                            "people": ["abc", "def"],
                            "declaredDate": 20200123,
                            "startDate": 20200224,
                            "entitlementDate": 20200325,
                            "closedDate": 20200426,
                            "claimSuspension": { "suspensionDate": null },
                            "coupleContract": false,
                            "createdDateTime": { "$dateKey": "2020-12-12T10:37:45.000" }, 
                            "contractType": "INITIAL"
                        }
                    """

        val requiredFields =
            listOf("_id", "people", "contractType", "coupleContract", "createdDateTime").map(::row).toTypedArray()

        val optionalFields =
            listOf("startDate", "closedDate", "declaredDate", "claimSuspension", "entitlementDate").map(::row).toTypedArray()
    }

}
