package its.questions.gen.formulations.v2.generation

import its.model.definition.PropertyDef
import its.model.definition.types.BooleanType
import its.model.definition.types.Obj
import its.model.expressions.Operator
import its.model.expressions.literals.BooleanLiteral
import its.model.expressions.operators.CompareWithComparisonOperator
import its.model.expressions.operators.GetPropertyValue
import its.model.expressions.operators.LogicalNot
import its.model.expressions.utils.ParamsValuesExprList
import its.questions.gen.formulations.Localization
import its.questions.gen.formulations.TemplatingUtils.getLocalizedName
import its.questions.gen.formulations.TemplatingUtils.interpret
import its.questions.gen.formulations.v2.AbstractContext
import its.questions.gen.formulations.v2.generation.constant.CompareWithBoolean
import its.reasoner.LearningSituation
import its.reasoner.operators.OperatorReasoner

class CheckPropertyQuestionGeneration(learningSituation: LearningSituation, localization: Localization) :
    AbstractQuestionGeneration<CheckPropertyQuestionGeneration.CheckPropertyContext>(learningSituation, localization) {

    override fun generate(context: CheckPropertyContext): String? {
        if (context.propertyDef.type is BooleanType) {
            val questionGenerator = CompareWithBoolean(learningSituation, localization)
            return questionGenerator.generateQuestion(
                CompareWithComparisonOperator(
                    GetPropertyValue(context.objExpr, context.propertyDef.name, context.paramsValues),
                    CompareWithComparisonOperator.ComparisonOperator.Equal,
                    BooleanLiteral(!context.isInverted)
                ))
        }
        val question = context.propertyDef.metadata.getString(localization.codePrefix, "question_check_property")
        val contextVars = mutableMapOf(
            "obj" to context.objExpr.use(OperatorReasoner.defaultReasoner(learningSituation))!!
        )
        context.paramsMap.forEach { (paramName, operator) ->
            contextVars[paramName] = operator.use(OperatorReasoner.defaultReasoner(learningSituation))!!
        }
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
            val propertyDef = operator.getPropertyDef(learningSituation.domainModel)
            val paramsMap = operator.paramsValues.asMap(propertyDef.paramsDecl)
            return CheckPropertyContext(operator.objectExpr, propertyDef, false, paramsMap, operator.paramsValues)
        } else if (operator is LogicalNot && operator.operandExpr is GetPropertyValue) {
            val getPropertyValue = operator.operandExpr as GetPropertyValue
            val propertyDef = getPropertyValue.getPropertyDef(learningSituation.domainModel)
            val paramsMap = getPropertyValue.paramsValues.asMap(propertyDef.paramsDecl)
            return CheckPropertyContext(
                getPropertyValue.objectExpr,
                propertyDef,
                true,
                paramsMap,
                getPropertyValue.paramsValues)
        }
        return null
    }
    class CheckPropertyContext(
        val objExpr: Operator, // переменная или любое другое выражение
        val propertyDef: PropertyDef, // для получения мета данных (локализации и тд)
        val isInverted: Boolean = false,
        val paramsMap: Map<String, Operator>,
        val  paramsValues: ParamsValuesExprList
    ) : AbstractContext
}