package its.questions.gen.strategies

import its.model.definition.types.Obj
import its.model.nodes.*
import its.model.nodes.visitors.DecisionTreeBehaviour
import its.questions.gen.QuestioningSituation
import its.questions.gen.formulations.TemplatingUtils.asNextStep
import its.questions.gen.formulations.TemplatingUtils.description
import its.questions.gen.formulations.TemplatingUtils.explanation
import its.questions.gen.formulations.TemplatingUtils.generateAnswer
import its.questions.gen.formulations.TemplatingUtils.generateExplanation
import its.questions.gen.formulations.TemplatingUtils.getLocalizedName
import its.questions.gen.formulations.TemplatingUtils.nextStepBranchResult
import its.questions.gen.formulations.TemplatingUtils.nextStepExplanation
import its.questions.gen.formulations.TemplatingUtils.nextStepQuestion
import its.questions.gen.formulations.TemplatingUtils.question
import its.questions.gen.formulations.TemplatingUtils.text
import its.questions.gen.formulations.TemplatingUtils.trivialityExplanation
import its.questions.gen.states.*
import its.questions.gen.visitors.GetPossibleJumps.Companion.getPossibleJumps
import its.questions.gen.visitors.GetPossibleResults
import its.questions.gen.visitors.ValueToAnswerString.toLocalizedString
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

        private fun <Node : AggregationNode, BranchInfo> createAggregationState(
            node: Node,
            helper: AggregationHelper<Node, BranchInfo>,
        ): QuestionState {
            return when (node.aggregationMethod) {
                AggregationMethod.AND, AggregationMethod.OR -> createSimAggregationState(node, helper)
                AggregationMethod.HYP -> createHypAggregationState(node, helper)
                AggregationMethod.MUTEX -> createMutexAggregationState(node, helper)
            }
        }

        private fun <Node : AggregationNode, BranchInfo> createSimAggregationState(
            node: Node,
            helper: AggregationHelper<Node, BranchInfo>,
        ): QuestionState { //сначала задать вопрос про результаты веток
            val aggregationQuestion = AggregationQuestionState(node, helper)

            val possibleResults = BranchResult.entries.filter { it != BranchResult.NULL || node.canHaveNullResult() }
            //Затем спрашиваем про общий результат узла
            val nodeResultQuestion = object : CorrectnessCheckQuestionState<BranchResult>() {
                override fun text(situation: QuestioningSituation): String {
                    val descr = node.description(situation, BranchResult.CORRECT)
                    return situation.localization.IS_IT_TRUE_THAT(descr)
                }

                override fun options(situation: QuestioningSituation): List<SingleChoiceOption<Correctness<BranchResult>>> {
                    val correctAnswer = node.getAnswer(situation)
                    return possibleResults.map {
                        SingleChoiceOption(
                            when (it) {
                                BranchResult.CORRECT -> situation.localization.TRUE
                                BranchResult.ERROR -> situation.localization.FALSE
                                BranchResult.NULL -> situation.localization.NO_EFFECT
                            },
                            Explanation(
                                if (it != BranchResult.NULL) situation.localization.SIM_AGGREGATION_EXPLANATION(
                                    node.aggregationMethod,
                                    node.description(situation, BranchResult.CORRECT),
                                    helper.getBranchDescriptions(situation).joinToString(", "),
                                    it == BranchResult.CORRECT
                                )
                                else situation.localization.SIM_AGGREGATION_NULL_EXPLANATION(
                                    helper.getBranchDescriptions(situation).joinToString(", "),
                                ), type = ExplanationType.Error, shouldPause = false
                            ),
                            Correctness(it, it == correctAnswer),
                        )
                    }
                }
            }
            aggregationQuestion.linkNested(nodeResultQuestion)

            possibleResults.forEach { branchResult ->
                nodeResultQuestion.linkTo(nextStep(node, branchResult)) { situation, answer ->
                    node.getAnswer(situation) == branchResult
                }
            }
            return aggregationQuestion
        }

        private fun <Node : AggregationNode, BranchInfo> createMutexAggregationState(
            node: Node,
            helper: AggregationHelper<Node, BranchInfo>,
        ): QuestionState {
            data class BranchInfoWithResult(
                val branchInfo: BranchInfo,
                val assumedResult: BranchResult,
            )

            //сначала задать вопрос, какая из веток выполняется
            val question = object : CorrectnessCheckQuestionState<BranchInfoWithResult?>() {
                override fun text(situation: QuestioningSituation): String {
                    return situation.localization.WHICH_IS_TRUE_HERE
                }

                override fun options(situation: QuestioningSituation): List<SingleChoiceOption<Correctness<BranchInfoWithResult?>>> {
                    val branchInfos = helper.getBranchInfo(situation)
                    val results = branchInfos.associateWith { helper.getBranchResult(situation, it) }
                    val correctBranch = branchInfos.firstOrNull { results[it] != BranchResult.NULL }
                    val correctBranchExplanation = if (correctBranch != null) situation.localization.IN_THIS_SITUATION(
                        helper.getBranchDescription(situation, correctBranch, results[correctBranch]!!)
                    )
                    else situation.localization.NONE_OF_THE_ABOVE_APPLIES
                    val explanation = Explanation(
                        situation.localization.THATS_INCORRECT + " " + correctBranchExplanation
                    )
                    return branchInfos.flatMap { branchInfo ->
                            GetPossibleResults().process(helper.getThoughtBranch(branchInfo))
                                .filter { it != BranchResult.NULL }
                                .map { possibleBranchResult ->
                                    SingleChoiceOption(
                                        helper.getBranchDescription(situation, branchInfo, possibleBranchResult),
                                        explanation,
                                        Correctness<BranchInfoWithResult?>(
                                            BranchInfoWithResult(branchInfo, possibleBranchResult),
                                            branchInfo == correctBranch
                                        )
                                    )
                                }
                        }.plus(
                            SingleChoiceOption(
                                situation.localization.NONE_OF_THE_ABOVE_APPLIES,
                                explanation,
                                Correctness(null, correctBranch == null)
                            )
                        )
                }

                override fun additionalActions(
                    situation: QuestioningSituation,
                    chosenAnswer: Correctness<BranchInfoWithResult?>,
                ) {
                    if (chosenAnswer.answerInfo == null) return
                    helper.addAssumedResult(
                        chosenAnswer.answerInfo.branchInfo, situation, chosenAnswer.answerInfo.assumedResult
                    )
                    helper.onGoIntoBranch(situation, chosenAnswer.answerInfo.branchInfo)
                }
            }

            //Если выбрал неправильную ветку, то углубляемся в нее, чтобы объяснить, почему неправильно
            val thoughtBranches = helper.getThoughtBranches()
            val branchAutomata =
                thoughtBranches.associateWith { QuestioningStrategy.defaultFullBranchStrategy.build(it) }
            thoughtBranches.filter { branch -> branchAutomata[branch]!!.hasQuestions() }.forEach { branch ->
                question.linkTo(branchAutomata[branch]!!.initState) { situation, answer ->
                    !answer.isCorrect && answer.answerInfo?.branchInfo?.let { helper.getThoughtBranch(it) } == branch
                }
            }

            //После того, как вышли (или если не заходили), то направляемся к вопросам о переходах из узла
            val nextSteps = node.outcomes.keys.associateWith { outcomeValue -> nextStep(node, outcomeValue) }
            val shadowSkipState = object : SkipQuestionState() {
                override fun skip(situation: QuestioningSituation): QuestionStateChange {
                    return QuestionStateChange(null, nextSteps[node.getAnswer(situation)])
                }

                override val reachableStates: Collection<QuestionState>
                    get() = nextSteps.values

            }
            thoughtBranches.filter { branch -> !branchAutomata[branch]!!.hasQuestions() }.forEach { branch ->
                question.linkTo(shadowSkipState) { situation, answer ->
                    !answer.isCorrect && answer.answerInfo?.branchInfo?.let { helper.getThoughtBranch(it) } == branch
                }
            }
            question.linkTo(shadowSkipState) { situation, chosenAnswer ->
                chosenAnswer.isCorrect || chosenAnswer.answerInfo == null
            }
            branchAutomata.values.forEach { it.finalize(shadowSkipState) }

            return question
        }

        private fun <Node : AggregationNode, BranchInfo> createHypAggregationState(
            node: Node,
            helper: AggregationHelper<Node, BranchInfo>,
        ): QuestionState {
            return getNotImplementedState()
        }

        override fun process(node: BranchAggregationNode): QuestionState {
            val question = createAggregationState(node, BranchAggregationHelper(node))
            nodeStates[node] = question
            return question
        }

        override fun process(node: CycleAggregationNode): QuestionState { //Сначала попросить выбрать все подходящие переменные цикла
            val objectSelectQuestion = object : MultipleChoiceQuestionState<Obj>() {
                override fun text(situation: QuestioningSituation): String {
                    return node.question(situation)
                }

                override fun options(situation: QuestioningSituation): List<MultipleChoiceOption<Obj>> {
                    val searchResult = DecisionTreeReasoner(situation).searchWithErrors(node)
                    val options = searchResult.correct.map { obj ->
                            val objectName = obj.getLocalizedName(situation.domainModel, situation.localizationCode)
                            MultipleChoiceOption(
                                objectName,
                                obj,
                                true,
                                situation.localization.ALSO_FITS_THE_CRITERIA(objectName),
                            )
                        }.plus(searchResult.errors.flatMap { (error, objects) -> objects.map { error to it } }
                            .map { (error, obj) ->
                                val objectName = obj.getLocalizedName(situation.domainModel, situation.localizationCode)
                                MultipleChoiceOption(
                                    objectName, obj, false, error.explanation(situation, obj.objectName)
                                )
                            })
                    return options
                }
            }

            //далее переходим к вопросам о самой агрегации
            val aggregationQuestion = createAggregationState(node, CycleAggregationHelper(node))
            objectSelectQuestion.linkTo(aggregationQuestion)
            nodeStates[node] = objectSelectQuestion
            return objectSelectQuestion
        }

        override fun process(node: QuestionNode): QuestionState {
            if(currentBranch.isTrivial())
                return RedirectQuestionState()

            val question = object : CorrectnessCheckQuestionState<Any>() {
                override fun text(situation: QuestioningSituation): String {
                    return node.question(situation)
                }

                override fun options(situation: QuestioningSituation): List<SingleChoiceOption<Correctness<Any>>> {
                    val answer = node.getAnswer(situation)
                    val correctOutcome = node.outcomes[answer]!!
                    val explText = correctOutcome.explanation(situation)
                        ?:node.expr.generateExplanation(situation, correctOutcome.key)
                    return node.outcomes.map { SingleChoiceOption(
                        it.text(situation)
                            ?:node.expr.generateAnswer(situation, it.key)
                            ?:it.key.toLocalizedString(situation),
                        if(explText != null) Explanation(explText, type = ExplanationType.Error) else null,
                        Correctness(it.key, it == correctOutcome),
                    )}
                }

                override fun preliminarySkip(situation: QuestioningSituation): QuestionStateChange? {
                    val nextState = getStateFromLinks(situation, Correctness(true, true))
                    if(node.isSwitch)
                        return QuestionStateChange(null, nextState)
                    if(node.trivialityExpr?.use(OperatorReasoner.defaultReasoner(situation)) == true)
                        return QuestionStateChange(node.trivialityExplanation(situation)?.let { Explanation(it, shouldPause = false) }, nextState)
                    return super.preliminarySkip(situation)
                }
            }

            node.outcomes.keys.forEach { outcomeValue ->
                question.linkTo(nextStep(node, outcomeValue)) { situation, answer ->
                    node.getAnswer(situation) == outcomeValue
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

            for(part in node.parts.asReversed()){
                val newQuestion = object : CorrectnessCheckQuestionState<Any>() {
                    override fun text(situation: QuestioningSituation): String {
                        return part.question(situation)
                    }

                    override fun options(situation: QuestioningSituation): List<SingleChoiceOption<Correctness<Any>>> {
                        val answer = part.expr.use(OperatorReasoner.defaultReasoner(situation))
                        val correctOutcome = part.possibleOutcomes.first { it.value == answer }
                        val explText = correctOutcome.explanation(situation)
                        return part.possibleOutcomes.map { SingleChoiceOption(
                            it.text(situation) ?: it.value.toLocalizedString(situation),
                            explText?.let { Explanation(it, ExplanationType.Error) },
                            Correctness(it.value, it == correctOutcome),
                        )}
                    }
                }
                newQuestion.linkTo(question)
                question = newQuestion
            }

            nodeStates[node] = question
            return question
        }

        override fun process(branch: ThoughtBranch): QuestionState {
            branchDiving.push(branch)
            val nextState = branch.start.use(this)
            if (nextState is RedirectQuestionState || branch.isTrivial())
                return nextState

            val question = object : CorrectnessCheckQuestionState<DecisionTreeNode>() {
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

            question.linkTo(nextState)

            branchDiving.pop()
            preNodeStates[branch.start] = question
            return question
        }

        private fun ThoughtBranch.canHaveNullResult() : Boolean {
            return this.start.checkNullResult()
        }

        private fun DecisionTreeNode.checkNullResult(): Boolean {
            return this is BranchResultNode && this.value == BranchResult.NULL
                   || (this is AggregationNode && this.canHaveNullResult())
                   || (this is LinkNode<*> && this.children.any { it.checkNullResult() })
        }

        private fun AggregationNode.canHaveNullResult(): Boolean {
            //TODO нормальное определение возможных результатов
            return this.outcomes.keys.contains(BranchResult.NULL)
        }

        fun <AnswerType : Any> nextStep(node: LinkNode<AnswerType>, answer: AnswerType): QuestionState {
            val outcome = node.outcomes[answer]
            val nextNode = outcome?.node
            val currentBranch = currentBranch
            val branchHasNullResults = currentBranch.canHaveNullResult()

            val question = object : CorrectnessCheckQuestionState<DecisionTreeNode>() {
                override fun text(situation: QuestioningSituation): String {
                    return outcome?.nextStepQuestion(situation) ?: situation.localization.DEFAULT_NEXT_STEP_QUESTION
                }

                override fun options(situation: QuestioningSituation): List<SingleChoiceOption<Correctness<DecisionTreeNode>>> {
                    val explanation = outcome?.nextStepExplanation(situation)
                        ?.let{ explText -> Explanation(explText, type = ExplanationType.Error)}
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
                        .plus(BranchResult.entries
                            .filter { it != BranchResult.NULL || branchHasNullResults }
                            .map { result ->
                                SingleChoiceOption(
                                    outcome?.nextStepBranchResult(situation, result)
                                    ?: situation.localization.WE_CAN_CONCLUDE_THAT(
                                        currentBranch.description(
                                            situation,
                                            result
                                        )
                                    ),
                                    explanation,
                                    Correctness<DecisionTreeNode>(
                                        BranchResultNode(result, null),
                                        (nextNode is BranchResultNode && result == nextNode.value)
                                        || (nextNode == null && result == answer)
                                    ),
                                )
                            }
                        )
                    //WARN Дополнительные опции не работают с situation.addGivenAnswer() потому что BranchResultNode сравниваются по ссылке.
                    //Пока с этим ничего не делаем, однако в дальнейшем это может повлиять на что-то
                    return options
                }
            }

            question.linkTo(nextNode?.use(this) ?: RedirectQuestionState())

            nextNode?.let { preNodeStates[nextNode] = question }
            return question
        }
    }
}