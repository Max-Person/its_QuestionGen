package its.questions.questiontypes

class Prompt<Info>(val text: String, val options : List<Pair<String, Info>>){
    fun ask(): Info {
        if(options.size == 1)
            return options[0].second
        println()
        println(text)
        println("(элемент управления, не является вопросом)")
        options.forEachIndexed {i, option -> println(" ${i+1}. ${option.first}") }

        var answers = getAnswers()
        while(answers.size != 1 || answers.any { it-1 !in options.indices}){
            println("Укажите одно число для ответа")
            answers = getAnswers()
        }
        val answer = answers.single()
        return options[answer-1].second
    }
}