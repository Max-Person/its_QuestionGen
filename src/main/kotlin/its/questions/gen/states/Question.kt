package its.questions.gen.states

import its.model.definition.ThisShouldNotHappen
import its.model.nodes.BranchResult
import java.util.*
import kotlin.math.sign


class Question(
    val text: String,
    options : List<String>,
    val type: QuestionType,
    val shouldShuffle: Boolean = true,
    matchingOptions: List<String> = emptyList(),
) : QuestionStateResult {

    val options : MutableList<Pair<String, Int>>
    val matchingOptions: List<Pair<String, Int>>
    init{
        this.options = options.mapIndexed{index: Int, s: String -> s to index }.toMutableList()
        this.matchingOptions = matchingOptions.mapIndexed { index: Int, s: String -> s to index }
    }

    val isAggregation
        get() = type == QuestionType.matching

    private val scanner = Scanner(System.`in`)
    private val incorrectAnswerInput = "Неверный формат ввода. Введите ответ заново: "
    private fun getAnswers(): List<Int>{
        print("Ваш ответ: ")
        while(true){
            try{
                val input = scanner.nextLine()
                val ints: List<Int> = if (input.isBlank()) emptyList()
                else input.split(Regex("(,\\s*|\\s+)")).map { str -> str.toInt() }
                if(isAggregation) {
                    if (ints.any { it - 1 !in matchingOptions.indices })
                        throw NumberFormatException()
                }
                else {
                    if (ints.any { it - 1 !in options.indices })
                        throw NumberFormatException()
                    if(type == QuestionType.single && ints.size != 1)
                        throw NumberFormatException()
                }
                return ints

            }catch (e: NumberFormatException){
                print(incorrectAnswerInput)
            }
        }
    }

    fun ask(): List<Int>{
        println(text)
        if (matchingOptions.isNotEmpty()) println(matchingOptions.map { (text, i) -> "${i + 1}-$text" })
        if(shouldShuffle) options.shuffle()

        options.forEachIndexed {i, option -> println("   ${i+1}. ${option.first}") }

        val answers = getAnswers()
        return if(!isAggregation)
            answers.map { options[it -1].second }
        else {
            answers.mapIndexed { index, answer ->
                val result = when (answer.sign) {
                    1 -> BranchResult.CORRECT
                    0 -> BranchResult.NULL
                    -1 -> BranchResult.ERROR
                    else -> throw ThisShouldNotHappen()
                }
                result.ordinal to options[index].second
            }.sortedBy { it.second }.map { it.first }
        }
    }
}