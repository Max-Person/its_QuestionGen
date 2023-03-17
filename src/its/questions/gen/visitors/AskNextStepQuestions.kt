package its.questions.gen.visitors

import its.model.expressions.literals.BooleanLiteral
import its.model.nodes.*
import its.model.nodes.visitors.SimpleDecisionTreeBehaviour
import its.questions.gen.QuestionGenerator
import its.questions.gen.TemplatingUtils._static.asNextStep
import its.questions.gen.TemplatingUtils._static.description
import its.questions.gen.TemplatingUtils._static.nextStepBranchResult
import its.questions.gen.TemplatingUtils._static.nextStepExplanation
import its.questions.gen.TemplatingUtils._static.nextStepQuestion
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
        val outcome = node.next.info.first { it.key == answer } //TODO возможно стоит изменить систему Outcomes чтобы вот такие конструкции были проще
        val explanation = outcome.nextStepExplanation(q.templating)
        val correct = node.correctNext(q.answers)
        val jumps = node.getPossibleJumps(q.answers)

        val options = jumps.filter { it !is BranchResultNode}.map{AnswerOption(
            it.asNextStep(q.templating),
            it == correct,
            explanation
        )}.plus(AnswerOption(
            outcome.nextStepBranchResult(q.templating, true) ?:"Можно заключить, что ${currentBranch.description(q.templating, true)}",
            correct is BranchResultNode && correct.value == BooleanLiteral(true),
            explanation
        )).plus(AnswerOption(
            outcome.nextStepBranchResult(q.templating, false) ?:"Можно заключить, что ${currentBranch.description(q.templating, false)}",
            correct is BranchResultNode && correct.value == BooleanLiteral(false),
            explanation
        ))

        val q1 = SingleChoiceQuestion(
            outcome.nextStepQuestion(q.templating) ?: defaultNextStepQuestion,
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
            it.asNextStep(q.templating),
            it == branch.start,
            branch.nextStepExplanation(q.templating),
        )}
        val q1 = SingleChoiceQuestion(
            branch.nextStepQuestion(q.templating) ?: defaultNextStepQuestion,
            options
        )

        return q1.ask() to branch.start
    }

    override fun process(node: UndeterminedResultNode): Pair<Boolean, DecisionTreeNode?> {
        return true to null
    }
}