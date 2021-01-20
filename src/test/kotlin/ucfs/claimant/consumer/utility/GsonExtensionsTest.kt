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
import ucfs.claimant.consumer.domain.DataKeyDecryptionServiceData
import ucfs.claimant.consumer.utility.GsonExtensions.getObject
import ucfs.claimant.consumer.utility.GsonExtensions.json
import ucfs.claimant.consumer.utility.GsonExtensions.jsonObject
import ucfs.claimant.consumer.utility.GsonExtensions.list
import ucfs.claimant.consumer.utility.GsonExtensions.nullableInteger
import ucfs.claimant.consumer.utility.GsonExtensions.nullableString
import ucfs.claimant.consumer.utility.GsonExtensions.string

class GsonExtensionsTest : StringSpec() {

    init {

        "Nullable string return value if present" {
            val obj = Gson().fromJson("""{ "key": "123" }""", JsonObject::class.java)
            val result = obj.nullableString("key")
            result shouldBe "123"
        }

        "Nullable string return value if empty" {
            val obj = Gson().fromJson("""{ "key": "" }""", JsonObject::class.java)
            val result = obj.nullableString("key")
            result shouldBe ""
        }

        "Nullable string return value if blank" {
            val obj = Gson().fromJson("""{ "key": "   " }""", JsonObject::class.java)
            val result = obj.nullableString("key")
            result shouldBe "   "
        }

        "Nullable string returns null if value null" {
            val obj = Gson().fromJson("""{ "key": null }""", JsonObject::class.java)
            val result = obj.nullableString("key")
            result shouldBe null
        }

        "Nullable string returns null if key not present" {
            val obj = Gson().fromJson("""{}""", JsonObject::class.java)
            val result = obj.nullableInteger("key")
            result shouldBe null
        }

        "Nullable integer return value if present" {
            val obj = Gson().fromJson("""{ "key": 123 }""", JsonObject::class.java)
            val result = obj.nullableInteger("key")
            result shouldBe 123
        }

        "Nullable integer returns null if value null" {
            val obj = Gson().fromJson("""{ "key": null }""", JsonObject::class.java)
            val result = obj.nullableInteger("key")
            result shouldBe null
        }

        "Nullable integer returns null if key not present" {
            val obj = Gson().fromJson("""{}""", JsonObject::class.java)
            val result = obj.nullableInteger("key")
            result shouldBe null
        }

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
            val result = obj.string("grandparent", "parent", "child")
            result shouldBeRight "childValue"
        }

        "Returns left of value if value is null" {
            val obj = Gson().fromJson("""
                {
                    "parent": {
                        "child": null
                    }
                }
            """.trimIndent(), JsonObject::class.java)
            val result = obj.string("parent", "child")
            result shouldBeLeft { (obj, str) ->
                obj.toString() shouldMatchJson """{"child": null}"""
                str shouldBe "child"
            }
        }

        "Returns left of value if string is object" {
            val obj = fourGenerations()
            val result = obj.string("grandparent", "parent", "child")
            result shouldBeLeft { (obj, str) ->
                obj.json() shouldMatchJson """{ "child": { "grandchild": "grandchildValue"} }"""
                str shouldBe "child"
            }
        }

        "Returns left of object and missing key if string not found" {
            val obj = threeGenerations()
            val result = obj.string("grandparent", "parent", "orphan")
            result shouldBeLeft { (obj, str) ->
                obj.json() shouldMatchJson """{ "child":"childValue" }"""
                str shouldBe "orphan"
            }
        }

        "Returns left of object and missing key if parent not found" {
            val obj = threeGenerations()
            val result = obj.string("grandparent", "notthere", "child")
            result shouldBeLeft { (obj, str) ->
                obj.json() shouldMatchJson """{"parent": {"child": "childValue" }}"""
                str shouldBe "notthere"
            }
        }

        "String json object <T> returns correct class" {
            val json = datakeyServiceResult()
            val result = json.jsonObject(DataKeyDecryptionServiceData::class.java)
            result shouldBeRight DataKeyDecryptionServiceData(encryptingKeyId, plaintextDatakey, encryptedDatakey)
        }

        "String json object <T> returns left if malformed json" {
            val json = malformedDatakeyServiceResult()
            val result = json.jsonObject(DataKeyDecryptionServiceData::class.java)
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
            val json = datakeyServiceResult()
            val result = json.jsonObject()
            result shouldBeRight {
                it.json() shouldMatchJson json
            }
        }

        "ByteArray json object returns left if malformed json" {
            val json = malformedDatakeyServiceResult()
            val result = json.jsonObject()
            result shouldBeLeft { error ->
                error.shouldBeInstanceOf<JsonParseException>()
            }
        }

        "Array returns right of list" {
            val validJson = """
                {
                    "grandparent": {
                        "parent": {
                            "child": [ "string1", "string2", "string3"]
                        }
                    }
                }
            """.trimIndent()

            val obj = Gson().fromJson(validJson, JsonObject::class.java)
            val actual = obj.list<String>("grandparent", "parent", "child")
            actual shouldBeRight {
                it shouldBe listOf("string1", "string2", "string3")
            }
        }

        "String instead of array returns left" {
            val obj = Gson().fromJson(threeGenerations(), JsonObject::class.java)
            val actual = obj.list<String>("grandparent", "parent", "child")
            actual shouldBeLeft { (json, elementName) ->
                json.toString() shouldMatchJson """{ "child": "childValue" }"""
                elementName shouldBe "child"
            }
        }

        "Object instead of array returns left" {
            val obj = Gson().fromJson(fourGenerations(), JsonObject::class.java)
            val actual = obj.list<String>("grandparent", "parent", "child")
            actual shouldBeLeft { (json, elementName) ->
                json.toString() shouldMatchJson """{ "child": { "grandchild": "grandchildValue" } }"""
                elementName shouldBe "child"
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

    private fun String.toJsonObject() = Gson().fromJson(this, JsonObject::class.java)
}
