package its.questions.gen.visitors

import its.model.nodes.*
import its.model.nodes.visitors.SimpleDecisionTreeBehaviour
import its.questions.gen.QuestioningSituation
import its.reasoner.nodes.DecisionTreeReasoner._static.getAnswer
import its.reasoner.nodes.DecisionTreeReasoner._static.getCorrectPath

class GetPossibleEndingNodes private constructor(val branch: ThoughtBranch, val situation: QuestioningSituation) :
    SimpleDecisionTreeBehaviour<Unit> {
    val consideredNodes: List<DecisionTreeNode> = branch.getCorrectPath(situation)
    val set = mutableSetOf<DecisionTreeNode>()
    lateinit var correct : DecisionTreeNode

    // ---------------------- Удобства ---------------------------

    companion object _static{
        @JvmStatic
        fun ThoughtBranch.getPossibleEndingNodes(situation: QuestioningSituation) : Set<DecisionTreeNode>{
            val behaviour = GetPossibleEndingNodes(this, situation)
            this.start.use(behaviour)
            return behaviour.set
        }
    }

    private fun DecisionTreeNode.getEndingNodes(){
        this.use(this@GetPossibleEndingNodes)
    }

    // ---------------------- Функции поведения ---------------------------

    override fun <AnswerType> process(node: LinkNode<AnswerType>) {
        node.outcomes.forEach { it.node.getEndingNodes() }
        if(node.outcomes.any{it.node is BranchResultNode})
            set.add(node)
        if(node.outcomes.any{it.node is BranchResultNode && consideredNodes.contains(it.node)})
            correct = node
    }

    override fun process(node: FindActionNode) {
        //Особое поведение, так как не интересуют конечные узлы, которые используют несуществующие переменные
        node.outcomes[node.getAnswer(situation)]?.node?.getEndingNodes()
        if(node.outcomes.any{it.node is BranchResultNode})
            set.add(node)
        if(node.outcomes.any{it.node is BranchResultNode && consideredNodes.contains(it.node)})
            correct = node
    }

    override fun process(node: BranchResultNode) {}

    override fun process(branch: ThoughtBranch) {
        branch.start.getEndingNodes()
    }
}