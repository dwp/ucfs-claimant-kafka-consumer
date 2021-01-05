package ucfs.claimant.consumer.transformer.impl

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import ucfs.claimant.consumer.utility.GsonExtensions.json

object GsonTestUtility {

    fun String.withoutFields(vararg fields: String): String {
        val obj = gson.fromJson(this, JsonObject::class.java)
        fields.forEach(obj::remove)
        return obj.json()
    }

    fun String.withNullFields(vararg fields: String): String {
        val obj = gson.fromJson(this, JsonObject::class.java)
        fields.forEach(obj::remove)
        fields.forEach { key ->
            obj.add(key, JsonNull.INSTANCE)
        }
        return gson.toJson(obj)
    }

    fun jsonObject(json: String): JsonObject = gson.fromJson(json, JsonObject::class.java)

    val gson: Gson = GsonBuilder().serializeNulls().create()
}
