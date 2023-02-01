package its.questions.gen.visitors

import its.model.nodes.BranchResultNode
import its.model.nodes.DecisionTreeNode
import its.model.nodes.ThoughtBranch
import its.model.nodes.visitors.DecisionTreeVisitor
import its.model.nodes.visitors.SimpleDecisionTreeVisitor

class GetEndingNodes : SimpleDecisionTreeVisitor<Set<DecisionTreeNode>>() {
    val set = mutableSetOf<DecisionTreeNode>()
    override fun process(node: DecisionTreeNode): Set<DecisionTreeNode>  {return set}
    override fun process(branch: ThoughtBranch): Set<DecisionTreeNode>  {return set}
    override fun process(
        branch: ThoughtBranch,
        info: Map<DecisionTreeVisitor.InfoSource, Set<DecisionTreeNode>>
    ): Set<DecisionTreeNode> {return set}

    override fun process(
        node: DecisionTreeNode,
        info: Map<DecisionTreeVisitor.InfoSource, Set<DecisionTreeNode>>
    ): Set<DecisionTreeNode> {
        if(info.any{(source, _) -> source.node is BranchResultNode})
            set.add(node)
        return set
    }
}