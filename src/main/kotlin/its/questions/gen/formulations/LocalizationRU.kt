package its.questions.gen.formulations

object LocalizationRU : Localization {
    override val codePrefix: String = "RU"


    override val TRUE = "Верно"
    override val FALSE = "Неверно"

    override val NOT_IMPORTANT = "Не имеет значения"

    override val YES = "Да"
    override val NO = "Нет"

    override val GREATER = "Больше"
    override val LESS = "Меньше"
    override val EQUAL = "Равно"
    override val NOT_EQUAL = "Не равно"

    override val CANNOT_BE_DETERMINED = "Невозможно определить"

    override val THATS_CORRECT = "Верно."
    override val THATS_INCORRECT = "Это неверно."
    override fun THATS_INCORRECT_BECAUSE(reason: String): String = "Это неверно, поскольку $reason."
    override fun IN_THIS_SITUATION(fact: String): String = "В данной ситуации $fact."

    override fun WHY_DO_YOU_THINK_THAT(assumed_result: String): String = "Почему вы считаете, что $assumed_result?"
    override val LETS_FIGURE_IT_OUT = "Давайте разберемся."
    override fun WE_CAN_CONCLUDE_THAT(result: String): String  = "Можно заключить, что $result."
    override fun SO_WEVE_DISCUSSED_WHY(result: String): String = "Итак, мы обсудили, почему $result."
    override fun WE_ALREADY_DISCUSSED_THAT(fact: String): String = "Мы уже говорили о том, что $fact."

    override fun IS_IT_TRUE_THAT(statement: String): String = "Верно ли, что $statement?"
    override fun WHY_IS_IT_THAT(statement: String): String = "Почему $statement?"

    override fun DEFAULT_REASONING_START_QUESTION(reasoning_topic: String): String = "С чего надо начать, чтобы проверить, что $reasoning_topic?"
    override val DEFAULT_NEXT_STEP_QUESTION: String = "Какой следующий шаг необходим для решения задачи?"

    override val WHAT_DO_YOU_WANT_TO_DISCUSS_FURTHER: String = "В чем бы вы хотели разобраться подробнее?"
    override val NO_FURTHER_DISCUSSION_NEEDED: String = "Подробный разбор не нужен"

    override val IMPOSSIBLE_TO_FIND: String = "Невозможно найти."
    override fun ALSO_FITS_THE_CRITERIA(object_descr: String) = "$object_descr also fits the criteria"

    override fun AGGREGATION_CORRECT_EXPL(answer_descr: String, branches_descr: String): String = "Вы верно оценили ситуацию, однако это значит, что $answer_descr - из-за того, что $branches_descr"
    override fun AGGREGATION_INCORRECT_BRANCHES_DESCR(branches_descr: String): String = THATS_INCORRECT_BECAUSE(branches_descr)
    override fun AGGREGATION_MISSED_BRANCHES_DESCR_PRIMARY(branches_descr: String): String = "Это неверно, поскольку вы не упомянули, что $branches_descr - это влияет на ситуацию в данном случае."
    override fun AGGREGATION_MISSED_BRANCHES_DESCR_CONCAT(branches_descr: String): String = "Вы также не упомянули, что $branches_descr - это влияет на ситуацию в данном случае."
}