package its.questions.gen.formulations.v2.generation

import its.model.definition.PropertyDef
import its.model.definition.types.Obj
import its.model.expressions.Operator
import its.model.expressions.operators.GetPropertyValue
import its.questions.gen.formulations.Localization
import its.questions.gen.formulations.TemplatingUtils.getLocalizedName
import its.questions.gen.formulations.TemplatingUtils.interpret
import its.questions.gen.formulations.v2.AbstractContext
import its.reasoner.LearningSituation
import its.reasoner.operators.OperatorReasoner

class CheckPropertyQuestionGeneration(learningSituation: LearningSituation, localization: Localization) :
    AbstractQuestionGeneration<CheckPropertyQuestionGeneration.CheckPropertyContext>(learningSituation, localization) {

    override fun generate(context: CheckPropertyContext): String {
        val question = context.propertyDef.metadata.getString(localization.codePrefix, "question_check_property")
        val contextVars = mapOf(
            "obj" to context.objExpr.use(OperatorReasoner.defaultReasoner(learningSituation)) as Obj
        )
        if (question != null) {
            return question.interpret(learningSituation, localization.codePrefix, contextVars)
        }
        return localization.CHECK_OBJ_PROPERTY_OR_CLASS(
            context.propertyDef.getLocalizedName(localization.codePrefix),
            (context.objExpr.use(OperatorReasoner.defaultReasoner(learningSituation)) as Obj)
                .findIn(learningSituation.domainModel)!!
                .getLocalizedName(localization.codePrefix)
        )
    }

    override fun fits(operator: Operator): CheckPropertyContext? {
        if (operator is GetPropertyValue) {
            return CheckPropertyContext(operator.objectExpr, operator.getPropertyDef(learningSituation.domainModel))
        }
        return null
    }
    class CheckPropertyContext(
        val objExpr: Operator, // переменная или любое другое выражение
        val propertyDef: PropertyDef, // для получения мета данных (локализации и тд)
    ) : AbstractContext
}