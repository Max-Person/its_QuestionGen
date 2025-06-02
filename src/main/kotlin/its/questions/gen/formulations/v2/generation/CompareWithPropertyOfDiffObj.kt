package its.questions.gen.formulations.v2.generation

import its.model.definition.PropertyDef
import its.model.definition.types.Comparison
import its.model.definition.types.Obj
import its.model.definition.types.ObjectType
import its.model.expressions.Operator
import its.model.expressions.operators.Compare
import its.model.expressions.operators.CompareWithComparisonOperator
import its.model.expressions.operators.GetPropertyValue
import its.questions.gen.formulations.Localization
import its.questions.gen.formulations.TemplatingUtils.getLocalizedName
import its.questions.gen.formulations.TemplatingUtils.topLevelLlmCleanup
import its.questions.gen.formulations.TemplatingUtils.interpret
import its.questions.gen.formulations.v2.AbstractContext
import its.questions.gen.visitors.ValueToAnswerString.toLocalizedString
import its.reasoner.LearningSituation
import its.reasoner.operators.OperatorReasoner

class CompareWithPropertyOfDiffObj(learningSituation: LearningSituation, localization: Localization) :
    AbstractQuestionGeneration(learningSituation, localization) {

    override fun fits(operator: Operator): ComparePropertyOfDiffObjContext? {
        if (operator is CompareWithComparisonOperator &&
            operator.firstExpr is GetPropertyValue && operator.secondExpr is GetPropertyValue
        ) {
            val getPropVal1 = operator.firstExpr as GetPropertyValue
            val getPropVal2 = operator.secondExpr as GetPropertyValue
            if (!getPropVal1.paramsValues.getExprList().isEmpty() && !getPropVal2.paramsValues.getExprList().isEmpty()) {
                return null
            }
            val objectType1 = getPropVal1.objectExpr.resolvedType(learningSituation.domainModel) as ObjectType
            val classDef1 = objectType1.findIn(learningSituation.domainModel)
            val propertyDef1 = classDef1.findPropertyDef(getPropVal1.propertyName)!!

            val objectType2 = getPropVal2.objectExpr.resolvedType(learningSituation.domainModel) as ObjectType
            val classDef2 = objectType2.findIn(learningSituation.domainModel)
            val propertyDef2 = classDef2.findPropertyDef(getPropVal1.propertyName)!!

            if (propertyDef1 == propertyDef2) {
                return ComparePropertyOfDiffObjContext(
                    getPropVal1.objectExpr, getPropVal2.objectExpr,
                    operator.operator, propertyDef1
                )
            }
        }
        if (operator is Compare && operator.firstExpr is GetPropertyValue && operator.secondExpr is GetPropertyValue) {
            val getPropVal1 = operator.firstExpr as GetPropertyValue
            val objectType1 = getPropVal1.objectExpr.resolvedType(learningSituation.domainModel) as ObjectType
            val classDef1 = objectType1.findIn(learningSituation.domainModel)
            val propertyDef1 = classDef1.findPropertyDef(getPropVal1.propertyName)!!

            val getPropVal2 = operator.secondExpr as GetPropertyValue
            val objectType2 = getPropVal2.objectExpr.resolvedType(learningSituation.domainModel) as ObjectType
            val classDef2 = objectType2.findIn(learningSituation.domainModel)
            val propertyDef2 = classDef2.findPropertyDef(getPropVal1.propertyName)!!

            if (propertyDef1 == propertyDef2) {
                return ComparePropertyOfDiffObjContext(
                    getPropVal1.objectExpr, getPropVal2.objectExpr,
                    null, propertyDef1
                )
            }
        }
        return null
    }
}

class ComparePropertyOfDiffObjContext(
    val objExpr1: Operator, // переменная или любое другое выражение
    val objExpr2: Operator, // переменная или любое другое выражение
    val operator: CompareWithComparisonOperator.ComparisonOperator?, // оператор
    val propertyDef: PropertyDef, // для получения мета данных (локализации и тд)
) : AbstractContext {
    override fun generate(learningSituation: LearningSituation, localization: Localization): String? {
        if (operator != null) {
            return localization.COMPARE_WITH_SAME_PROPS_OF_DIFF_OBJ(
                propertyDef.getLocalizedName(localization.codePrefix),
                (objExpr1.use(OperatorReasoner.defaultReasoner(learningSituation)) as Obj)
                    .findIn(learningSituation.domainModel)!!
                    .getLocalizedName(localization.codePrefix),
                (objExpr2.use(OperatorReasoner.defaultReasoner(learningSituation)) as Obj)
                    .findIn(learningSituation.domainModel)!!
                    .getLocalizedName(localization.codePrefix),
                operator
            ).topLevelLlmCleanup()
        } else {
            return localization.COMPARE_A_PROPERTY_WITH_SAME_PROPS_OF_DIFF_OBJ(
                propertyDef.getLocalizedName(localization.codePrefix),
                (objExpr1.use(OperatorReasoner.defaultReasoner(learningSituation)) as Obj)
                    .findIn(learningSituation.domainModel)!!
                    .getLocalizedName(localization.codePrefix),
                (objExpr2.use(OperatorReasoner.defaultReasoner(learningSituation)) as Obj)
                    .findIn(learningSituation.domainModel)!!
                    .getLocalizedName(localization.codePrefix)
            ).topLevelLlmCleanup()
        }
    }

    override fun generateAnswer(learningSituation: LearningSituation, localization: Localization, value : Any): String? {
        return if (operator != null) {
            if ((operator == CompareWithComparisonOperator.ComparisonOperator.NotEqual ||
                        operator == CompareWithComparisonOperator.ComparisonOperator.GreaterEqual ||
                        operator == CompareWithComparisonOperator.ComparisonOperator.LessEqual) && value is Boolean) {
                return super.generateAnswer(learningSituation, localization, !value)
            }
            return super.generateAnswer(learningSituation, localization, value)
        } else {
            val obj1 = objExpr1.use(OperatorReasoner.defaultReasoner(learningSituation)) as Obj
            val objName1 = obj1.findIn(learningSituation.domainModel)!!
                .getLocalizedName(localization.codePrefix)

            val obj2 = objExpr2.use(OperatorReasoner.defaultReasoner(learningSituation)) as Obj
            val objName2 = obj2.findIn(learningSituation.domainModel)!!
                .getLocalizedName(localization.codePrefix)
            when (value) {
                Comparison.Values.Greater -> localization.GREATER_THAN(objName1)
                Comparison.Values.Equal -> localization.EQUAL
                Comparison.Values.Less -> localization.GREATER_THAN(objName2)
                else -> super.generateAnswer(learningSituation, localization, value)
            }
        }
    }

    override fun generateExplanation(
        learningSituation: LearningSituation,
        localization: Localization,
        correctAnswer: Any
    ): String? {
        val assertion1 = propertyDef.metadata.getString(localization.codePrefix, "assertion")

        val contextVars = mutableMapOf(
            "obj" to objExpr1.use(OperatorReasoner.defaultReasoner(learningSituation))!!
        )
        contextVars["value"] = (objExpr1.use(OperatorReasoner.defaultReasoner(learningSituation)) as Obj)
            .findIn(learningSituation.domainModel)!!
            .getPropertyValue(propertyDef.name)
        val value1 = contextVars["value"]

        val interpretedAssertion1 = assertion1?.interpret(learningSituation, localization.codePrefix, contextVars)

        contextVars["obj"] = objExpr2.use(OperatorReasoner.defaultReasoner(learningSituation))!!
        contextVars["value"] = (objExpr2.use(OperatorReasoner.defaultReasoner(learningSituation)) as Obj)
            .findIn(learningSituation.domainModel)!!
            .getPropertyValue(propertyDef.name)
        val value2 = contextVars["value"]

        val interpretedAssertion2 = assertion1?.interpret(learningSituation, localization.codePrefix, contextVars)


        if (interpretedAssertion1 != null && interpretedAssertion2 != null) {
            return localization.THATS_INCORRECT_BECAUSE(
                localization.COMPARE_PROP_OF_DIFF_OBJS_EXPL(interpretedAssertion1, interpretedAssertion2)
            )
        }
        return localization.THATS_INCORRECT_BECAUSE(
            localization.COMPARE_PROP_OF_DIFF_OBJS_EXPL(
                localization.DEFAULT_PROP_ASSERTION(
                    propertyDef.getLocalizedName(localization.codePrefix),
                    (objExpr1.use(OperatorReasoner.defaultReasoner(learningSituation)) as Obj)
                        .findIn(learningSituation.domainModel)!!
                        .getLocalizedName(localization.codePrefix),
                    value1!!.toLocalizedString(learningSituation, localization.codePrefix)
                ),
                localization.DEFAULT_PROP_ASSERTION(
                    propertyDef.getLocalizedName(localization.codePrefix),
                    (objExpr2.use(OperatorReasoner.defaultReasoner(learningSituation)) as Obj)
                        .findIn(learningSituation.domainModel)!!
                        .getLocalizedName(localization.codePrefix),
                    value2!!.toLocalizedString(learningSituation, localization.codePrefix)
                )
            )
        ).topLevelLlmCleanup()
    }
}