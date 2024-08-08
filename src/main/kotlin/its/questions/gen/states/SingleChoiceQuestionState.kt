package its.questions.gen.states

import its.questions.gen.QuestioningSituation

abstract class SingleChoiceQuestionState<AnswerInfo>(
    links: Set<QuestionStateLink<AnswerInfo>>
) : GeneralQuestionState<AnswerInfo>(links) {

    protected data class SingleChoiceOption<AnswerInfo>(
        val text : String,
        val explanation: Explanation?,
        val assocAnswer: AnswerInfo,
    )

    protected abstract fun text(situation: QuestioningSituation) : String
    protected abstract fun options(situation: QuestioningSituation) : List<SingleChoiceOption<AnswerInfo>>
    protected open fun explanationIfSkipped(situation: QuestioningSituation, skipOption: SingleChoiceOption<AnswerInfo>) : Explanation? {return null}
    protected open fun preliminarySkip(situation: QuestioningSituation) : QuestionStateChange? {return null}
    protected open fun explanation(situation: QuestioningSituation, chosenOption: SingleChoiceOption<AnswerInfo>) : Explanation? {return chosenOption.explanation}
    protected open fun additionalActions(situation: QuestioningSituation, chosenAnswer: AnswerInfo) {}

    override fun getQuestion(situation: QuestioningSituation): QuestionStateResult {
        val skipChange = preliminarySkip(situation)
        if(skipChange != null)
            return skipChange

        val text = text(situation).prependId()
        val options = options(situation)
        if(options.size == 1){
            val change = proceedWithAnswer(situation, options.single())
            return change.copy(explanation = explanationIfSkipped(situation, options.single()))
        }

        return Question(text, options.map{option -> option.text}, QuestionType.single)
    }

    override fun proceedWithAnswer(situation: QuestioningSituation, answer: List<Int>): QuestionStateChange {
        require(answer.size == 1){
            "Invalid answer to a SingleChoiceQuestionState: $answer"
        }

        val chosenOption = options(situation)[answer.single()]

        return proceedWithAnswer(situation, chosenOption)
    }

    private fun proceedWithAnswer(situation: QuestioningSituation, chosenOption: SingleChoiceOption<AnswerInfo>): QuestionStateChange {
        situation.addGivenAnswer(id, options(situation).indexOf(chosenOption))
        additionalActions(situation, chosenOption.assocAnswer)

        val explanation = explanation(situation, chosenOption)
        val nextState = links.first { link -> link.condition(situation, chosenOption.assocAnswer) }.nextState
        return QuestionStateChange(explanation, nextState)
    }

    fun previouslyChosenAnswer(situation: QuestioningSituation) : AnswerInfo?{
        val ans = situation.givenAnswer(id)
        return if(ans != null) options(situation)[ans].assocAnswer else null
    }
}