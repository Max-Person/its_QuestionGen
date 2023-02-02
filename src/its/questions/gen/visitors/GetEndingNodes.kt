package its.questions.gen.visitors

import its.model.nodes.*
import its.model.nodes.visitors.DecisionTreeBehaviour

class GetEndingNodes(val consideredNodes: List<DecisionTreeNode>) : DecisionTreeBehaviour<Unit> {
    val set = mutableSetOf<DecisionTreeNode>()
    lateinit var correct : DecisionTreeNode

    private fun DecisionTreeNode.getEndingNodes(){
        this.use(this@GetEndingNodes)
    }
    
    override fun process(node: BranchResultNode) {}

    override fun process(node: CycleAggregationNode) {
        node.next.values.forEach { it.getEndingNodes() }
        if(node.next.values.any{it is BranchResultNode})
            set.add(node)
        if(node.next.values.any{it is BranchResultNode && consideredNodes.contains(it)})
            correct = node
    }

    override fun process(node: FindActionNode) {
        node.nextIfFound.getEndingNodes()
        node.nextIfNone?.getEndingNodes()
        if(node.nextIfFound is BranchResultNode || node.nextIfNone is BranchResultNode)
            set.add(node)
        if(node.nextIfFound is BranchResultNode && consideredNodes.contains(node.nextIfFound) ||
            node.nextIfNone is BranchResultNode && consideredNodes.contains(node.nextIfNone) )
            correct = node
    }

    override fun process(node: LogicAggregationNode) {
        node.next.values.forEach { it.getEndingNodes() }
        if(node.next.values.any{it is BranchResultNode})
            set.add(node)
        if(node.next.values.any{it is BranchResultNode && consideredNodes.contains(it)})
            correct = node
    }

    override fun process(node: PredeterminingFactorsNode) {
        node.undetermined.getEndingNodes()
        if(node.undetermined is BranchResultNode)
            set.add(node)
        if(node.undetermined is BranchResultNode && consideredNodes.contains(node.undetermined))
            correct = node
    }

    override fun process(node: QuestionNode) {
        node.next.values.forEach { it.getEndingNodes() }
        if(node.next.values.any{it is BranchResultNode})
            set.add(node)
        if(node.next.values.any{it is BranchResultNode && consideredNodes.contains(it)})
            correct = node
    }

    override fun process(node: StartNode) {
        node.main.getEndingNodes()
    }

    override fun process(branch: ThoughtBranch) {
        branch.start.getEndingNodes()
    }

    override fun process(node: UndeterminedResultNode) {}
}