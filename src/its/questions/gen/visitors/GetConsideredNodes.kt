package its.questions.gen.visitors

import its.model.expressions.Literal
import its.model.nodes.*
import its.model.nodes.visitors.DecisionTreeBehaviour
import its.questions.fileToMap

class GetConsideredNodes(val answers: Map<String, String>) : DecisionTreeBehaviour<List<DecisionTreeNode>> {
    constructor(path: String) : this(fileToMap(path, ':'))

    // ---------------------- Удобство ---------------------------

    private fun DecisionTreeNode.getAnswer(): String{
        return answers[this.additionalInfo[ALIAS_ATR]!!]!!
    }

    private fun DecisionTreeNode.getConsideredNodes(): List<DecisionTreeNode>{
        return this.use(this@GetConsideredNodes)
    }

    // ---------------------- Функции поведения ---------------------------

    override fun process(node: BranchResultNode): List<DecisionTreeNode> {
        return listOf(node)
    }

    override fun process(node: CycleAggregationNode): List<DecisionTreeNode> {
        val out = mutableListOf<DecisionTreeNode>(node)
        //TODO для разных значений цикла
        // out.addAll(node.thoughtBranch.consideredChildren())
        out.addAll(node.next[node.getAnswer().toBoolean()]!!.getConsideredNodes())
        return out.toList()
    }

    override fun process(node: FindActionNode): List<DecisionTreeNode> {
        val out = mutableListOf<DecisionTreeNode>(node)
        if(node.getAnswer() == "found")
            out.addAll(node.nextIfFound.getConsideredNodes())
        else
            out.addAll(node.nextIfNone!!.getConsideredNodes())
        return out.toList()
    }

    override fun process(node: LogicAggregationNode): List<DecisionTreeNode> {
        val out = mutableListOf<DecisionTreeNode>(node)
        //node.thoughtBranches.forEach{out.addAll(it.getConsideredNodes())}
        out.addAll(node.next[node.getAnswer().toBoolean()]!!.getConsideredNodes())
        return out.toList()
    }

    override fun process(node: PredeterminingFactorsNode): List<DecisionTreeNode> {
        val out = mutableListOf<DecisionTreeNode>(node)
        out.addAll(node.next[node.getAnswer()]!!.getConsideredNodes())
        return out.toList()
    }

    override fun process(node: QuestionNode): List<DecisionTreeNode> {
        val out = mutableListOf<DecisionTreeNode>(node)
        out.addAll(node.next[Literal.fromString(node.getAnswer(), node.type, node.enumOwner)]!!.getConsideredNodes())
        return out.toList()
    }

    override fun process(node: StartNode): List<DecisionTreeNode> {
        val out = mutableListOf<DecisionTreeNode>(node)
        out.addAll(node.main.getConsideredNodes())
        return out.toList()
    }

    override fun process(branch: ThoughtBranch): List<DecisionTreeNode> {
        val out = mutableListOf<DecisionTreeNode>(branch)
        out.addAll(branch.start.getConsideredNodes())
        return out.toList()
    }

    override fun process(node: UndeterminedResultNode): List<DecisionTreeNode> {
        return listOf(node)
    }
}