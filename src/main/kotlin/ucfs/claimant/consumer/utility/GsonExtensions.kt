package ucfs.claimant.consumer.utility

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.rightIfNotNull
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.reflect.TypeToken
import ucfs.claimant.consumer.utility.FunctionalUtility.encase
import uk.gov.dwp.dataworks.logging.DataworksLogger
import java.lang.reflect.Type

object GsonExtensions {

    fun Any.json(): String = gson.toJson(this)

    fun JsonObject.nullableInteger(vararg path: String): Number? = integer(*path).orNull()
    fun JsonObject.nullableObject(vararg path: String): JsonObject? = getObject(*path).orNull()

    fun JsonObject.string(vararg path: String): Either<Pair<JsonObject, String>, String> =
        primitive(JsonPrimitive::isString, JsonPrimitive::getAsString, *path)

    fun JsonObject.integer(vararg path: String): Either<Pair<JsonObject, String>, Int> =
        primitive(JsonPrimitive::isNumber, JsonPrimitive::getAsInt, *path)

    fun JsonObject.boolean(vararg path: String): Either<Pair<JsonObject, String>, Boolean> =
        primitive(JsonPrimitive::isBoolean, JsonPrimitive::getAsBoolean, *path)

    fun JsonObject.getObject(vararg path: String): Either<Pair<JsonObject, String>, JsonObject> =
            when (path.size) {
                1 -> get(path[0])?.takeIf(JsonElement::isJsonObject)?.asJsonObject.rightIfNotNull {
                    logger.warn(
                        "Failed to extract descendant object",
                        "path" to path.joinToString("/")
                    )
                    Pair(this, path.joinToString())
                }
                else -> getChildObject(path[0]).flatMap { it.getObject(*path.sliceArray(1 until path.size)) }
            }

    @Suppress("UNCHECKED_CAST")
    fun <T> JsonObject.list(vararg path: String): Either<Pair<JsonObject, String>, List<T>> =
        when (path.size) {
            1 -> (get(path[0])?.takeIf(JsonElement::isJsonArray)?.asJsonArray?.let {
                val f: () -> List<T> = {
                    val type: Type = object: TypeToken<List<T>>() {}.type
                    gson.fromJson(it.toString(), type)
                }
                encase { f() }.mapLeft {
                    logger.warn("Failed to parse descendant array", "exception" to "${it.message}",
                        "path" to path.joinToString("/"))
                    Pair(this, path.joinToString()).left()
                }
            } ?: run {
                logger.warn("Failed to extract descendant array","path" to path.joinToString("/"))
                Pair(this, path.joinToString()).left()
            }) as Either<Pair<JsonObject, String>, List<T>>

            else -> getChildObject(path[0]).flatMap {
                it.list(*path.sliceArray(1 until path.size))
            }
        }

    fun <T> String.jsonObject(classOfT: Class<T>) = this.encase {
        gson.fromJson(this, classOfT)
    }

    fun String.jsonObject() = this.encase {
        gson.fromJson(this, JsonObject::class.java)
    }

    private fun <T> JsonObject.primitive(predicate: JsonPrimitive.() -> Boolean,
                                         extractor: JsonPrimitive.() -> T,
                                         vararg path: String): Either<Pair<JsonObject, String>, T> =
        when (path.size) {
            1 -> get(path[0])?.
                takeIf(JsonElement::isJsonPrimitive)?.asJsonPrimitive?.
                takeIf(predicate)?.run(extractor).rightIfNotNull {
                    logger.warn("Failed to extract descendant primitive","path" to path.joinToString("/"))
                    Pair(this, path.joinToString())
                }
            else -> getChildObject(path[0]).flatMap { it.primitive(predicate, extractor, *path.sliceArray(1 until path.size)) }
        }

    private fun JsonObject.getChildObject(childName: String): Either<Pair<JsonObject, String>, JsonObject> =
            get(childName)?.takeIf(JsonElement::isJsonObject)?.asJsonObject.rightIfNotNull {
                logger.warn("Failed to extract child object from parent", "child_name" to childName)
                Pair(this, childName)
            }

    private val logger = DataworksLogger.getLogger(GsonExtensions::class)
    private val gson = GsonBuilder().serializeNulls().create()
}
