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
import its.questions.gen.formulations.v2.AbstractContext
import its.reasoner.LearningSituation
import its.reasoner.operators.OperatorReasoner

class CompareWithPropertyOfDiffObj(learningSituation: LearningSituation, localization: Localization) :
    AbstractQuestionGeneration(learningSituation, localization) {

    override fun fits(operator: Operator): ComparePropertyOfDiffObjContext? {
        if (operator is CompareWithComparisonOperator &&
            operator.firstExpr is GetPropertyValue && operator.secondExpr is GetPropertyValue
        ) {
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
            )
        } else {
            return localization.COMPARE_A_PROPERTY_WITH_SAME_PROPS_OF_DIFF_OBJ(
                propertyDef.getLocalizedName(localization.codePrefix),
                (objExpr1.use(OperatorReasoner.defaultReasoner(learningSituation)) as Obj)
                    .findIn(learningSituation.domainModel)!!
                    .getLocalizedName(localization.codePrefix),
                (objExpr2.use(OperatorReasoner.defaultReasoner(learningSituation)) as Obj)
                    .findIn(learningSituation.domainModel)!!
                    .getLocalizedName(localization.codePrefix)
            )
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
}