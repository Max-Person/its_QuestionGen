package its.questions.gen.visitors

import its.model.definition.ThisShouldNotHappen
import its.model.definition.types.Clazz
import its.model.definition.types.Comparison
import its.model.definition.types.EnumValue
import its.model.definition.types.Obj
import its.questions.gen.QuestioningSituation
import its.questions.gen.formulations.Localization
import its.questions.gen.formulations.TemplatingUtils.getLocalizedName
import its.reasoner.LearningSituation

object ValueToAnswerString {

    // ---------------------- Удобства ---------------------------

    @JvmStatic
    fun Any.toLocalizedString(learningSituation: LearningSituation, locCode : String) : String{
        return when(this){
            is Clazz -> this.toLocalizedString(learningSituation, locCode)
            is EnumValue -> this.toLocalizedString(learningSituation, locCode)
            is Boolean -> this.toLocalizedString(locCode)
            is Double -> this.toString()
            is Int -> this.toString()
            is String -> this
            is Obj -> this.toLocalizedString(learningSituation, locCode)
            else -> throw ThisShouldNotHappen()
        }
    }

    @JvmStatic
    fun Any.toLocalizedString(situation: QuestioningSituation) : String{
        return toLocalizedString(situation, situation.localizationCode)
    }

    // ---------------------- Функции поведения ---------------------------
    private fun Clazz.toLocalizedString(situation: LearningSituation, locCode : String): String {
        return this@toLocalizedString.getLocalizedName(situation.domainModel, locCode)
    }

    private fun EnumValue.toLocalizedString(situation: LearningSituation, locCode : String): String {
        return when (this) {
            Comparison.Values.Greater -> Localization.getLocalization(locCode).GREATER
            Comparison.Values.Less -> Localization.getLocalization(locCode).LESS
            Comparison.Values.Equal -> Localization.getLocalization(locCode).EQUAL
            else -> this@toLocalizedString.getLocalizedName(situation.domainModel, locCode)

        }
    }

    private fun Boolean.toLocalizedString(locCode : String): String {
        return if(this) Localization.getLocalization(locCode).YES else Localization.getLocalization(locCode).NO
    }

    private fun Obj.toLocalizedString(situation: LearningSituation, locCode : String): String {
        return this.getLocalizedName(situation.domainModel, locCode)
    }
}