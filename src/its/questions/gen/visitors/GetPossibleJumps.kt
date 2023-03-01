package its.questions.gen.visitors

import its.model.nodes.*
import its.model.nodes.visitors.DecisionTreeBehaviour

class GetPossibleJumps private constructor( val answers: Map<String, String>) : DecisionTreeBehaviour<List<DecisionTreeNode>>{

    // ---------------------- Удобства ---------------------------

    companion object _static{
        @JvmStatic
        fun DecisionTreeNode.getPossibleJumps(answers: Map<String, String>) : List<DecisionTreeNode>{
            return this.use(GetPossibleJumps(answers)).filter { it != this }
        }
    }

    private fun DecisionTreeNode.getPossibleJumps(): List<DecisionTreeNode>{
        return this.use(this@GetPossibleJumps)
    }

    fun <AnswerType : Any> process(node: LinkNode<AnswerType>): List<DecisionTreeNode> {
        val l = mutableListOf<DecisionTreeNode>(node)
        node.next.values.forEach { l.addAll(it.getPossibleJumps()) }
        return l
    }

    // ---------------------- Функции поведения ---------------------------

    override fun process(node: BranchResultNode): List<DecisionTreeNode> {
        return listOf(node)
    }

    override fun process(node: CycleAggregationNode): List<DecisionTreeNode> {
        return process(node as LinkNode<*>)
    }

    override fun process(node: FindActionNode): List<DecisionTreeNode> {
        //Особое поведение, так как не интересуют конечные узлы, которые используют несуществующие переменные
        val l = mutableListOf<DecisionTreeNode>(node)
        val next = node.next[node.getAnswer(answers)?:"none"]
        if(next != null)
            l.addAll(next.getPossibleJumps())
        return l
    }

    override fun process(node: LogicAggregationNode): List<DecisionTreeNode> {
        return process(node as LinkNode<*>)
    }

    override fun process(node: PredeterminingFactorsNode): List<DecisionTreeNode> {
        return process(node as LinkNode<*>)
    }

    override fun process(node: QuestionNode): List<DecisionTreeNode> {
        return process(node as LinkNode<*>)
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