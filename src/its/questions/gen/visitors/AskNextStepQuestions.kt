package its.questions.gen.visitors

import its.model.expressions.Literal
import its.model.expressions.literals.BooleanLiteral
import its.model.nodes.*
import its.model.nodes.visitors.DecisionTreeBehaviour
import its.questions.gen.QuestionGenerator
import its.questions.gen.TemplatingUtils._static.replaceAlternatives
import its.questions.gen.visitors.GetPossibleJumps._static.getPossibleJumps
import its.questions.questiontypes.*

class AskNextStepQuestions(
    val q : QuestionGenerator,
    val currentBranch : ThoughtBranch,
) : DecisionTreeBehaviour<Pair<Boolean, DecisionTreeNode?>> {

    companion object {
        const val defaultNextStepQuestion = "Какой следующий шаг необходим для решения задачи?"
    }

    override fun process(node: BranchResultNode): Pair<Boolean, DecisionTreeNode?> {
        return true to null
    }

    override fun process(node: CycleAggregationNode): Pair<Boolean, DecisionTreeNode?> {
        val answer = q.answers[node.additionalInfo[ALIAS_ATR]].toBoolean()
        val outcomeInfo = node.next.additionalInfo(answer)!!
        val correct = node.next[answer]
        val jumps = node.getPossibleJumps(q.knownVariables)

        var options = jumps.filter { it !is BranchResultNode}.map{AnswerOption(
            q.templating.process(it.additionalInfo["asNextStep"]!!),
            it == correct,
            q.templating.process(outcomeInfo["nextStepExplanation"]?.replaceAlternatives(answer)?:"")
        )}
        options = options.plus(AnswerOption(
            (outcomeInfo["nextStepBranchResult"]?:"Можно заключить, что ${q.templating.process(currentBranch.additionalInfo["description"]!!)}").replaceAlternatives(true),
            correct is BranchResultNode && correct.value == BooleanLiteral(true),
            q.templating.process(outcomeInfo["nextStepExplanation"]?.replaceAlternatives(answer)?:"")
        )).plus(AnswerOption(
            (outcomeInfo["nextStepBranchResult"]?:"Можно заключить, что ${q.templating.process(currentBranch.additionalInfo["description"]!!)}").replaceAlternatives(false),
            correct is BranchResultNode && correct.value == BooleanLiteral(false),
            q.templating.process(outcomeInfo["nextStepExplanation"]?.replaceAlternatives(answer)?:"")
        ))

        val q1 = SingleChoiceQuestion(
            q.templating.process(outcomeInfo["nextStepQuestion"]?.replaceAlternatives(answer)?:defaultNextStepQuestion),
            options
        )
        return q1.ask() to node.next[answer]
    }

    override fun process(node: FindActionNode): Pair<Boolean, DecisionTreeNode?> {
        val answer = q.answers[node.additionalInfo[ALIAS_ATR]]!!
        val outcomeInfo = node.next.additionalInfo(answer)!!
        val correct = node.next[answer]
        val jumps = node.getPossibleJumps(q.knownVariables)

        var options = jumps.filter { it !is BranchResultNode }.map{AnswerOption(
            q.templating.process(it.additionalInfo["asNextStep"]!!),
            it == node.next[answer],
            q.templating.process(outcomeInfo["nextStepExplanation"]?:"")
        )}
        options = options.plus(AnswerOption(
            (outcomeInfo["nextStepBranchResult"]?:"Можно заключить, что ${q.templating.process(currentBranch.additionalInfo["description"]!!)}").replaceAlternatives(true),
            correct is BranchResultNode && correct.value == BooleanLiteral(true),
            q.templating.process(outcomeInfo["nextStepExplanation"]?:"")
        )).plus(AnswerOption(
            (outcomeInfo["nextStepBranchResult"]?:"Можно заключить, что ${q.templating.process(currentBranch.additionalInfo["description"]!!)}").replaceAlternatives(false),
            correct is BranchResultNode && correct.value == BooleanLiteral(false),
            q.templating.process(outcomeInfo["nextStepExplanation"]?:"")
        ))

        val q1 = SingleChoiceQuestion(
            q.templating.process(outcomeInfo["nextStepQuestion"]?: defaultNextStepQuestion),
            options
        )
        return q1.ask() to node.next[answer]
    }

    override fun process(node: LogicAggregationNode): Pair<Boolean, DecisionTreeNode?> {
        val answer = q.answers[node.additionalInfo[ALIAS_ATR]].toBoolean()
        val outcomeInfo = node.next.additionalInfo(answer)!!
        val correct = node.next[answer]
        val jumps = node.getPossibleJumps(q.knownVariables)

        var options = jumps.filter { it !is BranchResultNode}.map{AnswerOption(
            q.templating.process(it.additionalInfo["asNextStep"]!!),
            it == correct,
            q.templating.process(outcomeInfo["nextStepExplanation"]?.replaceAlternatives(answer)?:"")
        )}
        options = options.plus(AnswerOption(
            (outcomeInfo["nextStepBranchResult"]?:"Можно заключить, что ${q.templating.process(currentBranch.additionalInfo["description"]!!)}").replaceAlternatives(true),
            correct is BranchResultNode && correct.value == BooleanLiteral(true),
            q.templating.process(outcomeInfo["nextStepExplanation"]?.replaceAlternatives(answer)?:"")
        )).plus(AnswerOption(
            (outcomeInfo["nextStepBranchResult"]?:"Можно заключить, что ${q.templating.process(currentBranch.additionalInfo["description"]!!)}").replaceAlternatives(false),
            correct is BranchResultNode && correct.value == BooleanLiteral(false),
            q.templating.process(outcomeInfo["nextStepExplanation"]?.replaceAlternatives(answer)?:"")
        ))

        val q1 = SingleChoiceQuestion(
            q.templating.process(outcomeInfo["nextStepQuestion"]?.replaceAlternatives(answer)?:defaultNextStepQuestion),
            options
        )
        return q1.ask() to node.next[answer]
    }

    override fun process(node: PredeterminingFactorsNode): Pair<Boolean, DecisionTreeNode?> {
        val answer = q.answers[node.additionalInfo[ALIAS_ATR]]!!
        val outcomeInfo = node.next.additionalInfo(answer)!!
        val correct = node.next[answer]
        val jumps = node.getPossibleJumps(q.knownVariables)

        var options = jumps.filter { it !is BranchResultNode}.map{AnswerOption(
            q.templating.process(it.additionalInfo["asNextStep"]!!),
            it == correct,
            q.templating.process(outcomeInfo["nextStepExplanation"]?:"")
        )}
        options = options.plus(AnswerOption(
            (outcomeInfo["nextStepBranchResult"]?:"Можно заключить, что ${q.templating.process(currentBranch.additionalInfo["description"]!!)}").replaceAlternatives(true),
            correct is BranchResultNode && correct.value == BooleanLiteral(true),
            q.templating.process(outcomeInfo["nextStepExplanation"]?:"")
        )).plus(AnswerOption(
            (outcomeInfo["nextStepBranchResult"]?:"Можно заключить, что ${q.templating.process(currentBranch.additionalInfo["description"]!!)}").replaceAlternatives(false),
            correct is BranchResultNode && correct.value == BooleanLiteral(false),
            q.templating.process(outcomeInfo["nextStepExplanation"]?:"")
        ))

        val q1 = SingleChoiceQuestion(
            q.templating.process(outcomeInfo["nextStepQuestion"]?: defaultNextStepQuestion),
            options
        )
        return q1.ask() to node.next[answer]
    }

    override fun process(node: QuestionNode): Pair<Boolean, DecisionTreeNode?> {
        val answer = Literal.fromString(q.answers[node.additionalInfo[ALIAS_ATR]]!!, node.type, node.enumOwner)
        val outcomeInfo = node.next.additionalInfo(answer)!!
        val correct = node.next[answer]
        val jumps = node.getPossibleJumps(q.knownVariables)

        var options = jumps.filter { it !is BranchResultNode}.map{AnswerOption(
            q.templating.process(it.additionalInfo["asNextStep"]!!),
            it == correct,
            q.templating.process(outcomeInfo["nextStepExplanation"]?:"")
        )}
        options = options.plus(AnswerOption(
            (outcomeInfo["nextStepBranchResult"]?:"Можно заключить, что ${q.templating.process(currentBranch.additionalInfo["description"]!!)}").replaceAlternatives(true),
            correct is BranchResultNode && correct.value == BooleanLiteral(true),
            q.templating.process(outcomeInfo["nextStepExplanation"]?:"")
        )).plus(AnswerOption(
            (outcomeInfo["nextStepBranchResult"]?:"Можно заключить, что ${q.templating.process(currentBranch.additionalInfo["description"]!!)}").replaceAlternatives(false),
            correct is BranchResultNode && correct.value == BooleanLiteral(false),
            q.templating.process(outcomeInfo["nextStepExplanation"]?:"")
        ))
        val q1 = SingleChoiceQuestion(
            q.templating.process(outcomeInfo["nextStepQuestion"]?: defaultNextStepQuestion),
            options
        )

        return q1.ask() to node.next[answer]
    }

    override fun process(node: StartNode): Pair<Boolean, DecisionTreeNode?> {
        return true to node.main
    }

    override fun process(branch: ThoughtBranch): Pair<Boolean, DecisionTreeNode?> {
        val jumps = branch.getPossibleJumps(q.knownVariables)

        val options = jumps.filter { it !is BranchResultNode}.map{AnswerOption(
            q.templating.process(it.additionalInfo["asNextStep"]!!),
            it == branch.start,
            q.templating.process(branch.additionalInfo["nextStepExplanation"]?:"")
        )}
        val q1 = SingleChoiceQuestion(
            q.templating.process(branch.additionalInfo["nextStepQuestion"]?: defaultNextStepQuestion),
            options
        )

        return q1.ask() to branch.start
    }

    override fun process(node: UndeterminedResultNode): Pair<Boolean, DecisionTreeNode?> {
        return true to null
    }
}