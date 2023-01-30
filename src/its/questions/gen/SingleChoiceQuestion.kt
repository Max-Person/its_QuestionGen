package its.questions.gen

class SingleChoiceQuestion(text: String, options : List<AnswerOption>) : Question(text, options){
    init{
        require(options.count { it.isTrue } == 1)
        require(options.all { it.isInverted == null })
    }

    override fun ask(): AnswerStatus {
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
            if(getExplanationHelped()) AnswerStatus.EXPLAINED else AnswerStatus.STUCK
        }
    }
}