package its.questions.gen.visitors

import its.model.expressions.types.*
import its.questions.gen.QuestioningSituation

object ValueToAnswerString : Types.ValueBehaviour<QuestioningSituation, String>() {

    // ---------------------- Удобства ---------------------------

    @JvmStatic
    fun Any.toAnswerString(situation: QuestioningSituation) : String{
        return this.exec(situation)
    }

    // ---------------------- Функции поведения ---------------------------
    override fun Clazz.exec(param: QuestioningSituation): String {
        return param.domainLocalization.localizedClassName(this)
    }

    override fun ComparisonResult.exec(param: QuestioningSituation): String {
        return when (this) {
            ComparisonResult.Greater -> param.localization.GREATER
            ComparisonResult.Less -> param.localization.LESS
            ComparisonResult.Equal -> param.localization.EQUAL
            ComparisonResult.NotEqual -> param.localization.NOT_EQUAL
            else -> param.localization.CANNOT_BE_DETERMINED
        }
    }

    override fun EnumValue.exec(param: QuestioningSituation): String {
        return param.domainLocalization.localizedEnumValue(this)
    }

    override fun Obj.exec(param: QuestioningSituation): String {
        TODO("Not yet implemented")
    }

    override fun Boolean.exec(param: QuestioningSituation): String {
        return if(this) param.localization.YES else param.localization.NO
    }

    override fun Double.exec(param: QuestioningSituation): String {
        return this.toString()
    }

    override fun Int.exec(param: QuestioningSituation): String {
        return this.toString()
    }

    override fun String.exec(param: QuestioningSituation): String {
        return this
    }
}