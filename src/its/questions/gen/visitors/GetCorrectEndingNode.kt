package its.questions.gen.visitors

import its.model.nodes.BranchResultNode
import its.model.nodes.DecisionTreeNode
import its.model.nodes.ThoughtBranch
import its.model.nodes.visitors.DecisionTreeVisitor
import its.model.nodes.visitors.SimpleDecisionTreeVisitor

class GetCorrectEndingNode(val consideredNodes: List<DecisionTreeNode>) : SimpleDecisionTreeVisitor<Pair<DecisionTreeNode?, BranchResultNode?>>() {
    var endingNode: DecisionTreeNode? = null
    var resultNode: BranchResultNode? = null
    override fun process(node: DecisionTreeNode): Pair<DecisionTreeNode?, BranchResultNode?> {return endingNode to resultNode}
    override fun process(branch: ThoughtBranch): Pair<DecisionTreeNode?, BranchResultNode?> {return endingNode to resultNode}
    override fun process(
        branch: ThoughtBranch,
        info: Map<DecisionTreeVisitor.InfoSource, Pair<DecisionTreeNode?, BranchResultNode?>>
    ): Pair<DecisionTreeNode?, BranchResultNode?> {return endingNode to resultNode}

    override fun process(
        node: DecisionTreeNode,
        info: Map<DecisionTreeVisitor.InfoSource, Pair<DecisionTreeNode?, BranchResultNode?>>
    ): Pair<DecisionTreeNode?, BranchResultNode?> {
        if(info.any { (source, _) -> source.node is BranchResultNode && consideredNodes.contains(source.node)}){
            endingNode = node
            resultNode = info.keys.first { source -> source.node is BranchResultNode && consideredNodes.contains(source.node)}.node as BranchResultNode
        }
        return endingNode to resultNode
    }
}