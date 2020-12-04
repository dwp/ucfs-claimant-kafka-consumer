package ucfs.claimant.consumer.transformer.impl

import arrow.core.Either
import com.google.gson.JsonObject
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.assertions.json.shouldMatchJson
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import ucfs.claimant.consumer.domain.CipherServiceEncryptionResult
import ucfs.claimant.consumer.service.EncryptionService
import ucfs.claimant.consumer.transformer.impl.GsonTestUtility.withNullFields

class StatementTransformerTest: StringSpec(){
    init {

        "Returns right if all fields present" {
            statementTransformer().transform(inputWithAllFields) shouldBeRight {
                it shouldMatchJson outputWithAllFields
            }
        }

        "Returns left if required field missing" {
            forAll(*requiredFields) { field ->
                statementTransformer().transform(inputWithAllFields.withNullFields(field)) shouldBeLeft {
                    it.shouldBeInstanceOf<Pair<JsonObject, String>>()
                    GsonTestUtility.gson.toJson(it.first) shouldMatchJson inputWithAllFields.withNullFields(field)
                    it.second shouldBe field
                }
            }
        }
    }

    private fun statementTransformer(): StatementTransformer = StatementTransformer(encryptionService())

    private fun encryptionService(): EncryptionService =
            mock {
                on {
                    encrypt("123.45")
                } doReturn Either.Right(cipherServiceEncryptionResult())
            }

    private fun cipherServiceEncryptionResult(): CipherServiceEncryptionResult =
            CipherServiceEncryptionResult(encryptingKeyId, initialisationVector, encryptedDataKey, cipherText)

    companion object {
        private const val dateKey = "\$date"
        private const val takeHomePay = "123.45"
        private const val encryptingKeyId = "encryptingKeyId"
        private const val encryptedDataKey = "encryptingKeyId"
        private const val cipherText = "cipherText"
        private const val initialisationVector = "initialisationVector"
        val requiredFields =
            listOf("_id", "people", "createdDateTime", "assessmentPeriod", "takeHomePay").map(::row).toTypedArray()

        private val inputWithAllFields = """
        {
            "_id": {
                "statementId": "123456"
            },
            "_version": 1,
            "people": [ "person1", "person2" ],
            "assessmentPeriod": {
                "endDate": 20280131,
                "startDate": 20280101,
                "contractId": "6e2b4428-2da5-4f77-9904-e0c2fc850c4f",
                "paymentDate": 20280123,
                "processDate": null,
                "createdDateTime": "2027-12-01T00:00:00.000000Z",
                "assessmentPeriodId": "5395ec9d-d054-4bf4-b027-cc11d1ec473d"
            },
            "standardAllowanceElement": "317.82",
            "housingElement": "0.00",
            "housingElementRent": "0.00",
            "housingElementServiceCharges": "0.00",
            "childElement": "0.00",
            "numberEligibleChildren": 0,
            "disabledChildElement": "0.00",
            "numberEligibleDisabledChildren": 0,
            "childcareElement": "0.00",
            "numberEligibleChildrenInChildCare": 0,
            "carerElement": "0.00",
            "numberPeopleCaredFor": 0,
            "takeHomePay": "$takeHomePay",
            "takeHomeBreakdown": {
                "rte": "0.00",
                "selfReported": "0.00",
                "selfEmployed": "0.00",
                "selfEmployedWithMif": "0.00"
            },
            "unaffectedPayElement": "0.00",
            "totalReducedForHomePay": "0.00",
            "otherIncomeAdjustment": "0.00",
            "capitalAdjustment": "0.00",
            "totalAdjustments": "0.00",
            "fraudPenalties": "0.00",
            "sanctions": "317.82",
            "advances": "0.00",
            "deductions": "0.00",
            "totalPayment": "0.00",
            "createdDateTime": "2020-12-11T10:12:34.000",
            "earningsSource": null,
            "otherBenefitAwards": [],
            "overlappingBenefits": [],
            "totalOtherBenefitsAdjustment": "0",
            "capApplied": null,
            "type": "CALCULATED",
            "preAdjustmentTotal": "317.82",
            "_entityVersion": 4,
            "_lastModifiedDateTime": "2020-12-11T10:12:34.000",
            "workCapabilityElement": null,
            "benefitCapThreshold": null,
            "benefitCapAdjustment": null,
            "gracePeriodEndDate": null,
            "landlordPayment": "0"
        }
        """.trimIndent()

        private const val outputWithAllFields = """ {
                "_id": {"statementId":"123456"},
                "people": ["person1","person2"],
                "createdDateTime": {"$dateKey": "2020-12-11T10:12:34.000"},
                "assessmentPeriod": {"endDate":20280131,"startDate":20280101,"contractId":"6e2b4428-2da5-4f77-9904-e0c2fc850c4f","paymentDate":20280123,"processDate":null,"createdDateTime":"2027-12-01T00:00:00.000000Z","assessmentPeriodId":"5395ec9d-d054-4bf4-b027-cc11d1ec473d"},
                "takeHomePay": "initialisationVectorcipherText",
                "encryptedTakeHomePay": {
                    "keyId": "encryptingKeyId",
                    "takeHomePay": "initialisationVectorcipherText",
                    "cipherTextBlob": "encryptingKeyId"
                }
            }"""

    }
}
