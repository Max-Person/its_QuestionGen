package its.questions.gen.visitors

import its.model.nodes.*
import its.model.nodes.visitors.DecisionTreeBehaviour

class GetEndingNodes private constructor(val consideredNodes: List<DecisionTreeNode>, val answers: Map<String, String>) : DecisionTreeBehaviour<Unit> {
    val set = mutableSetOf<DecisionTreeNode>()
    lateinit var correct : DecisionTreeNode

    // ---------------------- Удобства ---------------------------

    companion object _static{
        @JvmStatic
        fun ThoughtBranch.getAllEndingNodes(consideredNodes: List<DecisionTreeNode>, answers: Map<String, String>) : Set<DecisionTreeNode>{
            val behaviour = GetEndingNodes(consideredNodes, answers)
            this.use(behaviour)
            return behaviour.set
        }

        @JvmStatic
        fun ThoughtBranch.getCorrectEndingNode(consideredNodes: List<DecisionTreeNode>, answers: Map<String, String>) : DecisionTreeNode{
            val behaviour = GetEndingNodes(consideredNodes, answers)
            this.use(behaviour)
            return behaviour.correct
        }
    }

    private fun DecisionTreeNode.getEndingNodes(){
        this.use(this@GetEndingNodes)
    }

    fun <AnswerType : Any> process(node: LinkNode<AnswerType>) {
        node.next.values.forEach { it.getEndingNodes() }
        if(node.next.values.any{it is BranchResultNode})
            set.add(node)
        if(node.next.values.any{it is BranchResultNode && consideredNodes.contains(it)})
            correct = node
    }

    // ---------------------- Функции поведения ---------------------------
    
    override fun process(node: BranchResultNode) {}

    override fun process(node: CycleAggregationNode) {
        process(node as LinkNode<*>)
    }

    override fun process(node: FindActionNode) {
        //Особое поведение, так как не интересуют конечные узлы, которые используют несуществующие переменные
        node.next[node.getAnswer(answers)?:"none"]?.getEndingNodes()
        if(node.nextIfFound is BranchResultNode || node.nextIfNone is BranchResultNode)
            set.add(node)
        if(node.next.values.any{it is BranchResultNode && consideredNodes.contains(it)})
            correct = node
    }

    override fun process(node: LogicAggregationNode) {
        process(node as LinkNode<*>)
    }

    override fun process(node: PredeterminingFactorsNode) {
        process(node as LinkNode<*>)
    }

    override fun process(node: QuestionNode) {
        process(node as LinkNode<*>)
    }

    override fun process(node: StartNode) {
        node.main.getEndingNodes()
    }

    override fun process(branch: ThoughtBranch) {
        branch.start.getEndingNodes()
    }

    override fun process(node: UndeterminedResultNode) {}
}