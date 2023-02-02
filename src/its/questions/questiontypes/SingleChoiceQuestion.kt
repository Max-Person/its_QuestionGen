package its.questions.questiontypes

class SingleChoiceQuestion(val shouldBeFinal : Boolean, val text: String, val options : List<AnswerOption>){
    init{
        require(options.count { it.isTrue } == 1)
    }

    fun ask(): AnswerStatus {
        if(options.size == 1)
            return AnswerStatus.CORRECT
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
            AnswerStatus.CORRECT
        } else{
            println(shuffle[answer-1].explanation)
            println("В данном случае правильным ответом является ${options.first { it.isTrue }.text}.")
            if(!shouldBeFinal)
                AnswerStatus.INCORRECT_CONTINUE
            else if(getExplanationHelped())
                AnswerStatus.INCORRECT_EXPLAINED
            else
                AnswerStatus.INCORRECT_STUCK
        }
    }
}