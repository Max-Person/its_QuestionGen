package its.questions.gen.formulations

import com.github.max_person.templating.InterpretationData
import com.github.max_person.templating.TemplateInterpolationParser
import com.github.max_person.templating.TemplateSection
import its.model.definition.ThisShouldNotHappen
import its.model.definition.loqi.OperatorLoqiBuilder
import its.model.definition.types.*
import its.model.expressions.Operator
import its.questions.gen.formulations.TemplatingUtils.getLocalizedName
import its.reasoner.LearningSituation
import its.reasoner.operators.DomainInterpreterReasoner
import its.reasoner.operators.OperatorReasoner.Companion.evalAs

object OperatorTemplateParser : TemplateInterpolationParser<OperatorTemplateParser.OperatorSection> {

    const val LEARNING_SITUATION = "LearningSituation"
    const val LOCALIZATION_CODE = "LocalizationCode"
    const val CONTEXT_VARS = "ContextVars"

    override fun parse(interpolationContent: TemplateInterpolationParser.InterpolationContent): OperatorSection {
        val operator = OperatorLoqiBuilder.buildExp(interpolationContent.content)
        return OperatorSection(operator)
    }

    class OperatorSection(private val operator: Operator) : TemplateSection {
        override fun interpret(data: InterpretationData): String {
            val contextVars = data.getVar(CONTEXT_VARS) as Map<String, Obj>
            val situation = data.getVar(LEARNING_SITUATION) as LearningSituation
            val reasoner = DomainInterpreterReasoner(situation, contextVars)
            val res = operator.evalAs<Any>(reasoner)
            val localizationCode = data.getVar(LOCALIZATION_CODE).toString()
            return when (Type.of(res)) {
                    BooleanType -> (res as Boolean).toString()
                    is DoubleType -> (res as Double).toString()
                    is IntegerType -> (res as Int).toString()
                    StringType -> res as String
                    is ClassType -> (res as Clazz).getLocalizedName(situation.domainModel, localizationCode)
                    is ObjectType -> (res as Obj).getLocalizedName(situation.domainModel, localizationCode)
                    is EnumType -> (res as EnumValue).getLocalizedName(situation.domainModel, localizationCode)
                    NoneType, AnyType -> throw ThisShouldNotHappen()
                }

        }
    }
}