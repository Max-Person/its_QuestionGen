package its.questions.gen.formulations.v2.generation

import its.model.definition.PropertyDef
import its.model.definition.types.BooleanType
import its.model.definition.types.Obj
import its.model.expressions.Operator
import its.model.expressions.literals.BooleanLiteral
import its.model.expressions.operators.CompareWithComparisonOperator
import its.model.expressions.operators.GetPropertyValue
import its.model.expressions.operators.LogicalNot
import its.questions.gen.formulations.Localization
import its.questions.gen.formulations.TemplatingUtils.getLocalizedName
import its.questions.gen.formulations.TemplatingUtils.interpret
import its.questions.gen.formulations.TemplatingUtils.interpretTopLevel
import its.questions.gen.formulations.TemplatingUtils.topLevelLlmCleanup
import its.questions.gen.formulations.v2.AbstractContext
import its.questions.gen.formulations.v2.generation.constant.CompareWithConstantContext
import its.questions.gen.visitors.ValueToAnswerString.toLocalizedString
import its.reasoner.LearningSituation
import its.reasoner.operators.OperatorReasoner

class CheckPropertyQuestionGeneration(learningSituation: LearningSituation, localization: Localization) :
    AbstractQuestionGeneration(learningSituation, localization) {

    override fun fits(operator: Operator): AbstractContext? {
        if (operator is GetPropertyValue) {
            val propertyDef = operator.getPropertyDef(learningSituation)
            val paramsMap = operator.paramsValues.asMap(propertyDef.paramsDecl)
            if (propertyDef.type is BooleanType) {
                return CompareWithConstantContext(
                    BooleanLiteral(true),
                    CompareWithComparisonOperator.ComparisonOperator.Equal,
                    operator.objectExpr,
                    propertyDef,
                    paramsMap
                )
            }
            return CheckPropertyContext(operator.objectExpr, propertyDef, paramsMap)
        } else if (operator is LogicalNot && operator.operandExpr is GetPropertyValue) {
            val getPropertyValue = operator.operandExpr as GetPropertyValue
            val propertyDef = getPropertyValue.getPropertyDef(learningSituation)
            val paramsMap = getPropertyValue.paramsValues.asMap(propertyDef.paramsDecl)
            return CompareWithConstantContext(
                BooleanLiteral(false),
                CompareWithComparisonOperator.ComparisonOperator.Equal,
                getPropertyValue.objectExpr,
                propertyDef,
                paramsMap
            )
        }
        return null
    }
}

class CheckPropertyContext(
    val objExpr: Operator, // переменная или любое другое выражение
    val propertyDef: PropertyDef, // для получения мета данных (локализации и тд)
    val paramsMap: Map<String, Operator>
) : AbstractContext {
    override fun generate(learningSituation: LearningSituation, localization: Localization): String? {
        val question = propertyDef.metadata.getString(localization.codePrefix, "question")
        val contextVars = mutableMapOf(
            "obj" to objExpr.use(OperatorReasoner.defaultReasoner(learningSituation))!!
        )
        paramsMap.forEach { (paramName, operator) ->
            contextVars[paramName] = operator.use(OperatorReasoner.defaultReasoner(learningSituation))!!
        }
        if (question != null) {
            return question.interpretTopLevel(learningSituation, localization.codePrefix, contextVars)
        }
        return localization.CHECK_OBJ_PROPERTY_OR_CLASS(
            propertyDef.getLocalizedName(localization.codePrefix),
            (objExpr.use(OperatorReasoner.defaultReasoner(learningSituation)) as Obj)
                .findIn(learningSituation.domainModel)!!
                .getLocalizedName(localization.codePrefix)
        ).topLevelLlmCleanup()
    }

    override fun generateExplanation(
        learningSituation: LearningSituation,
        localization: Localization,
        correctAnswer: Any
    ): String {
        val assertion = propertyDef.metadata.getString(localization.codePrefix, "assertion")
        val obj = objExpr.use(OperatorReasoner.defaultReasoner(learningSituation)) as Obj
        val params = paramsMap.map { (name, expr) -> name to expr.use(OperatorReasoner.defaultReasoner(learningSituation))!!}.toMap()
        val value = obj.findIn(learningSituation.domainModel)!!.getPropertyValue(propertyDef.name, params)
        if (assertion != null) {
            val contextVars = mutableMapOf(
                "obj" to obj,
                "value" to value
            )
            contextVars.putAll(params)
            val interpreted = assertion.interpret(learningSituation, localization.codePrefix, contextVars)
            return localization.THATS_INCORRECT_BECAUSE(interpreted).topLevelLlmCleanup()
        }
        return localization.THATS_INCORRECT_BECAUSE(
            localization.COMPARE_PROP_EXPL(
                propertyDef.getLocalizedName(localization.codePrefix),
                obj.findIn(learningSituation.domainModel)!!
                    .getLocalizedName(localization.codePrefix),
                value.toLocalizedString(learningSituation, localization.codePrefix)
            )
        ).topLevelLlmCleanup()
    }
}