package ucfs.claimant.consumer.utility

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.assertions.json.shouldMatchJson
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import ucfs.claimant.consumer.domain.DataKeyServiceResult
import ucfs.claimant.consumer.utility.JsonExtensions.getObject
import ucfs.claimant.consumer.utility.JsonExtensions.getString
import ucfs.claimant.consumer.utility.JsonExtensions.jsonObject

class JsonUtilityTest : StringSpec() {

    init {
        "Returns right of object if object found" {
            val obj = fourGenerations()
            val result = obj.getObject("grandparent", "parent", "child")
            result shouldBeRight {
                it.json() shouldMatchJson """{ "grandchild": "grandchildValue"}"""
            }
        }

        "Returns left of missing object parent if object not found" {
            val obj = fourGenerations()
            val result = obj.getObject("grandparent", "parent", "orphan")
            result shouldBeLeft { (obj, str) ->
                obj.json() shouldMatchJson """{"child": { "grandchild": "grandchildValue"}}"""
                str shouldBe "orphan"
            }
        }

        "Returns left of missing object parent if object parent found" {
            val obj = fourGenerations()
            val result = obj.getObject("grandparent", "notthere", "child")
            result shouldBeLeft { (obj, str) ->
                obj.json() shouldMatchJson """{ "parent": { "child": { "grandchild": "grandchildValue" } } }"""
                str shouldBe "notthere"
            }
        }

        "Returns left of value if object is string" {
            val obj = threeGenerations()
            val result = obj.getObject("grandparent", "parent", "child")
            result shouldBeLeft { (obj, str) ->
                obj.json() shouldMatchJson """{ "child": "childValue" }"""
                str shouldBe "child"
            }
        }

        "Returns right of value if string found" {
            val obj = threeGenerations()
            val result = obj.getString("grandparent", "parent", "child")
            result shouldBeRight "childValue"
        }

        "Returns left of value if string is object" {
            val obj = fourGenerations()
            val result = obj.getString("grandparent", "parent", "child")
            result shouldBeLeft { (obj, str) ->
                obj.json() shouldMatchJson """{ "child": { "grandchild": "grandchildValue"} }"""
                str shouldBe "child"
            }
        }

        "Returns left of object and missing key if string not found" {
            val obj = threeGenerations()
            val result = obj.getString("grandparent", "parent", "orphan")
            result shouldBeLeft { (obj, str) ->
                obj.json() shouldMatchJson """{ "child":"childValue" }"""
                str shouldBe "orphan"
            }
        }

        "Returns left of object and missing key if parent not found" {
            val obj = threeGenerations()
            val result = obj.getString("grandparent", "notthere", "child")
            result shouldBeLeft { (obj, str) ->
                obj.json() shouldMatchJson """{"parent": {"child": "childValue" }}"""
                str shouldBe "notthere"
            }
        }

        "String json object <T> returns correct class" {
            val json = datakeyServiceResult()
            val result = json.jsonObject(DataKeyServiceResult::class.java)
            result shouldBeRight DataKeyServiceResult(encryptingKeyId, plaintextDatakey, encryptedDatakey)
        }

        "String json object <T> returns left if malformed json" {
            val json = malformedDatakeyServiceResult()
            val result = json.jsonObject(DataKeyServiceResult::class.java)
            result shouldBeLeft { error ->
                error.shouldBeInstanceOf<JsonParseException>()
            }
        }

        "String json object returns left if malformed json" {
            val json = malformedDatakeyServiceResult()
            val result = json.jsonObject()
            result shouldBeLeft { error ->
                error.shouldBeInstanceOf<JsonParseException>()
            }
        }

        "String json object returns json object" {
            val json = datakeyServiceResult()
            val result = json.jsonObject()
            result shouldBeRight {
                it.json() shouldMatchJson json
            }
        }

        "ByteArray json object returns json object" {
            val json = datakeyServiceResult().toByteArray()
            val result = json.jsonObject()
            result shouldBeRight {
                it.json() shouldMatchJson String(json)
            }
        }

        "ByteArray json object returns left if malformed json" {
            val json = malformedDatakeyServiceResult().toByteArray()
            val result = json.jsonObject()
            result shouldBeLeft { error ->
                error.shouldBeInstanceOf<JsonParseException>()
            }
        }

    }

    private fun malformedDatakeyServiceResult(): String = datakeyServiceResult().substring(20)

    private fun datakeyServiceResult(): String =
            """{
                "dataKeyEncryptionKeyId": "$encryptingKeyId",
                "plaintextDataKey": "$plaintextDatakey",
                "ciphertextDataKey": "$encryptedDatakey"
            }"""

    private val encryptingKeyId = "encryptingKeyId"
    private val plaintextDatakey = "plaintextDatakey"
    private val encryptedDatakey = "encryptedDatakey"

    private fun threeGenerations(): JsonObject = """
                {
                    "grandparent": {
                        "parent": {
                            "child": "childValue"
                        }
                    }
                }
            """.trimIndent().toJsonObject()

    private fun fourGenerations(): JsonObject = """
                {
                    "grandparent": {
                        "parent": {
                            "child": { "grandchild": "grandchildValue"}
                        }
                    }
                }
            """.trimIndent().toJsonObject()

    private fun JsonObject.json() = Gson().toJson(this)
    private fun String.toJsonObject() = Gson().fromJson(this, JsonObject::class.java)
}
