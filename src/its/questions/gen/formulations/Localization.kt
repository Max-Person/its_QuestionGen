package its.questions.gen.formulations

sealed interface Localization {
    val codePrefix: String


    val TRUE: String
    val FALSE: String

    val NOT_IMPORTANT: String

    val YES: String
    val NO: String

    val GREATER: String
    val LESS: String
    val EQUAL: String
    val NOT_EQUAL: String

    val CANNOT_BE_DETERMINED: String

    val THATS_CORRECT: String
    val THATS_INCORRECT: String
    fun THATS_INCORRECT_BECAUSE(reason: String): String
    fun IN_THIS_SITUATION(fact: String): String

    fun WHY_DO_YOU_THINK_THAT(assumed_result: String): String
    val LETS_FIGURE_IT_OUT: String
    fun WE_CAN_CONCLUDE_THAT(result: String): String
    fun SO_WEVE_DISCUSSED_WHY(result: String): String
    fun WE_ALREADY_DISCUSSED_THAT(fact: String): String

    fun IS_IT_TRUE_THAT(statement: String): String
    fun WHY_IS_IT_THAT(statement: String): String

    fun DEFAULT_REASONING_START_QUESTION(reasoning_topic: String): String
    val DEFAULT_NEXT_STEP_QUESTION: String

    val WHAT_DO_YOU_WANT_TO_DISCUSS_FURTHER: String
    val NO_FURTHER_DISCUSSION_NEEDED: String

    val IMPOSSIBLE_TO_FIND: String

    fun AGGREGATION_CORRECT_EXPL(answer_descr: String, branches_descr: String) : String
    fun AGGREGATION_INCORRECT_BRANCHES_DESCR(branches_descr: String): String
    fun AGGREGATION_MISSED_BRANCHES_DESCR_PRIMARY(branches_descr: String): String
    fun AGGREGATION_MISSED_BRANCHES_DESCR_CONCAT(branches_descr: String): String

    companion object _static{
        @JvmStatic
        val localizations = mapOf(
            LocalizationRU.codePrefix to LocalizationRU,
            LocalizationEN.codePrefix to LocalizationEN
        )
    }
}