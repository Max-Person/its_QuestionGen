package its.questions.gen.formulations

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.stream.JsonReader
import its.model.DomainModel
import its.model.dictionaries.*
import its.model.models.*
import java.io.File
import java.net.URL

class LocalizedDomainModel(directoryURL: URL) : DomainModel<ClassModel, DecisionTreeVarModel, EnumModel, PropertyModel, RelationshipModel>(
    ClassesDictionary(),
    DecisionTreeVarsDictionary(),
    EnumsDictionary(),
    PropertiesDictionary(),
    RelationshipsDictionary(),
    directoryURL
    )
{
    constructor(directory: String) : this(File(directory).toURI().toURL())

    val localizations : Map<String, DomainLocalization>

    init {
        val reader = JsonReader((directoryURL + "localization.json").openStream().bufferedReader())
        val obj = Gson().fromJson<JsonObject>(reader, JsonObject::class.java)
        localizations = obj.entrySet().map {
            if(Localization.localizations.containsKey(it.key)){
                DomainLocalization(it.value.asJsonObject, Localization.localizations[it.key]!!)
            }
            else null
        }.filterNotNull().map{it.general.codePrefix to it}.toMap()
    }

    companion object _static{
        @JvmStatic
        fun localization(localizationCode : String) : Localization {
            return domainLocalization(localizationCode).general
        }

        @JvmStatic
        fun domainLocalization(localizationCode : String) : DomainLocalization {
            return (instance!! as LocalizedDomainModel).localizations[localizationCode]!!
        }
    }

}