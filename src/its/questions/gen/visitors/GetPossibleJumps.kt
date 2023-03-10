package its.questions.gen.visitors

import its.model.nodes.*
import its.model.nodes.visitors.DecisionTreeBehaviour

class GetPossibleJumps( val answers: Map<String, String>) : DecisionTreeBehaviour<List<DecisionTreeNode>>{

    companion object _static{

        @JvmStatic
        fun DecisionTreeNode.getPossibleJumps(answers: Map<String, String>) : List<DecisionTreeNode>{
            return this.use(GetPossibleJumps(answers)).filter { it != this }
        }
    }

    private fun DecisionTreeNode.getPossibleJumps(): List<DecisionTreeNode>{
        return this.use(this@GetPossibleJumps)
    }


    override fun process(node: BranchResultNode): List<DecisionTreeNode> {
        return listOf(node)
    }

    override fun process(node: CycleAggregationNode): List<DecisionTreeNode> {
        val l = mutableListOf<DecisionTreeNode>(node)
        return l
    }

    override fun process(node: FindActionNode): List<DecisionTreeNode> {
        val l = mutableListOf<DecisionTreeNode>(node)
        l.addAll(node.next[answers[node.additionalInfo[ALIAS_ATR]]]!!.getPossibleJumps())
        return l
    }

    override fun process(node: LogicAggregationNode): List<DecisionTreeNode> {
        return listOf(node).plus(node.next[true]!!.getPossibleJumps()).plus(node.next[false]!!.getPossibleJumps())
    }

    override fun process(node: PredeterminingFactorsNode): List<DecisionTreeNode> {
        val l = mutableListOf<DecisionTreeNode>(node)
        node.next.values.forEach{l.addAll(it.getPossibleJumps())}
        return l
    }

    override fun process(node: QuestionNode): List<DecisionTreeNode> {
        val l = mutableListOf<DecisionTreeNode>(node)
        node.next.values.forEach{l.addAll(it.getPossibleJumps())}
        return l
    }

    override fun process(node: StartNode): List<DecisionTreeNode> {
        return listOf(node).plus(node.main.getPossibleJumps())
    }

    override fun process(branch: ThoughtBranch): List<DecisionTreeNode> {
        return listOf<DecisionTreeNode>().plus(branch.start.getPossibleJumps())
    }

    override fun process(node: UndeterminedResultNode): List<DecisionTreeNode> {
        return listOf<DecisionTreeNode>()
    }
}