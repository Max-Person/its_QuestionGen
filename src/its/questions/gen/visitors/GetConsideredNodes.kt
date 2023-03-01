package its.questions.gen.visitors

import its.model.nodes.*
import its.model.nodes.visitors.SimpleDecisionTreeBehaviour

class GetConsideredNodes private constructor(val answers: Map<String, String>) :
    SimpleDecisionTreeBehaviour<List<DecisionTreeNode>> {

    // ---------------------- Удобства ---------------------------

    companion object _static{
        @JvmStatic
        fun ThoughtBranch.getConsideredNodes(answers: Map<String, String>) : List<DecisionTreeNode>{
            return this.use(GetConsideredNodes(answers))
        }
    }

    private fun DecisionTreeNode.getConsideredNodes(): List<DecisionTreeNode>{
        return this.use(this@GetConsideredNodes)
    }

    // ---------------------- Функции поведения ---------------------------

    override fun <AnswerType : Any> process(node: LinkNode<AnswerType>): List<DecisionTreeNode>{
        val out = mutableListOf<DecisionTreeNode>(node)
        out.addAll(node.correctNext(answers).getConsideredNodes())
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