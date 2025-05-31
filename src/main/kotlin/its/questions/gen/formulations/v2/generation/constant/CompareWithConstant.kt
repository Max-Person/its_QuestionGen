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
import its.questions.gen.formulations.TemplatingUtils.interpretTopLevel
import its.questions.gen.formulations.TemplatingUtils.topLevelLlmCleanup
import its.questions.gen.formulations.v2.AbstractContext
import its.questions.gen.formulations.v2.generation.AbstractQuestionGeneration
import its.questions.gen.visitors.ValueToAnswerString.toLocalizedString
import its.reasoner.LearningSituation
import its.reasoner.operators.OperatorReasoner

abstract class CompareWithConstant(learningSituation: LearningSituation, localization: Localization) :
    AbstractQuestionGeneration(
        learningSituation,
        localization
    ) {

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
                return createContext(valueLiteral, operator.operator, objExpr, propertyDef, paramsMap)
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
                return createContext(valueLiteral, null, objExpr, propertyDef, paramsMap)
            }
        }
        return null
    }

    protected open fun createContext(valueLiteral: ValueLiteral<*, *>,
                                     operator: CompareWithComparisonOperator.ComparisonOperator?,
                                     objExpr: Operator,
                                     propertyDef: PropertyDef,
                                     paramsMap: Map<String, Operator>) : CompareWithConstantContext  {
        return CompareWithConstantContext(valueLiteral, operator, objExpr, propertyDef, paramsMap)
    }

    protected abstract fun typeFits(type: Type<*>): Boolean
}

open class CompareWithConstantContext(
    val valueConstant: ValueLiteral<*, *>, // справа от оператора сравнения
    val operator: CompareWithComparisonOperator.ComparisonOperator?, // оператор
    val objExpr: Operator, // переменная
    val propertyDef: PropertyDef, // для получения мета данных (локализации и тд)
    val paramsMap: Map<String, Operator>
) : AbstractContext {
    override fun generate(learningSituation: LearningSituation, localization: Localization): String? {
        val questionOrAssertion = getQuestionOrAssertion(learningSituation, localization)
        if (questionOrAssertion != null) {
            return questionOrAssertion
        }
        val obj = objExpr.use(OperatorReasoner.defaultReasoner(learningSituation)) as Obj
        return localization.COMPARE_A_PROPERTY_TO_A_CONSTANT(
            propertyDef.getLocalizedName(localization.codePrefix),
            obj.findIn(learningSituation.domainModel)!!
                .getLocalizedName(localization.codePrefix),
            valueConstant.value.toLocalizedString(learningSituation, localization.codePrefix)
        ).topLevelLlmCleanup()
    }

    protected fun getQuestionOrAssertion(learningSituation: LearningSituation, localization: Localization): String? {
        val question = propertyDef.metadata.getString(localization.codePrefix, "compareValueQuestion")
        val obj = objExpr.use(OperatorReasoner.defaultReasoner(learningSituation)) as Obj
        val contextVars = mutableMapOf(
            "obj" to obj,
            "value" to valueConstant.value
        )
        paramsMap.forEach { (paramName, operator) ->
            contextVars[paramName] = operator.use(OperatorReasoner.defaultReasoner(learningSituation))!!
        }
        if (question != null) {
            return question.interpretTopLevel(learningSituation, localization.codePrefix, contextVars)
        }
        val assertion = propertyDef.metadata.getString(localization.codePrefix, "assertion")
        if (assertion != null) {
            val interpreted = assertion.interpret(learningSituation, localization.codePrefix, contextVars)
            return localization.IS_IT_TRUE_THAT(interpreted).topLevelLlmCleanup()
        }
        return null
    }

    override fun generateAnswer(learningSituation: LearningSituation, localization: Localization, value : Any): String? {
        var valueToPass = value
        if ((operator == CompareWithComparisonOperator.ComparisonOperator.NotEqual ||
                    operator == CompareWithComparisonOperator.ComparisonOperator.GreaterEqual ||
                    operator == CompareWithComparisonOperator.ComparisonOperator.LessEqual) && value is Boolean) {
            valueToPass = !value
        }
        return super.generateAnswer(learningSituation, localization, valueToPass)
    }
}