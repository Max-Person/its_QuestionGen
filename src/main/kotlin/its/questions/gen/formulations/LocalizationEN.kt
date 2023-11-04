package its.questions.gen.formulations

object LocalizationEN : Localization {
    override val codePrefix: String = "EN"


    override val TRUE = "True"
    override val FALSE = "False"

    override val NOT_IMPORTANT = "Doesn't matter"

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

    override fun WHY_DO_YOU_THINK_THAT(assumed_result: String): String = "Why do you think that $assumed_result?"
    override val LETS_FIGURE_IT_OUT = "Let's figure it out."
    override fun WE_CAN_CONCLUDE_THAT(result: String): String  = "We can conclude that $result."
    override fun SO_WEVE_DISCUSSED_WHY(result: String): String = "So, we've discussed why $result."
    override fun WE_ALREADY_DISCUSSED_THAT(fact: String): String = "We have already seen that $fact."

    override fun IS_IT_TRUE_THAT(statement: String): String = "Is it true that $statement?"
    override fun WHY_IS_IT_THAT(statement: String): String = "Why is it that $statement?"

    override fun DEFAULT_REASONING_START_QUESTION(reasoning_topic: String): String = "What is the first step to determine if $reasoning_topic?"
    override val DEFAULT_NEXT_STEP_QUESTION: String = "What is the next reasoning step in this case?"

    override val WHAT_DO_YOU_WANT_TO_DISCUSS_FURTHER: String = "What do you want to discuss further?"
    override val NO_FURTHER_DISCUSSION_NEEDED: String = "No further discussion is needed"

    override val IMPOSSIBLE_TO_FIND: String = "None can be found."

    override fun AGGREGATION_CORRECT_EXPL(answer_descr: String, branches_descr: String): String = "You've judged the situation correctly, but in this case it means that $answer_descr because $branches_descr"
    override fun AGGREGATION_INCORRECT_BRANCHES_DESCR(branches_descr: String): String = THATS_INCORRECT_BECAUSE(branches_descr)
    override fun AGGREGATION_MISSED_BRANCHES_DESCR_PRIMARY(branches_descr: String): String = "That's incorrect, because you did not consider that $branches_descr - this matters in this case."
    override fun AGGREGATION_MISSED_BRANCHES_DESCR_CONCAT(branches_descr: String): String = "You also did not consider that $branches_descr - this matters in this case."
}