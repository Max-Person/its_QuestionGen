package its.questions.gen.states

import its.model.expressions.Literal
import its.model.expressions.literals.BooleanLiteral
import its.model.nodes.*
import its.model.nodes.visitors.DecisionTreeBehaviour
import its.questions.gen.TemplatingUtils._static.asNextStep
import its.questions.gen.TemplatingUtils._static.description
import its.questions.gen.TemplatingUtils._static.explanation
import its.questions.gen.TemplatingUtils._static.nextStepBranchResult
import its.questions.gen.TemplatingUtils._static.nextStepExplanation
import its.questions.gen.TemplatingUtils._static.nextStepQuestion
import its.questions.gen.TemplatingUtils._static.question
import its.questions.gen.TemplatingUtils._static.text
import its.questions.gen.visitors.GetPossibleJumps._static.getPossibleJumps
import its.questions.gen.visitors.LiteralToString._static.toAnswerString
import its.questions.gen.visitors.correctNext
import its.questions.gen.visitors.getAnswer
import its.questions.gen.visitors.isTrivial
import java.util.*

class SequentialAutomataCreation : DecisionTreeBehaviour<QuestionState> {
    private val branchDiving : Stack<ThoughtBranch> = Stack()
    private val currentBranch
        get() = branchDiving.peek()

    override fun process(node: BranchResultNode): QuestionState {
        val nodeVal = node.value == BooleanLiteral(true)
        val currentBranch = currentBranch

        val redir = RedirectQuestionState()
        val skip = object : SkipQuestionState() {
            override fun skip(situation: ILearningSituation): QuestionStateChange {
                val explanation = Explanation("Итак, мы обсудили, почему ${currentBranch.description(situation.templating, nodeVal)}")
                return QuestionStateChange(explanation, redir)
            }

            override val reachableStates: Collection<QuestionState>
                get() = listOf(redir)
        }

        return skip
    }

    override fun process(node: CycleAggregationNode): QuestionState {
        TODO("Not yet implemented")
    }

    override fun process(node: FindActionNode): QuestionState {
        val nextSteps = node.next.keys.map { answer -> answer to nextStep(node, answer)}.toMap()

        val skip = object : SkipQuestionState() {
            override fun skip(situation: ILearningSituation): QuestionStateChange {
                val correctAnswer = node.getAnswer(situation.answers)!!

                val explanation = Explanation("Мы уже говорили о том, что ${node.next.getFull(correctAnswer)!!.explanation(situation.templating)}")
                val nextState = nextSteps[correctAnswer]
                return QuestionStateChange(explanation, nextState)
            }

            override val reachableStates: Collection<QuestionState>
                get() = nextSteps.values
        }

        return skip
    }

    override fun process(node: LogicAggregationNode): QuestionState {
        //сначала задать вопрос о результате выполнения узла
        //TODO не задавать, если ветка тривиальна
        val aggregationRedirect = RedirectQuestionState()

        val nextSteps = node.next.map { (outcome) -> outcome to nextStep(node, outcome)}.toMap()
        val mainLinks = node.next.map { (outcome) -> GeneralQuestionState.QuestionStateLink<Pair<Boolean, Boolean>>(
            {situation, answer -> answer.second && (answer.first == outcome) },
            nextSteps[outcome]!!
        )}.plus(GeneralQuestionState.QuestionStateLink<Pair<Boolean, Boolean>>(
            {situation, answer -> !answer.second },
            aggregationRedirect
        )).toSet()

        val resultQuestion = object : CorrectnessCheckQuestionState<Boolean>(mainLinks) {
            override fun text(situation: ILearningSituation): String {
                val descr = node.description(situation.templating, true)
                return "Верно ли, что $descr?"
            }

            override fun options(situation: ILearningSituation): List<SingleChoiceOption<Pair<Boolean, Boolean>>> {
                val correctAnswer = node.getAnswer(situation.answers)!!
                return node.next.info.map { SingleChoiceOption(
                    if(it.key) "Верно" else "Неверно",
                    Explanation("Это не так. Давайте разберемся"),
                    it.key to (it.key == correctAnswer),
                )}
            }
        }

        //Если результат выполнения узла был выбран неверно, то спросить про результаты веток
        val branchSelectRedirect = RedirectQuestionState()

        val aggregationQuestion = AggregationQuestionState(node, setOf(
            GeneralQuestionState.QuestionStateLink(
                { situation, answer -> answer && node.getAnswer(situation.answers) == true },
                nextSteps[true]!!
            ),
            GeneralQuestionState.QuestionStateLink(
                { situation, answer -> answer && node.getAnswer(situation.answers) == false },
                nextSteps[false]!!
            ),
            GeneralQuestionState.QuestionStateLink(
                { situation, answer -> !answer },
                branchSelectRedirect
            ),
        ))
        aggregationRedirect.redir = aggregationQuestion


        //Если были сделаны ошибки в ветках, то спросить про углубление в одну из веток
        val branchAutomata = node.thoughtBranches.map { it to QuestionAutomata(it.use(this))}.toMap()
        val branchLinks = node.thoughtBranches.map{branch -> GeneralQuestionState.QuestionStateLink<ThoughtBranch>(
            {situation, answer -> answer == branch }, branchAutomata[branch]!!.initState
        )}.toSet()

        val branchSelectQuestion = object : SingleChoiceQuestionState<ThoughtBranch>(branchLinks) {
            override fun text(situation: ILearningSituation): String {
                return "В чем бы вы хотели разобраться подробнее?"
            }

            override fun options(situation: ILearningSituation): List<SingleChoiceOption<ThoughtBranch>> {
                val options = node.thoughtBranches.filter{branch ->
                    branch.getAnswer(situation.answers) != situation.assumedResult(branch)
                }.map { branch ->
                    SingleChoiceOption<ThoughtBranch>(
                        "Почему ${branch.description(situation.templating, branch.getAnswer(situation.answers)!!)}?",
                        null,
                        branch
                    )
                }
                return options
            }
        }
        branchSelectRedirect.redir = branchSelectQuestion


        //После выхода из веток пропускающее состояние определяет, к какому из верных ответов надо совершить переход
        val shadowSkip = object : SkipQuestionState(){
            override fun skip(situation: ILearningSituation): QuestionStateChange {
                return QuestionStateChange(null, nextSteps[node.getAnswer(situation.answers)!!])
            }

            override val reachableStates: Collection<QuestionState>
                get() = nextSteps.values
        }
        branchAutomata.values.forEach{it.finalize(shadowSkip)}

        return resultQuestion
    }

    override fun process(node: PredeterminingFactorsNode): QuestionState {
        //сначала задать вопрос о результате выполнения узла
        val branchSelectRedirect = RedirectQuestionState()

        val nextSteps = node.next.map { (outcome) -> outcome to nextStep(node, outcome)}.toMap()
        val mainLinks = node.next.map { (outcome) -> GeneralQuestionState.QuestionStateLink<Pair<String, Boolean>>(
            {situation, answer -> answer.second && answer.first == outcome },
            nextSteps[outcome]!!
        )}.plus(GeneralQuestionState.QuestionStateLink<Pair<String, Boolean>>(
            {situation, answer -> !answer.second },
            branchSelectRedirect
        )).toSet()

        val mainQuestion = object : CorrectnessCheckQuestionState<String>(mainLinks) {
            override fun text(situation: ILearningSituation): String {
                return node.question(situation.templating)
            }

            override fun options(situation: ILearningSituation): List<SingleChoiceOption<Pair<String, Boolean>>> {
                val answer = node.getAnswer(situation.answers)!!
                val correctOutcome = node.next.info.first { it.key == answer } //TODO возможно стоит изменить систему Outcomes чтобы вот такие конструкции были проще
                return node.next.info.map { SingleChoiceOption(
                    it.text(situation.templating)!!,
                    Explanation("Это неверно, поскольку ${it.explanation(situation.templating, false)}. " +
                            "В этой ситуации ${correctOutcome.explanation(situation.templating, true)}"),
                    it.key to (it == correctOutcome),
                )}
            }
        }

        //Если результат выполнения узла был выбран неверно, то спросить про углубление в одну из веток
        val shadowRedirect = RedirectQuestionState()
        val branches = node.next.info.map {it.decidingBranch}.filterNotNull()
        val branchAutomata = branches.map { it to QuestionAutomata(it.use(this))}.toMap()
        val branchLinks = branches.map{branch -> GeneralQuestionState.QuestionStateLink<ThoughtBranch?>(
            {situation, answer -> answer == branch }, branchAutomata[branch]!!.initState
        )}.plus(GeneralQuestionState.QuestionStateLink<ThoughtBranch?>(
            {situation, answer -> answer == null }, shadowRedirect
        )).toSet()

        val branchSelectQuestion = object : SingleChoiceQuestionState<ThoughtBranch?>(branchLinks) {
            override fun text(situation: ILearningSituation): String {
                return "В чем бы вы хотели разобраться подробнее?"
            }

            override fun options(situation: ILearningSituation): List<SingleChoiceOption<ThoughtBranch?>> {
                val correctAnswer = node.getAnswer(situation.answers)!!
                val correctBranch = node.next.predeterminingBranch(correctAnswer)

                val chosenAnswer = mainQuestion.previouslyChosenAnswer(situation)!!.first
                val incorrectBranch = node.next.predeterminingBranch(chosenAnswer)

                val options = mutableListOf<SingleChoiceOption<ThoughtBranch?>>()
                if(incorrectBranch != null && !incorrectBranch.isTrivial())
                    options.add(SingleChoiceOption(
                        "Почему ${incorrectBranch.description(situation.templating, false)}?",
                        null,
                        incorrectBranch,
                    ))
                if(correctBranch != null && !correctBranch.isTrivial())
                    options.add(SingleChoiceOption(
                        "Почему ${correctBranch.description(situation.templating, true)}?",
                        null,
                        correctBranch,
                    ))
                if(options.isEmpty())
                    options.add(SingleChoiceOption(
                        "Подробный разбор не нужен",
                        null,
                        null,
                    ))
                return options
            }
        }
        branchSelectRedirect.redir = branchSelectQuestion


        //После выхода из веток пропускающее состояние определяет, к какому из верных ответов надо совершить переход
        val shadowSkip = object : SkipQuestionState(){
            override fun skip(situation: ILearningSituation): QuestionStateChange {
                return QuestionStateChange(null, nextSteps[node.getAnswer(situation.answers)!!])
            }

            override val reachableStates: Collection<QuestionState>
                get() = nextSteps.values
        }
        branchAutomata.values.forEach{it.finalize(shadowSkip)}
        shadowRedirect.redir = shadowSkip

        return mainQuestion
    }

    override fun process(node: QuestionNode): QuestionState {
        val links = node.next.keys.map { outcomeLiteral ->
            GeneralQuestionState.QuestionStateLink<Pair<Literal, Boolean>>(
                {situation, answer -> node.getAnswer(situation.answers) == outcomeLiteral },
                nextStep(node, outcomeLiteral)
            )
        }.toSet()

        val question = object : CorrectnessCheckQuestionState<Literal>(links) {
            override fun text(situation: ILearningSituation): String {
                return node.question(situation.templating)
            }

            override fun options(situation: ILearningSituation): List<SingleChoiceOption<Pair<Literal, Boolean>>> {
                val answer = node.getAnswer(situation.answers)!!
                val correctOutcome = node.next.info.first { it.key == answer } //TODO возможно стоит изменить систему Outcomes чтобы вот такие конструкции были проще
                val explText = correctOutcome.explanation(situation.templating)
                return node.next.info.map { SingleChoiceOption(
                    it.text(situation.templating)?:it.key.toAnswerString(situation),
                    if(explText != null) Explanation(explText) else null,
                    it.key to (it == correctOutcome),
                )}
            }
        }

        return question
    }

    override fun process(node: StartNode): QuestionState {
        return node.main.use(this)
    }

    override fun process(branch: ThoughtBranch): QuestionState {
        branchDiving.push(branch)
        val nextState = branch.start.use(this)

        val question = object : CorrectnessCheckQuestionState<DecisionTreeNode>(setOf(
            QuestionStateLink({situation, answer ->  true}, nextState)
        )) {
            override fun text(situation: ILearningSituation): String {
                return branch.nextStepQuestion(situation.templating) ?: "С чего надо начать, чтобы проверить, что ${branch.description(situation.templating, true)}?"
            }

            override fun options(situation: ILearningSituation): List<SingleChoiceOption<Pair<DecisionTreeNode, Boolean>>> {
                val jumps = branch.getPossibleJumps(situation.answers)

                return jumps.filter { it !is BranchResultNode}.map{
                    SingleChoiceOption(
                        it.asNextStep(situation.templating),
                        Explanation(branch.nextStepExplanation(situation.templating)?:""),
                        it to (it == branch.start)
                    )
                }
            }
        }

        branchDiving.pop()
        return question
    }

    override fun process(node: UndeterminedResultNode): QuestionState {
        TODO("Not yet implemented")
    }

    fun <AnswerType : Any> nextStep(node: LinkNode<AnswerType>, answer: AnswerType): QuestionState {
        val outcome = node.next.info.first { it.key == answer } //TODO возможно стоит изменить систему Outcomes чтобы вот такие конструкции были проще
        val nextState = outcome.value.use(this)
        val currentBranch = currentBranch

        val question = object : CorrectnessCheckQuestionState<DecisionTreeNode>(setOf(
            QuestionStateLink({situation, answer ->  true}, nextState)
        )) {
            override fun text(situation: ILearningSituation): String {
                return outcome.nextStepQuestion(situation.templating) ?: defaultNextStepQuestion
            }

            override fun options(situation: ILearningSituation): List<SingleChoiceOption<Pair<DecisionTreeNode, Boolean>>> {
                val explanation = outcome.nextStepExplanation(situation.templating)?:""
                val correct = node.correctNext(situation.answers)
                val correctIsResultTrue = correct is BranchResultNode && correct.value == BooleanLiteral(true)
                val correctIsResultFalse = correct is BranchResultNode && correct.value == BooleanLiteral(false)
                val jumps = node.getPossibleJumps(situation.answers) //TODO? правильная работа со структурой дерева, включая известность переменных

                val options = jumps.filter { it !is BranchResultNode}.map{SingleChoiceOption(
                    it.asNextStep(situation.templating),
                    Explanation(explanation),
                    it to (it == correct),
                )}.plus(SingleChoiceOption<Pair<DecisionTreeNode, Boolean>>(
                    outcome.nextStepBranchResult(situation.templating, true) ?:"Можно заключить, что ${currentBranch.description(situation.templating, true)}",
                    Explanation(explanation),
                    BranchResultNode(BooleanLiteral(true)) to correctIsResultTrue,
                )).plus(SingleChoiceOption<Pair<DecisionTreeNode, Boolean>>(
                    outcome.nextStepBranchResult(situation.templating, false) ?:"Можно заключить, что ${currentBranch.description(situation.templating, false)}",
                    Explanation(explanation),
                    BranchResultNode(BooleanLiteral(false)) to correctIsResultFalse,
                ))
                return options
            }
        }

        return question
    }

    // ---------------------- Удобства ---------------------------

    companion object _static{
        const val defaultNextStepQuestion = "Какой следующий шаг необходим для решения задачи?"

        @JvmStatic
        fun create(startNode: StartNode) : QuestionAutomata{
            val initState = startNode.use(SequentialAutomataCreation())
            val automata = QuestionAutomata(initState)
            val endState = object : SkipQuestionState(){
                override fun skip(situation: ILearningSituation): QuestionStateChange {
                    return QuestionStateChange(Explanation("Конец"), null)
                }

                override val reachableStates: Collection<QuestionState>
                    get() = emptyList()

            }
            automata.finalize(endState)
            return automata
        }
    }
}