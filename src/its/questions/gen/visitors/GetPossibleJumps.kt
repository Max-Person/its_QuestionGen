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
        if(!knownVariables.containsAll(node.selectorExpr.accept(GetUsedVariables())))
            return listOf()
        return listOf(node).plus(node.next[true]!!.getPossibleJumps()).plus(node.next[false]!!.getPossibleJumps())
    }

    override fun process(node: FindActionNode): List<DecisionTreeNode> {
        if(!knownVariables.containsAll(node.selectorExpr.accept(GetUsedVariables())))
            return listOf()
        return listOf(node).plus(node.nextIfFound.getPossibleJumps()).let { if(node.nextIfNone != null) it.plus(node.nextIfNone!!.getPossibleJumps()) else it }
    }

    override fun process(node: LogicAggregationNode): List<DecisionTreeNode> {
        return listOf(node).plus(node.next[true]!!.getPossibleJumps()).plus(node.next[false]!!.getPossibleJumps())
    }

    override fun process(node: PredeterminingFactorsNode): List<DecisionTreeNode> {
        return listOf(node).plus(node.undetermined.getPossibleJumps()) //TODO логика обработки предрешающих факторов
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
        return listOf<DecisionTreeNode>().plus(node.linkedPredetermining.getPossibleJumps()) //TODO
    }
}