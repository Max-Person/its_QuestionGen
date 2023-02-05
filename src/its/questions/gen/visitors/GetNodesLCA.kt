package its.questions.gen.visitors

import its.model.nodes.*
import its.model.nodes.visitors.DecisionTreeBehaviour

class GetNodesLCA(val a : DecisionTreeNode, val b : DecisionTreeNode) : DecisionTreeBehaviour<Int> {
    var lca : DecisionTreeNode? = null

    companion object _static{
        const val none_found = 0
        const val a_found = 1
        const val b_found = 2

        @JvmStatic
        fun DecisionTreeNode.getNodesLCA(a : DecisionTreeNode, b : DecisionTreeNode) : DecisionTreeNode?{
            val v = GetNodesLCA(a, b)
            this.use(v)
            return v.lca
        }
    }

    private fun DecisionTreeNode.getNodesLCA(): Int{
        return this.use(this@GetNodesLCA)
    }


    override fun process(node: BranchResultNode): Int {
        if(node == a)
            return a_found
        if(node == b)
            return b_found
        return none_found
    }

    override fun process(node: CycleAggregationNode): Int {
        var res = none_found
        if(node == a)
            res = res or a_found
        if(node == b)
            res = res or b_found
        node.next.values.forEach { res = res or it.getNodesLCA() }
        if(res and a_found != 0 && res and b_found != 0 && lca == null)
            lca = node
        return res
    }

    override fun process(node: FindActionNode): Int {
        var res = none_found
        if(node == a)
            res = res or a_found
        if(node == b)
            res = res or b_found
        node.next.values.forEach { res = res or it.getNodesLCA() }
        if(res and a_found != 0 && res and b_found != 0 && lca == null)
            lca = node
        return res
    }

    override fun process(node: LogicAggregationNode): Int {
        var res = none_found
        if(node == a)
            res = res or a_found
        if(node == b)
            res = res or b_found
        node.next.values.forEach { res = res or it.getNodesLCA() }
        if(res and a_found != 0 && res and b_found != 0 && lca == null)
            lca = node
        return res
    }

    override fun process(node: PredeterminingFactorsNode): Int {
        var res = none_found
        if(node == a)
            res = res or a_found
        if(node == b)
            res = res or b_found
        node.next.values.forEach { res = res or it.getNodesLCA() }
        if(res and a_found != 0 && res and b_found != 0 && lca == null)
            lca = node
        return res
    }

    override fun process(node: QuestionNode): Int {
        var res = none_found
        if(node == a)
            res = res or a_found
        if(node == b)
            res = res or b_found
        node.next.values.forEach { res = res or it.getNodesLCA() }
        if(res and a_found != 0 && res and b_found != 0 && lca == null)
            lca = node
        return res
    }

    override fun process(node: StartNode): Int {
        var res = none_found
        if(node == a)
            res = res or a_found
        if(node == b)
            res = res or b_found
        res = res or node.main.getNodesLCA()
        if(res and a_found != 0 && res and b_found != 0 && lca == null)
            lca = node
        return res
    }

    override fun process(branch: ThoughtBranch): Int {
        var res = none_found
        if(branch == a)
            res = res or a_found
        if(branch == b)
            res = res or b_found
        res = res or branch.start.getNodesLCA()
        if(res and a_found != 0 && res and b_found != 0 && lca == null)
            lca = branch
        return res
    }

    override fun process(node: UndeterminedResultNode): Int {
        if(node == a)
            return a_found
        if(node == b)
            return b_found
        return none_found
    }
}