package its.questions.gen.formulations

import its.model.expressions.operators.CompareWithComparisonOperator
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

    override fun COMPARE_A_PROPERTY_TO_A_CONSTANT(propertyName: String, objName: String, propertyVal: String) : String {
        return "Is $propertyName of $objName equal to $propertyVal?"
    }

    private val operatorMap = mapOf(
        CompareWithComparisonOperator.ComparisonOperator.Equal to "equal to",
        CompareWithComparisonOperator.ComparisonOperator.NotEqual to "equal to",
        CompareWithComparisonOperator.ComparisonOperator.GreaterEqual to "greater than",
        CompareWithComparisonOperator.ComparisonOperator.Greater to "greater than",
        CompareWithComparisonOperator.ComparisonOperator.Less to "less than",
        CompareWithComparisonOperator.ComparisonOperator.LessEqual to "less than"
    )

    override fun COMPARE_A_PROPERTY_TO_A_NUMERIC_CONST(
        propertyName: String,
        objName: String,
        propertyVal: String,
        operator: CompareWithComparisonOperator.ComparisonOperator
    ): String {
        return "Is $propertyName of $objName ${operatorMap[operator]} $propertyVal?"
    }

    override fun COMPARE_A_PROPERTY(propertyName: String, objName: String, propertyVal: String): String {
        return "Compare the value of $propertyVal of $objName with value $propertyVal"
    }

    override fun CHECK_OBJ_PROPERTY_OR_CLASS(propertyName: String, objName: String): String {
        return "What is the $propertyName of $objName"
    }

    override fun COMPARE_WITH_SAME_PROPS_OF_DIFF_OBJ(
        propertyName: String,
        objName1: String,
        objName2: String,
        operator: CompareWithComparisonOperator.ComparisonOperator
    ): String {
        return "Is $propertyName of $objName1 ${operatorMap[operator]} that of $objName2?"
    }

    override fun COMPARE_A_PROPERTY_WITH_SAME_PROPS_OF_DIFF_OBJ(
        propertyName: String,
        objName1: String,
        objName2: String
    ): String {
        return "Compare $propertyName of $objName1 with $propertyName of $objName2"
    }

    override fun CHECK_OBJECT_CLASS(className: String, objName: String): String {
        return "Which $className is $objName?"
    }

    override fun IS_OBJ_A_CLASS(className: String, objName: String): String {
        return "Is $objName ${if (startsWithVowel(className)) "an" else "a"} $className?"
    }

    fun startsWithVowel(word: String): Boolean {
        if (word.isEmpty()) return false
        return when (word.first().lowercaseChar()) {
            'a', 'e', 'i', 'o', 'u' -> true
            else -> false
        }
    }
}