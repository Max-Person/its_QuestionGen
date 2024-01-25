package its.questions.gen.visitors

import its.model.definition.ThisShouldNotHappen
import its.model.nodes.*
import its.model.nodes.visitors.SimpleDecisionTreeBehaviour

//TODO переделать с использованием parent
class GetNodesLCA private constructor(val a : DecisionTreeNode, val b : DecisionTreeNode) : SimpleDecisionTreeBehaviour<Int> {
    var lca : DecisionTreeNode? = null
    var previous : DecisionTreeNode? = null

    // ---------------------- Удобства ---------------------------

    companion object _static{
        const val none_found = 0
        const val a_found = 1
        const val b_found = 2

        @JvmStatic
        fun ThoughtBranch.getNodesLCA(a : DecisionTreeNode, b : DecisionTreeNode) : DecisionTreeNode?{
            return start.getNodesLCA(a, b)
        }

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

    // ---------------------- Функции поведения ---------------------------

    override fun <AnswerType> process(node: LinkNode<AnswerType>): Int {
        var res = none_found
        if(node == a)
            res = res or a_found
        if(node == b)
            res = res or b_found
        node.outcomes.forEach { res = res or it.node.getNodesLCA() }
        if(res and a_found != 0 && res and b_found != 0 ){
            if(lca == null)
                lca = node
            else if(previous == null)
                previous = node
        }
        return res
    }

    override fun process(node: BranchResultNode): Int {
        if(node == a)
            return a_found
        if(node == b)
            return b_found
        return none_found
    }

    override fun process(branch: ThoughtBranch): Int {
        throw ThisShouldNotHappen()
    }
}