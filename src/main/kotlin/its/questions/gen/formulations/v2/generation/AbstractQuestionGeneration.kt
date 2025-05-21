package its.questions.gen.formulations.v2.generation

import its.model.expressions.Operator
import its.questions.gen.formulations.Localization
import its.questions.gen.formulations.v2.AbstractContext
import its.questions.gen.visitors.ValueToAnswerString.toLocalizedString
import its.reasoner.LearningSituation

abstract class AbstractQuestionGeneration<T : AbstractContext>(protected val learningSituation: LearningSituation,
                                                               val localization: Localization) {

    fun generateQuestion(operator: Operator) : String? {
        val context = fits(operator)
        if (context != null) {
            return generate(context)
        }
        return null
    }

    open fun generateAnswer(context: T, value : Any) : String? {
        return value.toLocalizedString(learningSituation, localization.codePrefix)
    }

    protected abstract fun generate(context: T): String?
    protected abstract fun fits(operator: Operator) : T?
}