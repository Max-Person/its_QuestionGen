package its.questions.gen.formulations.v2.generation.constant

import its.model.definition.PropertyDef
import its.model.definition.types.NumericType
import its.model.definition.types.Obj
import its.model.definition.types.Type
import its.model.expressions.Operator
import its.model.expressions.literals.ValueLiteral
import its.model.expressions.operators.CompareWithComparisonOperator
import its.questions.gen.formulations.Localization
import its.questions.gen.formulations.TemplatingUtils.getLocalizedName
import its.questions.gen.visitors.ValueToAnswerString.toLocalizedString
import its.reasoner.LearningSituation
import its.reasoner.operators.OperatorReasoner

class CompareWithNumeric(learningSituation: LearningSituation, localization: Localization) : CompareWithConstant(learningSituation, localization) {
    override fun createContext(
        valueLiteral: ValueLiteral<*, *>,
        operator: CompareWithComparisonOperator.ComparisonOperator?,
        objExpr: Operator,
        propertyDef: PropertyDef,
        paramsMap: Map<String, Operator>
    ): CompareWithConstantContext {
        return CompareWithNumericConstantContext(valueLiteral, operator, objExpr, propertyDef, paramsMap)
    }

    override fun typeFits(type: Type<*>): Boolean {
        return type is NumericType
    }
}

class CompareWithNumericConstantContext(
    valueConstant: ValueLiteral<*, *>, // справа от оператора сравнения
    operator: CompareWithComparisonOperator.ComparisonOperator?, // оператор
    objExpr: Operator, // переменная
    propertyDef: PropertyDef, // для получения мета данных (локализации и тд)
    paramsMap: Map<String, Operator>
) : CompareWithConstantContext(valueConstant, operator, objExpr, propertyDef, paramsMap) {
    override fun generate(learningSituation: LearningSituation, localization: Localization): String? {
        if (operator != null) {
            if (operator == CompareWithComparisonOperator.ComparisonOperator.Equal ||
                operator == CompareWithComparisonOperator.ComparisonOperator.NotEqual) {
                val questionOrAssertion = getQuestionOrAssertion(learningSituation, localization)
                if (questionOrAssertion != null) {
                    return questionOrAssertion
                }
            }
            return localization.COMPARE_A_PROPERTY_TO_A_NUMERIC_CONST(
                propertyDef.getLocalizedName(localization.codePrefix), // получение названия свойства
                (objExpr.use(OperatorReasoner.defaultReasoner(learningSituation)) as Obj)
                    .findIn(learningSituation.domainModel)!!
                    .getLocalizedName(localization.codePrefix), // получение имени объекта, записанного в переменную
                valueConstant.value.toLocalizedString(learningSituation, localization.codePrefix), // получение значения константы
                operator // получение оператора
            )
        } else {
            return localization.COMPARE_A_PROPERTY(
                propertyDef.getLocalizedName(localization.codePrefix), // получение названия свойства
                (objExpr.use(OperatorReasoner.defaultReasoner(learningSituation)) as Obj)
                    .findIn(learningSituation.domainModel)!!
                    .getLocalizedName(localization.codePrefix), // получение имени объекта, записанного в переменную
                valueConstant.value.toLocalizedString(learningSituation, localization.codePrefix) // получение значения константы
            )
        }
    }

}