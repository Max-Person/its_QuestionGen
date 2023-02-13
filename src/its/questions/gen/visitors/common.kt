package its.questions.gen.visitors

import its.model.nodes.BranchResultNode
import its.model.nodes.LinkNode
import its.model.nodes.ThoughtBranch

internal const val ALIAS_ATR = "alias"

internal fun ThoughtBranch.isTrivial() : Boolean{
    return start !is LinkNode || (start as LinkNode).children.all { it is BranchResultNode }
}