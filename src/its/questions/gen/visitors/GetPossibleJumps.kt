package its.questions.gen.visitors

import its.model.nodes.*
import its.model.nodes.visitors.DecisionTreeBehaviour

class GetPossibleJumps( val knownVariables : Set<String>) : DecisionTreeBehaviour<List<DecisionTreeNode>>{

    companion object _static{

        @JvmStatic
        fun DecisionTreeNode.getPossibleJumps(knownVariables : Set<String>) : List<DecisionTreeNode>{
            return this.use(GetPossibleJumps(knownVariables)).filter { it != this }
        }
    }

    private fun DecisionTreeNode.getPossibleJumps(): List<DecisionTreeNode>{
        return this.use(this@GetPossibleJumps)
    }


    override fun process(node: BranchResultNode): List<DecisionTreeNode> {
        return listOf(node)
    }

    override fun process(node: CycleAggregationNode): List<DecisionTreeNode> {
        val l = mutableListOf<DecisionTreeNode>()
        if(knownVariables.containsAll(node.selectorExpr.accept(GetUsedVariables())))
            l.add(node)
        node.next.values.forEach{l.addAll(it.getPossibleJumps())}
        return l
    }

    override fun process(node: FindActionNode): List<DecisionTreeNode> {
        val l = mutableListOf<DecisionTreeNode>()
        if(knownVariables.containsAll(node.selectorExpr.accept(GetUsedVariables())))
            l.add(node)
        node.next.values.forEach{l.addAll(it.getPossibleJumps())}
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
        val l = mutableListOf<DecisionTreeNode>()
        if(knownVariables.containsAll(node.expr.accept(GetUsedVariables())))
            l.add(node)
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