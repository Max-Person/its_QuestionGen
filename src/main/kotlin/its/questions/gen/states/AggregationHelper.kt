package its.questions.gen.states

import its.model.definition.types.Obj
import its.model.nodes.*
import its.questions.gen.QuestioningSituation
import its.questions.gen.formulations.TemplatingUtils.alias
import its.questions.gen.formulations.TemplatingUtils.description
import its.reasoner.nodes.DecisionTreeReasoner
import its.reasoner.nodes.DecisionTreeReasoner.Companion.solve

/**
 * TODO Class Description
 */
sealed class AggregationHelper<Node : AggregationNode, BranchInfo>(
    val node: Node,
) {
    abstract fun getBranchInfo(situation: QuestioningSituation): List<BranchInfo>
    abstract fun getBranchAlias(branchInfo: BranchInfo): String
    abstract fun getBranchResult(situation: QuestioningSituation, branchInfo: BranchInfo): BranchResult
    abstract fun getBranchDescription(
        situation: QuestioningSituation,
        branchInfo: BranchInfo,
        result: BranchResult,
    ): String

    fun getBranchDescriptions(situation: QuestioningSituation) =
        getBranchInfo(situation).map { getBranchDescription(situation, it, BranchResult.CORRECT) }

    fun addAssumedResult(branchInfo: BranchInfo, situation: QuestioningSituation, result: BranchResult) {
        situation.assumedResults[getBranchAlias(branchInfo)] = result
    }

    fun getAssumedResult(branchInfo: BranchInfo, situation: QuestioningSituation): BranchResult? {
        return situation.assumedResults[getBranchAlias(branchInfo)]
    }

    abstract fun getThoughtBranches(): List<ThoughtBranch>
    abstract fun getThoughtBranch(branchInfo: BranchInfo): ThoughtBranch

    open fun onGoIntoBranch(situation: QuestioningSituation, branchInfo: BranchInfo) {}
}

class BranchAggregationHelper(node: BranchAggregationNode) :
    AggregationHelper<BranchAggregationNode, ThoughtBranch>(node) {

    override fun getBranchInfo(situation: QuestioningSituation): List<ThoughtBranch> {
        return node.thoughtBranches
    }

    override fun getBranchAlias(branchInfo: ThoughtBranch): String {
        return branchInfo.alias
    }

    override fun getBranchResult(situation: QuestioningSituation, branchInfo: ThoughtBranch): BranchResult {
        return branchInfo.solve(situation.forEval()).branchResult
    }

    override fun getBranchDescription(
        situation: QuestioningSituation,
        branchInfo: ThoughtBranch,
        result: BranchResult,
    ): String {
        return branchInfo.description(situation, result)
    }


    override fun getThoughtBranches(): List<ThoughtBranch> {
        return node.thoughtBranches
    }

    override fun getThoughtBranch(branchInfo: ThoughtBranch): ThoughtBranch {
        return branchInfo
    }
}

class CycleAggregationHelper(node: CycleAggregationNode) : AggregationHelper<CycleAggregationNode, Obj>(node) {

    override fun getBranchInfo(situation: QuestioningSituation): List<Obj> {
        return DecisionTreeReasoner(situation).searchWithErrors(node).correct
    }

    override fun getBranchDescription(situation: QuestioningSituation, branchInfo: Obj, result: BranchResult): String {
        val varName = node.variable.varName
        val alreadyContains = situation.decisionTreeVariables.containsKey(varName)
        situation.decisionTreeVariables[varName] = branchInfo
        val description = node.thoughtBranch.description(situation, result)
        if (alreadyContains) situation.decisionTreeVariables.remove(varName)
        return description
    }

    override fun getBranchResult(situation: QuestioningSituation, branchInfo: Obj): BranchResult {
        return node.thoughtBranch.solve(situation.forEval()
            .also { it.decisionTreeVariables[node.variable.varName] = branchInfo }).branchResult
    }

    override fun getBranchAlias(branchInfo: Obj): String {
        return node.thoughtBranch.alias + branchInfo.objectName
    }


    override fun getThoughtBranches(): List<ThoughtBranch> {
        return listOf(node.thoughtBranch)
    }

    override fun getThoughtBranch(branchInfo: Obj): ThoughtBranch {
        return node.thoughtBranch
    }

    override fun onGoIntoBranch(situation: QuestioningSituation, branchInfo: Obj) {
        situation.decisionTreeVariables[node.variable.varName] = branchInfo
    }
}