package its.questions.gen.visitors

import its.model.definition.ThisShouldNotHappen
import its.model.definition.types.Clazz
import its.model.definition.types.Comparison
import its.model.definition.types.EnumValue
import its.questions.gen.QuestioningSituation

object ValueToAnswerString {

    // ---------------------- Удобства ---------------------------

    @JvmStatic
    fun Any.toAnswerString(situation: QuestioningSituation) : String{
        return when(this){
            is Clazz -> this.toAnswerString(situation)
            is EnumValue -> this.toAnswerString(situation)
            is Boolean -> this.toAnswerString(situation)
            is Double -> this.toAnswerString(situation)
            is Int -> this.toAnswerString(situation)
            is String -> this.toAnswerString(situation)
            else -> throw ThisShouldNotHappen()
        }
    }

    // ---------------------- Функции поведения ---------------------------
    private fun Clazz.toAnswerString(situation: QuestioningSituation): String {
        with(situation.formulations){
            return this@toAnswerString.localizedName
        }
    }

    private fun EnumValue.toAnswerString(situation: QuestioningSituation): String {
        return when (this) {
            Comparison.Values.Greater -> situation.localization.GREATER
            Comparison.Values.Less -> situation.localization.LESS
            Comparison.Values.Equal -> situation.localization.EQUAL
            else -> with(situation.formulations){
                this@toAnswerString.localizedName
            }
        }
    }

    private fun Boolean.toAnswerString(situation: QuestioningSituation): String {
        return if(this) situation.localization.YES else situation.localization.NO
    }

    private fun Double.toAnswerString(situation: QuestioningSituation): String {
        return this.toString()
    }

    private fun Int.toAnswerString(situation: QuestioningSituation): String {
        return this.toString()
    }

    private fun String.toAnswerString(situation: QuestioningSituation): String {
        return this
    }
}