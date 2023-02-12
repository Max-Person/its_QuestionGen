package its.questions.questiontypes

import its.model.nodes.LogicalOp

class AggregationQuestion<AssocType : Any>(val text: String, val op: LogicalOp, val result: Boolean, options : List<AnswerOption<AssocType>>) {
    val options : MutableList<AnswerOption<AssocType>>
    init{
        this.options = options.toMutableList()
    }

    data class AnswerOption<AssocType>(
        val assocValue: AssocType,
        val text: String,
        val isTrue: Boolean,
        val textActual: String? = null,
        val isInverted: Boolean = false,
        val isDistractor: Boolean = false,
    ){
        internal fun actual() : Boolean{
            return if(isInverted == null || !isInverted) !isTrue else isTrue
        }
    }

    private fun Int.option() : AnswerOption<AssocType>{
        return if(this > 0) options[this-1] else options[-this-1]
    }

    private fun Int.isCorrect() : Boolean{
        return if(this > 0) this.option().isTrue else !this.option().isTrue
    }

    fun ask(): Set<AssocType> {
        println()
        println(text)
        println("(Агрегирующий вопрос - укажите номер ответа с отрицанием, чтобы показать что он влияет отрицательно)")
        options.shuffle()
        options.forEachIndexed {i, option -> println("   ${i+1}. ${option.text}") }

        var answers = getAnswers()
        while(answers.any { it-1 !in options.indices  && -it-1 !in options.indices }){
            println("Неверный формат ввода для агрегирующего вопроса.")
            answers = getAnswers()
        }


        val incorrectOptions = answers.filter { it.option().isDistractor || !it.isCorrect()  }.map{it.option()}
        val missedOptions = options.filterIndexed{i, option -> !option.isDistractor && !answers.contains(i+1) && !answers.contains(-(i+1)) &&
                !(op == LogicalOp.AND && result == false && option.actual() == true ) && !(op == LogicalOp.OR && result == true && option.actual() == false )}

        return if(incorrectOptions.isEmpty() && missedOptions.isEmpty()){
            println("Верно.")
            emptySet()
        } else{
            if(!incorrectOptions.isEmpty())
                println("Это неверно, поскольку ${incorrectOptions.joinToString(separator = ", ", transform = {it.textActual!!})}.")
            if(!missedOptions.isEmpty())
                println((if(incorrectOptions.isEmpty()) "Это неверно, поскольку вы " else "Вы также ") +
                        "не упомянули, что ${missedOptions.joinToString(separator = ", ", transform = {it.textActual!!})} - это влияет на ситуацию в данном случае.")
            val out = mutableSetOf<AssocType>()
            out.addAll(incorrectOptions.filter { !it.isDistractor }.map{it.assocValue})
            out.addAll(missedOptions.map{it.assocValue})
            out
        }
    }

}