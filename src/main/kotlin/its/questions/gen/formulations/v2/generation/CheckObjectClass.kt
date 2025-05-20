package its.questions.gen.formulations.v2.generation

import its.model.definition.ClassDef
import its.model.definition.PropertyDef
import its.model.definition.ThisShouldNotHappen
import its.model.definition.types.Obj
import its.model.definition.types.ObjectType
import its.model.expressions.Operator
import its.model.expressions.literals.ClassLiteral
import its.model.expressions.operators.CheckClass
import its.model.expressions.operators.CompareWithComparisonOperator
import its.model.expressions.operators.GetClass
import its.questions.gen.formulations.Localization
import its.questions.gen.formulations.TemplatingUtils.getLocalizedName
import its.questions.gen.formulations.v2.AbstractContext
import its.reasoner.LearningSituation
import its.reasoner.operators.OperatorReasoner

class CheckObjectClass(learningSituation: LearningSituation, val localization: Localization) :
    AbstractQuestionGeneration<CheckObjectClass.CheckClassContext>(learningSituation) {

    override fun generate(context: CheckClassContext): String {
        when (context.operator) {
            is CheckClass -> {
                return localization.IS_OBJ_A_CLASS(
                    context.classDef!!.metadata.getString(localization.codePrefix, "name")!!,
                    (context.objExpr.use(OperatorReasoner.defaultReasoner(learningSituation)) as Obj)
                        .findIn(learningSituation.domainModel)!!
                        .getLocalizedName(localization.codePrefix)
                )
            }

            is GetClass -> {
                return localization.CHECK_OBJECT_CLASS(
                    context.classDef!!.metadata.getString(localization.codePrefix, "name")!!,
                    (context.objExpr.use(OperatorReasoner.defaultReasoner(learningSituation)) as Obj)
                        .findIn(learningSituation.domainModel)!!
                        .getLocalizedName(localization.codePrefix)
                )
            }

            else -> {
                throw ThisShouldNotHappen()
            }
        }
    }

    override fun fits(operator: Operator): CheckClassContext? {
        if (operator is CheckClass) {
            val objectType = operator.objectExpr.resolvedType(learningSituation.domainModel) as ObjectType
            val classDef = objectType.findIn(learningSituation.domainModel)
            return CheckClassContext(operator.objectExpr, operator.classExpr, operator, classDef)
        }
        if (operator is GetClass) {
            val objectType = operator.objectExpr.resolvedType(learningSituation.domainModel) as ObjectType
            val classDef = objectType.findIn(learningSituation.domainModel)
            return CheckClassContext(operator.objectExpr, null, operator, classDef)
        }
        return null
    }

    class CheckClassContext(
        val objExpr: Operator,
        val classExpr: Operator?,
        val operator: Operator,
        val classDef: ClassDef?
    ) : AbstractContext
}