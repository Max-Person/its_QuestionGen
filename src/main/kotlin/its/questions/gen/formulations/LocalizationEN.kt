package its.questions.gen.formulations

import its.model.nodes.AggregationMethod

object LocalizationEN : Localization {
    override val codePrefix: String = "EN"


    override val TRUE = "True"
    override val FALSE = "False"

    override val NOT_IMPORTANT = "Doesn't matter"
    override val NO_EFFECT = "Has no effect"

    override val YES = "Yes"
    override val NO = "No"

    override val GREATER = "Greater"
    override val LESS = "Less"
    override val EQUAL = "Equal"
    override val NOT_EQUAL = "Not equal"

    override val CANNOT_BE_DETERMINED = "Cannot be determined"

    override val THATS_CORRECT = "Correct."
    override val THATS_INCORRECT = "That's incorrect."
    override fun THATS_INCORRECT_BECAUSE(reason: String): String = "That's incorrect, because $reason."
    override fun IN_THIS_SITUATION(fact: String): String = "In this case, $fact."
    override val PLEASE_MATCH = "Please match the options to the answers"

    override fun WHY_DO_YOU_THINK_THAT(assumed_result: String): String = "Why do you think that $assumed_result?"
    override val LETS_FIGURE_IT_OUT = "Let's figure it out."
    override fun WE_CAN_CONCLUDE_THAT(result: String): String  = "We can conclude that $result."
    override fun SO_WEVE_DISCUSSED_WHY(result: String): String = "So, we've discussed why $result."
    override fun WE_ALREADY_DISCUSSED_THAT(fact: String): String = "We have already seen that $fact."
    override val WHICH_IS_TRUE_HERE = "Which is true in this situation?"
    override val NONE_OF_THE_ABOVE_APPLIES = "None of the above"

    override fun IS_IT_TRUE_THAT(statement: String): String = "Is it true that $statement?"
    override fun WHY_IS_IT_THAT(statement: String): String = "Why is it that $statement?"

    override fun DEFAULT_REASONING_START_QUESTION(reasoning_topic: String): String = "What is the first step to determine if $reasoning_topic?"
    override val DEFAULT_NEXT_STEP_QUESTION: String = "What is the next reasoning step in this case?"

    override val WHAT_DO_YOU_WANT_TO_DISCUSS_FURTHER: String = "What do you want to discuss further?"
    override val NO_FURTHER_DISCUSSION_NEEDED: String = "No further discussion is needed"

    override val IMPOSSIBLE_TO_FIND: String = "None can be found."

    override fun ALSO_FITS_THE_CRITERIA(object_descr: String) = "$object_descr also fits the criteria"

    override fun AGGREGATION_CORRECT_EXPL(answer_descr: String, branches_descr: String): String = "You've judged the situation correctly, but in this case it means that $answer_descr because $branches_descr"
    override fun AGGREGATION_INCORRECT_BRANCHES_DESCR(branches_descr: String): String = THATS_INCORRECT_BECAUSE(branches_descr)
    override fun AGGREGATION_MISSED_BRANCHES_DESCR_PRIMARY(branches_descr: String): String = "That's incorrect, because you did not consider that $branches_descr - this matters in this case."
    override fun AGGREGATION_MISSED_BRANCHES_DESCR_CONCAT(branches_descr: String): String = "You also did not consider that $branches_descr - this matters in this case."
    override fun SIM_AGGREGATION_EXPLANATION(
        aggregationMethod: AggregationMethod,
        aggregationDescription: String,
        branchesDescription: String,
        isCorrect: Boolean,
    ): String =
        "That's incorrect. In order to determine if $aggregationDescription, " + "${if (aggregationMethod == AggregationMethod.AND) "all" else "at least one"} of the factors mentioned " + "(${branchesDescription}) should apply. " + "And in this case they ${if (isCorrect) "do" else "don't"}"

    override fun SIM_AGGREGATION_NULL_EXPLANATION(branchesDescription: String): String =
        "That's incorrect, because in this case none of the factors mentioned (${branchesDescription}) " + "have no effect, which means that no determined result can be decided on this stage."
}