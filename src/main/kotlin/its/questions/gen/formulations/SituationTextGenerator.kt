package its.questions.gen.formulations

import com.github.max_person.templating.TemplatingSafeMethod
import its.model.definition.DomainDefWithMeta
import its.model.definition.DomainRef
import its.model.definition.MetadataProperty
import its.model.definition.types.Obj
import its.questions.gen.QuestioningSituation
import its.questions.gen.formulations.TemplatingUtils.toCase

/**
 * TODO
 */
class SituationTextGenerator(val situation : QuestioningSituation) {

    @TemplatingSafeMethod("val")
    fun getVariableValue(varName: String, case: Case) : String{
        return situation.decisionTreeVariables[varName]!!.localizedName.toCase(case)
    }

    @TemplatingSafeMethod("obj")
    fun getEntity(alias: String, case: Case) : String{
        return Obj(alias).localizedName.toCase(case)
    }

    @TemplatingSafeMethod("class")
    fun getVariableClassname(varName: String, case: Case) : String{
        return situation.decisionTreeVariables[varName]!!
            .findInOrUnkown(situation.domain)
            .clazz
            .localizedName
            .toCase(case)
    }

    val DomainDefWithMeta<*>.localizedName: String
        get() = this.metadata[MetadataProperty("localizedName", situation.localizationCode)].toString()

    val <T : DomainDefWithMeta<T>> DomainRef<T>.localizedName
        get() = this.findInOrUnkown(situation.domain).localizedName
}
