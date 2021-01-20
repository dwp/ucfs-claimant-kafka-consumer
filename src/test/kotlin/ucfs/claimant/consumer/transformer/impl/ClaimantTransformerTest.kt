package ucfs.claimant.consumer.transformer.impl

import com.google.gson.JsonObject
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.assertions.json.shouldMatchJson
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import ucfs.claimant.consumer.repository.SaltRepository
import ucfs.claimant.consumer.transformer.impl.GsonTestUtility.jsonObject
import ucfs.claimant.consumer.utility.GsonExtensions.json

class ClaimantTransformerTest: StringSpec() {
    init {
        "Transforms valid json" {
            transformer().transform(jsonObject(validJson)) shouldBeRight {
                it shouldMatchJson expectedOutput
            }
        }

        "Returns right if nino missing" {
            transformer().transform(jsonObject(invalidJsonMissingNino)) shouldBeRight {
                it shouldMatchJson noNinoOutput
            }
        }

        "Returns right if nino empty" {
            transformer().transform(jsonObject(invalidJsonEmptyNino)) shouldBeRight {
                it shouldMatchJson noNinoOutput
            }
        }

        "Returns right if nino blank" {
            transformer().transform(jsonObject(invalidJsonBlankNino)) shouldBeRight {
                it shouldMatchJson noNinoOutput
            }
        }

        "Returns right if nino null" {
            transformer().transform(jsonObject(invalidJsonNullNino)) shouldBeRight {
                it shouldMatchJson noNinoOutput
            }
        }

        "Returns left if _id missing" {
            transformer().transform(jsonObject(invalidJsonMissingId)) shouldBeLeft {
                it.shouldBeTypeOf<Pair<JsonObject, String>>()
                it.first.json() shouldMatchJson invalidJsonMissingId
                it.second shouldBe "_id"
            }
        }

        "Returns left if everything missing" {
            transformer().transform(jsonObject(invalidJsonMissingEverything)) shouldBeLeft {
                it.shouldBeTypeOf<Pair<JsonObject, String>>()
                it.first.json() shouldMatchJson invalidJsonMissingEverything
                it.second shouldBe "_id"
            }
        }
    }

    companion object {


        private const val validJson =
            """{
                "_id": {
                    "citizenId": "2bee0d32-4e18-477c-b5b1-b46d7952a927"
                },
                "nino": "AA123456A"
            }"""

        private const val invalidJsonMissingId =
            """{
                "nino": "AA123456A"
            }"""

        private const val invalidJsonMissingNino =
            """{
                "_id": {
                    "citizenId": "2bee0d32-4e18-477c-b5b1-b46d7952a927"
                }
            }"""

        private const val invalidJsonEmptyNino =
            """{
                "_id": {
                    "citizenId": "2bee0d32-4e18-477c-b5b1-b46d7952a927"
                },
                "nino": ""
            }"""

        private const val invalidJsonBlankNino =
            """{
                "_id": {
                    "citizenId": "2bee0d32-4e18-477c-b5b1-b46d7952a927"
                },
                "nino": "    "
            }"""

        private const val invalidJsonNullNino =
            """{
                "_id": {
                    "citizenId": "2bee0d32-4e18-477c-b5b1-b46d7952a927"
                },
                "nino": null
            }"""

        private const val invalidJsonMissingEverything = "{}"

        private const val expectedOutput =
            """{
                "_id": {
                    "citizenId": "2bee0d32-4e18-477c-b5b1-b46d7952a927"
                },
                "nino": "xFJrf8lbU4G-LB3gx6uF0z531bs0DIVYQ5o5514Y5OrrlxEriQ_W-jEum6bgveIL9gFwwRswDXz8lgqmTQCgFg=="
            }"""

        private const val noNinoOutput =
            """{
                "_id": {
                    "citizenId": "2bee0d32-4e18-477c-b5b1-b46d7952a927"
                },
                "nino": ""
            }"""

        private fun transformer() = ClaimantTransformer(saltProvider())
        private fun saltProvider() = mock<SaltRepository> { on { salt() } doReturn "SALT" }
    }
}
