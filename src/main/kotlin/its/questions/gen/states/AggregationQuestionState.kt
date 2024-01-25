package its.questions.gen.states

import its.model.nodes.LogicAggregationNode
import its.model.nodes.LogicalOp
import its.model.nodes.ThoughtBranch
import its.questions.gen.QuestioningSituation
import its.questions.gen.formulations.Localization
import its.questions.gen.formulations.TemplatingUtils.description
import its.reasoner.nodes.DecisionTreeReasoner._static.getAnswer

//Можно выделить над ним некий AssociationQuestionState, но пока что это не нужно
class AggregationQuestionState(
    val node: LogicAggregationNode,
    links: Set<QuestionStateLink<Boolean>>
) : GeneralQuestionState<Boolean>(links) {
    enum class AggregationImpact{
        Negative,
        None,
        Positive,
    }

    protected fun text(situation: QuestioningSituation) : String {
        val answer = node.getAnswer(situation)
        val descrIncorrect = node.description(situation.localizationCode, situation.templating, !answer)
        return situation.localization.WHY_DO_YOU_THINK_THAT(descrIncorrect)
    }

    override fun getQuestion(situation: QuestioningSituation): QuestionStateResult {
        val text = "$id. ${text(situation)}"
        val options = node.thoughtBranches.map { branch ->
            branch.description(situation.localizationCode, situation.templating, true)
        }
        return Question(text, options, isAggregation = true)
    }

    companion object _static {
        @JvmStatic
        fun aggregationMatching(l: Localization) = mapOf(l.TRUE to 1, l.NOT_IMPORTANT to 0, l.FALSE to -1)
    }

    override fun proceedWithAnswer(situation: QuestioningSituation, answer: List<Int>): QuestionStateChange {
        val givenAnswer : Map<ThoughtBranch, AggregationImpact> = node.thoughtBranches.mapIndexed{ind, branch ->
            val op = answer[ind]
            branch to
                    if(op < 0) AggregationImpact.Negative
                    else if(op > 0) AggregationImpact.Positive
                    else AggregationImpact.None
        }.toMap()

        val results : Map<ThoughtBranch, Boolean> = node.thoughtBranches.map{ branch -> branch to branch.getAnswer(situation)}.toMap()
        val actualImpact : Map<ThoughtBranch, AggregationImpact> = results.map{ (branch, res) ->
            branch to
                    if (res) AggregationImpact.Positive
                    else AggregationImpact.Negative
        }.toMap()

        val nodeRes = node.getAnswer(situation)
        val incorrectBranches = givenAnswer.filter{(branch, impact) -> impact != AggregationImpact.None && impact != actualImpact[branch]}.keys
        val missedBranches = givenAnswer.filter{(branch, impact) ->
            impact == AggregationImpact.None &&
                    !(node.logicalOp == LogicalOp.AND && !nodeRes && results[branch]!!) &&
                    !(node.logicalOp == LogicalOp.OR && nodeRes && !results[branch]!!)
        }.keys

        node.thoughtBranches.forEach { branch ->
            if(incorrectBranches.contains(branch) || missedBranches.contains(branch))
                situation.addAssumedResult(branch, !results[branch]!!)
            else
                situation.addAssumedResult(branch, results[branch]!!)
        }

        val explanation = Explanation(
            if (incorrectBranches.isEmpty() && missedBranches.isEmpty()) {
                situation.localization.AGGREGATION_CORRECT_EXPL(answer_descr = node.description(
                    situation.localizationCode,
                    situation.templating,
                    nodeRes
                ),
                    branches_descr = node.thoughtBranches
                        .filter { it.getAnswer(situation) == nodeRes }
                        .joinToString(separator = ", ", transform = { it.description(situation.localizationCode, situation.templating, nodeRes) })
                )
            } else {
                (if (!incorrectBranches.isEmpty())
                    situation.localization.AGGREGATION_INCORRECT_BRANCHES_DESCR(
                        branches_descr = incorrectBranches.joinToString(
                            separator = ", ",
                            transform = { it.description(situation.localizationCode, situation.templating, it.getAnswer(situation)) })
                    )
                else
                    "").plus(
                    if (!missedBranches.isEmpty()) {
                        val missed_branches_descr = missedBranches.joinToString(
                            separator = ", ",
                            transform = { it.description(situation.localizationCode, situation.templating, it.getAnswer(situation)) })
                        if (incorrectBranches.isEmpty())
                            situation.localization.AGGREGATION_MISSED_BRANCHES_DESCR_PRIMARY(missed_branches_descr)
                        else
                            situation.localization.AGGREGATION_MISSED_BRANCHES_DESCR_CONCAT(missed_branches_descr)
                    } else ""
                )
            },
            type = ExplanationType.Error
        )

        val nextState = links.first { link -> link.condition(situation, incorrectBranches.isEmpty() && missedBranches.isEmpty()) }.nextState
        return QuestionStateChange(explanation, nextState)
    }
}