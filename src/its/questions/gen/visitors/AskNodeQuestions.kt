package its.questions.gen.visitors

import its.model.expressions.Literal
import its.model.nodes.*
import its.model.nodes.visitors.DecisionTreeBehaviour
import its.questions.gen.QuestionGenerator
import its.questions.gen.TemplatingUtils._static.replaceAlternatives
import its.questions.questiontypes.*

class AskNodeQuestions(val q : QuestionGenerator) : DecisionTreeBehaviour<Boolean> {
    override fun process(node: BranchResultNode): Boolean {
        return true
    }

    override fun process(node: CycleAggregationNode): Boolean {
        TODO("Not yet implemented")
    }

    override fun process(node: FindActionNode): Boolean {
        val answer = q.answers[node.additionalInfo[ALIAS_ATR]]!!
        println("\nМы уже говорили о том, что ${q.templating.process(node.additionalInfo["explanation"]!!.replaceAlternatives(answer == "found"))}")
        return true
    }

    override fun process(node: LogicAggregationNode): Boolean {
        val answer = q.answers[node.additionalInfo[ALIAS_ATR]].toBoolean()
        val descr = q.templating.process(node.additionalInfo["description"]!!.replaceAlternatives(true))
        val q1 = SingleChoiceQuestion(
            "Верно ли, что $descr?",
            listOf(
                AnswerOption("Верно", answer, "Это неверно." ),
                AnswerOption("Неверно", !answer, "Это неверно." ),)
        )
        if(q1.ask() == true)
            return true

        val descrIncorrect = q.templating.process(node.additionalInfo["description"]!!.replaceAlternatives(!answer))
        val q2 = AggregationQuestion(
            "Почему вы считаете, что $descrIncorrect?",
            node.logicalOp,
            !answer,
            node.thoughtBranches.map {
                val branchAnswer = q.answers[it.additionalInfo[ALIAS_ATR]].toBoolean()
                AggregationQuestion.AnswerOption(
                    it,
                    q.templating.process(it.additionalInfo["description"]!!.replaceAlternatives(true)),
                    branchAnswer,
                    q.templating.process(it.additionalInfo["description"]!!.replaceAlternatives(branchAnswer)),
                    )
            }
        )

        val incorrect = q2.ask()
        if(incorrect.isEmpty())
            return true

        val branch = Prompt(
            "Хотите ли вы разобраться подробнее?",
            incorrect.map {
                val branchAnswer = q.answers[it.additionalInfo[ALIAS_ATR]].toBoolean()
                "Почему " + q.templating.process(it.additionalInfo["description"]!!.replaceAlternatives(branchAnswer)) + "?" to it
            }.plus("Мне все понятно." to null)
        ).ask()

        if(branch != null)
            q.process(branch, !q.answers[branch.additionalInfo[ALIAS_ATR]].toBoolean())

        return false
    }

    override fun process(node: PredeterminingFactorsNode): Boolean {
        val answer = q.answers[node.additionalInfo[ALIAS_ATR]]!!
        val question = q.templating.process(node.additionalInfo["question"]!!)
        val q1 = SingleChoiceQuestion(
            question,
            node.next.info.map { AnswerOption(
                q.templating.process(it.additionalInfo["text"]!!),
                it.key == answer,
                "Это неверно, " + q.templating.process(it.additionalInfo["explanation"]!!.replaceAlternatives(false) + ". В этой ситуации " + node.next.additionalInfo(answer)!!["explanation"]!!.replaceAlternatives(true)),
                it.decidingBranch
            )}
        )

        val chosen = q1.askWithInfo()
        if(chosen.first == true) return true

        val incorrectBranch = chosen.second as ThoughtBranch?
        val correctBranch = node.next.getFull(answer)!!.decidingBranch
        val options = mutableListOf<Pair<String, ThoughtBranch?>>()
        if(incorrectBranch != null)
            options.add("Почему " + q.templating.process(incorrectBranch.additionalInfo["description"]!!.replaceAlternatives(false)) + "?" to incorrectBranch)
        if(correctBranch != null)
            options.add("Почему " + q.templating.process(correctBranch.additionalInfo["description"]!!.replaceAlternatives(true)) + "?" to correctBranch)
        options.add("Мне все понятно." to null)

        val branch = Prompt(
            "Хотите ли вы разобраться подробнее?",
            options
        ).ask()

        if(branch != null)
            q.process(branch, !q.answers[branch.additionalInfo[ALIAS_ATR]].toBoolean())

        return false
    }

    override fun process(node: QuestionNode): Boolean {
        val answer = Literal.fromString(q.answers[node.additionalInfo[ALIAS_ATR]]!!, node.type, node.enumOwner)
        val question = q.templating.process(node.additionalInfo["question"]!!)
        val q1 = SingleChoiceQuestion(
            question,
            node.next.keys.map { AnswerOption(
                q.templating.process(node.next.additionalInfo(it)?.get("text")?:it.use(LiteralToString(q))),
                it == answer,
                q.templating.process(node.next.additionalInfo(answer)?.get("explanation")?:""),
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