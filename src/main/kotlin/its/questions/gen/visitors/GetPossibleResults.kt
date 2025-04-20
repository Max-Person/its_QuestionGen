package its.questions.gen.visitors

import its.model.nodes.*
import its.model.nodes.visitors.SimpleDecisionTreeBehaviour

/**
 * TODO Class Description
 */
class GetPossibleResults : SimpleDecisionTreeBehaviour<Set<BranchResult>> {
    override fun <AnswerType : Any> process(node: LinkNode<AnswerType>): Set<BranchResult> {
        val possibleResults = node.children.flatMap { it.use(this) }.toMutableSet()
        if (node is AggregationNode) {
            possibleResults.addAll(BranchResult.entries.filter { !node.outcomes.containsKey(it) })
        }
        return possibleResults
    }

    override fun process(node: BranchResultNode): Set<BranchResult> {
        return setOf(node.value)
    }

    override fun process(branch: ThoughtBranch): Set<BranchResult> {
        return branch.start.use(this)
    }

}