package its.questions.gen.states

import its.questions.gen.QuestioningSituation

abstract class CorrectnessCheckQuestionState<AnswerInfo> (
    links: Set<QuestionStateLink<Correctness<AnswerInfo>>>
) : SingleChoiceQuestionState<CorrectnessCheckQuestionState.Correctness<AnswerInfo>>(links) {
    data class Correctness<AnswerInfo>(
        val answerInfo: AnswerInfo,
        val isCorrect: Boolean,
    )

    override fun explanation(
        situation: QuestioningSituation,
        chosenOption: SingleChoiceOption<Correctness<AnswerInfo>>
    ): Explanation? {
        return if (chosenOption.assocAnswer.isCorrect)
            Explanation(situation.localization.THATS_CORRECT, type = ExplanationType.Success, shouldPause = false)
        else
            chosenOption.explanation ?: Explanation(
                situation.localization.THATS_INCORRECT,
                type = ExplanationType.Error,
                shouldPause = false
            )
    }
}