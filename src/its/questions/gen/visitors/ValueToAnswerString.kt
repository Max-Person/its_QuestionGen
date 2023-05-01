package its.questions.gen.visitors

import its.model.DomainModel
import its.model.expressions.types.*
import its.questions.inputs.LearningSituation
import its.questions.inputs.QClassModel
import its.questions.inputs.QEnumModel

object ValueToAnswerString : Types.ValueBehaviour<LearningSituation, String>() {

    // ---------------------- Удобства ---------------------------

    @JvmStatic
    fun Any.toAnswerString(situation: LearningSituation) : String{
        return this.exec(situation)
    }

    // ---------------------- Функции поведения ---------------------------
    override fun Clazz.exec(param: LearningSituation): String {
        return (DomainModel.classesDictionary.get(this.name) as QClassModel).textName
    }

    override fun ComparisonResult.exec(param: LearningSituation): String {
        return when (this) {
            ComparisonResult.Greater -> "Больше"
            ComparisonResult.Less -> "Меньше"
            ComparisonResult.Equal -> "Равно"
            ComparisonResult.NotEqual -> "Не равно"
            else -> "Невозможно определить"
        }
    }

    override fun EnumValue.exec(param: LearningSituation): String {
        val enum = (DomainModel.enumsDictionary.get(this.ownerEnum) as QEnumModel)
        return enum.textValues[enum.values.indexOf(this.value)]
    }

    override fun Obj.exec(param: LearningSituation): String {
        TODO("Not yet implemented")
    }

    override fun Boolean.exec(param: LearningSituation): String {
        return if(this) "Да" else "Нет"
    }

    override fun Double.exec(param: LearningSituation): String {
        return this.toString()
    }

    override fun Int.exec(param: LearningSituation): String {
        return this.toString()
    }

    override fun String.exec(param: LearningSituation): String {
        return this
    }
}