package its.questions.gen.formulations.v2.generation

import its.model.definition.PropertyDef
import its.model.definition.types.ObjectType
import its.model.expressions.ExpressionContext
import its.model.expressions.Operator
import its.model.expressions.operators.GetPropertyValue
import its.questions.gen.formulations.Localization
import its.questions.gen.formulations.v2.AbstractContext
import its.reasoner.LearningSituation

abstract class AbstractQuestionGeneration(protected val learningSituation: LearningSituation,
                                                               val localization: Localization) {
    abstract fun fits(operator: Operator) : AbstractContext?

    protected fun Operator.resolvedType(learningSituation: LearningSituation) =
        this.resolvedType(
            learningSituation.domainModel,
            ExpressionContext(
                decisionTreeVariableTypes = learningSituation.decisionTreeVariables.map { (name, value) ->
                    name to ObjectType(value.findIn(learningSituation.domainModel)!!.className)
                }.toMap()
            )
        )

    protected fun GetPropertyValue.getPropertyDef(learningSituation: LearningSituation): PropertyDef {
        val objectType = objectExpr.resolvedType(learningSituation) as ObjectType
        val classDef = objectType.findIn(learningSituation.domainModel)
        return classDef.findPropertyDef(propertyName)!!
    }
}