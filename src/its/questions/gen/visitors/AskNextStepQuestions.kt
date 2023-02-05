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
) : DecisionTreeBehaviour<Pair<AnswerStatus, DecisionTreeNode?>> {

    companion object {
        const val defaultNextStepQuestion = "Какой следующий шаг необходим для решения задачи?"
    }

    override fun process(node: BranchResultNode): Pair<AnswerStatus, DecisionTreeNode?> {
        return AnswerStatus.CORRECT to null
    }

    override fun process(node: CycleAggregationNode): Pair<AnswerStatus, DecisionTreeNode?> {
        val answer = q.answers[node.additionalInfo[ALIAS_ATR]].toBoolean()
        val correct = node.next[answer]
        val jumps = node.getPossibleJumps(q.knownVariables)

        var options = jumps.filter { it !is BranchResultNode}.map{AnswerOption(
            q.templating.process(it.additionalInfo["asNextStep"]!!),
            it == correct,
            q.templating.process(node.next.additionalInfo(answer)?.get("nextStepExplanation")?.replaceAlternatives(answer)?:"")
        )}
        options = options.plus(AnswerOption(
            "Можно заключить, что " + q.templating.process(currentBranch.additionalInfo["description"]!!.replaceAlternatives(true)),
            correct is BranchResultNode && correct.value == BooleanLiteral(true),
            q.templating.process(node.next.additionalInfo(answer)?.get("nextStepExplanation")?.replaceAlternatives(answer)?:"")
        )).plus(AnswerOption(
            "Можно заключить, что " + q.templating.process(currentBranch.additionalInfo["description"]!!.replaceAlternatives(false)),
            correct is BranchResultNode && correct.value == BooleanLiteral(false),
            q.templating.process(node.next.additionalInfo(answer)?.get("nextStepExplanation")?.replaceAlternatives(answer)?:"")
        ))

        val q1 = SingleChoiceQuestion(
            true,
            q.templating.process(node.additionalInfo["nextStepQuestion"]?.replaceAlternatives(answer)?:defaultNextStepQuestion),
            options
        )
        return q1.ask() to node.next[answer]
    }

    override fun process(node: FindActionNode): Pair<AnswerStatus, DecisionTreeNode?> {
        val answer = q.answers[node.additionalInfo[ALIAS_ATR]]!!
        val correct = node.next[answer]
        val jumps = node.getPossibleJumps(q.knownVariables)

        var options = jumps.filter { it !is BranchResultNode }.map{AnswerOption(
            q.templating.process(it.additionalInfo["asNextStep"]!!),
            it == node.next[answer],
            q.templating.process(node.next.additionalInfo(answer)?.get("nextStepExplanation")?:"")
        )}
        options = options.plus(AnswerOption(
            "Можно заключить, что " + q.templating.process(currentBranch.additionalInfo["description"]!!.replaceAlternatives(true)),
            correct is BranchResultNode && correct.value == BooleanLiteral(true),
            q.templating.process(node.next.additionalInfo(answer)?.get("nextStepExplanation")?:"")
        )).plus(AnswerOption(
            "Можно заключить, что " + q.templating.process(currentBranch.additionalInfo["description"]!!.replaceAlternatives(false)),
            correct is BranchResultNode && correct.value == BooleanLiteral(false),
            q.templating.process(node.next.additionalInfo(answer)?.get("nextStepExplanation")?:"")
        ))

        val q1 = SingleChoiceQuestion(
            true,
            q.templating.process(node.additionalInfo["nextStepQuestion"]?: defaultNextStepQuestion),
            options
        )
        return q1.ask() to node.next[answer]
    }

    override fun process(node: LogicAggregationNode): Pair<AnswerStatus, DecisionTreeNode?> {
        val answer = q.answers[node.additionalInfo[ALIAS_ATR]].toBoolean()
        val correct = node.next[answer]
        val jumps = node.getPossibleJumps(q.knownVariables)

        var options = jumps.filter { it !is BranchResultNode}.map{AnswerOption(
            q.templating.process(it.additionalInfo["asNextStep"]!!),
            it == correct,
            q.templating.process(node.next.additionalInfo(answer)?.get("nextStepExplanation")?.replaceAlternatives(answer)?:"")
        )}
        options = options.plus(AnswerOption(
            "Можно заключить, что " + q.templating.process(currentBranch.additionalInfo["description"]!!.replaceAlternatives(true)),
            correct is BranchResultNode && correct.value == BooleanLiteral(true),
            q.templating.process(node.next.additionalInfo(answer)?.get("nextStepExplanation")?.replaceAlternatives(answer)?:"")
        )).plus(AnswerOption(
            "Можно заключить, что " + q.templating.process(currentBranch.additionalInfo["description"]!!.replaceAlternatives(false)),
            correct is BranchResultNode && correct.value == BooleanLiteral(false),
            q.templating.process(node.next.additionalInfo(answer)?.get("nextStepExplanation")?.replaceAlternatives(answer)?:"")
        ))

        val q1 = SingleChoiceQuestion(
            true,
            q.templating.process(node.additionalInfo["nextStepQuestion"]?.replaceAlternatives(answer)?:defaultNextStepQuestion),
            options
        )
        return q1.ask() to node.next[answer]
    }

    override fun process(node: PredeterminingFactorsNode): Pair<AnswerStatus, DecisionTreeNode?> {
        val answer = q.answers[node.additionalInfo[ALIAS_ATR]]!!
        val correct = node.next[answer]
        val jumps = node.getPossibleJumps(q.knownVariables)

        var options = jumps.filter { it !is BranchResultNode}.map{AnswerOption(
            q.templating.process(it.additionalInfo["asNextStep"]!!),
            it == correct,
            q.templating.process(node.next.additionalInfo(answer)?.get("nextStepExplanation")?:"")
        )}
        options = options.plus(AnswerOption(
            "Можно заключить, что " + q.templating.process(currentBranch.additionalInfo["description"]!!.replaceAlternatives(true)),
            correct is BranchResultNode && correct.value == BooleanLiteral(true),
            q.templating.process(node.next.additionalInfo(answer)?.get("nextStepExplanation")?:"")
        )).plus(AnswerOption(
            "Можно заключить, что " + q.templating.process(currentBranch.additionalInfo["description"]!!.replaceAlternatives(false)),
            correct is BranchResultNode && correct.value == BooleanLiteral(false),
            q.templating.process(node.next.additionalInfo(answer)?.get("nextStepExplanation")?:"")
        ))

        val q1 = SingleChoiceQuestion(
            true,
            q.templating.process(node.additionalInfo["nextStepQuestion"]?: defaultNextStepQuestion),
            options
        )
        return q1.ask() to node.next[answer]
    }

    override fun process(node: QuestionNode): Pair<AnswerStatus, DecisionTreeNode?> {
        val answer = Literal.fromString(q.answers[node.additionalInfo[ALIAS_ATR]]!!, node.type, node.enumOwner)
        val correct = node.next[answer]
        val jumps = node.getPossibleJumps(q.knownVariables)

        var options = jumps.filter { it !is BranchResultNode}.map{AnswerOption(
            q.templating.process(it.additionalInfo["asNextStep"]!!),
            it == correct,
            q.templating.process(node.next.additionalInfo(answer)?.get("nextStepExplanation")?:"")
        )}
        options = options.plus(AnswerOption(
            "Можно заключить, что " + q.templating.process(currentBranch.additionalInfo["description"]!!.replaceAlternatives(true)),
            correct is BranchResultNode && correct.value == BooleanLiteral(true),
            q.templating.process(node.next.additionalInfo(answer)?.get("nextStepExplanation")?:"")
        )).plus(AnswerOption(
            "Можно заключить, что " + q.templating.process(currentBranch.additionalInfo["description"]!!.replaceAlternatives(false)),
            correct is BranchResultNode && correct.value == BooleanLiteral(false),
            q.templating.process(node.next.additionalInfo(answer)?.get("nextStepExplanation")?:"")
        ))
        val q1 = SingleChoiceQuestion(
            true,
            q.templating.process(node.additionalInfo["nextStepQuestion"]?: defaultNextStepQuestion),
            options
        )

        return q1.ask() to node.next[answer]
    }

    override fun process(node: StartNode): Pair<AnswerStatus, DecisionTreeNode?> {
        return AnswerStatus.CORRECT to node.main
    }

    override fun process(branch: ThoughtBranch): Pair<AnswerStatus, DecisionTreeNode?> {
        return AnswerStatus.CORRECT to branch.start
    }

    override fun process(node: UndeterminedResultNode): Pair<AnswerStatus, DecisionTreeNode?> {
        return AnswerStatus.CORRECT to null
    }
}