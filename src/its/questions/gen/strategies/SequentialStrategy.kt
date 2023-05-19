package its.questions.gen.strategies

import its.model.nodes.*
import its.model.nodes.visitors.DecisionTreeBehaviour
import its.questions.gen.QuestioningSituation
import its.questions.gen.formulations.TemplatingUtils._static.asNextStep
import its.questions.gen.formulations.TemplatingUtils._static.description
import its.questions.gen.formulations.TemplatingUtils._static.explanation
import its.questions.gen.formulations.TemplatingUtils._static.nextStepBranchResult
import its.questions.gen.formulations.TemplatingUtils._static.nextStepExplanation
import its.questions.gen.formulations.TemplatingUtils._static.nextStepQuestion
import its.questions.gen.formulations.TemplatingUtils._static.question
import its.questions.gen.formulations.TemplatingUtils._static.text
import its.questions.gen.states.*
import its.questions.gen.visitors.GetPossibleJumps._static.getPossibleJumps
import its.questions.gen.visitors.ValueToAnswerString.toAnswerString
import its.reasoner.nodes.DecisionTreeReasoner._static.getAnswer
import java.util.*

object SequentialStrategy : QuestioningStrategyWithInfo<SequentialStrategy.SequentialAutomataInfo>{
    data class SequentialAutomataInfo(
        val nodeStates : MutableMap<DecisionTreeNode, QuestionState>,
        val preNodeStates : MutableMap<DecisionTreeNode, QuestionState>,
    )

    override fun buildWithInfo(branch: ThoughtBranch) : QuestioningStrategyWithInfo.StrategyOutput<SequentialAutomataInfo> {
        val build = SequentialAutomataBuilder()
        val initState = branch.use(build)
        val automata = QuestionAutomata(initState)
        return QuestioningStrategyWithInfo.StrategyOutput(automata, SequentialAutomataInfo(build.nodeStates, build.preNodeStates))
    }


    private class SequentialAutomataBuilder : DecisionTreeBehaviour<QuestionState> {
        private fun ThoughtBranch.isTrivial() : Boolean{
            return start !is LinkNode<*> || (start as LinkNode<*>).children.all { it is BranchResultNode }
        }

        val nodeStates = mutableMapOf<DecisionTreeNode, QuestionState>()
        val preNodeStates = mutableMapOf<DecisionTreeNode, QuestionState>()
        private val branchDiving : Stack<ThoughtBranch> = Stack()
        private val currentBranch
            get() = branchDiving.peek()

        override fun process(node: BranchResultNode): QuestionState {
            return RedirectQuestionState()
        }

        override fun process(node: CycleAggregationNode): QuestionState {
            TODO("Not yet implemented")
        }

        override fun process(node: WhileAggregationNode): QuestionState {
            TODO("Not yet implemented")
        }

        override fun process(node: FindActionNode): QuestionState {
            if(currentBranch.isTrivial())
                return RedirectQuestionState()

            val nextSteps = node.next.keys.map { answer -> answer to nextStep(node, answer)}.toMap()

            val skip = object : SkipQuestionState() {
                override fun skip(situation: QuestioningSituation): QuestionStateChange {
                    val correctAnswer = node.getAnswer(situation)

                    val explanation = Explanation(situation.localization.WE_ALREADY_DISCUSSED_THAT(fact = node.next.getFull(correctAnswer)!!.explanation(situation.localizationCode, situation.templating)!!))
                    val nextState = nextSteps[correctAnswer]
                    return QuestionStateChange(explanation, nextState)
                }

                override val reachableStates: Collection<QuestionState>
                    get() = nextSteps.values
            }

            nodeStates[node] = skip
            return skip
        }

        private fun QuestionAutomata.hasQuestions() : Boolean{
            return this.any { state -> state is GeneralQuestionState<*> || (state is RedirectQuestionState && state.redirectsTo() is GeneralQuestionState<*>) }
        }

        override fun process(node: LogicAggregationNode): QuestionState {
            //сначала задать вопрос о результате выполнения узла
            val aggregationRedirect = RedirectQuestionState()

            val endRedir = RedirectQuestionState()
            var nextSteps : Map<Boolean, QuestionState> = mapOf(true to endRedir, false to endRedir)

            val initQuestion =
                if(currentBranch.isTrivial()) aggregationRedirect
                else {
                    nextSteps = node.next.map { (outcome) -> outcome to nextStep(node, outcome)}.toMap()
                    val mainLinks = node.next.map { (outcome) -> GeneralQuestionState.QuestionStateLink<Pair<Boolean, Boolean>>(
                        {situation, answer -> answer.second && (answer.first == outcome) },
                        nextSteps[outcome]!!
                    )}.plus(GeneralQuestionState.QuestionStateLink<Pair<Boolean, Boolean>>(
                        {situation, answer -> !answer.second },
                        aggregationRedirect
                    )).toSet()

                    object : CorrectnessCheckQuestionState<Boolean>(mainLinks) {
                        override fun text(situation: QuestioningSituation): String {
                            val descr = node.description(situation.localizationCode, situation.templating, true)
                            return situation.localization.IS_IT_TRUE_THAT(descr)
                        }

                        override fun options(situation: QuestioningSituation): List<SingleChoiceOption<Pair<Boolean, Boolean>>> {
                            val correctAnswer = node.getAnswer(situation)
                            return node.next.info.map {
                                SingleChoiceOption(
                                    if (it.key) situation.localization.TRUE else situation.localization.FALSE,
                                    Explanation(situation.localization.THATS_INCORRECT + " " + situation.localization.LETS_FIGURE_IT_OUT, type = ExplanationType.Error, shouldPause = false),
                                    it.key to (it.key == correctAnswer),
                                )
                            }
                        }
                    }
            }

            //Если результат выполнения узла был выбран неверно, то спросить про результаты веток
            val branchSelectRedirect = RedirectQuestionState()

            val aggregationQuestion = AggregationQuestionState(node, setOf(
                GeneralQuestionState.QuestionStateLink(
                    { situation, answer -> answer && node.getAnswer(situation) == true },
                    nextSteps[true]!!
                ),
                GeneralQuestionState.QuestionStateLink(
                    { situation, answer -> answer && node.getAnswer(situation) == false },
                    nextSteps[false]!!
                ),
                GeneralQuestionState.QuestionStateLink(
                    { situation, answer -> !answer },
                    branchSelectRedirect
                ),
            ))
            aggregationRedirect.redir = aggregationQuestion


            //Если были сделаны ошибки в ветках, то спросить про углубление в одну из веток
            val branchAutomata = node.thoughtBranches.map { it to QuestioningStrategy.defaultFullBranchStrategy.build(it)}.toMap()
            val worthAsking = node.thoughtBranches.map{it to (branchAutomata[it]!!.hasQuestions())}.toMap()
            val branchLinks = node.thoughtBranches.filter{worthAsking[it]!!}.map{branch -> GeneralQuestionState.QuestionStateLink<ThoughtBranch>(
                {situation, answer -> answer == branch }, branchAutomata[branch]!!.initState
            )}.toSet()

            val branchSelectQuestion = object : SingleChoiceQuestionState<ThoughtBranch>(branchLinks) {
                override fun text(situation: QuestioningSituation): String {
                    return situation.localization.WHAT_DO_YOU_WANT_TO_DISCUSS_FURTHER
                }

                override fun options(situation: QuestioningSituation): List<SingleChoiceOption<ThoughtBranch>> {
                    val options = node.thoughtBranches.filter{branch ->
                        branch.getAnswer(situation) != situation.assumedResult(branch)
                    }.map { branch ->
                        SingleChoiceOption<ThoughtBranch>(
                            situation.localization.WHY_IS_IT_THAT(statement = branch.description(situation.localizationCode, situation.templating,
                                branch.getAnswer(situation)
                            )),
                            Explanation(situation.localization.LETS_FIGURE_IT_OUT, shouldPause = false),
                            branch
                        )
                    }
                    return options
                }

                override fun explanationIfSkipped(situation: QuestioningSituation, skipOption: SingleChoiceOption<ThoughtBranch>): Explanation {
                    return Explanation(situation.localization.LETS_FIGURE_IT_OUT, shouldPause = false)
                }
            }
            branchSelectRedirect.redir = branchSelectQuestion


            //После выхода из веток пропускающее состояние определяет, к какому из верных ответов надо совершить переход
            val shadowSkip = object : SkipQuestionState(){
                override fun skip(situation: QuestioningSituation): QuestionStateChange {
                    return QuestionStateChange(null, nextSteps[node.getAnswer(situation)])
                }

                override val reachableStates: Collection<QuestionState>
                    get() = nextSteps.values
            }
            branchAutomata.values.forEach{it.finalize(shadowSkip)}

            nodeStates[node] = initQuestion
            return initQuestion
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
                override fun text(situation: QuestioningSituation): String {
                    return node.question(situation.localizationCode, situation.templating)
                }

                override fun options(situation: QuestioningSituation): List<SingleChoiceOption<Pair<String, Boolean>>> {
                    val answer = node.getAnswer(situation)
                    val correctOutcome = node.next.info.first { it.key == answer } //TODO возможно стоит изменить систему Outcomes чтобы вот такие конструкции были проще
                    return node.next.info.map { SingleChoiceOption(
                        it.text(situation.localizationCode, situation.templating)!!,
                        Explanation(situation.localization.THATS_INCORRECT_BECAUSE(reason = it.explanation(situation.localizationCode, situation.templating, false)) + " " +
                                situation.localization.IN_THIS_SITUATION(fact = correctOutcome.explanation(situation.localizationCode, situation.templating, true)),
                                type = ExplanationType.Error),
                        it.key to (it == correctOutcome),
                    )}
                }
            }

            //Если результат выполнения узла был выбран неверно, то спросить про углубление в одну из веток
            val shadowRedirect = RedirectQuestionState()
            val branches = node.next.info.map {it.decidingBranch}.filterNotNull()
            val branchAutomata = branches.map { it to QuestioningStrategy.defaultFullBranchStrategy.build(it)}.toMap()
            val worthAsking = branches.map{it to (branchAutomata[it]!!.hasQuestions())}.toMap()
            val branchLinks = branches.filter{worthAsking[it]!!}.map{branch -> GeneralQuestionState.QuestionStateLink<ThoughtBranch?>(
                {situation, answer -> answer == branch }, branchAutomata[branch]!!.initState
            )}.plus(GeneralQuestionState.QuestionStateLink<ThoughtBranch?>(
                {situation, answer -> answer == null }, shadowRedirect
            )).toSet()

            val branchSelectQuestion = object : SingleChoiceQuestionState<ThoughtBranch?>(branchLinks) {
                override fun text(situation: QuestioningSituation): String {
                    return situation.localization.WHAT_DO_YOU_WANT_TO_DISCUSS_FURTHER
                }

                override fun options(situation: QuestioningSituation): List<SingleChoiceOption<ThoughtBranch?>> {
                    val correctAnswer = node.getAnswer(situation)
                    val correctBranch = node.next.predeterminingBranch(correctAnswer)

                    val chosenAnswer = mainQuestion.previouslyChosenAnswer(situation)!!.first
                    val incorrectBranch = node.next.predeterminingBranch(chosenAnswer)

                    val options = mutableListOf<SingleChoiceOption<ThoughtBranch?>>()
                    if(incorrectBranch != null && worthAsking[incorrectBranch]!!)
                        options.add(SingleChoiceOption(
                            situation.localization.WHY_IS_IT_THAT(incorrectBranch.description(situation.localizationCode, situation.templating, false)),
                            Explanation(situation.localization.LETS_FIGURE_IT_OUT, shouldPause = false),
                            incorrectBranch,
                        ))
                    if(correctBranch != null && worthAsking[correctBranch]!!)
                        options.add(SingleChoiceOption(
                            situation.localization.WHY_IS_IT_THAT(correctBranch.description(situation.localizationCode, situation.templating, true)),
                            Explanation(situation.localization.LETS_FIGURE_IT_OUT, shouldPause = false),
                            correctBranch,
                        ))
                    if(options.isEmpty())
                        options.add(SingleChoiceOption(
                            situation.localization.NO_FURTHER_DISCUSSION_NEEDED,
                            null,
                            null,
                        ))
                    return options
                }

                override fun explanationIfSkipped(situation: QuestioningSituation, skipOption: SingleChoiceOption<ThoughtBranch?>): Explanation? {
                    return if(skipOption.assocAnswer != null) Explanation(situation.localization.LETS_FIGURE_IT_OUT, shouldPause = false) else null
                }

                override fun additionalActions(situation: QuestioningSituation, chosenAnswer: ThoughtBranch?) {
                    super.additionalActions(situation, chosenAnswer)
                    //Не думаю, что имеет значение, где добавлять эту информацию,
                    // но теоретически ее можно было бы записывать на один вопрос раньше, и использовать вместо mainQuestion.previouslyChosenAnswer(...)
                    if(chosenAnswer != null) situation.addAssumedResult(chosenAnswer, !chosenAnswer.getAnswer(situation))
                }
            }
            branchSelectRedirect.redir = branchSelectQuestion


            //После выхода из веток пропускающее состояние определяет, к какому из верных ответов надо совершить переход
            val shadowSkip = object : SkipQuestionState(){
                override fun skip(situation: QuestioningSituation): QuestionStateChange {
                    return QuestionStateChange(null, nextSteps[node.getAnswer(situation)])
                }

                override val reachableStates: Collection<QuestionState>
                    get() = nextSteps.values
            }
            branchAutomata.values.forEach{it.finalize(shadowSkip)}
            shadowRedirect.redir = shadowSkip

            nodeStates[node] = mainQuestion
            return mainQuestion
        }

        override fun process(node: QuestionNode): QuestionState {
            if(currentBranch.isTrivial())
                return RedirectQuestionState()

            val links = node.next.keys.map { outcomeLiteral ->
                GeneralQuestionState.QuestionStateLink<Pair<Any, Boolean>>(
                    { situation, answer -> node.getAnswer(situation) == outcomeLiteral },
                    nextStep(node, outcomeLiteral)
                )
            }.toSet()

            val question = object : CorrectnessCheckQuestionState<Any>(links) {
                override fun text(situation: QuestioningSituation): String {
                    return node.question(situation.localizationCode, situation.templating)
                }

                override fun options(situation: QuestioningSituation): List<SingleChoiceOption<Pair<Any, Boolean>>> {
                    val answer = node.getAnswer(situation)
                    val correctOutcome = node.next.info.first { it.key == answer } //TODO возможно стоит изменить систему Outcomes чтобы вот такие конструкции были проще
                    val explText = correctOutcome.explanation(situation.localizationCode, situation.templating)
                    return node.next.info.map { SingleChoiceOption(
                        it.text(situation.localizationCode, situation.templating)?:it.key.toAnswerString(situation),
                        if(explText != null) Explanation(explText, type = ExplanationType.Error) else null,
                        it.key to (it == correctOutcome),
                    )}
                }
            }

            nodeStates[node] = question
            return question
        }

        override fun process(node: StartNode): QuestionState {
            return node.main.use(this)
        }

        override fun process(branch: ThoughtBranch): QuestionState {
            branchDiving.push(branch)
            val nextState = branch.start.use(this)
            if(nextState is RedirectQuestionState)
                return nextState

            val question = object : CorrectnessCheckQuestionState<DecisionTreeNode>(setOf(
                QuestionStateLink({situation, answer ->  true}, nextState)
            )) {
                override fun text(situation: QuestioningSituation): String {
                    return branch.nextStepQuestion(situation.localizationCode, situation.templating) ?: situation.localization.DEFAULT_REASONING_START_QUESTION(reasoning_topic = branch.description(situation.localizationCode, situation.templating, true))
                }

                override fun options(situation: QuestioningSituation): List<SingleChoiceOption<Pair<DecisionTreeNode, Boolean>>> {
                    val jumps = branch.getPossibleJumps(situation)

                    return jumps.filter { it !is BranchResultNode}.map{
                        SingleChoiceOption(
                            it.asNextStep(situation.localizationCode, situation.templating),
                            Explanation(branch.nextStepExplanation(situation.localizationCode, situation.templating)!!, type = ExplanationType.Error),
                            it to (it == branch.start)
                        )
                    }
                }
            }

            branchDiving.pop()
            preNodeStates[branch.start] = question
            return question
        }

        override fun process(node: UndeterminedResultNode): QuestionState {
            TODO("Not yet implemented")
        }

        fun <AnswerType : Any> nextStep(node: LinkNode<AnswerType>, answer: AnswerType): QuestionState {
            val outcome = node.next.info.first { it.key == answer } //TODO возможно стоит изменить систему Outcomes чтобы вот такие конструкции были проще
            val nextNode = outcome.value
            val nextNodeIsResultTrue = nextNode is BranchResultNode && nextNode.value == true
            val nextNodeIsResultFalse = nextNode is BranchResultNode && nextNode.value == false
            val nextState = nextNode.use(this)
            val currentBranch = currentBranch

            val question = object : CorrectnessCheckQuestionState<DecisionTreeNode>(setOf(
                QuestionStateLink({situation, answer ->  true}, nextState)
            )) {
                override fun text(situation: QuestioningSituation): String {
                    return outcome.nextStepQuestion(situation.localizationCode, situation.templating) ?: situation.localization.DEFAULT_NEXT_STEP_QUESTION
                }

                override fun options(situation: QuestioningSituation): List<SingleChoiceOption<Pair<DecisionTreeNode, Boolean>>> {
                    val explText = outcome.nextStepExplanation(situation.localizationCode, situation.templating)
                    val explanation = if(explText == null) null else Explanation(explText, type = ExplanationType.Error)
                    val jumps = node.getPossibleJumps(situation) //TODO? правильная работа со структурой дерева, включая известность переменных

                    val options = jumps.filter { it !is BranchResultNode}.map{SingleChoiceOption(
                        it.asNextStep(situation.localizationCode, situation.templating),
                        explanation,
                        it to (it == nextNode),
                    )}.plus(SingleChoiceOption<Pair<DecisionTreeNode, Boolean>>(
                        outcome.nextStepBranchResult(situation.localizationCode, situation.templating, true)
                            ?: situation.localization.WE_CAN_CONCLUDE_THAT(result = currentBranch.description(situation.localizationCode, situation.templating, true)),
                        explanation,
                        BranchResultNode(true, null) to nextNodeIsResultTrue,
                    )).plus(SingleChoiceOption<Pair<DecisionTreeNode, Boolean>>(
                        outcome.nextStepBranchResult(situation.localizationCode, situation.templating, false)
                            ?: situation.localization.WE_CAN_CONCLUDE_THAT(result = currentBranch.description(situation.localizationCode, situation.templating, false)),
                        explanation,
                        BranchResultNode(false, null) to nextNodeIsResultFalse,
                    ))
                    //WARN Дополнительные опции не работают с situation.addGivenAnswer() потому что BranchResultNode сравниваются по ссылке.
                    //Пока с этим ничего не делаем, однако в дальнейшем это может повлиять на что-то
                    return options
                }
            }

            preNodeStates[nextNode] = question
            return question
        }
    }
}