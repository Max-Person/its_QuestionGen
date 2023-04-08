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
    protected open fun shouldBeSkipped(situation: LearningSituation) : QuestionStateChange? {return null}
    protected open fun explanation(chosenOption: SingleChoiceOption<AnswerInfo>) : Explanation? {return chosenOption.explanation}
    protected open fun additionalActions(situation: LearningSituation, chosenAnswer: AnswerInfo) {}

    override fun getQuestion(situation: LearningSituation): QuestionStateResult {
        val text = "$id. ${text(situation)}"
        val options = options(situation)
        if(options.size == 1){
            val nextState = links.first { link -> link.condition(situation, options.single().assocAnswer) }.nextState
            return QuestionStateChange(null, nextState)
        }

        val skipChange = shouldBeSkipped(situation)
        if(skipChange != null)
            return skipChange

        return Question(text, options.map{option -> option.text})
    }

    override fun proceedWithAnswer(situation: LearningSituation, answer: List<Int>): QuestionStateChange {
        require(answer.size == 1){
            "Invalid answer to a SingleChoiceQuestionState: $answer"
        }

        val chosenOption = options(situation)[answer.single()]

        situation.addGivenAnswer(id, answer.single())
        additionalActions(situation, chosenOption.assocAnswer)

        val explanation = explanation(chosenOption)
        val nextState = links.first { link -> link.condition(situation, chosenOption.assocAnswer) }.nextState
        return QuestionStateChange(explanation, nextState)
    }

    fun previouslyChosenAnswer(situation: LearningSituation) : AnswerInfo?{
        val ans = situation.givenAnswer(id)
        return if(ans != null) options(situation)[ans].assocAnswer else null
    }
}