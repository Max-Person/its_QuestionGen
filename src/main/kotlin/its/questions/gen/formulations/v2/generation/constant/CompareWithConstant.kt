package its.questions.gen.formulations.v2.generation.constant

import its.model.definition.PropertyDef
import its.model.definition.types.Obj
import its.model.definition.types.Type
import its.model.expressions.Operator
import its.model.expressions.literals.ValueLiteral
import its.model.expressions.operators.CompareWithComparisonOperator
import its.model.expressions.operators.GetPropertyValue
import its.questions.gen.formulations.Localization
import its.questions.gen.formulations.TemplatingUtils.getLocalizedName
import its.questions.gen.formulations.v2.AbstractContext
import its.questions.gen.formulations.v2.generation.AbstractQuestionGeneration
import its.reasoner.LearningSituation
import its.reasoner.operators.OperatorReasoner

abstract class CompareWithConstant(learningSituation: LearningSituation, val localization: Localization) :
    AbstractQuestionGeneration<CompareWithConstant.CompareWithConstantContext>(
        learningSituation
    ) {
    override fun generate(context: CompareWithConstantContext): String {
        val question = context.propertyDef.metadata.getString(localization.codePrefix, "question")
        if (question != null) {
            return question // TODO тут должен быть вызов шаблонизатора
        }
        val assertion = context.propertyDef.metadata.getString(localization.codePrefix, "assertion")
        if (assertion != null) {
            return localization.IS_IT_TRUE_THAT(assertion) // TODO тут тоже шаблонизатор
        }
        return localization.COMPARE_A_PROPERTY_TO_A_CONSTANT(
            context.propertyDef.metadata.getString(localization.codePrefix, "name")!!,
            (context.objExpr.use(OperatorReasoner.defaultReasoner(learningSituation)) as Obj)
                .findIn(learningSituation.domainModel)!!
                .getLocalizedName(localization.codePrefix),
            context.valueConstant.value.toString()
        )
    }

    override fun fits(operator: Operator): CompareWithConstantContext? {
        if (operator is CompareWithComparisonOperator
            && operator.firstExpr is GetPropertyValue
            && operator.secondExpr is ValueLiteral<*, *>
        ) {
            val propertyValue = operator.firstExpr as GetPropertyValue
            val objExpr = propertyValue.objectExpr
            val valueLiteral = operator.secondExpr as ValueLiteral<*, *>

            val propertyDef = propertyValue.getPropertyDef(learningSituation.domainModel)
            if (typeFits(propertyDef.type)) {
                return CompareWithConstantContext(
                    valueLiteral, operator.operator, objExpr, propertyDef
                )
            }

        }
        return null
    }

    protected abstract fun typeFits(type: Type<*>): Boolean

    class CompareWithConstantContext(
        val valueConstant: ValueLiteral<*, *>, // справа от оператора сравнения
        val operator: CompareWithComparisonOperator.ComparisonOperator, // оператор
        val objExpr: Operator, // переменная
        val propertyDef: PropertyDef, // для получения мета данных (локализации и тд)
    ) : AbstractContext

}