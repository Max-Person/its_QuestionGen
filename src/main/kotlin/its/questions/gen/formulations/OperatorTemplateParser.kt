package its.questions.gen.formulations

import com.github.max_person.templating.InterpretationData
import com.github.max_person.templating.TemplateInterpolationParser
import com.github.max_person.templating.TemplateSection
import its.model.definition.ThisShouldNotHappen
import its.model.definition.loqi.OperatorBuilder
import its.model.definition.types.*
import its.model.expressions.Operator
import its.questions.gen.QuestioningSituation
import its.reasoner.operators.OperatorReasoner
import its.reasoner.operators.OperatorReasoner.Companion.evalAs

object OperatorTemplateParser : TemplateInterpolationParser<OperatorTemplateParser.OperatorSection> {

    const val QUESTIONING_SITUATION = "QuestioningSituation"

    override fun parse(interpolationContent: TemplateInterpolationParser.InterpolationContent): OperatorSection {
        val operator = OperatorBuilder.buildExp(interpolationContent.content)
        return OperatorSection(operator)
    }

    class OperatorSection(private val operator: Operator) : TemplateSection {
        override fun interpret(data: InterpretationData): String {
            val questioningSituation = data.getVar(QUESTIONING_SITUATION) as QuestioningSituation
            val reasoner = OperatorReasoner.defaultReasoner(questioningSituation)
            val res = operator.evalAs<Any>(reasoner)
            return with (questioningSituation.formulations) {
                when (Type.of(res)) {
                    BooleanType -> (res as Boolean).toString()
                    is DoubleType -> (res as Double).toString()
                    is IntegerType -> (res as Int).toString()
                    StringType -> res as String
                    is ClassType -> (res as Clazz).localizedName
                    is ObjectType -> (res as Obj).localizedName
                    is EnumType -> (res as EnumValue).localizedName
                    NoneType, AnyType -> throw ThisShouldNotHappen()
                }
            }
        }
    }
}