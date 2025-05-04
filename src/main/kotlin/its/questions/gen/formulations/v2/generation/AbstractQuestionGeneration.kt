package its.questions.gen.formulations.v2.generation

import its.model.expressions.Operator
import its.questions.gen.formulations.v2.AbstractContext
import its.reasoner.LearningSituation

abstract class AbstractQuestionGeneration<T : AbstractContext>(protected val learningSituation: LearningSituation) {

    fun generateQuestion(operator: Operator) : String? {
        val context = fits(operator)
        if (context != null) {
            return generate(context)
        }
        return null
    }

    protected abstract fun generate(context: T) : String
    protected abstract fun fits(operator: Operator) : T?
}