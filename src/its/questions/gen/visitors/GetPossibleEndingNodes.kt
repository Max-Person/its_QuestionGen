package its.questions.gen.visitors

import its.model.nodes.*
import its.model.nodes.visitors.SimpleDecisionTreeBehaviour
import its.questions.gen.visitors.GetConsideredNodes._static.getConsideredNodes
import its.questions.inputs.LearningSituation

class GetPossibleEndingNodes private constructor(val branch: ThoughtBranch, val situation: LearningSituation) :
    SimpleDecisionTreeBehaviour<Unit> {
    val consideredNodes: List<DecisionTreeNode> = branch.getConsideredNodes(situation)
    val set = mutableSetOf<DecisionTreeNode>()
    lateinit var correct : DecisionTreeNode

    // ---------------------- Удобства ---------------------------

    companion object _static{
        @JvmStatic
        fun ThoughtBranch.getPossibleEndingNodes(situation: LearningSituation) : Set<DecisionTreeNode>{
            val behaviour = GetPossibleEndingNodes(this, situation)
            this.use(behaviour)
            return behaviour.set
        }
    }

    private fun DecisionTreeNode.getEndingNodes(){
        this.use(this@GetPossibleEndingNodes)
    }

    // ---------------------- Функции поведения ---------------------------

    override fun <AnswerType : Any> process(node: LinkNode<AnswerType>) {
        node.next.values.forEach { it.getEndingNodes() }
        if(node.next.values.any{it is BranchResultNode})
            set.add(node)
        if(node.next.values.any{it is BranchResultNode && consideredNodes.contains(it)})
            correct = node
    }

    override fun process(node: FindActionNode) {
        //Особое поведение, так как не интересуют конечные узлы, которые используют несуществующие переменные
        node.next[node.getAnswer(situation)?:"none"]?.getEndingNodes()
        if(node.nextIfFound is BranchResultNode || node.nextIfNone is BranchResultNode)
            set.add(node)
        if(node.next.values.any{it is BranchResultNode && consideredNodes.contains(it)})
            correct = node
    }

    override fun process(node: BranchResultNode) {}

    override fun process(node: StartNode) {
        node.main.getEndingNodes()
    }

    override fun process(branch: ThoughtBranch) {
        branch.start.getEndingNodes()
    }

    override fun process(node: UndeterminedResultNode) {}
}