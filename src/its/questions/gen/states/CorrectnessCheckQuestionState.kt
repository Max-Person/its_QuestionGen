package its.questions.gen.states

abstract class CorrectnessCheckQuestionState<AnswerInfo> (
    links: Set<QuestionStateLink<Pair<AnswerInfo, Boolean>>>
) : SingleChoiceQuestionState<Pair<AnswerInfo, Boolean>>(links) {
    override fun explanation(chosenOption: SingleChoiceOption<Pair<AnswerInfo, Boolean>>): Explanation? {
        if(chosenOption.assocAnswer.second)
            return Explanation("Верно.", shouldPause = false)
        else
            return chosenOption.explanation ?: Explanation("Это неверно.", shouldPause = false)
    }
}