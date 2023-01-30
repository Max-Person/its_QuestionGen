package its.questions.gen

import java.lang.NumberFormatException
import java.util.*

//NOTE:SUBJECT_TO_CHANGE: формат вопросов надо будет поменять для соответствия ТЗ, да и в принципе
abstract class Question(val text: String, val options : List<AnswerOption>) {
    init{
        require(options.all { it.isTrue || it.explanation != null })
    }

    enum class AnswerStatus{
        CORRECT,
        EXPLAINED,
        STUCK,
    }

    data class AnswerOption(
        val text: String,
        val isTrue: Boolean,
        val explanation: String? = null,
        val isInverted: Boolean? = null
    )

    protected fun getAnswers(): List<Int>{
        print("Ваш ответ: ")
        val scanner = Scanner(System.`in`)
        val input = scanner.nextLine()
        while(true){
            try{
                input.split(',').map{str ->
                    str.toInt()
                }.run {
                    scanner.close()
                    return this
                }
            }catch (e: NumberFormatException){
                print("Неверный формат ввода. Введите ответ заново: ")
            }
        }
    }

    protected fun getExplanationHelped() : Boolean{
        println("Понятна ли вам ошибка? Введите Y если да, и N если нет и вы хотите рассмотреть ошибки дальше: ")
        val scanner = Scanner(System.`in`)
        var input = scanner.nextLine()
        while (input.lowercase() != "y" && input.lowercase() != "n"){
            print("Неверный формат ввода. Введите ответ заново: ")
            input = scanner.nextLine()
        }
        return input.lowercase() == "y"

    }

    abstract fun ask() : AnswerStatus
}