package its.questions.gen.states

import its.questions.inputs.LearningSituation

abstract class SingleChoiceQuestionState<AnswerInfo>(
    links: Set<QuestionStateLink<AnswerInfo>>
) : GeneralQuestionState<AnswerInfo>(links) {

    protected data class SingleChoiceOption<AnswerInfo>(
        val text : String,
        val explanation: Explanation?,
        val assocAnswer: AnswerInfo,
    )

    protected abstract fun text(situation: LearningSituation) : String
    protected abstract fun options(situation: LearningSituation) : List<SingleChoiceOption<AnswerInfo>>
    protected open fun explanationIfSkipped(situation: LearningSituation, skipOption: SingleChoiceOption<AnswerInfo>) : Explanation? {return null}
    protected open fun preliminarySkip(situation: LearningSituation) : QuestionStateChange? {return null}
    protected open fun explanation(situation: LearningSituation, chosenOption: SingleChoiceOption<AnswerInfo>) : Explanation? {return chosenOption.explanation}
    protected open fun additionalActions(situation: LearningSituation, chosenAnswer: AnswerInfo) {}

    override fun getQuestion(situation: LearningSituation): QuestionStateResult {
        val skipChange = preliminarySkip(situation)
        if(skipChange != null)
            return skipChange

        val text = "$id. ${text(situation)}"
        val options = options(situation)
        if(options.size == 1){
            val change = proceedWithAnswer(situation, options.single())
            return change.copy(explanation = explanationIfSkipped(situation, options.single()))
        }

        return Question(text, options.map{option -> option.text})
    }

    override fun proceedWithAnswer(situation: LearningSituation, answer: List<Int>): QuestionStateChange {
        require(answer.size == 1){
            "Invalid answer to a SingleChoiceQuestionState: $answer"
        }

        val chosenOption = options(situation)[answer.single()]

        return proceedWithAnswer(situation, chosenOption)
    }

    private fun proceedWithAnswer(situation: LearningSituation, chosenOption: SingleChoiceOption<AnswerInfo>): QuestionStateChange {
        situation.addGivenAnswer(id, options(situation).indexOf(chosenOption))
        additionalActions(situation, chosenOption.assocAnswer)

        val explanation = explanation(situation, chosenOption)
        val nextState = links.first { link -> link.condition(situation, chosenOption.assocAnswer) }.nextState
        return QuestionStateChange(explanation, nextState)
    }

    fun previouslyChosenAnswer(situation: LearningSituation) : AnswerInfo?{
        val ans = situation.givenAnswer(id)
        return if(ans != null) options(situation)[ans].assocAnswer else null
    }
}