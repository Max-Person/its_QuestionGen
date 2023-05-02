package its.questions.gen.states

import its.questions.inputs.LearningSituation

abstract class CorrectnessCheckQuestionState<AnswerInfo> (
    links: Set<QuestionStateLink<Pair<AnswerInfo, Boolean>>>
) : SingleChoiceQuestionState<Pair<AnswerInfo, Boolean>>(links) {
    override fun explanation(situation: LearningSituation, chosenOption: SingleChoiceOption<Pair<AnswerInfo, Boolean>>): Explanation? {
        if(chosenOption.assocAnswer.second)
            return Explanation(situation.localization.THATS_CORRECT, shouldPause = false)
        else
            return chosenOption.explanation ?: Explanation(situation.localization.THATS_INCORRECT, shouldPause = false)
    }
}