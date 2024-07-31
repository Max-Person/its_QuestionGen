package its.questions.gen.strategies

import its.model.nodes.BranchResultNode
import its.model.nodes.DecisionTreeNode
import its.model.nodes.LinkNode
import its.model.nodes.ThoughtBranch
import its.questions.gen.QuestioningSituation
import its.questions.gen.formulations.TemplatingUtils.description
import its.questions.gen.formulations.TemplatingUtils.endingCause
import its.questions.gen.states.*
import its.questions.gen.visitors.GetNodesLCA._static.getNodesLCA
import its.questions.gen.visitors.GetPossibleEndingNodes._static.getPossibleEndingNodes
import its.reasoner.nodes.DecisionTreeReasoner._static.getAnswer
import its.reasoner.nodes.DecisionTreeReasoner._static.getCorrectPath

object FullBranchStrategy : QuestioningStrategy {

    private data class NodeLCAinfo(
        val node: DecisionTreeNode,
        val startAbove: Boolean,
    )

    override fun build(branch: ThoughtBranch) : QuestionAutomata{
        val sequential = SequentialStrategy.buildWithInfo(branch)
        val variableValueAutomata = VariableValueStrategy.build(branch)

        val endingNodes = branch.start.getEndingNodes()
        if(endingNodes.size <= 1){
            variableValueAutomata.finalize(sequential.automata.initState)
            return QuestionAutomata(variableValueAutomata.initState).finalizeForBranch(branch)
        }

        val knownLCAs = mutableSetOf<DecisionTreeNode>()
        val endingNodeSelectlinks = mutableListOf<GeneralQuestionState.QuestionStateLink<NodeLCAinfo>>()
        for(endA in endingNodes){
            for(endB in endingNodes.subList(endingNodes.indexOf(endA), endingNodes.size)){
                val lca = branch.getNodesLCA(endA, endB)!!
                if(knownLCAs.contains(lca))
                    continue
                knownLCAs.add(lca)
                endingNodeSelectlinks.add(GeneralQuestionState.QuestionStateLink(
                    {_, chosenAnswer -> chosenAnswer.node == lca && !chosenAnswer.startAbove },
                    sequential.info.nodeStates[lca]!!
                ))
                endingNodeSelectlinks.add(GeneralQuestionState.QuestionStateLink(
                    {_, chosenAnswer -> chosenAnswer.node == lca && chosenAnswer.startAbove },
                    sequential.info.preNodeStates[lca]!!
                ))
            }
        }

        val endingNodeSelect = object : SingleChoiceQuestionState<NodeLCAinfo>(endingNodeSelectlinks.toSet()){
            override fun text(situation: QuestioningSituation): String {
                return situation.localization.WHY_DO_YOU_THINK_THAT(assumed_result = branch.description(situation.localizationCode, situation.templating, situation.assumedResult(branch)!!))
            }

            override fun options(situation: QuestioningSituation): List<SingleChoiceOption<NodeLCAinfo>> {
                val considered = branch.getCorrectPath(situation)
                val possibleEndingNodes = branch.getPossibleEndingNodes(situation)
                val correctEndingNode = endingNodes.first { considered.contains(it) }

                val options = possibleEndingNodes.map{end ->
                    val lca = branch.getNodesLCA(end, correctEndingNode)!!
                    SingleChoiceOption<NodeLCAinfo>(
                        end.endingCause(situation.localizationCode, situation.templating),
                        Explanation(situation.localization.LETS_FIGURE_IT_OUT),
                        NodeLCAinfo(lca, lca != end)
                    )
                }
                return options
            }
        }


        variableValueAutomata.finalize(endingNodeSelect)
        return QuestionAutomata(variableValueAutomata.initState).finalizeForBranch(branch)
    }

    private fun QuestionAutomata.finalizeForBranch(branch: ThoughtBranch): QuestionAutomata{
        val redir = RedirectQuestionState()
        val branchEnd = object : SkipQuestionState() {
            override fun skip(situation: QuestioningSituation): QuestionStateChange {
                val explanation = Explanation(situation.localization.SO_WEVE_DISCUSSED_WHY(result = branch.description(situation.localizationCode, situation.templating, branch.getAnswer(situation))))
                return QuestionStateChange(explanation, redir)
            }

            override val reachableStates: Collection<QuestionState>
                get() = listOf(redir)
        }
        this.finalize(branchEnd)
        return QuestionAutomata(this.initState)
    }

    @JvmStatic
    private fun DecisionTreeNode.getEndingNodes() : List<DecisionTreeNode> {
        val list = mutableListOf<DecisionTreeNode>()
        if(this is LinkNode<*>){
            outcomes.forEach { list.addAll(it.node.getEndingNodes()) }
            if(outcomes.any{it.node is BranchResultNode })
                list.add(this)
        }
        return list
    }
}