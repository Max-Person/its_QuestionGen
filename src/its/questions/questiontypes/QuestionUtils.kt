package its.questions.questiontypes

import java.lang.NumberFormatException
import java.util.*

//NOTE:SUBJECT_TO_CHANGE: формат вопросов надо будет поменять для соответствия ТЗ, да и в принципе
data class AnswerOption(
    val text: String,
    val isTrue: Boolean,
    val explanation: String? = null,
    val assocValue: Any? = null,
)

private val scanner = Scanner(System.`in`)

internal fun getAnswers(): List<Int>{
    print("Ваш ответ: ")
    while(true){
        try{
            val input = scanner.nextLine()
            return if(input.isBlank()) emptyList() else input.split(',').map{ str ->str.toInt()}
        }catch (e: NumberFormatException){
            print("Неверный формат ввода. Введите ответ заново: ")
        }
    }
}