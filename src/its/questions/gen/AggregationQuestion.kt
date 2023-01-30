package its.questions.gen

import its.model.nodes.LogicalOp

class AggregationQuestion(shouldBeFinal : Boolean, text: String, val op: LogicalOp, val result: Boolean, options : List<AnswerOption>)
    : Question(shouldBeFinal, text, options) {
    override fun ask(): AnswerStatus {
        println(text)
        println("(Агрегирующий вопрос - укажите номер ответа с отрицанием, чтобы показать что он влияет отрицательно)")
        val shuffle = options.shuffled()
        shuffle.forEachIndexed {i, option -> println("   ${i+1}. ${option.text}") }

        var answers = getAnswers()
        while(answers.any { it-1 !in shuffle.indices  && -(it-1) !in shuffle.indices }){
            println("Неверный формат ввода для вопроса с единственным вариантом ответа.")
            answers = getAnswers()
        }

        var correct = true
        shuffle.forEachIndexed { i, option ->
            correct = correct
                    && if(option.isTrue) answers.contains(i+1) else answers.contains(-(i+1)) //очередная опция указана в ответе
                    && ((op == LogicalOp.AND && result == false && option.actual() == true ) //или может быть опущена
                        || (op == LogicalOp.OR && result == true && option.actual() == false ))
        }

        return if(correct){
            AnswerStatus.CORRECT
        } else{
            //TODO("Вывод объяснений для агрегационного вопроса")
            println("Это неверно.")
            if(!shouldBeFinal)
                AnswerStatus.INCORRECT_CONTINUE
            else if(getExplanationHelped())
                AnswerStatus.INCORRECT_EXPLAINED
            else
                AnswerStatus.INCORRECT_STUCK
        }
    }

    private fun AnswerOption.actual() : Boolean{
        return if(isInverted == null || !isInverted) !isTrue else isTrue
    }
}