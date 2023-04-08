package its.questions.gen.visitors

import its.model.expressions.Literal
import its.model.nodes.*
import its.questions.inputs.LearningSituation
import kotlin.reflect.full.isSubclassOf

internal const val ALIAS_ATR = "alias"

internal fun ThoughtBranch.isTrivial() : Boolean{
    return start !is LinkNode<*> || (start as LinkNode<*>).children.all { it is BranchResultNode }
}

internal fun <AnswerType : Any> LinkNode<AnswerType>.getAnswer(situation: LearningSituation): AnswerType?{
    val strAnswer = situation.answers[this.additionalInfo[ALIAS_ATR]!!] ?: return null
    val answerType = answerType
    if(answerType.isSubclassOf(Literal::class)){
        require(this is QuestionNode){
            "Узел, имеющий литерал как ответ, должен быть узлом вопроса"
        }
        return Literal.fromString(strAnswer, type, enumOwner) as AnswerType
    }
    return when(answerType){
        String::class -> strAnswer as AnswerType
        Boolean::class -> strAnswer.toBoolean() as AnswerType
        Int::class -> strAnswer.toInt() as AnswerType
        Double::class -> strAnswer.toDouble() as AnswerType
        else -> throw IllegalStateException("Неподдерживаемый тип ответа на узел")
    }
}

internal fun ThoughtBranch.getAnswer(situation: LearningSituation): Boolean?{
    val strAnswer = situation.answers[this.additionalInfo[ALIAS_ATR]!!] ?: return null
    return strAnswer.toBoolean()
}

internal fun LinkNode<*>.correctNext(situation: LearningSituation) : DecisionTreeNode{
    return this.next[this.getAnswer(situation)]!!
}