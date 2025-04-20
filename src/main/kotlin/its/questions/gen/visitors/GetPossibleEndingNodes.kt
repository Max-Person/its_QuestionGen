package its.questions.gen.visitors

import its.model.nodes.*
import its.model.nodes.visitors.SimpleDecisionTreeBehaviour
import its.questions.gen.QuestioningSituation
import its.reasoner.nodes.DecisionTreeReasoner.Companion.getAnswer
import its.reasoner.nodes.DecisionTreeReasoner.Companion.solve

class GetPossibleEndingNodes(
    val branch: ThoughtBranch,
    val situation: QuestioningSituation? = null,
) : SimpleDecisionTreeBehaviour<GetPossibleEndingNodes.PossibleEndingNodes> {

    private val correctResultingNode = situation?.forEval()?.let { branch.solve(it).resultingNode }

    data class PossibleEndingNodes(
        val endingNodes: Set<DecisionTreeNode> = setOf(),
        val correctEndingNode: DecisionTreeNode? = null,
    ) {
        operator fun plus(other: PossibleEndingNodes): PossibleEndingNodes {
            return PossibleEndingNodes(
                endingNodes + other.endingNodes, correctEndingNode ?: other.correctEndingNode
            )
        }
    }

    fun get(): PossibleEndingNodes {
        return process(branch)
    }

    // ---------------------- Функции поведения ---------------------------

    override fun <AnswerType : Any> process(node: LinkNode<AnswerType>): PossibleEndingNodes {
        val childrenRes = node.outcomes.map { it.node.use(this) }.reduce(PossibleEndingNodes::plus)

        return childrenRes + getCurrentRes(node)
    }

    private fun <AnswerType : Any> getCurrentRes(node: LinkNode<AnswerType>): PossibleEndingNodes {
        return PossibleEndingNodes(
            if (isAggregationEndingNode(node) || node.outcomes.any { it.node is BranchResultNode }) setOf(node) else setOf(),
            if (node == correctResultingNode || node.outcomes.any { it.node is BranchResultNode && correctResultingNode == it.node }) node else null
        )
    }

    private fun isAggregationEndingNode(node: LinkNode<*>): Boolean {
        return node is AggregationNode && !node.outcomes.keys.containsAll(BranchResult.entries)
    }

    override fun process(node: FindActionNode): PossibleEndingNodes {
        if (situation == null) return process(node as LinkNode<*>)

        //Особое поведение, так как не интересуют конечные узлы, которые используют несуществующие переменные
        val childrenRes = node.outcomes[node.getAnswer(situation)]?.node?.use(this) ?: PossibleEndingNodes()

        return childrenRes + getCurrentRes(node)
    }

    override fun process(node: BranchResultNode): PossibleEndingNodes {
        return PossibleEndingNodes()
    }

    override fun process(branch: ThoughtBranch): PossibleEndingNodes {
        return branch.start.use(this)
    }
}