package its.questions.gen.states

import its.model.Utils
import its.questions.gen.QuestioningSituation

/**
 * Состояние автомата вопросов, создающее вопросы с множественным выбором
 */
abstract class MultipleChoiceQuestionState<AnswerInfo>(val nextState: QuestionState)
    : GeneralQuestionState<AnswerInfo>(setOf(QuestionStateLink({_, _ -> true}, nextState))) {

    protected data class MultipleChoiceOption<AnswerInfo>(
        val text : String,
        val assocAnswer: AnswerInfo,
        val isCorrect: Boolean,
        //Для корректных - почему они должны быть включены, для некорректных - почему не должны
        val explanation: String,
    )

    protected abstract fun text(situation: QuestioningSituation) : String
    protected abstract fun options(situation: QuestioningSituation) : List<MultipleChoiceOption<AnswerInfo>>

    protected open val shouldSkipIfASingleOption = false
    protected open fun explanationIfSkipped(
        situation: QuestioningSituation,
        skipOption: MultipleChoiceOption<AnswerInfo>
    ) : Explanation? {
        return null
    }

    protected open fun preliminarySkip(situation: QuestioningSituation) : QuestionStateChange? {
        return null
    }

    protected open fun additionalActions(situation: QuestioningSituation, chosenAnswers: List<AnswerInfo>) {}


    override fun getQuestion(situation: QuestioningSituation): QuestionStateResult {
        val skipChange = preliminarySkip(situation)
        if(skipChange != null)
            return skipChange

        val text = text(situation).prependId()
        val options = options(situation)
        if(shouldSkipIfASingleOption && options.size == 1){
            val change = proceedWithAnswer(situation, options, options)
            return change.copy(explanation = explanationIfSkipped(situation, options.single()))
        }

        return Question(text, options.map{option -> option.text}, QuestionType.multiple)
    }

    override fun proceedWithAnswer(situation: QuestioningSituation, answer: List<Int>): QuestionStateChange {
        val options = options(situation)
        val chosenOptions = options.filterIndexed{i, opt -> answer.contains(i)}
        return proceedWithAnswer(situation, options, chosenOptions)
    }

    private fun proceedWithAnswer(
        situation: QuestioningSituation,
        allOptions: List<MultipleChoiceOption<AnswerInfo>>,
        chosenOptions: List<MultipleChoiceOption<AnswerInfo>>
    ) : QuestionStateChange {
        additionalActions(situation, chosenOptions.map { it.assocAnswer })

        val correctOptions = allOptions.filter { it.isCorrect }
        val (notChosen, _, excessive) = Utils.getCollectionsDifference(correctOptions, chosenOptions)
        val incorrect = notChosen.plus(excessive)

        val explanation = if(incorrect.isEmpty())
            Explanation(situation.localization.THATS_CORRECT, type = ExplanationType.Success, shouldPause = false)
        else
            Explanation(
                listOf(situation.localization.THATS_INCORRECT)
                    .plus(incorrect.map { it.explanation })
                    .joinToString("\n"),
                type = ExplanationType.Error
            )

        return QuestionStateChange(explanation, nextState)
    }
}
