package its.questions.gen.visitors

import its.model.expressions.types.ParseValue.parseValue
import its.model.nodes.*
import its.questions.inputs.LearningSituation

internal const val ALIAS_ATR = "alias"

internal fun ThoughtBranch.isTrivial() : Boolean{
    return start !is LinkNode<*> || (start as LinkNode<*>).children.all { it is BranchResultNode }
}

internal fun <AnswerType : Any> LinkNode<AnswerType>.getAnswer(situation: LearningSituation): AnswerType?{
    val strAnswer = situation.answers[this.additionalInfo[ALIAS_ATR]!!] ?: return null
    return strAnswer.parseValue(answerType) as AnswerType
}

internal fun ThoughtBranch.getAnswer(situation: LearningSituation): Boolean?{
    val strAnswer = situation.answers[this.additionalInfo[ALIAS_ATR]!!] ?: return null
    return strAnswer.toBoolean()
}

internal fun LinkNode<*>.correctNext(situation: LearningSituation) : DecisionTreeNode{
    return this.next[this.getAnswer(situation)]!!
}