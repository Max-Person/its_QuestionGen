package its.questions.gen.formulations.v2.generation.constant

import its.model.definition.types.NumericType
import its.model.definition.types.Type
import its.questions.gen.formulations.Localization
import its.questions.gen.formulations.TemplatingUtils.getLocalizedName
import its.reasoner.LearningSituation

class CompareWithNumeric(learningSituation: LearningSituation, localization: Localization) : CompareWithConstant(learningSituation, localization) {
    override fun typeFits(type: Type<*>): Boolean {
        return type is NumericType
    }

    override fun generate(context: CompareWithConstantContext): String {
        return localization.COMPARE_A_PROPERTY_TO_A_NUMERIC_CONST(
            context.propertyDef.metadata.getString(localization.codePrefix, "name"), // получение названия свойства
            learningSituation
                .decisionTreeVariables[context.varLiteral.name]!!
                .findIn(learningSituation.domainModel)!!
                .getLocalizedName(localization.codePrefix), // получение имени объекта, записанного в переменную
            context.valueConstant.value.toString(), // получение значения константы
            context.operator // получение оператора
        )
    }
}