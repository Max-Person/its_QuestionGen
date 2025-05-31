package its.questions.gen.formulations.v2.generation

import its.model.definition.RelationshipDef
import its.model.definition.types.ObjectType
import its.model.expressions.Operator
import its.model.expressions.operators.CheckRelationship
import its.questions.gen.formulations.Localization
import its.questions.gen.formulations.TemplatingUtils.interpretTopLevel
import its.questions.gen.formulations.v2.AbstractContext
import its.reasoner.LearningSituation
import its.reasoner.operators.OperatorReasoner

class CheckExistenceOfRelationship(learningSituation: LearningSituation, localization: Localization) :
    AbstractQuestionGeneration(
        learningSituation,
        localization
    ) {

    override fun fits(operator: Operator): CheckExistenceOfRelationshipContext? {
        if (operator is CheckRelationship) {
            val objectType = operator.subjectExpr.resolvedType(learningSituation.domainModel) as ObjectType
            val classDef = objectType.findIn(learningSituation.domainModel)
            val relDef = classDef.findRelationshipDef(operator.relationshipName)!!
            val paramsMap = operator.paramsValues.asMap(relDef.effectiveParams)
            return CheckExistenceOfRelationshipContext(operator.subjectExpr, operator.objectExprs, relDef, paramsMap)
        }
        return null
    }
}
class CheckExistenceOfRelationshipContext(
    val subjExpr: Operator,
    val objectExprs: List<Operator>,
    val relationShipDef: RelationshipDef,
    val paramsMap: Map<String, Operator>
) : AbstractContext {
    override fun generate(learningSituation: LearningSituation, localization: Localization): String? {
        val question = relationShipDef.metadata.getString(localization.codePrefix, "question")
        val contextVars = mutableMapOf(
            "subj" to subjExpr.use(OperatorReasoner.defaultReasoner(learningSituation))!!
        )
        objectExprs.forEachIndexed { index, operator ->
            contextVars["obj${index + 1}"] = operator.use(OperatorReasoner.defaultReasoner(learningSituation))!!
        }
        paramsMap.forEach { (paramName, operator) ->
            contextVars[paramName] = operator.use(OperatorReasoner.defaultReasoner(learningSituation))!!
        }
        if (question != null) {
            return question.interpretTopLevel(learningSituation, localization.codePrefix, contextVars)
        }
        val assertion = relationShipDef.metadata.getString(localization.codePrefix, "assertion")
        if (assertion != null) {
            val interpreted = assertion.interpretTopLevel(learningSituation, localization.codePrefix, contextVars)
            return localization.IS_IT_TRUE_THAT(interpreted)
        }
        return null
    }
}