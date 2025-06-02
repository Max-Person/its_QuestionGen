package its.questions.gen.formulations

import com.github.max_person.templating.InterpretationData
import com.github.max_person.templating.TemplateInterpolationParser
import com.github.max_person.templating.TemplateSection
import its.model.definition.ThisShouldNotHappen
import its.model.definition.loqi.OperatorLoqiBuilder
import its.model.definition.types.*
import its.model.expressions.Operator
import its.questions.gen.formulations.TemplatingUtils.getLocalizedName
import its.questions.gen.visitors.ValueToAnswerString.toLocalizedString
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
            val contextVars = data.getVar(CONTEXT_VARS) as Map<String, Any>
            val situation = data.getVar(LEARNING_SITUATION) as LearningSituation
            val reasoner = DomainInterpreterReasoner(situation, contextVars)
            val res = operator.evalAs<Any>(reasoner)
            val localizationCode = data.getVar(LOCALIZATION_CODE).toString()
            return res.toLocalizedString(situation, localizationCode)
        }
    }
}