package its.questions.gen.strategies

import its.model.nodes.DecisionTreeNode
import its.model.nodes.ThoughtBranch
import its.questions.gen.QuestioningSituation
import its.questions.gen.formulations.TemplatingUtils.description
import its.questions.gen.formulations.TemplatingUtils.endingCause
import its.questions.gen.states.*
import its.questions.gen.visitors.GetNodesLCA._static.getNodesLCA
import its.questions.gen.visitors.GetPossibleEndingNodes
import its.reasoner.nodes.DecisionTreeReasoner.Companion.solve

object FullBranchStrategy : QuestioningStrategy {

    private data class NodeLCAinfo(
        val node: DecisionTreeNode,
        val startAbove: Boolean,
    )

    override fun build(branch: ThoughtBranch) : QuestionAutomata{
        val sequential = SequentialStrategy.buildWithInfo(branch)
        val variableValueAutomata = VariableValueStrategy.build(branch)

        val endingNodes = GetPossibleEndingNodes(branch).get().endingNodes.toList()
        if(endingNodes.size <= 1){
            variableValueAutomata.finalize(sequential.automata.initState)
            return QuestionAutomata(variableValueAutomata.initState).finalizeForBranch(branch)
        }

        val endingNodeSelect = object : SingleChoiceQuestionState<NodeLCAinfo>() {
            override fun text(situation: QuestioningSituation): String {
                return situation.assumedResult(branch)
                           ?.let { assumedResult -> situation.localization.WHY_DO_YOU_THINK_THAT(branch.description(situation, assumedResult)) }
                       ?:situation.localization.WHICH_IS_TRUE_HERE
            }

            override fun options(situation: QuestioningSituation): List<SingleChoiceOption<NodeLCAinfo>> {
                val (possibleEndingNodes, correctEndingNode) = GetPossibleEndingNodes(branch, situation).get()

                val options = possibleEndingNodes.map{end ->
                    val lca = branch.getNodesLCA(end, correctEndingNode!!)!!
                    SingleChoiceOption<NodeLCAinfo>(
                        end.endingCause(situation),
                        Explanation(situation.localization.LETS_FIGURE_IT_OUT),
                        NodeLCAinfo(lca, lca != end)
                    )
                }
                return options
            }
        }

        val knownLCAs = mutableSetOf<DecisionTreeNode>()
        for (endA in endingNodes) {
            for (endB in endingNodes.subList(endingNodes.indexOf(endA), endingNodes.size)) {
                val lca = branch.getNodesLCA(endA, endB)!!
                if (knownLCAs.contains(lca)) continue
                knownLCAs.add(lca)

                endingNodeSelect.linkTo(sequential.info.nodeStates[lca]!!) { _, chosenAnswer ->
                    chosenAnswer.node == lca && !chosenAnswer.startAbove
                }
                endingNodeSelect.linkTo(sequential.info.preNodeStates[lca]!!) { _, chosenAnswer ->
                    chosenAnswer.node == lca && chosenAnswer.startAbove
                }
            }
        }


        variableValueAutomata.finalize(endingNodeSelect)
        return QuestionAutomata(variableValueAutomata.initState).finalizeForBranch(branch)
    }

    private fun QuestionAutomata.finalizeForBranch(branch: ThoughtBranch): QuestionAutomata{
        val redir = RedirectQuestionState()
        val branchEnd = object : SkipQuestionState() {
            override fun skip(situation: QuestioningSituation): QuestionStateChange {
                val explanation = Explanation(situation.localization.SO_WEVE_DISCUSSED_WHY(
                    branch.description(situation, branch.solve(situation).branchResult)
                )
                )
                return QuestionStateChange(explanation, redir)
            }

            override val reachableStates: Collection<QuestionState>
                get() = listOf(redir)
        }
        this.finalize(branchEnd)
        return QuestionAutomata(this.initState)
    }
}