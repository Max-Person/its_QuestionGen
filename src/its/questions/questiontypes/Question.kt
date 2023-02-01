package its.questions.questiontypes

import java.lang.NumberFormatException
import java.util.*

//NOTE:SUBJECT_TO_CHANGE: формат вопросов надо будет поменять для соответствия ТЗ, да и в принципе
abstract class Question(val shouldBeFinal : Boolean, val text: String, val options : List<AnswerOption>) {
    init{
        require(options.all { it.isTrue || it.explanation != null })
    }

    enum class AnswerStatus{
        CORRECT,
        INCORRECT_CONTINUE,
        INCORRECT_EXPLAINED,
        INCORRECT_STUCK,
    }

    data class AnswerOption(
        val text: String,
        val isTrue: Boolean,
        val explanation: String? = null,
        val isInverted: Boolean? = null
    )

    private val scanner = Scanner(System.`in`)

    protected fun getAnswers(): List<Int>{
        print("Ваш ответ: ")
        val input = scanner.nextLine()
        while(true){
            try{
                return input.split(',').map{str ->str.toInt()}
            }catch (e: NumberFormatException){
                print("Неверный формат ввода. Введите ответ заново: ")
            }
        }
    }

    protected fun getExplanationHelped() : Boolean{
        println("Понятна ли вам ошибка? Введите Y если да, и N если нет и вы хотите рассмотреть ошибки дальше: ")
        var input = scanner.nextLine()
        while (input.lowercase() != "y" && input.lowercase() != "n"){
            print("Неверный формат ввода. Введите ответ заново: ")
            input = scanner.nextLine()
        }
        return input.lowercase() == "y"

    }

    abstract fun ask() : AnswerStatus
}