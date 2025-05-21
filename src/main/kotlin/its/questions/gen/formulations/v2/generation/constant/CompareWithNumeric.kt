package its.questions.gen.formulations.v2.generation.constant

import its.model.definition.types.NumericType
import its.model.definition.types.Obj
import its.model.definition.types.Type
import its.model.expressions.operators.CompareWithComparisonOperator
import its.questions.gen.formulations.Localization
import its.questions.gen.formulations.TemplatingUtils.getLocalizedName
import its.questions.gen.visitors.ValueToAnswerString.toLocalizedString
import its.reasoner.LearningSituation
import its.reasoner.operators.OperatorReasoner

class CompareWithNumeric(learningSituation: LearningSituation, localization: Localization) : CompareWithConstant(learningSituation, localization) {
    override fun typeFits(type: Type<*>): Boolean {
        return type is NumericType
    }

    override fun generate(context: CompareWithConstantContext): String? {
        if (context.operator != null) {
            if (context.operator == CompareWithComparisonOperator.ComparisonOperator.Equal ||
                context.operator == CompareWithComparisonOperator.ComparisonOperator.NotEqual) {
                val questionOrAssertion = getQuestionOrAssertion(context)
                if (questionOrAssertion != null) {
                    return questionOrAssertion
                }
            }
            return localization.COMPARE_A_PROPERTY_TO_A_NUMERIC_CONST(
                context.propertyDef.getLocalizedName(localization.codePrefix), // получение названия свойства
                (context.objExpr.use(OperatorReasoner.defaultReasoner(learningSituation)) as Obj)
                    .findIn(learningSituation.domainModel)!!
                    .getLocalizedName(localization.codePrefix), // получение имени объекта, записанного в переменную
                context.valueConstant.value.toLocalizedString(learningSituation, localization.codePrefix), // получение значения константы
                context.operator // получение оператора
            )
        } else {
            return localization.COMPARE_A_PROPERTY(
                context.propertyDef.getLocalizedName(localization.codePrefix), // получение названия свойства
                (context.objExpr.use(OperatorReasoner.defaultReasoner(learningSituation)) as Obj)
                    .findIn(learningSituation.domainModel)!!
                    .getLocalizedName(localization.codePrefix), // получение имени объекта, записанного в переменную
                context.valueConstant.value.toLocalizedString(learningSituation, localization.codePrefix) // получение значения константы
            )
        }
    }
}