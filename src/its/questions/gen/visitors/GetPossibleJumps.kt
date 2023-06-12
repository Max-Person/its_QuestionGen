package its.questions.gen.visitors

import its.model.nodes.*
import its.model.nodes.visitors.SimpleDecisionTreeBehaviour
import its.questions.gen.QuestioningSituation
import its.reasoner.nodes.DecisionTreeReasoner._static.getAnswer

class GetPossibleJumps private constructor( val situation: QuestioningSituation) : SimpleDecisionTreeBehaviour<List<DecisionTreeNode>>{
    private var isTransitional = false

    // ---------------------- Удобства ---------------------------

    companion object _static{
        @JvmStatic
        fun DecisionTreeNode.getPossibleJumps(situation: QuestioningSituation) : List<DecisionTreeNode>{
            return this.use(GetPossibleJumps(situation)).filter { it != this }
        }
    }

    private fun DecisionTreeNode.getPossibleJumps(): List<DecisionTreeNode>{
        return this.use(this@GetPossibleJumps)
    }

    // ---------------------- Функции поведения ---------------------------

    override fun <AnswerType : Any> process(node: LinkNode<AnswerType>): List<DecisionTreeNode> {
        val l = mutableListOf<DecisionTreeNode>(node)
        isTransitional = true
        node.next.values.forEach { l.addAll(it.getPossibleJumps()) }
        return l
    }

    override fun process(node: FindActionNode): List<DecisionTreeNode> {
        //Особое поведение, так как не интересуют узлы, которые используют переменные
        val l = mutableListOf<DecisionTreeNode>(node)
        val next = if(isTransitional) node.nextIfNone else node.next[node.getAnswer(situation)]
        isTransitional = true
        if(next != null)
            l.addAll(next.getPossibleJumps())
        return l
    }

    override fun process(node: BranchResultNode): List<DecisionTreeNode> {
        return listOf(node)
    }

    override fun process(node: StartNode): List<DecisionTreeNode> {
        return listOf(node).plus(node.main.getPossibleJumps())
    }

    override fun process(branch: ThoughtBranch): List<DecisionTreeNode> {
        return listOf<DecisionTreeNode>().plus(branch.start.getPossibleJumps())
    }

    override fun process(node: UndeterminedResultNode): List<DecisionTreeNode> {
        return listOf()
    }
}