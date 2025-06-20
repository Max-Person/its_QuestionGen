package its.questions.gen.formulations.v2.generation

import its.model.expressions.Operator
import its.questions.gen.formulations.Localization
import its.questions.gen.formulations.v2.AbstractContext
import its.questions.gen.formulations.v2.generation.constant.CompareWithBoolean
import its.questions.gen.formulations.v2.generation.constant.CompareWithEnum
import its.questions.gen.formulations.v2.generation.constant.CompareWithNumeric
import its.questions.gen.formulations.v2.generation.constant.CompareWithString
import its.reasoner.LearningSituation

class QuestionGeneratorFabric(val learningSituation: LearningSituation, val localization: Localization) {

    private val generators = listOf(
        CompareWithBoolean(learningSituation, localization),
        CompareWithEnum(learningSituation, localization),
        CompareWithNumeric(learningSituation, localization),
        CompareWithString(learningSituation, localization),
        CheckPropertyQuestionGeneration(learningSituation, localization),
        CompareWithPropertyOfDiffObj(learningSituation, localization),
        CheckExistenceOfRelationship(learningSituation, localization),
        CheckObjectClass(learningSituation, localization)
    )

    fun getContext(operator : Operator) : AbstractContext? {
        return generators.stream()
            .map { it.fits(operator) }
            .filter {it != null}
            .findFirst().orElse(null)
    }
}