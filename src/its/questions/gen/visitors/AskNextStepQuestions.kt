package its.questions.gen.visitors

import its.model.expressions.literals.BooleanLiteral
import its.model.nodes.*
import its.model.nodes.visitors.SimpleDecisionTreeBehaviour
import its.questions.gen.QuestionGenerator
import its.questions.gen.TemplatingUtils._static.replaceAlternatives
import its.questions.gen.visitors.GetPossibleJumps._static.getPossibleJumps
import its.questions.questiontypes.AnswerOption
import its.questions.questiontypes.SingleChoiceQuestion

class AskNextStepQuestions private constructor(
    val q : QuestionGenerator,
    val currentBranch : ThoughtBranch,
) : SimpleDecisionTreeBehaviour<Pair<Boolean, DecisionTreeNode?>> {

    // ---------------------- Удобства ---------------------------

    companion object _static{
        const val defaultNextStepQuestion = "Какой следующий шаг необходим для решения задачи?"

        @JvmStatic
        fun DecisionTreeNode.askNextStepQuestions(q : QuestionGenerator, currentBranch : ThoughtBranch) : Pair<Boolean, DecisionTreeNode?>{
            return this.use(AskNextStepQuestions(q, currentBranch))
        }
    }

    // ---------------------- Функции поведения ---------------------------

    override fun <AnswerType : Any> process(node: LinkNode<AnswerType>): Pair<Boolean, DecisionTreeNode?>{
        val answer = node.getAnswer(q.answers)!!
        val outcomeInfo = node.next.additionalInfo(answer)!!
        val correct = node.correctNext(q.answers)
        val jumps = node.getPossibleJumps(q.answers)

        val options = jumps.filter { it !is BranchResultNode}.map{AnswerOption(
            q.templating.process(it.additionalInfo["asNextStep"]!!),
            it == correct,
            q.templating.process(outcomeInfo["nextStepExplanation"]?:"")
        )}.plus(AnswerOption(
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

    override fun process(node: BranchResultNode): Pair<Boolean, DecisionTreeNode?> {
        return true to null
    }

    override fun process(node: StartNode): Pair<Boolean, DecisionTreeNode?> {
        return true to node.main
    }

    override fun process(branch: ThoughtBranch): Pair<Boolean, DecisionTreeNode?> {
        val jumps = branch.getPossibleJumps(q.answers)

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