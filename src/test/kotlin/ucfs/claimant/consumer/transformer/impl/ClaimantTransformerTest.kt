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
import ucfs.claimant.consumer.utility.GsonExtensions.json

class ClaimantTransformerTest: StringSpec() {
    init {
        "Transforms valid json" {
            transformer().transform(validJson) shouldBeRight {
                it shouldMatchJson expectedOutput
            }
        }

        "Returns left if nino missing" {
            transformer().transform(invalidJsonMissingNino) shouldBeLeft {
                it.shouldBeTypeOf<Pair<JsonObject, String>>()
                it.first.json() shouldMatchJson invalidJsonMissingNino
                it.second shouldBe "nino"
            }
        }

        "Returns left if _id missing" {
            transformer().transform(invalidJsonMissingId) shouldBeLeft {
                it.shouldBeTypeOf<Pair<JsonObject, String>>()
                it.first.json() shouldMatchJson invalidJsonMissingId
                it.second shouldBe "_id"
            }
        }

        "Returns left if everything missing" {
            transformer().transform(invalidJsonMissingEverything) shouldBeLeft {
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

        private const val invalidJsonMissingEverything = "{}"

        private const val expectedOutput =
            """{
                "_id": {
                    "citizenId": "2bee0d32-4e18-477c-b5b1-b46d7952a927"
                },
                "nino": "xFJrf8lbU4G-LB3gx6uF0z531bs0DIVYQ5o5514Y5OrrlxEriQ_W-jEum6bgveIL9gFwwRswDXz8lgqmTQCgFg=="
            }"""

        private fun transformer() = ClaimantTransformer(saltProvider())
        private fun saltProvider() = mock<SaltRepository> { on { salt() } doReturn "SALT" }
    }
}
