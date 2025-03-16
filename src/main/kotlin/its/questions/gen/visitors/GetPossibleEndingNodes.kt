package its.questions.gen.visitors

import its.model.nodes.*
import its.model.nodes.visitors.SimpleDecisionTreeBehaviour
import its.questions.gen.QuestioningSituation
import its.reasoner.AmbiguousObjectException
import its.reasoner.nodes.DecisionTreeReasoner.Companion.getAnswer
import its.reasoner.nodes.DecisionTreeReasoner.Companion.solve

class GetPossibleEndingNodes private constructor(val branch: ThoughtBranch, val situation: QuestioningSituation) :
    SimpleDecisionTreeBehaviour<Unit> {
        // TODO подумать что будет если это узел агрегации, сейчас только бранч резулт ноде
    val correctEndingNode: DecisionTreeNode = branch.solve(situation).resultingNode
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
        try {
            this.use(this@GetPossibleEndingNodes)
        }
        catch (_: AmbiguousObjectException){}
    }

    // ---------------------- Функции поведения ---------------------------

    override fun <AnswerType : Any> process(node: LinkNode<AnswerType>) {
        node.outcomes.forEach { it.node.getEndingNodes() }
        if(node.outcomes.any{it.node is BranchResultNode})
            set.add(node)
        if(node.outcomes.any{it.node is BranchResultNode && correctEndingNode == it.node})
            correct = node
    }

    override fun process(node: FindActionNode) {
        //Особое поведение, так как не интересуют конечные узлы, которые используют несуществующие переменные
        node.outcomes[node.getAnswer(situation)]?.node?.getEndingNodes()
        if(node.outcomes.any{it.node is BranchResultNode})
            set.add(node)
        if(node.outcomes.any{it.node is BranchResultNode && correctEndingNode == it.node})
            correct = node
    }

    override fun process(node: BranchResultNode) {}

    override fun process(branch: ThoughtBranch) {
        branch.start.getEndingNodes()
    }
}