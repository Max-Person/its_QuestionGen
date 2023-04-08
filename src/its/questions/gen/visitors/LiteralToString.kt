package its.questions.gen.visitors

import its.model.DomainModel
import its.model.expressions.Literal
import its.model.expressions.literals.*
import its.model.expressions.types.ComparisonResult
import its.model.expressions.visitors.LiteralBehaviour
import its.questions.inputs.LearningSituation
import its.questions.inputs.QClassModel
import its.questions.inputs.QEnumModel
import its.questions.inputs.usesQDictionaries

class LiteralToString private constructor(val q: LearningSituation) : LiteralBehaviour<String> {
    init {
        require(DomainModel.usesQDictionaries())
    }

    // ---------------------- Удобства ---------------------------

    companion object _static{
        @JvmStatic
        fun Literal.toAnswerString(q: LearningSituation) : String{
            return this.use(LiteralToString(q))
        }
    }

    // ---------------------- Функции поведения ---------------------------

    override fun process(literal: BooleanLiteral): String {
        return if(literal.value.toBoolean()) "Да" else "Нет"
    }

    override fun process(literal: ClassLiteral): String {
        return (DomainModel.classesDictionary.get(literal.value) as QClassModel).textName
    }

    override fun process(literal: ComparisonResultLiteral): String {
        val comparison = ComparisonResult.fromString(literal.value)
        return when (comparison) {
            ComparisonResult.Greater -> "Больше"
            ComparisonResult.Less -> "Меньше"
            ComparisonResult.Equal -> "Равно"
            ComparisonResult.NotEqual -> "Не равно"
            else -> "Невозможно определить"
        }
    }

    override fun process(literal: DecisionTreeVarLiteral): String {
        return q.entityDictionary.getByVariable(literal.value)!!.specificName
    }

    override fun process(literal: DoubleLiteral): String {
        return literal.value
    }

    override fun process(literal: EnumLiteral): String {
        val enum = (DomainModel.enumsDictionary.get(literal.owner) as QEnumModel)
        return enum.textValues[enum.values.indexOf(literal.value)]
    }

    override fun process(literal: IntegerLiteral): String {
        return literal.value
    }

    override fun process(literal: ObjectLiteral): String {
        return q.entityDictionary.get(literal.value)!!.specificName
    }

    override fun process(literal: PropertyLiteral): String {
        TODO("Не должна возникнуть необходимость преобразовывать литерал свойства в строку")
    }

    override fun process(literal: RelationshipLiteral): String {
        TODO("Не должна возникнуть необходимость преобразовывать литерал отношения в строку")
    }

    override fun process(literal: StringLiteral): String {
        return literal.value
    }
}