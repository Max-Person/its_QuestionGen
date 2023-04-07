package its.questions.gen.states

import java.util.*


class Question(val text: String, options : List<String>, val isAggregation: Boolean = false, val shouldShuffle : Boolean = true) : QuestionStateResult{
    val options : MutableList<Pair<String, Int>>
    init{
        this.options = options.mapIndexed{index: Int, s: String -> s to index }.toMutableList()
    }

    private val scanner = Scanner(System.`in`)
    private val incorrectAnswerInput = "Неверный формат ввода. Введите ответ заново: "
    private fun getAnswers(): List<Int>{
        print("Ваш ответ: ")
        while(true){
            try{
                val input = scanner.nextLine()
                val ints: List<Int> = if(input.isBlank()) emptyList() else input.split(',').map{ str ->str.toInt()}
                if(isAggregation) {
                    if (ints.any { it - 1 !in options.indices && -it - 1 !in options.indices })
                        throw NumberFormatException()
                }
                else {
                    if (ints.size != 1 || ints.any { it - 1 !in options.indices })
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
        if(shouldShuffle) options.shuffle()

        options.forEachIndexed {i, option -> println("   ${i+1}. ${option.first}") }

        val answers = getAnswers()
        return if(!isAggregation)
            listOf(options[answers.single()-1].second)
        else {
            options.sortedBy { option -> option.second }.mapIndexed { i, op ->
                if(answers.contains(i+1))
                    1
                else if(answers.contains(-i-1))
                    -1
                else
                    0
            }
        }
    }
}