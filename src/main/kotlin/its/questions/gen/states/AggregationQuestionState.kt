package its.questions.gen.states

import its.model.definition.types.Obj
import its.model.nodes.*
import its.questions.gen.QuestioningSituation
import its.questions.gen.formulations.Localization
import its.questions.gen.formulations.TemplatingUtils.alias
import its.questions.gen.formulations.TemplatingUtils.description
import its.questions.gen.strategies.QuestionAutomata
import its.questions.gen.strategies.QuestioningStrategy
import its.reasoner.nodes.DecisionTreeReasoner
import its.reasoner.nodes.DecisionTreeReasoner.Companion.getAnswer
import its.reasoner.nodes.DecisionTreeReasoner.Companion.solve

//Можно выделить над ним некий AssociationQuestionState, но пока что это не нужно
sealed class AggregationQuestionState<Node : AggregationNode, BranchInfo>(
    val node: Node,
    nodeResultNextStates: Map<BranchResult, QuestionState>,
) : GeneralQuestionState<Boolean>() {
    init { //Если ответили правильно, то переходим к выходам из узла
        nodeResultNextStates.forEach { (nodeResult, nextState) ->
            linkTo(nextState) { situation, answer ->
                answer && node.getAnswer(situation.forEval()) == nodeResult
            }
        }

        //Если хотя бы одна неправильная, то спросить в какую углубиться
        val (branchSelectQuestion, branchAutomata) = createSelectBranchState()
        linkTo(branchSelectQuestion) { situation, chosenAnswer ->
            !chosenAnswer
        }

        //После выхода из веток пропускающее состояние определяет, к какому из верных ответов надо совершить переход
        val shadowSkip = object : SkipQuestionState() {
            override fun skip(situation: QuestioningSituation): QuestionStateChange {
                return QuestionStateChange(null, nodeResultNextStates[node.getAnswer(situation)])
            }

            override val reachableStates: Collection<QuestionState>
                get() = nodeResultNextStates.values
        }
        branchAutomata.forEach { it.finalize(shadowSkip) }
    }

    enum class AggregationImpact{
        Negative,
        None,
        Positive,
    }

    abstract fun getBranchInfo(situation: QuestioningSituation) : List<BranchInfo>
    abstract fun getBranchAlias(branchInfo: BranchInfo) : String
    abstract fun getBranchResult(situation: QuestioningSituation, branchInfo: BranchInfo): BranchResult
    abstract fun getBranchDescription(situation: QuestioningSituation, branchInfo: BranchInfo, result: BranchResult) : String

    protected fun text(situation: QuestioningSituation) : String {
        val assumedAnswer = situation.assumedResults[node.alias]!!
        val descrIncorrect = node.description(situation, assumedAnswer)
        return situation.localization.WHY_DO_YOU_THINK_THAT(descrIncorrect)
    }

    override fun getQuestion(situation: QuestioningSituation): QuestionStateResult {
        val text = text(situation).prependId()
        val options = getBranchInfo(situation).map{getBranchDescription(situation, it, BranchResult.CORRECT)}
        return Question(text, options, QuestionType.matching)
    }

    companion object _static {
        @JvmStatic
        fun aggregationMatching(l: Localization) = mapOf(l.TRUE to 1, l.NOT_IMPORTANT to 0, l.FALSE to -1)
    }

    override fun proceedWithAnswer(situation: QuestioningSituation, answer: List<Int>): QuestionStateChange {
        val branches = getBranchInfo(situation)
        val givenAnswer : Map<BranchInfo, AggregationImpact> = branches.mapIndexed{ ind, branch ->
            val op = answer[ind]
            branch to
                    if(op < 0) AggregationImpact.Negative
                    else if(op > 0) AggregationImpact.Positive
                    else AggregationImpact.None
        }.toMap()

        val results : Map<BranchInfo, BranchResult> = branches
            .map{ branch -> branch to getBranchResult(situation, branch)}
            .toMap()
        val actualImpact : Map<BranchInfo, AggregationImpact> = results.map{ (branch, res) ->
            branch to
                    if (res == BranchResult.CORRECT) AggregationImpact.Positive // TODO
                    else AggregationImpact.Negative
        }.toMap()

        val nodeRes = node.getAnswer(situation.forEval())
        val incorrectBranches = givenAnswer.filter{(branch, impact) -> impact != AggregationImpact.None && impact != actualImpact[branch]}.keys
        val missedBranches = givenAnswer.filter{(branch, impact) ->
            impact == AggregationImpact.None &&
                    !(node.aggregationMethod == AggregationMethod.AND && nodeRes == BranchResult.ERROR
                            && results[branch] == BranchResult.CORRECT) &&
                    !(node.aggregationMethod == AggregationMethod.OR && nodeRes == BranchResult.CORRECT
                            && results[branch] == BranchResult.ERROR)
        }.keys

        // TODO спросить про результат ветки при углублении чтобы узнать предполагаемый результат
//        branches.forEach { branch ->
//            val branchResult = results[branch]!!
//            val assumedResult = if(incorrectBranches.contains(branch) || missedBranches.contains(branch)) {
//                !branchResult
//            } else {
//                branchResult
//            }
//            situation.assumedResults.put(getBranchAlias(branch), assumedResult)
//            situation.addAssumedResult(getThoughtBranch(branch), assumedResult)
//        }

        val isAnswerCorrect = incorrectBranches.isEmpty() && missedBranches.isEmpty()
        val explanation = Explanation(
            if (isAnswerCorrect) {
                situation.localization.AGGREGATION_CORRECT_EXPL(
                    answer_descr = node.description(
                        situation,
                        nodeRes
                    ),
                    branches_descr = branches
                        .filter { results[it] == nodeRes }
                        .map { getBranchDescription(situation, it, nodeRes) }
                        .joinToString(", ")
                )
            } else {
                (if (!incorrectBranches.isEmpty())
                    situation.localization.AGGREGATION_INCORRECT_BRANCHES_DESCR(
                        incorrectBranches
                            .map { getBranchDescription(situation, it, results[it]!!) }
                            .joinToString(", ")
                    )
                else
                    "").plus(
                    if (!missedBranches.isEmpty()) {
                        val missed_branches_descr = missedBranches
                            .map { getBranchDescription(situation, it, results[it]!!) }
                            .joinToString(", ")
                        if (incorrectBranches.isEmpty())
                            situation.localization.AGGREGATION_MISSED_BRANCHES_DESCR_PRIMARY(missed_branches_descr)
                        else
                            situation.localization.AGGREGATION_MISSED_BRANCHES_DESCR_CONCAT(missed_branches_descr)
                    } else ""
                )
            },
            type = ExplanationType.Error
        )

        return QuestionStateChange(explanation, getStateFromLinks(situation, isAnswerCorrect))
    }

    abstract fun getThoughtBranches(): List<ThoughtBranch>
    abstract fun getThoughtBranch(branchInfo: BranchInfo): ThoughtBranch
    open fun onGoIntoBranch(situation: QuestioningSituation, branchInfo: BranchInfo){}

    data class SelectBranchState<BranchInfo>(
        val state: SingleChoiceQuestionState<BranchInfo>,
        val branchAutomata: List<QuestionAutomata>,
    )

    fun createSelectBranchState(): SelectBranchState<BranchInfo> {
        //Если были сделаны ошибки в ветках, то спросить про углубление в одну из веток
        val state = object : SingleChoiceQuestionState<BranchInfo>() {
            override fun text(situation: QuestioningSituation): String {
                return situation.localization.WHAT_DO_YOU_WANT_TO_DISCUSS_FURTHER
            }

            override fun options(situation: QuestioningSituation): List<SingleChoiceOption<BranchInfo>> {
                val branchInfos = getBranchInfo(situation)
                val branchResults = branchInfos.associateWith { getBranchResult(situation, it) }
                val options = branchInfos
                    .filter{branch ->
                        branchResults[branch] != situation.assumedResults[getBranchAlias(branch)]
                    }.map { branch ->
                        SingleChoiceOption(
                            situation.localization.WHY_IS_IT_THAT(getBranchDescription(
                                situation,
                                branch,
                                branchResults[branch]!!
                            )),
                            Explanation(situation.localization.LETS_FIGURE_IT_OUT, shouldPause = false),
                            branch
                        )
                    }
                return options
            }

            override fun explanationIfSkipped(
                situation: QuestioningSituation,
                skipOption: SingleChoiceOption<BranchInfo>
            ): Explanation {
                return Explanation(situation.localization.LETS_FIGURE_IT_OUT, shouldPause = false)
            }

            override fun additionalActions(situation: QuestioningSituation, chosenAnswer: BranchInfo) {
                onGoIntoBranch(situation, chosenAnswer)
            }
        }

        val thoughtBranches = getThoughtBranches()
        val branchAutomata = thoughtBranches.associateWith { QuestioningStrategy.defaultFullBranchStrategy.build(it) }
        thoughtBranches.filter { branch -> branchAutomata[branch]!!.hasQuestions() }.forEach { branch ->
                state.linkTo(branchAutomata[branch]!!.initState) { situation, answer ->
                    getThoughtBranch(answer) == branch
                }
            }

        return SelectBranchState(state, branchAutomata.values.toList())
    }
}

class LogicalAggregationState(
    node: BranchAggregationNode,
    nextStates: Map<BranchResult, QuestionState>,
) : AggregationQuestionState<BranchAggregationNode, ThoughtBranch>(node, nextStates) {
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
        result: BranchResult
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

class CycleAggregationState(
    node: CycleAggregationNode,
    nodeResultNextStates: Map<BranchResult, QuestionState>,
) : AggregationQuestionState<CycleAggregationNode, Obj>(node, nodeResultNextStates) {
    override fun getBranchInfo(situation: QuestioningSituation): List<Obj> {
        return DecisionTreeReasoner(situation).searchWithErrors(node).correct
    }

    override fun getBranchDescription(situation: QuestioningSituation, branchInfo: Obj, result: BranchResult): String {
        val varName = node.variable.varName
        val alreadyContains = situation.decisionTreeVariables.containsKey(varName)
        situation.decisionTreeVariables[varName] = branchInfo
        val description = node.thoughtBranch.description(situation, result)
        if(alreadyContains) situation.decisionTreeVariables.remove(varName)
        return description
    }

    override fun getBranchResult(situation: QuestioningSituation, branchInfo: Obj): BranchResult {
        return node.thoughtBranch.solve(
            situation.forEval().also { it.decisionTreeVariables[node.variable.varName] = branchInfo }
        ).branchResult
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