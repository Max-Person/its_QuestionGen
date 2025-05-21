package its.questions.gen.formulations

import its.model.expressions.operators.CompareWithComparisonOperator
import its.model.nodes.AggregationMethod
import its.questions.gen.formulations.TemplatingUtils.toCase
import org.apache.lucene.morphology.russian.RussianLuceneMorphology

object LocalizationRU : Localization {
    override val codePrefix: String = "RU"


    override val TRUE = "Верно"
    override val FALSE = "Неверно"

    override val NOT_IMPORTANT = "Не имеет значения"
    override val NO_EFFECT = "Не влияет"

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
    override val PLEASE_MATCH = "Пожалуйста, сопоставьте ответы"

    override fun WHY_DO_YOU_THINK_THAT(assumed_result: String): String = "Почему вы считаете, что $assumed_result?"
    override val LETS_FIGURE_IT_OUT = "Давайте разберемся."
    override fun WE_CAN_CONCLUDE_THAT(result: String): String  = "Можно заключить, что $result."
    override fun SO_WEVE_DISCUSSED_WHY(result: String): String = "Итак, мы обсудили, почему $result."
    override fun WE_ALREADY_DISCUSSED_THAT(fact: String): String = "Мы уже говорили о том, что $fact."

    override val WHICH_IS_TRUE_HERE = "Что из перечисленного применимо в данной ситуации?"
    override val NONE_OF_THE_ABOVE_APPLIES = "Ничто из вышеперечисленного не применимо"

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
    override fun SIM_AGGREGATION_EXPLANATION(
        aggregationMethod: AggregationMethod,
        aggregationDescription: String,
        branchesDescription: String,
        isCorrect: Boolean,
    ): String =
        "Это неверно. Чтобы понять, что $aggregationDescription, " + "${if (aggregationMethod == AggregationMethod.AND) "все" else "хотя бы один"} из описанных выше факторов (${branchesDescription}) " + "${if (aggregationMethod == AggregationMethod.AND) "должны" else "должен"} выполняться. " + if (isCorrect) "В данном случае так и происходит." else "Однако в данном случае это не так."

    override fun SIM_AGGREGATION_NULL_EXPLANATION(branchesDescription: String): String =
        "Это неверно, поскольку в данной ситуации все из описанных выше факторов (${branchesDescription}) " + "не влияют на решение, а значит, об общем результате в данном случае говорить не приходится."

    override fun COMPARE_A_PROPERTY_TO_A_CONSTANT(propertyName: String, objName: String, propertyVal: String): String {
        return "Имеет ли $propertyName ${objName.toCase(Case.Gen)} значение $propertyVal?" // TODO использовать это для оператора равно
    }

    private val morphology = RussianLuceneMorphology()

    private val operatorMap = mapOf(
        CompareWithComparisonOperator.ComparisonOperator.Greater to "Больше ли",
        CompareWithComparisonOperator.ComparisonOperator.LessEqual to "Больше ли",
        CompareWithComparisonOperator.ComparisonOperator.Less to "Меньше ли",
        CompareWithComparisonOperator.ComparisonOperator.GreaterEqual to "Меньше ли"
    )

    private val genderToEqualOpMap = mapOf(
        "мр" to "Равен ли",
        "жр" to "Равна ли",
        "ср" to "Равно ли"
    )

    override fun COMPARE_A_PROPERTY_TO_A_NUMERIC_CONST(
        propertyName: String,
        objName: String,
        propertyVal: String,
        operator: CompareWithComparisonOperator.ComparisonOperator
    ): String {
        val operatorStr: String = if (operator == CompareWithComparisonOperator.ComparisonOperator.Equal ||
            operator == CompareWithComparisonOperator.ComparisonOperator.NotEqual) {
            val morphInfo = morphology.getMorphInfo(propertyName)
            val gender = extractGender(morphInfo)
            genderToEqualOpMap[gender]!!
        } else {
            operatorMap[operator]!!
        }
        return "$operatorStr $propertyName ${objName.toCase(Case.Gen)} $propertyVal?"
    }

    override fun COMPARE_A_PROPERTY(propertyName: String, objName: String, propertyVal: String): String {
        return "Сравните значение ${propertyName.toCase(Case.Gen)} ${objName.toCase(Case.Gen)} со значением $propertyVal"
    }

    override fun CHECK_OBJ_PROPERTY_OR_CLASS(propertyName: String, objName: String): String {
        return "Каково значение ${propertyName.toCase(Case.Gen)} ${objName.toCase(Case.Gen)}"
    }

    override fun COMPARE_WITH_SAME_PROPS_OF_DIFF_OBJ(
        propertyName: String,
        objName1: String,
        objName2: String,
        operator: CompareWithComparisonOperator.ComparisonOperator
    ): String {
        val operatorStr: String = if (operator == CompareWithComparisonOperator.ComparisonOperator.Equal ||
            operator == CompareWithComparisonOperator.ComparisonOperator.NotEqual) {
            val morphInfo = morphology.getMorphInfo(propertyName)
            val gender = extractGender(morphInfo)
            genderToEqualOpMap[gender]!!
        } else {
            operatorMap[operator]!!
        }

        val propertyOfObj2: String = if (operator == CompareWithComparisonOperator.ComparisonOperator.Equal ||
            operator == CompareWithComparisonOperator.ComparisonOperator.NotEqual) {
            propertyName.toCase(Case.Dat)
        } else {
            propertyName.toCase(Case.Gen)
        }
        return "$operatorStr $propertyName ${objName1.toCase(Case.Gen)} $propertyOfObj2 ${objName2.toCase(Case.Gen)}"
    }

    override fun COMPARE_A_PROPERTY_WITH_SAME_PROPS_OF_DIFF_OBJ(
        propertyName: String,
        objName1: String,
        objName2: String
    ): String {
        return "Сравните $propertyName ${objName1.toCase(Case.Gen)} с ${propertyName.toCase(Case.Ins)} ${objName2.toCase(Case.Gen)}"
    }

    override fun CHECK_OBJECT_CLASS(className: String, objName: String): String {
        return "Каким ${className.toCase(Case.Ins)} является $objName?"
    }

    override fun IS_OBJ_A_CLASS(className: String, objName: String): String {
        return "Является ли $objName ${className.toCase(Case.Ins)}"
    }

    override fun GREATER_THAN(objName: String): String {
        return "У ${objName.toCase(Case.Gen)} больше"
    }

    private fun extractGender(morphInfo: List<String>): String? {
        for (info in morphInfo) {
            if (info.contains("жр")) return "жр"
            if (info.contains("мр")) return "мр"
            if (info.contains("ср")) return "ср"
        }
        return null
    }
}