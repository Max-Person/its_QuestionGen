package its.questions.gen.visitors

import its.model.nodes.*
import its.model.nodes.visitors.SimpleDecisionTreeBehaviour
import its.questions.inputs.LearningSituation

class GetConsideredNodes private constructor(val situation: LearningSituation) :
    SimpleDecisionTreeBehaviour<List<DecisionTreeNode>> {

    // ---------------------- Удобства ---------------------------

    companion object _static{
        @JvmStatic
        fun ThoughtBranch.getConsideredNodes(situation: LearningSituation) : List<DecisionTreeNode>{
            return this.use(GetConsideredNodes(situation))
        }
    }

    private fun DecisionTreeNode.getConsideredNodes(): List<DecisionTreeNode>{
        return this.use(this@GetConsideredNodes)
    }

    // ---------------------- Функции поведения ---------------------------

    override fun <AnswerType : Any> process(node: LinkNode<AnswerType>): List<DecisionTreeNode>{
        val out = mutableListOf<DecisionTreeNode>(node)
        out.addAll(node.correctNext(situation).getConsideredNodes())
        return out.toList()
    }

    override fun process(node: BranchResultNode): List<DecisionTreeNode> {
        return listOf(node)
    }

    override fun process(node: StartNode): List<DecisionTreeNode> {
        val out = mutableListOf<DecisionTreeNode>(node)
        out.addAll(node.main.getConsideredNodes())
        return out.toList()
    }

    override fun process(branch: ThoughtBranch): List<DecisionTreeNode> {
        val out = mutableListOf<DecisionTreeNode>(branch)
        out.addAll(branch.start.getConsideredNodes())
        return out.toList()
    }

    override fun process(node: UndeterminedResultNode): List<DecisionTreeNode> {
        return listOf(node)
    }
}