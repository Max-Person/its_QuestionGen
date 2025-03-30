package its.questions.gen.strategies

import its.model.definition.types.Obj
import its.model.nodes.*
import its.model.nodes.visitors.DecisionTreeBehaviour
import its.questions.gen.QuestioningSituation
import its.questions.gen.formulations.TemplatingUtils.asNextStep
import its.questions.gen.formulations.TemplatingUtils.description
import its.questions.gen.formulations.TemplatingUtils.explanation
import its.questions.gen.formulations.TemplatingUtils.getLocalizedName
import its.questions.gen.formulations.TemplatingUtils.nextStepBranchResult
import its.questions.gen.formulations.TemplatingUtils.nextStepExplanation
import its.questions.gen.formulations.TemplatingUtils.nextStepQuestion
import its.questions.gen.formulations.TemplatingUtils.question
import its.questions.gen.formulations.TemplatingUtils.text
import its.questions.gen.formulations.TemplatingUtils.trivialityExplanation
import its.questions.gen.states.*
import its.questions.gen.visitors.GetPossibleJumps.Companion.getPossibleJumps
import its.questions.gen.visitors.ValueToAnswerString.toAnswerString
import its.reasoner.nodes.DecisionTreeReasoner
import its.reasoner.nodes.DecisionTreeReasoner.Companion.getAnswer
import its.reasoner.operators.OperatorReasoner
import java.util.*

object SequentialStrategy : QuestioningStrategyWithInfo<SequentialStrategy.SequentialAutomataInfo>{
    data class SequentialAutomataInfo(
        val nodeStates : MutableMap<DecisionTreeNode, QuestionState>,
        val preNodeStates : MutableMap<DecisionTreeNode, QuestionState>,
    )

    override fun buildWithInfo(branch: ThoughtBranch) : QuestioningStrategyWithInfo.StrategyOutput<SequentialAutomataInfo> {
        val build = SequentialAutomataBuilder()
        val initState = build.process(branch)
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
            val toEndRedirect = RedirectQuestionState()

            //1. спросить про результат узла в общем, как в агрегации
            var nextSteps: Map<BranchResult, QuestionState> = mapOf(
                BranchResult.CORRECT to toEndRedirect,
                BranchResult.ERROR to toEndRedirect,
                BranchResult.NULL to toEndRedirect
            )

            val firstToSecondRedirect = RedirectQuestionState()
            val nodeAnswerQuestionFirst: QuestionState
            if (currentBranch.isTrivial()) {
                nodeAnswerQuestionFirst = firstToSecondRedirect
            } else {
                nextSteps = node.outcomes.keys.map { outcome -> outcome to nextStep(node, outcome) }.toMap()
                val mainLinks = node.outcomes.keys.map { outcome ->
                    GeneralQuestionState.QuestionStateLink<CorrectnessCheckQuestionState.Correctness<BranchResult>>(
                        { situation, answer -> answer.isCorrect && (answer.answerInfo == outcome) },
                        nextSteps[outcome]!!
                    )
                }.plus(
                    GeneralQuestionState.QuestionStateLink(
                        { situation, answer -> !answer.isCorrect },
                        firstToSecondRedirect
                    )
                ).toSet()

                nodeAnswerQuestionFirst = object : CorrectnessCheckQuestionState<BranchResult>(mainLinks) {
                    override fun text(situation: QuestioningSituation): String {
                        val descr = node.description(situation, BranchResult.CORRECT)
                        return situation.localization.IS_IT_TRUE_THAT(descr)
                    }

                    override fun options(situation: QuestioningSituation): List<SingleChoiceOption<Correctness<BranchResult>>> {
                        val correctAnswer = node.getAnswer(situation)
                        return node.outcomes.map {
                            SingleChoiceOption(
                                when (it.key) {
                                    BranchResult.CORRECT -> situation.localization.TRUE
                                    BranchResult.ERROR -> situation.localization.FALSE
                                    BranchResult.NULL -> situation.localization.CANNOT_BE_DETERMINED
                                },
                                Explanation(
                                    situation.localization.THATS_INCORRECT + " " + situation.localization.LETS_FIGURE_IT_OUT,
                                    type = ExplanationType.Error,
                                    shouldPause = false
                                ),
                                Correctness(it.key, it.key == correctAnswer),
                            )
                        }
                    }
                }
            }

            //2. Если неверно, то попросить выбрать все подходящие переменные цикла
            val secondToThirdRedirect = RedirectQuestionState()
            val objectSelectQuestionSecond = object : MultipleChoiceQuestionState<Obj>(secondToThirdRedirect) {
                override fun text(situation: QuestioningSituation): String {
                    return node.question(situation)
                }

                override fun options(situation: QuestioningSituation): List<MultipleChoiceOption<Obj>> {
                    val searchResult = DecisionTreeReasoner(situation).searchWithErrors(node)
                    val options = searchResult.correct
                        .map { obj ->
                            val objectName = obj.getLocalizedName(situation.domainModel, situation.localizationCode)
                            MultipleChoiceOption(
                                objectName,
                                obj,
                                true,
                                situation.localization.ALSO_FITS_THE_CRITERIA(objectName),
                            )
                        }
                        .plus(searchResult.errors
                            .flatMap { (error, objects) -> objects.map { error to it }}
                            .map { (error, obj) ->
                                val objectName = obj.getLocalizedName(situation.domainModel, situation.localizationCode)
                                MultipleChoiceOption(
                                    objectName,
                                    obj,
                                    false,
                                    error.explanation(situation, obj.objectName)
                                )
                            }
                        )
                    return options
                }
            }
            firstToSecondRedirect.redir = objectSelectQuestionSecond

            //3. Спросить про конкретные переменные цикла, как они влияют на агрегацию
            val thirdToFourthRedirect = RedirectQuestionState()
            val variableAggregationQuestionThird = CycleAggregationState(
                node,
                nextSteps,
                thirdToFourthRedirect
            )
            secondToThirdRedirect.redir = variableAggregationQuestionThird

            //Если хотя бы одна неправильная, то спросить в какую углубиться
            val (selectBranchQuestionFourth, branchAutomata) = variableAggregationQuestionThird.createSelectBranchState()
            thirdToFourthRedirect.redir = selectBranchQuestionFourth

            //После выхода из веток пропускающее состояние определяет, к какому из верных ответов надо совершить переход
            val shadowSkip = object : SkipQuestionState(){
                override fun skip(situation: QuestioningSituation): QuestionStateChange {
                    return QuestionStateChange(null, nextSteps[node.getAnswer(situation)])
                }

                override val reachableStates: Collection<QuestionState>
                    get() = nextSteps.values
            }
            branchAutomata.forEach{it.finalize(shadowSkip)}

            nodeStates[node] = nodeAnswerQuestionFirst
            return nodeAnswerQuestionFirst
        }

        override fun process(node: WhileCycleNode): QuestionState {
            return getNotImplementedState()
        }

        private fun getNotImplementedState() : QuestionState{
            val redirect = RedirectQuestionState()
            return object : SkipQuestionState(){
                override fun skip(situation: QuestioningSituation): QuestionStateChange {
                    return QuestionStateChange(
                        Explanation("<A question state for this node is not yet implemented"),
                        redirect
                    )
                }

                override val reachableStates: Collection<QuestionState>
                    get() = listOf(redirect)

            }
        }

        override fun process(node: FindActionNode): QuestionState {
            if(currentBranch.isTrivial())
                return RedirectQuestionState()

            val nextSteps = node.outcomes.keys.map { answer -> answer to nextStep(node, answer)}.toMap()

            val skip = object : SkipQuestionState() {
                override fun skip(situation: QuestioningSituation): QuestionStateChange {
                    val correctAnswer = node.getAnswer(situation)

                    val explanation = Explanation(situation.localization.WE_ALREADY_DISCUSSED_THAT(
                            node.outcomes[correctAnswer]!!
                                .explanation(situation)!!
                    ))
                    val nextState = nextSteps[correctAnswer]
                    return QuestionStateChange(explanation, nextState)
                }

                override val reachableStates: Collection<QuestionState>
                    get() = nextSteps.values
            }

            nodeStates[node] = skip
            return skip
        }

        override fun process(node: BranchAggregationNode): QuestionState {
            //сначала задать вопрос о результате выполнения узла
            val aggregationRedirect = RedirectQuestionState()

            val endRedir = RedirectQuestionState()
            var nextSteps : Map<BranchResult, QuestionState> = mapOf(BranchResult.CORRECT to endRedir,
                BranchResult.ERROR to endRedir, BranchResult.NULL to endRedir)

            val initQuestion: QuestionState
            if (currentBranch.isTrivial()) {
                initQuestion = aggregationRedirect
            } else {
                nextSteps = node.outcomes.keys.map { outcome -> outcome to nextStep(node, outcome) }.toMap()
                val mainLinks = node.outcomes.keys.map { outcome ->
                    GeneralQuestionState.QuestionStateLink<CorrectnessCheckQuestionState.Correctness<BranchResult>>(
                        { situation, answer -> answer.isCorrect && (answer.answerInfo == outcome) },
                        nextSteps[outcome]!!
                    )
                }.plus(
                    GeneralQuestionState.QuestionStateLink(
                        { situation, answer -> !answer.isCorrect },
                        aggregationRedirect
                    )
                ).toSet()

                initQuestion = object : CorrectnessCheckQuestionState<BranchResult>(mainLinks) {
                    override fun text(situation: QuestioningSituation): String {
                        val descr = node.description(situation, BranchResult.CORRECT)
                        return situation.localization.IS_IT_TRUE_THAT(descr)
                    }

                    override fun options(situation: QuestioningSituation): List<SingleChoiceOption<Correctness<BranchResult>>> {
                        val correctAnswer = node.getAnswer(situation)
                        return node.outcomes.map {
                            SingleChoiceOption(
                                when (it.key) {
                                    BranchResult.CORRECT -> situation.localization.TRUE
                                    BranchResult.ERROR -> situation.localization.FALSE
                                    BranchResult.NULL -> situation.localization.CANNOT_BE_DETERMINED
                                },
                                Explanation(
                                    situation.localization.THATS_INCORRECT + " " + situation.localization.LETS_FIGURE_IT_OUT,
                                    type = ExplanationType.Error,
                                    shouldPause = false
                                ),
                                Correctness(it.key, it.key == correctAnswer),
                            )
                        }
                    }
                }
            }

            //Если результат выполнения узла был выбран неверно, то спросить про результаты веток
            val branchSelectRedirect = RedirectQuestionState()

            val aggregationQuestion = LogicalAggregationState(
                node,
                nextSteps,
                branchSelectRedirect
            )
            aggregationRedirect.redir = aggregationQuestion

            //Если были сделаны ошибки в ветках, то спросить про углубление в одну из веток
            val (selectBranchState, branchAutomata) = aggregationQuestion.createSelectBranchState()
            branchSelectRedirect.redir = selectBranchState

            //После выхода из веток пропускающее состояние определяет, к какому из верных ответов надо совершить переход
            val shadowSkip = object : SkipQuestionState(){
                override fun skip(situation: QuestioningSituation): QuestionStateChange {
                    return QuestionStateChange(null, nextSteps[node.getAnswer(situation)])
                }

                override val reachableStates: Collection<QuestionState>
                    get() = nextSteps.values
            }
            branchAutomata.forEach{it.finalize(shadowSkip)}

            nodeStates[node] = initQuestion
            return initQuestion
        }

        override fun process(node: QuestionNode): QuestionState {
            if(currentBranch.isTrivial())
                return RedirectQuestionState()

            val links = node.outcomes.keys.map { outcomeValue ->
                GeneralQuestionState.QuestionStateLink<CorrectnessCheckQuestionState.Correctness<Any>>(
                    { situation, answer -> node.getAnswer(situation) == outcomeValue },
                    nextStep(node, outcomeValue)
                )
            }.toSet()

            val question = object : CorrectnessCheckQuestionState<Any>(links) {
                override fun text(situation: QuestioningSituation): String {
                    return node.question(situation)
                }

                override fun options(situation: QuestioningSituation): List<SingleChoiceOption<Correctness<Any>>> {
                    val answer = node.getAnswer(situation)
                    val correctOutcome = node.outcomes[answer]!!
                    val explText = correctOutcome.explanation(situation)
                    return node.outcomes.map { SingleChoiceOption(
                        it.text(situation)?:it.key.toAnswerString(situation),
                        if(explText != null) Explanation(explText, type = ExplanationType.Error) else null,
                        Correctness(it.key, it == correctOutcome),
                    )}
                }

                override fun preliminarySkip(situation: QuestioningSituation): QuestionStateChange? {
                    val nextState = links.first { it.condition(situation, Correctness(true, true)) }.nextState
                    if(node.isSwitch)
                        return QuestionStateChange(null, nextState)
                    if(node.trivialityExpr?.use(OperatorReasoner.defaultReasoner(situation)) == true)
                        return QuestionStateChange(node.trivialityExplanation(situation)?.let { Explanation(it, shouldPause = false) }, nextState)
                    return super.preliminarySkip(situation)
                }
            }

            nodeStates[node] = question
            return question
        }

        override fun processTupleQuestionNode(node: TupleQuestionNode): QuestionState {
            if(currentBranch.isTrivial())
                return RedirectQuestionState()

            val nextSteps = node.outcomes.keys.associateWith { nextStep(node, it) }
            var question : QuestionState = object : SkipQuestionState() {
                override fun skip(situation: QuestioningSituation): QuestionStateChange {
                    val nodeAnswer = node.getAnswer(situation)
                    return QuestionStateChange(null, nextSteps[nodeAnswer]!!)
                }

                override val reachableStates: Collection<QuestionState>
                    get() = nextSteps.values

            }

            fun linkToSingle(state: QuestionState) = setOf(
                GeneralQuestionState.QuestionStateLink<CorrectnessCheckQuestionState.Correctness<Any>>(
                { situation, answer -> true },
                state
            ))
            for(part in node.parts.asReversed()){
                question = object : CorrectnessCheckQuestionState<Any>(linkToSingle(question)) {
                    override fun text(situation: QuestioningSituation): String {
                        return part.question(situation)
                    }

                    override fun options(situation: QuestioningSituation): List<SingleChoiceOption<Correctness<Any>>> {
                        val answer = part.expr.use(OperatorReasoner.defaultReasoner(situation))
                        val correctOutcome = part.possibleOutcomes.first { it.value == answer }
                        val explText = correctOutcome.explanation(situation)
                        return part.possibleOutcomes.map { SingleChoiceOption(
                            it.text(situation) ?: it.value.toAnswerString(situation),
                            explText?.let { Explanation(it, ExplanationType.Error) },
                            Correctness(it.value, it == correctOutcome),
                        )}
                    }
                }
            }

            nodeStates[node] = question
            return question
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
                    return branch.nextStepQuestion(situation) ?: situation.localization.DEFAULT_REASONING_START_QUESTION(reasoning_topic = branch.description(situation, BranchResult.CORRECT))
                }

                override fun options(situation: QuestioningSituation): List<SingleChoiceOption<Correctness<DecisionTreeNode>>> {
                    val jumps = branch.start.getPossibleJumps(situation)

                    return jumps.filter { it !is BranchResultNode}.map{
                        SingleChoiceOption(
                            it.asNextStep(situation),
                            Explanation(branch.nextStepExplanation(situation), type = ExplanationType.Error),
                            Correctness(it, it == branch.start)
                        )
                    }
                }
            }

            branchDiving.pop()
            preNodeStates[branch.start] = question
            return question
        }

        fun <AnswerType : Any> nextStep(node: LinkNode<AnswerType>, answer: AnswerType): QuestionState {
            val outcome = node.outcomes[answer]!!
            val nextNode = outcome.node
            val nextState = nextNode.use(this)
            val currentBranch = currentBranch

            val question = object : CorrectnessCheckQuestionState<DecisionTreeNode>(setOf(
                QuestionStateLink({situation, answer ->  true}, nextState)
            )) {
                override fun text(situation: QuestioningSituation): String {
                    return outcome.nextStepQuestion(situation) ?: situation.localization.DEFAULT_NEXT_STEP_QUESTION
                }

                override fun options(situation: QuestioningSituation): List<SingleChoiceOption<Correctness<DecisionTreeNode>>> {
                    val explText = outcome.nextStepExplanation(situation)
                    val explanation = if(explText == null) null else Explanation(explText, type = ExplanationType.Error)
                    val jumps = node.getPossibleJumps(situation) //TODO? правильная работа со структурой дерева, включая известность переменных

                    val options = jumps
                        .filter { it !is BranchResultNode }
                        .map {
                            SingleChoiceOption(
                                it.asNextStep(situation),
                                explanation,
                                Correctness(it, it == nextNode),
                            )
                        }
                        .plus(BranchResult.entries.map { result ->
                            SingleChoiceOption(
                                outcome.nextStepBranchResult(situation, result)
                                    ?: situation.localization.WE_CAN_CONCLUDE_THAT(
                                        currentBranch.description(
                                            situation,
                                            result
                                        )
                                    ),
                                explanation,
                                Correctness(
                                    nextNode,
                                    nextNode is BranchResultNode && nextNode.value == BranchResult.CORRECT
                                ),
                            )
                        })
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