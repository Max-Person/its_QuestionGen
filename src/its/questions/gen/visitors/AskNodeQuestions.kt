package its.questions.gen.visitors

import its.model.nodes.*
import its.model.nodes.visitors.DecisionTreeBehaviour
import its.questions.gen.QuestionGenerator
import its.questions.gen.TemplatingUtils._static.description
import its.questions.gen.TemplatingUtils._static.explanation
import its.questions.gen.TemplatingUtils._static.question
import its.questions.gen.TemplatingUtils._static.text
import its.questions.gen.visitors.LiteralToString._static.toAnswerString
import its.questions.questiontypes.AggregationQuestion
import its.questions.questiontypes.AnswerOption
import its.questions.questiontypes.Prompt
import its.questions.questiontypes.SingleChoiceQuestion

class AskNodeQuestions private constructor(val q : QuestionGenerator) : DecisionTreeBehaviour<Boolean> {

    // ---------------------- Удобства ---------------------------

    companion object _static{
        @JvmStatic
        fun DecisionTreeNode.askNodeQuestions(q : QuestionGenerator) : Boolean{
            return this.use(AskNodeQuestions(q))
        }
    }

    // ---------------------- Функции поведения ---------------------------

    override fun process(node: BranchResultNode): Boolean {
        return true
    }

    override fun process(node: CycleAggregationNode): Boolean {
        TODO("Not yet implemented")
    }

    override fun process(node: FindActionNode): Boolean {
        val answer = node.getAnswer(q.answers)!!
        println("\nМы уже говорили о том, что ${node.next.getFull(answer)!!.explanation(q.templating)}")
        return true
    }

    override fun process(node: LogicAggregationNode): Boolean {
        val answer = node.getAnswer(q.answers)!!
        val descr = node.description(q.templating, true)
        val q1 = SingleChoiceQuestion(
            "Верно ли, что $descr?",
            listOf(
                AnswerOption("Верно", answer, "Это неверно." ),
                AnswerOption("Неверно", !answer, "Это неверно." ),)
        )
        /*if(q1.ask() == true)
            return true*/

        val descrIncorrect = node.description(q.templating, !answer)
        val fullExplanation = "${
            node.description(q.templating, answer)
        }, потому что ${
            node.thoughtBranches
                .filter{q.answers[it.additionalInfo[ALIAS_ATR]].toBoolean() == answer}
                .joinToString(separator = ", ", transform = {it.description(q.templating, answer)})
        }."
        val q2 = AggregationQuestion(
            "Почему вы считаете, что $descrIncorrect?",
            node.logicalOp,
            !answer,
            "Вы верно оценили ситуацию, однако в данной ситуации $fullExplanation",
            node.thoughtBranches.map {
                val branchAnswer = q.answers[it.additionalInfo[ALIAS_ATR]].toBoolean()
                AggregationQuestion.AnswerOption(
                    it,
                    it.description(q.templating, true),
                    branchAnswer,
                    it.description(q.templating, branchAnswer),
                    )
            }
        )

        val incorrect = q2.ask()
        if(incorrect.isEmpty())
            return false

        val branch =
            if(incorrect.size == 1){
                val branch = incorrect.single()
                val branchAnswer = q.answers[branch.additionalInfo[ALIAS_ATR]].toBoolean()
                println("\nДавайте разберем, почему ${branch.description(q.templating, branchAnswer)}")
                branch
            }
            else{
                Prompt(
                    "Давайте разберем одну из ошибок.",
                    incorrect.map {
                        val branchAnswer = q.answers[it.additionalInfo[ALIAS_ATR]].toBoolean()
                        "Почему ${it.description(q.templating, branchAnswer)}?" to it
                    }//.plus("Мне все понятно." to null)
                ).ask()
            }

        if(branch != null)
            q.process(branch, !q.answers[branch.additionalInfo[ALIAS_ATR]].toBoolean())

        if(incorrect.isNotEmpty()) println("\nВ данной ситуации $fullExplanation")
        return false
    }

    override fun process(node: PredeterminingFactorsNode): Boolean {
        val answer = node.getAnswer(q.answers)!!
        val question = node.question(q.templating)
        val correctOutcome = node.next.info.first { it.key == answer } //TODO возможно стоит изменить систему Outcomes чтобы вот такие конструкции были проще
        val q1 = SingleChoiceQuestion(
            question,
            node.next.info.map { AnswerOption(
                it.text(q.templating)!!,
                it == correctOutcome,
                "Это неверно, поскольку ${it.explanation(q.templating, false)}. В этой ситуации ${correctOutcome.explanation(q.templating, true)}",
                it.decidingBranch
            )}
        )

        val chosen = q1.askWithInfo()
        if(chosen.first == true) return true

        val incorrectBranch = chosen.second as ThoughtBranch?
        val correctBranch = node.next.getFull(answer)!!.decidingBranch
        val options = mutableListOf<Pair<String, ThoughtBranch?>>()
        if(incorrectBranch != null && !incorrectBranch.isTrivial())
            options.add("Почему ${incorrectBranch.description(q.templating, false)}?" to incorrectBranch)
        if(correctBranch != null && !correctBranch.isTrivial())
            options.add("Почему ${correctBranch.description(q.templating, true)}?" to correctBranch)
        options.add("Подробный разбор не нужен." to null)

        val branch = Prompt(
            "Хотите ли вы разобраться подробнее?",
            options
        ).ask()

        if(branch != null)
            q.process(branch, !q.answers[branch.additionalInfo[ALIAS_ATR]].toBoolean())

        return false
    }

    override fun process(node: QuestionNode): Boolean {
        val answer = node.getAnswer(q.answers)!!
        val question = node.question(q.templating)
        val correctOutcome = node.next.info.first { it.key == answer } //TODO возможно стоит изменить систему Outcomes чтобы вот такие конструкции были проще
        val q1 = SingleChoiceQuestion(
            question,
            node.next.info.map { AnswerOption(
                it.text(q.templating)?:it.key.toAnswerString(q),
                it == correctOutcome,
                correctOutcome.explanation(q.templating),
                )}
        )

        return q1.ask()
    }

    override fun process(node: StartNode): Boolean {
        return true
    }

    override fun process(branch: ThoughtBranch): Boolean {
        return true
    }

    override fun process(node: UndeterminedResultNode): Boolean {
        return true
    }
}