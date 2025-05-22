package its.questions.gen.formulations.v2.generation

import its.model.expressions.Operator
import its.questions.gen.formulations.Localization
import its.questions.gen.formulations.v2.AbstractContext
import its.reasoner.LearningSituation

abstract class AbstractQuestionGeneration(protected val learningSituation: LearningSituation,
                                                               val localization: Localization) {
    abstract fun fits(operator: Operator) : AbstractContext?
}