package its.questions.gen.formulations

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import its.model.expressions.types.EnumValue
import its.model.models.ClassModel

class DomainLocalization(
    val general: Localization,
    val classLocalization: Map<String, ClassLocalization>,
    val enumLocalization: Map<String, EnumLocalization>,
) {
    constructor(json: JsonObject, general: Localization): this(
        general,
        Gson().fromJson(json["classes"], object : TypeToken<Map<String, ClassLocalization>>() {}.type),
        Gson().fromJson(json["enums"], object : TypeToken<Map<String, EnumLocalization>>() {}.type),
    )

    data class ClassLocalization(
        val localizedName: String,
    )

    fun localizedClassName(c: ClassModel): String = classLocalization[c.name]!!.localizedName

    val ClassModel.localizedName
        get() = localizedClassName(this)



    data class EnumLocalization(
        val localizedName: String,
        val localizedValues: Map<String, String>,
    )

    fun localizedEnumValue(e: EnumValue): String = enumLocalization[e.ownerEnum]!!.localizedValues[e.value]!!

    val EnumValue.localized
        get() = localizedEnumValue(this)
}