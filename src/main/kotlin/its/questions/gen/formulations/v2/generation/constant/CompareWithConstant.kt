package its.questions.gen.formulations.v2.generation.constant

import its.model.definition.PropertyDef
import its.model.definition.types.Obj
import its.model.definition.types.Type
import its.model.expressions.Operator
import its.model.expressions.literals.ValueLiteral
import its.model.expressions.operators.Compare
import its.model.expressions.operators.CompareWithComparisonOperator
import its.model.expressions.operators.GetPropertyValue
import its.questions.gen.formulations.Localization
import its.questions.gen.formulations.TemplatingUtils.getLocalizedName
import its.questions.gen.formulations.TemplatingUtils.interpret
import its.questions.gen.formulations.v2.AbstractContext
import its.questions.gen.formulations.v2.generation.AbstractQuestionGeneration
import its.questions.gen.visitors.ValueToAnswerString.toLocalizedString
import its.reasoner.LearningSituation
import its.reasoner.operators.OperatorReasoner

abstract class CompareWithConstant(learningSituation: LearningSituation, localization: Localization) :
    AbstractQuestionGeneration<CompareWithConstant.CompareWithConstantContext>(
        learningSituation,
        localization
    ) {
    override fun generate(context: CompareWithConstantContext): String? {
        val questionOrAssertion = getQuestionOrAssertion(context)
        if (questionOrAssertion != null) {
            return questionOrAssertion
        }
        val obj = context.objExpr.use(OperatorReasoner.defaultReasoner(learningSituation)) as Obj
        return localization.COMPARE_A_PROPERTY_TO_A_CONSTANT(
            context.propertyDef.getLocalizedName(localization.codePrefix),
            obj.findIn(learningSituation.domainModel)!!
                .getLocalizedName(localization.codePrefix),
            context.valueConstant.value.toLocalizedString(learningSituation, localization.codePrefix)
        )
    }

    protected fun getQuestionOrAssertion(context: CompareWithConstantContext): String? {
        val question = context.propertyDef.metadata.getString(localization.codePrefix, "question")
        val obj = context.objExpr.use(OperatorReasoner.defaultReasoner(learningSituation)) as Obj
        val contextVars = mutableMapOf(
            "obj" to obj,
            "value" to context.valueConstant.value
        )
        context.paramsMap.forEach { (paramName, operator) ->
            contextVars[paramName] = operator.use(OperatorReasoner.defaultReasoner(learningSituation))!!
        }
        if (question != null) {
            return question.interpret(learningSituation, localization.codePrefix, contextVars)
        }
        val assertion = context.propertyDef.metadata.getString(localization.codePrefix, "assertion")
        if (assertion != null) {
            val interpreted = assertion.interpret(learningSituation, localization.codePrefix, contextVars)
            return localization.IS_IT_TRUE_THAT(interpreted)
        }
        return null
    }

    override fun generateAnswer(context: CompareWithConstantContext, value: Any): String? {
        var valueToPass = value
        if ((context.operator == CompareWithComparisonOperator.ComparisonOperator.NotEqual ||
                    context.operator == CompareWithComparisonOperator.ComparisonOperator.GreaterEqual ||
                    context.operator == CompareWithComparisonOperator.ComparisonOperator.LessEqual) && value is Boolean) {
            valueToPass = !value
        }
        return super.generateAnswer(context, valueToPass)
    }

    override fun fits(operator: Operator): CompareWithConstantContext? {
        if (operator is CompareWithComparisonOperator
            && operator.firstExpr is GetPropertyValue
            && operator.secondExpr is ValueLiteral<*, *>
        ) {
            val propertyValue = operator.firstExpr as GetPropertyValue
            val objExpr = propertyValue.objectExpr
            val valueLiteral = operator.secondExpr as ValueLiteral<*, *>

            val propertyDef = propertyValue.getPropertyDef(learningSituation.domainModel)
            val paramsMap = propertyValue.paramsValues.asMap(propertyDef.paramsDecl)
            if (typeFits(propertyDef.type)) {
                return CompareWithConstantContext(valueLiteral, operator.operator, objExpr, propertyDef, paramsMap)
            }
        }
        if (operator is Compare
            && operator.firstExpr is GetPropertyValue
            && operator.secondExpr is ValueLiteral<*, *>
        ) {

            val propertyValue = operator.firstExpr as GetPropertyValue
            val objExpr = propertyValue.objectExpr
            val valueLiteral = operator.secondExpr as ValueLiteral<*, *>

            val propertyDef = propertyValue.getPropertyDef(learningSituation.domainModel)
            val paramsMap = propertyValue.paramsValues.asMap(propertyDef.paramsDecl)
            if (typeFits(propertyDef.type)) {
                return CompareWithConstantContext(valueLiteral, null, objExpr, propertyDef, paramsMap)
            }
        }
        return null
    }

    protected abstract fun typeFits(type: Type<*>): Boolean

    class CompareWithConstantContext(
        val valueConstant: ValueLiteral<*, *>, // справа от оператора сравнения
        val operator: CompareWithComparisonOperator.ComparisonOperator?, // оператор
        val objExpr: Operator, // переменная
        val propertyDef: PropertyDef, // для получения мета данных (локализации и тд)
        val paramsMap: Map<String, Operator>
    ) : AbstractContext

}