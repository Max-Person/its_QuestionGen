package its.questions.gen.formulations.v2.generation

import its.model.definition.RelationshipDef
import its.model.definition.types.Obj
import its.model.definition.types.ObjectType
import its.model.expressions.Operator
import its.model.expressions.operators.CheckRelationship
import its.questions.gen.formulations.Localization
import its.questions.gen.formulations.TemplatingUtils.interpret
import its.questions.gen.formulations.v2.AbstractContext
import its.reasoner.LearningSituation
import its.reasoner.operators.OperatorReasoner

class CheckExistenceOfRelationship(learningSituation: LearningSituation, val localization: Localization) : AbstractQuestionGeneration<CheckExistenceOfRelationship.CheckExistenceOfRelationshipContext>(learningSituation) {

    override fun generate(context: CheckExistenceOfRelationshipContext): String? {
        val question = context.relationShipDef.metadata.getString(localization.codePrefix, "question")
        val contextVars = mutableMapOf(
            "subj" to context.subjExpr.use(OperatorReasoner.defaultReasoner(learningSituation)) as Obj,
        )
        context.objectExprs.forEachIndexed{index, operator ->
            contextVars["obj${index+1}"] = operator.use(OperatorReasoner.defaultReasoner(learningSituation)) as Obj
        }
        if (question != null) {
            return question.interpret(learningSituation, localization.codePrefix, contextVars)
        }
        val assertion = context.relationShipDef.metadata.getString(localization.codePrefix, "assertion")
        if (assertion != null) {
            val interpreted = assertion.interpret(learningSituation, localization.codePrefix, contextVars)
            return localization.IS_IT_TRUE_THAT(interpreted)
        }
        return null
    }

    override fun fits(operator: Operator): CheckExistenceOfRelationshipContext? {
        if (operator is CheckRelationship) {
            val objectType = operator.subjectExpr.resolvedType(learningSituation.domainModel) as ObjectType
            val classDef = objectType.findIn(learningSituation.domainModel)
            val relDef = classDef.findRelationshipDef(operator.relationshipName)!!

            return CheckExistenceOfRelationshipContext(operator.subjectExpr, operator.objectExprs, relDef)
        }
        return null
    }

    class CheckExistenceOfRelationshipContext(val subjExpr : Operator, val objectExprs: List<Operator>, val relationShipDef: RelationshipDef) : AbstractContext
}