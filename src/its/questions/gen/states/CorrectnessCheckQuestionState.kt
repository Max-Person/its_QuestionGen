package its.questions.gen.states

import its.questions.gen.QuestioningSituation

abstract class CorrectnessCheckQuestionState<AnswerInfo> (
    links: Set<QuestionStateLink<Pair<AnswerInfo, Boolean>>>
) : SingleChoiceQuestionState<Pair<AnswerInfo, Boolean>>(links) {
    override fun explanation(situation: QuestioningSituation, chosenOption: SingleChoiceOption<Pair<AnswerInfo, Boolean>>): Explanation? {
        if(chosenOption.assocAnswer.second)
            return Explanation(situation.localization.THATS_CORRECT, type = ExplanationType.Success, shouldPause = false)
        else
            return chosenOption.explanation ?: Explanation(situation.localization.THATS_INCORRECT, type = ExplanationType.Error, shouldPause = false)
    }
}