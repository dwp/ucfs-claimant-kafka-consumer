package ucfs.claimant.consumer.utility

import arrow.core.Either
import arrow.core.flatMap
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import ucfs.claimant.consumer.utility.FunctionalUtility.encase
import uk.gov.dwp.dataworks.logging.DataworksLogger
import java.io.ByteArrayInputStream
import java.io.InputStreamReader

object JsonExtensions {

    fun JsonObject.getString(vararg path: String): Either<Pair<JsonObject, String>, String> =
            if (path.size == 1) {
                get(path[0])?.takeIf(JsonElement::isJsonPrimitive)?.asJsonPrimitive?.takeIf(JsonPrimitive::isString)
                        ?.asString?.let {
                            Either.Right(it)
                        } ?: {
                    logger.error("Failed to extract descendant string",
                            "path" to path.joinToString("/"))
                    Either.Left(Pair(this, path.joinToString()))
                }()
            } else {
                getChildObject(path[0]).flatMap {
                    it.getString(*path.sliceArray(1 until path.size))
                }
            }

    fun JsonObject.getObject(vararg path: String): Either<Pair<JsonObject, String>, JsonObject> =
            if (path.size == 1) {
                get(path[0])?.takeIf(JsonElement::isJsonObject)?.asJsonObject?.let {
                    Either.Right(it)
                } ?: {
                    logger.error("Failed to extract descendant object",
                            "path" to path.joinToString("/"))
                    Either.Left(Pair(this, path.joinToString()))
                }()
            } else {
                getChildObject(path[0]).flatMap {
                    it.getObject(*path.sliceArray(1 until path.size))
                }
            }

    fun <T> String.jsonObject(classOfT: Class<T>) = encase {
        Gson().fromJson(this, classOfT)
    }

    fun String.jsonObject() = encase {
        Gson().fromJson(this, JsonObject::class.java)
    }

    fun ByteArray.jsonObject() = encase {
        Gson().fromJson(InputStreamReader(ByteArrayInputStream(this)), JsonObject::class.java)
    }

    private fun JsonObject.getChildObject(childName: String): Either<Pair<JsonObject, String>, JsonObject> =
            get(childName)?.takeIf(JsonElement::isJsonObject)?.asJsonObject?.let {
                Either.Right(it)
            } ?: {
                logger.error("Failed to extract child object from parent", "child_name" to childName)
                Either.Left(Pair(this, childName))
            }()

    private val logger = DataworksLogger.getLogger(JsonExtensions::class)
}



