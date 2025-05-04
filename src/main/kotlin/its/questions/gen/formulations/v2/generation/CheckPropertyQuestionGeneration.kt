package its.questions.gen.formulations.v2.generation

import its.model.definition.PropertyDef
import its.model.definition.types.Obj
import its.model.definition.types.ObjectType
import its.model.expressions.Operator
import its.model.expressions.operators.GetPropertyValue
import its.questions.gen.formulations.Localization
import its.questions.gen.formulations.TemplatingUtils.getLocalizedName
import its.questions.gen.formulations.v2.AbstractContext
import its.reasoner.LearningSituation
import its.reasoner.operators.OperatorReasoner

class CheckPropertyQuestionGeneration(learningSituation: LearningSituation, val localization: Localization) :
    AbstractQuestionGeneration<CheckPropertyQuestionGeneration.CheckPropertyContext>(learningSituation) {

    override fun generate(context: CheckPropertyContext): String {
        val question = context.propertyDef.metadata[localization.codePrefix, "question"]
        if (question != null && question is String) {
            return question // TODO тут должен быть вызов шаблонизатора
        }
        return localization.CHECK_OBJ_PROPERTY_OR_CLASS(
            context.propertyDef.metadata.getString(localization.codePrefix, "name"),
            (context.objExpr.use(OperatorReasoner.defaultReasoner(learningSituation)) as Obj)
                .findIn(learningSituation.domainModel)!!
                .getLocalizedName(localization.codePrefix)
        )
    }

    override fun fits(operator: Operator): CheckPropertyContext? {
        if (operator is GetPropertyValue) {
            val objectType = operator.objectExpr.resolvedType(learningSituation.domainModel) as ObjectType
            val classDef = objectType.findIn(learningSituation.domainModel)
            val propertyDef = classDef.findPropertyDef(operator.propertyName)!!

            return CheckPropertyContext(operator.objectExpr, propertyDef)
        }
        return null
    }
    class CheckPropertyContext(
        val objExpr: Operator, // переменная или любое другое выражение
        val propertyDef: PropertyDef, // для получения мета данных (локализации и тд)
    ) : AbstractContext
}