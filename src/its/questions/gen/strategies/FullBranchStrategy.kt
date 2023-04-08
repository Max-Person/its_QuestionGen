package its.questions.gen.strategies

import its.model.nodes.BranchResultNode
import its.model.nodes.DecisionTreeNode
import its.model.nodes.LinkNode
import its.model.nodes.ThoughtBranch
import its.questions.inputs.TemplatingUtils._static.description
import its.questions.inputs.TemplatingUtils._static.endingCause
import its.questions.gen.states.*
import its.questions.gen.visitors.GetConsideredNodes._static.getConsideredNodes
import its.questions.gen.visitors.GetPossibleEndingNodes._static.getPossibleEndingNodes
import its.questions.gen.visitors.GetNodesLCA._static.getNodesLCA
import its.questions.inputs.LearningSituation

object FullBranchStrategy : QuestioningStrategy {

    private data class NodeLCAinfo(
        val node: DecisionTreeNode,
        val startAbove: Boolean,
    )

    override fun build(branch: ThoughtBranch) : QuestionAutomata{
        val sequential = SequentialStrategy.buildWithInfo(branch)

        val endingNodes = branch.getEndingNodes()
        val endingNodeSelectlinks = mutableListOf<GeneralQuestionState.QuestionStateLink<NodeLCAinfo>>()
        for(endA in endingNodes){
            for(endB in endingNodes.subList(endingNodes.indexOf(endA), endingNodes.size)){
                val lca = branch.getNodesLCA(endA, endB)!!
                endingNodeSelectlinks.add(GeneralQuestionState.QuestionStateLink(
                    {_, chosenAnswer -> chosenAnswer.node == lca && !chosenAnswer.startAbove }, sequential.info.nodeStates[lca]!!
                ))
                endingNodeSelectlinks.add(GeneralQuestionState.QuestionStateLink(
                    {_, chosenAnswer -> chosenAnswer.node == lca && chosenAnswer.startAbove }, sequential.info.preNodeStates[lca]!!
                ))
            }
        }

        val endingNodeSelect = object : SingleChoiceQuestionState<NodeLCAinfo>(endingNodeSelectlinks.toSet()){
            override fun text(situation: LearningSituation): String {
                return "Почему вы считаете, что ${branch.description(situation.templating, situation.assumedResult(branch)!!)}?"
            }

            override fun options(situation: LearningSituation): List<SingleChoiceOption<NodeLCAinfo>> {
                val considered = branch.getConsideredNodes(situation)
                val possibleEndingNodes = branch.getPossibleEndingNodes(situation)
                val correctEndingNode = endingNodes.first { considered.contains(it) }

                val options = possibleEndingNodes.map{end ->
                    val lca = branch.getNodesLCA(end, correctEndingNode)!!
                    SingleChoiceOption<NodeLCAinfo>(
                        end.endingCause(situation.templating),
                        Explanation("Давайте разберемся."),
                        NodeLCAinfo(lca, lca != end)
                    )
                }
                return options
            }
        }


        val variableValueAutomata = VariableValueStrategy.build(branch)
        variableValueAutomata.finalize(endingNodeSelect)
        return QuestionAutomata(variableValueAutomata.initState)
    }

    @JvmStatic
    private fun DecisionTreeNode.getEndingNodes() : List<DecisionTreeNode> {
        val list = mutableListOf<DecisionTreeNode>()
        if(this is LinkNode<*>){
            next.values.forEach { list.addAll(it.getEndingNodes()) }
            if(next.values.any{it is BranchResultNode })
                list.add(this)
        }
        else if(this is ThoughtBranch){
            list.addAll(this.start.getEndingNodes())
        }
        return list
    }
}