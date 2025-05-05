package its.questions.gen.formulations.v2.generation

import its.model.definition.RelationshipDef
import its.model.definition.types.ObjectType
import its.model.expressions.Operator
import its.model.expressions.operators.CheckRelationship
import its.questions.gen.formulations.Localization
import its.questions.gen.formulations.v2.AbstractContext
import its.reasoner.LearningSituation

class CheckExistenceOfRelationship(learningSituation: LearningSituation, val localization: Localization) : AbstractQuestionGeneration<CheckExistenceOfRelationship.CheckExistenceOfRelationshipContext>(learningSituation) {

    override fun generate(context: CheckExistenceOfRelationshipContext): String {
        val question = context.relationShipDef.metadata.getString(localization.codePrefix, "question")
        if (question != null) {
            return question // TODO тут должен быть вызов шаблонизатора
        }
        val assertion = context.relationShipDef.metadata.getString(localization.codePrefix, "assertion")
        return localization.IS_IT_TRUE_THAT(assertion!!) // TODO тут тоже шаблонизатор
    }

    override fun fits(operator: Operator): CheckExistenceOfRelationshipContext? {
        if (operator is CheckRelationship) {
            val objectType = operator.resolvedType(learningSituation.domainModel) as ObjectType
            val classDef = objectType.findIn(learningSituation.domainModel)
            val relDef = classDef.findRelationshipDef(operator.relationshipName)!!

            return CheckExistenceOfRelationshipContext(operator.subjectExpr, operator.objectExprs, relDef)
        }
        return null
    }

    class CheckExistenceOfRelationshipContext(val subjExpr : Operator, val objectExprs: List<Operator>, val relationShipDef: RelationshipDef,) : AbstractContext
}