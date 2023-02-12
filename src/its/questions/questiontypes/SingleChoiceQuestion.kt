package its.questions.questiontypes

//TODO сделать большой рефакторинг
class SingleChoiceQuestion(val text: String, val options : List<AnswerOption>){
    init{
        require(options.count { it.isTrue } == 1)
    }
    fun ask(): Boolean{
        return askWithInfo().first
    }

    fun askWithInfo(): Pair<Boolean, Any?> {
        if(options.size == 1)
            return true to options.first().assocValue
        println()
        println(text)
        println("(Вопрос с единственным вариантом ответа)")
        val shuffle = options.shuffled()
        shuffle.forEachIndexed {i, option -> println(" ○ ${i+1}. ${option.text}") }

        var answers = getAnswers()
        while(answers.size != 1 || answers.any { it-1 !in shuffle.indices}){
            println("Неверный формат ввода для вопроса с единственным вариантом ответа.")
            answers = getAnswers()
        }
        val answer = answers.single()

        return if(shuffle[answer-1].isTrue){
            println("Верно.")
            true
        } else{
            if(shuffle[answer-1].explanation.isNullOrBlank())
                println("Это неверно. В данном случае правильным ответом является '${options.first { it.isTrue }.text}'.")
            else
                println(shuffle[answer-1].explanation)

            false
        } to shuffle[answer-1].assocValue
    }
}