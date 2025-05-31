package its.questions.gen.formulations.v2.generation

import its.model.definition.ClassDef
import its.model.definition.ClassRef
import its.model.definition.ThisShouldNotHappen
import its.model.definition.types.Obj
import its.model.definition.types.ObjectType
import its.model.expressions.Operator
import its.model.expressions.literals.ClassLiteral
import its.model.expressions.operators.CheckClass
import its.model.expressions.operators.GetClass
import its.questions.gen.formulations.Localization
import its.questions.gen.formulations.TemplatingUtils.getLocalizedName
import its.questions.gen.formulations.TemplatingUtils.topLevelLlmCleanup
import its.questions.gen.formulations.v2.AbstractContext
import its.reasoner.LearningSituation
import its.reasoner.operators.OperatorReasoner

class CheckObjectClass(learningSituation: LearningSituation, localization: Localization) :
    AbstractQuestionGeneration(learningSituation, localization) {

    override fun fits(operator: Operator): CheckClassContext? {
        if (operator is CheckClass && operator.classExpr is ClassLiteral) {
            val classDef = ClassRef((operator.classExpr as ClassLiteral).name).findIn(learningSituation.domainModel)
            return CheckClassContext(operator.objectExpr, operator, classDef)
        }
        if (operator is GetClass) {
            val objectType = operator.objectExpr.resolvedType(learningSituation.domainModel) as ObjectType
            val classDef = objectType.findIn(learningSituation.domainModel)
            return CheckClassContext(operator.objectExpr, operator, classDef)
        }
        return null
    }
}

class CheckClassContext(
    val objExpr: Operator,
    val operator: Operator,
    val classDef: ClassDef?
) : AbstractContext {
    override fun generate(learningSituation: LearningSituation, localization: Localization): String? {
        when (operator) {
            is CheckClass -> {
                return localization.IS_OBJ_A_CLASS(
                    classDef!!.getLocalizedName(localization.codePrefix),
                    (objExpr.use(OperatorReasoner.defaultReasoner(learningSituation)) as Obj)
                        .findIn(learningSituation.domainModel)!!
                        .getLocalizedName(localization.codePrefix)
                ).topLevelLlmCleanup()
            }

            is GetClass -> {
                return localization.CHECK_OBJECT_CLASS(
                    classDef!!.getLocalizedName(localization.codePrefix),
                    (objExpr.use(OperatorReasoner.defaultReasoner(learningSituation)) as Obj)
                        .findIn(learningSituation.domainModel)!!
                        .getLocalizedName(localization.codePrefix)
                ).topLevelLlmCleanup()
            }

            else -> {
                throw ThisShouldNotHappen()
            }
        }
    }
}