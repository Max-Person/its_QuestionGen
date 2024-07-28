package its.questions.gen.strategies

import its.model.Utils.nullCheck
import its.model.definition.types.Obj
import its.model.nodes.*
import its.model.nodes.visitors.DecisionTreeBehaviour
import its.questions.gen.QuestioningSituation
import its.questions.gen.formulations.TemplatingUtils.asNextStep
import its.questions.gen.formulations.TemplatingUtils.description
import its.questions.gen.formulations.TemplatingUtils.explanation
import its.questions.gen.formulations.TemplatingUtils.predeterminingExplanation
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
import its.reasoner.nodes.DecisionTreeReasoner._static.getAnswer
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
            var nextSteps : Map<Boolean, QuestionState> = mapOf(true to toEndRedirect, false to toEndRedirect)

            val firstToSecondRedirect = RedirectQuestionState()
            val nodeAnswerQuestionFirst =
                if(currentBranch.isTrivial()) firstToSecondRedirect
                else {
                    nextSteps = node.outcomes.keys.map { outcome -> outcome to nextStep(node, outcome)}.toMap()
                    val mainLinks = node.outcomes.keys.map { outcome ->
                        GeneralQuestionState.QuestionStateLink<Pair<Boolean, Boolean>>(
                            {situation, answer -> answer.second && (answer.first == outcome) },
                            nextSteps[outcome]!!
                        )
                    }.plus(GeneralQuestionState.QuestionStateLink(
                        {situation, answer -> !answer.second },
                        firstToSecondRedirect
                    )).toSet()

                    object : CorrectnessCheckQuestionState<Boolean>(mainLinks) {
                        override fun text(situation: QuestioningSituation): String {
                            val descr = node.description(situation.localizationCode, situation.templating, true)
                            return situation.localization.IS_IT_TRUE_THAT(descr)
                        }

                        override fun options(situation: QuestioningSituation): List<SingleChoiceOption<Pair<Boolean, Boolean>>> {
                            val correctAnswer = node.getAnswer(situation)
                            return node.outcomes.map {
                                SingleChoiceOption(
                                    if (it.key) situation.localization.TRUE else situation.localization.FALSE,
                                    Explanation(situation.localization.THATS_INCORRECT + " " + situation.localization.LETS_FIGURE_IT_OUT, type = ExplanationType.Error, shouldPause = false),
                                    it.key to (it.key == correctAnswer),
                                )
                            }
                        }
                    }
                }

            //2. Если неверно, то попросить выбрать все подходящие переменные цикла
            val secondToThirdRedirect = RedirectQuestionState()
            val objectSelectQuestionSecond = object : MultipleChoiceQuestionState<Obj>(secondToThirdRedirect) {
                override fun text(situation: QuestioningSituation): String {
                    return node.question(situation.localizationCode, situation.templating)
                }

                override fun options(situation: QuestioningSituation): List<MultipleChoiceOption<Obj>> {
                    val searchResult = DecisionTreeReasoner(situation).searchWithErrors(node)
                    val options = searchResult.correct
                        .map { obj ->
                            val objectName = with(situation.formulations) { obj.localizedName }
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
                                val objectName = with(situation.formulations) { obj.localizedName }
                                MultipleChoiceOption(
                                    objectName,
                                    obj,
                                    false,
                                    error.explanation(situation.localizationCode, situation.templating, obj.objectName)
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
                nextSteps[true]!!,
                nextSteps[false]!!,
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

        override fun process(node: WhileAggregationNode): QuestionState {
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
                                .explanation(situation.localizationCode, situation.templating)!!
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

        override fun process(node: LogicAggregationNode): QuestionState {
            //сначала задать вопрос о результате выполнения узла
            val aggregationRedirect = RedirectQuestionState()

            val endRedir = RedirectQuestionState()
            var nextSteps : Map<Boolean, QuestionState> = mapOf(true to endRedir, false to endRedir)

            val initQuestion =
                if(currentBranch.isTrivial()) aggregationRedirect
                else {
                    nextSteps = node.outcomes.keys.map { outcome -> outcome to nextStep(node, outcome)}.toMap()
                    val mainLinks = node.outcomes.keys.map { outcome -> GeneralQuestionState.QuestionStateLink<Pair<Boolean, Boolean>>(
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
                            return node.outcomes.map {
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

            val aggregationQuestion = LogicalAggregationState(
                node,
                nextSteps[true]!!,
                nextSteps[false]!!,
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

        override fun process(node: PredeterminingFactorsNode): QuestionState {
            //сначала задать вопрос о результате выполнения узла
            val branchSelectRedirect = RedirectQuestionState()

            val nextSteps = node.outcomes.keys.map { answer -> answer to nextStep(node, answer)}.toMap()
            val mainLinks = node.outcomes.keys.map { answer -> GeneralQuestionState.QuestionStateLink<Pair<ThoughtBranch?, Boolean>>(
                {situation, givenAnswer -> givenAnswer.second && givenAnswer.first == answer },
                nextSteps[answer]!!
            )}.plus(GeneralQuestionState.QuestionStateLink(
                {situation, answer -> !answer.second },
                branchSelectRedirect
            )).toSet()

            val mainQuestion = object : CorrectnessCheckQuestionState<ThoughtBranch?>(mainLinks) {
                override fun text(situation: QuestioningSituation): String {
                    return node.question(situation.localizationCode, situation.templating)
                }

                override fun options(situation: QuestioningSituation): List<SingleChoiceOption<Pair<ThoughtBranch?, Boolean>>> {
                    val answer = node.getAnswer(situation)
                    val correctOutcome = node.outcomes[answer]!!
                    return node.outcomes.map { SingleChoiceOption(
                        it.text(situation.localizationCode, situation.templating).nullCheck("PredeterminingFactorsNode's outcome has no ${situation.localizationCode} text"),
                        Explanation(situation.localization.THATS_INCORRECT_BECAUSE(reason = it.predeterminingExplanation(situation.localizationCode, situation.templating, false)) + " " +
                                situation.localization.IN_THIS_SITUATION(fact = correctOutcome.predeterminingExplanation(situation.localizationCode, situation.templating, true)),
                                type = ExplanationType.Error),
                        it.key to (it == correctOutcome),
                    )}
                }
            }

            //Если результат выполнения узла был выбран неверно, то спросить про углубление в одну из веток
            val shadowRedirect = RedirectQuestionState()
            val branches = node.outcomes.map {it.key}.filterNotNull()
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
                    val correctBranch = node.outcomes[correctAnswer]?.key

                    val chosenAnswer = mainQuestion.previouslyChosenAnswer(situation)!!.first
                    val incorrectBranch = node.outcomes[chosenAnswer]?.key

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

            val links = node.outcomes.keys.map { outcomeValue ->
                GeneralQuestionState.QuestionStateLink<Pair<Any, Boolean>>(
                    { situation, answer -> node.getAnswer(situation) == outcomeValue },
                    nextStep(node, outcomeValue)
                )
            }.toSet()

            val question = object : CorrectnessCheckQuestionState<Any>(links) {
                override fun text(situation: QuestioningSituation): String {
                    return node.question(situation.localizationCode, situation.templating)
                }

                override fun options(situation: QuestioningSituation): List<SingleChoiceOption<Pair<Any, Boolean>>> {
                    val answer = node.getAnswer(situation)
                    val correctOutcome = node.outcomes[answer]!!
                    val explText = correctOutcome.explanation(situation.localizationCode, situation.templating)
                    return node.outcomes.map { SingleChoiceOption(
                        it.text(situation.localizationCode, situation.templating)?:it.key.toAnswerString(situation),
                        if(explText != null) Explanation(explText, type = ExplanationType.Error) else null,
                        it.key to (it == correctOutcome),
                    )}
                }

                override fun preliminarySkip(situation: QuestioningSituation): QuestionStateChange? {
                    val nextState = links.first { it.condition(situation, true to true) }.nextState
                    if(node.isSwitch)
                        return QuestionStateChange(null, nextState)
                    if(node.trivialityExpr?.use(OperatorReasoner.defaultReasoner(situation)) == true)
                        return QuestionStateChange(node.trivialityExplanation(situation.localizationCode, situation.templating)?.let { Explanation(it, shouldPause = false) }, nextState)
                    return super.preliminarySkip(situation)
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
                    return branch.nextStepQuestion(situation.localizationCode, situation.templating) ?: situation.localization.DEFAULT_REASONING_START_QUESTION(reasoning_topic = branch.description(situation.localizationCode, situation.templating, true))
                }

                override fun options(situation: QuestioningSituation): List<SingleChoiceOption<Pair<DecisionTreeNode, Boolean>>> {
                    val jumps = branch.start.getPossibleJumps(situation)

                    return jumps.filter { it !is BranchResultNode}.map{
                        SingleChoiceOption(
                            it.asNextStep(situation.localizationCode, situation.templating),
                            Explanation(branch.nextStepExplanation(situation.localizationCode, situation.templating), type = ExplanationType.Error),
                            it to (it == branch.start)
                        )
                    }
                }
            }

            branchDiving.pop()
            preNodeStates[branch.start] = question
            return question
        }

        fun <AnswerType> nextStep(node: LinkNode<AnswerType>, answer: AnswerType): QuestionState {
            val outcome = node.outcomes[answer]!!
            val nextNode = outcome.node
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