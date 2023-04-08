package its.questions.gen.states

import its.model.nodes.LogicAggregationNode
import its.model.nodes.LogicalOp
import its.model.nodes.ThoughtBranch
import its.questions.inputs.TemplatingUtils._static.description
import its.questions.gen.visitors.getAnswer
import its.questions.inputs.LearningSituation

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

    protected fun text(situation: LearningSituation) : String {
        val answer = node.getAnswer(situation)!!
        val descrIncorrect = node.description(situation.templating, !answer)
        return "Почему вы считаете, что $descrIncorrect?"
    }

    override fun getQuestion(situation: LearningSituation): QuestionStateResult {
        val text = "$id. ${text(situation)}"
        val options = node.thoughtBranches.map { branch ->
            branch.description(situation.templating, true)
        }
        return Question(text, options, isAggregation = true)
    }

    override fun proceedWithAnswer(situation: LearningSituation, answer: List<Int>): QuestionStateChange {
        val givenAnswer : Map<ThoughtBranch, AggregationImpact> = node.thoughtBranches.mapIndexed{ind, branch ->
            val op = answer[ind]
            branch to
                    if(op < 0) AggregationImpact.Negative
                    else if(op > 0) AggregationImpact.Positive
                    else AggregationImpact.None
        }.toMap()

        val results : Map<ThoughtBranch, Boolean> = node.thoughtBranches.map{ branch -> branch to branch.getAnswer(situation)!!}.toMap()
        val actualImpact : Map<ThoughtBranch, AggregationImpact> = results.map{ (branch, res) ->
            branch to
                    if (res) AggregationImpact.Positive
                    else AggregationImpact.Negative
        }.toMap()

        val nodeRes = node.getAnswer(situation)!!
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
            if(incorrectBranches.isEmpty() && missedBranches.isEmpty()){
                "Вы верно оценили ситуацию, однако в данной ситуации ${node.description(situation.templating, nodeRes)}, потому что ${
                    node.thoughtBranches
                        .filter{it.getAnswer(situation) == nodeRes}
                        .joinToString(separator = ", ", transform = {it.description(situation.templating, nodeRes)})
                }."
            } else {
                (if(!incorrectBranches.isEmpty())
                    "Это неверно, поскольку ${incorrectBranches.joinToString(separator = ", ", transform = {it.description(situation.templating, it.getAnswer(situation)!!)})}."
                else
                    "").plus(
                    if(!missedBranches.isEmpty())
                        "${if(incorrectBranches.isEmpty()) "Это неверно, поскольку вы" else " Вы также "} не упомянули, что ${
                            missedBranches.joinToString(separator = ", ", transform = {it.description(situation.templating, it.getAnswer(situation)!!)})
                        } - это влияет на ситуацию в данном случае."
                    else
                        ""
                )
            }
        )

        val nextState = links.first { link -> link.condition(situation, incorrectBranches.isEmpty() && missedBranches.isEmpty()) }.nextState
        return QuestionStateChange(explanation, nextState)
    }
}