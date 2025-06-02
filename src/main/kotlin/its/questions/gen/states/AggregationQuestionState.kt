package its.questions.gen.states

import its.model.nodes.AggregationMethod
import its.model.nodes.AggregationNode
import its.model.nodes.BranchResult
import its.questions.gen.QuestioningSituation
import its.questions.gen.formulations.TemplatingUtils.description
import its.questions.gen.formulations.TemplatingUtils.nullFormulation
import its.questions.gen.strategies.QuestionAutomata
import its.questions.gen.strategies.QuestioningStrategy
import its.reasoner.nodes.DecisionTreeReasoner.Companion.getAnswer

//Можно выделить над ним некий AssociationQuestionState, но пока что это не нужно
/**
 * Общий класс для вопросов к узлам агрегации типа SIM
 */
class AggregationQuestionState<Node : AggregationNode, BranchInfo>(
    private val node: Node,
    private val helper: AggregationHelper<Node, BranchInfo>,
) : GeneralQuestionState<Boolean>() {
    init { //Если ответили правильно, то переходим к выходам из узла
        linkTo(RedirectQuestionState()) { situation, chosenAnswer -> chosenAnswer }

        //Если хотя бы одна неправильная, то спросить в какую углубиться
        val branchSelectQuestion = createSelectBranchState()
        linkTo(branchSelectQuestion) { situation, chosenAnswer -> !chosenAnswer }
    }

    internal fun linkNested(state: QuestionState) = QuestionAutomata(this).finalize(state)

    protected fun text(situation: QuestioningSituation) : String {
        return situation.localization.PLEASE_MATCH
    }

    override fun getQuestion(situation: QuestioningSituation): QuestionStateResult {
        val text = text(situation).prependId()
        val options = helper.getBranchDescriptions(situation)
        val matchingOptions = mapOf(
            BranchResult.CORRECT to situation.localization.TRUE,
            BranchResult.ERROR to situation.localization.FALSE,
            BranchResult.NULL to node.nullFormulation(situation)
        )
        return Question(text,
            options,
            QuestionType.matching,
            matchingOptions = BranchResult.entries.map { matchingOptions[it]!! })
    }

    override fun proceedWithAnswer(situation: QuestioningSituation, answer: List<Int>): QuestionStateChange {
        val branches = helper.getBranchInfo(situation)
        val givenAnswer: Map<BranchInfo, BranchResult> = branches.mapIndexed { ind, branch ->
            branch to BranchResult.entries[answer[ind]]
        }.toMap()

        val results: Map<BranchInfo, BranchResult> = branches.associateWith { branch ->
            helper.getBranchResult(situation, branch)
        }

        val nodeRes = node.getAnswer(situation.forEval())
        val incorrectBranches = givenAnswer.filter { (branch, result) ->
            result != BranchResult.NULL && result != results[branch]
        }.keys
        val missedBranches = givenAnswer.filter { (branch, result) ->
            result == BranchResult.NULL &&
                    !(node.aggregationMethod == AggregationMethod.AND && nodeRes == BranchResult.ERROR
                            && results[branch] == BranchResult.CORRECT) &&
                    !(node.aggregationMethod == AggregationMethod.OR && nodeRes == BranchResult.CORRECT
                            && results[branch] == BranchResult.ERROR)
        }.keys

        givenAnswer.forEach { (branch, assumedResult) ->
            helper.addAssumedResult(branch, situation, assumedResult)
        }

        val explanationText: String
        if (incorrectBranches.isNotEmpty() && missedBranches.isNotEmpty()) {
            explanationText =
                situation.localization.AGGREGATION_INCORRECT_BRANCHES_DESCR(incorrectBranches.joinToString(", ") { branch ->
                    helper.getBranchDescription(situation, branch, results[branch]!!)
                }) + situation.localization.AGGREGATION_MISSED_BRANCHES_DESCR_CONCAT(missedBranches.joinToString(", ") { branch ->
                    helper.getBranchDescription(situation, branch, results[branch]!!)
                })
        } else if (incorrectBranches.isNotEmpty()) {
            explanationText =
                situation.localization.AGGREGATION_INCORRECT_BRANCHES_DESCR(incorrectBranches.joinToString(", ") { branch ->
                    helper.getBranchDescription(situation, branch, results[branch]!!)
                })
        } else if (missedBranches.isNotEmpty()) {
            explanationText =
                situation.localization.AGGREGATION_MISSED_BRANCHES_DESCR_PRIMARY(incorrectBranches.joinToString(", ") { branch ->
                    helper.getBranchDescription(situation, branch, results[branch]!!)
                })
        } else {
            explanationText =
                situation.localization.AGGREGATION_CORRECT_EXPL(answer_descr = node.description(situation, nodeRes),
                    branches_descr = branches.filter { results[it] == nodeRes }
                        .joinToString(", ") { helper.getBranchDescription(situation, it, nodeRes) })
        }

        val isAnswerCorrect = incorrectBranches.isEmpty() && missedBranches.isEmpty()
        return QuestionStateChange(
            Explanation(explanationText, type = ExplanationType.Error), getStateFromLinks(situation, isAnswerCorrect)
        )
    }

    private fun createSelectBranchState(): SingleChoiceQuestionState<BranchInfo> {
        //Если были сделаны ошибки в ветках, то спросить про углубление в одну из веток
        val state = object : SingleChoiceQuestionState<BranchInfo>() {
            override fun text(situation: QuestioningSituation): String {
                return situation.localization.WHAT_DO_YOU_WANT_TO_DISCUSS_FURTHER
            }

            override fun options(situation: QuestioningSituation): List<SingleChoiceOption<BranchInfo>> {
                val branchInfos = helper.getBranchInfo(situation)
                val branchResults = branchInfos.associateWith { helper.getBranchResult(situation, it) }
                val options = branchInfos
                    .filter{branch ->
                        branchResults[branch] != helper.getAssumedResult(branch, situation)
                    }.map { branch ->
                        SingleChoiceOption(
                            situation.localization.WHY_IS_IT_THAT(
                                helper.getBranchDescription(
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
                helper.onGoIntoBranch(situation, chosenAnswer)
            }
        }

        val thoughtBranches = helper.getThoughtBranches()
        val branchAutomata = thoughtBranches.associateWith { QuestioningStrategy.defaultFullBranchStrategy.build(it) }
        thoughtBranches.forEach { branch ->
            val nextState = if(branchAutomata[branch]!!.hasQuestions())
                branchAutomata[branch]!!.initState
            else
                RedirectQuestionState()

            state.linkTo(nextState) { situation, answer ->
                helper.getThoughtBranch(answer) == branch
            }
        }

        return state
    }
}