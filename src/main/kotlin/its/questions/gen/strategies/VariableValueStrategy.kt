package its.questions.gen.strategies

import its.model.Utils.nullCheck
import its.model.definition.types.Obj
import its.model.nodes.*
import its.questions.gen.QuestioningSituation
import its.questions.gen.formulations.TemplatingUtils.explanation
import its.questions.gen.formulations.TemplatingUtils.getLocalizedName
import its.questions.gen.formulations.TemplatingUtils.question
import its.questions.gen.states.*
import its.questions.gen.visitors.getUsedVariables
import its.reasoner.nodes.DecisionTreeReasoner
import its.reasoner.nodes.DecisionTreeReasoner.Companion.getAnswer
import its.reasoner.nodes.DecisionTreeReasoner.Companion.solve

object VariableValueStrategy : QuestioningStrategy {
    private data class VariableInfo(
        val name : String,
        val declarationNode: FindActionNode,
        val prerequisites: List<String>,
        var isDeciding : Boolean = false,
    )

    private fun DecisionTreeNode.getVariableInfo(map: MutableMap<String, VariableInfo>) : Map<String, VariableInfo>{
        if(this is FindActionNode){
            val info = VariableInfo(
                varAssignment.variable.varName,
                this,
                varAssignment.valueExpr.getUsedVariables().toList()
            )
            map[info.name] = info
        }

        if(this is QuestionNode){
            expr.getUsedVariables().forEach { variable -> map[variable]?.isDeciding = true }
        }
        else if(this is FindActionNode && nextIfNone != null){
            map[varAssignment.variable.varName]?.isDeciding = true
        }

        if(this is LinkNode<*>){
            children.forEach { it.getVariableInfo(map) }
        }
        return map
    }

    override fun build(branch: ThoughtBranch) : QuestionAutomata {
        val infoMap = branch.start.getVariableInfo(mutableMapOf())
        val automata = create(infoMap.values.filter { it.isDeciding }.map{it.name}, infoMap, branch)
        return automata
    }

    @JvmStatic
    private fun create(variables: List<String>, infoMap: Map<String, VariableInfo>, currentBranch: ThoughtBranch): QuestionAutomata {
        var lastState : QuestionState = RedirectQuestionState()
        for(v in variables.reversed()){
            val varInfo = infoMap[v]!!
            val declarationNode = varInfo.declarationNode
            val nextState = lastState

            val question = object : CorrectnessCheckQuestionState<Obj?>() {
                override fun text(situation: QuestioningSituation): String {
                    return declarationNode.question(situation)
                }

                override fun options(situation: QuestioningSituation): List<SingleChoiceOption<Correctness<Obj?>>> {
                    val answer = declarationNode.getAnswer(situation)
                    val explanation = situation.localization.IN_THIS_SITUATION(
                        fact = declarationNode.outcomes[answer]!!.explanation(situation)!!
                    )

                    val possibleObjects = DecisionTreeReasoner(situation).processWithErrors(declarationNode)
                    val options = possibleObjects.errors.map { (error, objects) ->
                        objects.map{
                            SingleChoiceOption<Correctness<Obj?>>(
                                it.getLocalizedName(situation.domainModel, situation.localizationCode),
                                Explanation(
                                    "${error.explanation(situation, it.objectName)} $explanation",
                                    type = ExplanationType.Error
                                ),
                                Correctness(it, false)
                            )
                        }
                    }.flatten()
                        .plus(
                            if(possibleObjects.correct.isNotEmpty())
                                SingleChoiceOption<Correctness<Obj?>>(
                                    possibleObjects.correct.single().getLocalizedName(situation.domainModel, situation.localizationCode),
                                    null,
                                    Correctness(possibleObjects.correct.single(), true)
                                )
                            else
                                null
                        )
                        .filterNotNull()
                        .plus(
                            SingleChoiceOption<Correctness<Obj?>>(
                            if(declarationNode.nextIfNone != null)
                                declarationNode.nextIfNone!!.explanation(situation)
                                        .nullCheck("'none' outcome for Find Action Node $declarationNode has no ${situation.localizationCode} explanation.")
                                        .capitalize()
                            else
                                situation.localization.IMPOSSIBLE_TO_FIND
                            ,
                            Explanation("${situation.localization.THATS_INCORRECT} $explanation", type = ExplanationType.Error),
                                Correctness(null, possibleObjects.correct.isEmpty())
                        ))

                    return options
                }

                override fun additionalActions(situation: QuestioningSituation, chosenAnswer: Correctness<Obj?>) {
                    super.additionalActions(situation, chosenAnswer)
                    situation.discussedVariables[varInfo.name] = chosenAnswer.answerInfo?.objectName ?: ""
                }

                override fun preliminarySkip(situation: QuestioningSituation): QuestionStateChange? {
                    //TODO этот скип предполагался как повторный заход в это состояние - если несколько переменных зависят от текущей - но в этом случае скорее всего будут создаваться лишние состояния
                    // TODO
                    if(situation.discussedVariables.containsKey(varInfo.name)
                        || !currentBranch.solve(situation).containsWithNested(declarationNode)
                        || options(situation).isEmpty()){
                        return QuestionStateChange(null, nextState)
                    }
                    return super.preliminarySkip(situation)
                }
            }

            question.linkTo(nextState)

            val prerequisites = varInfo.prerequisites.filter { infoMap.containsKey(it) }
            val prerequisitesAutomata = if(!prerequisites.isEmpty()) create(prerequisites, infoMap, currentBranch) else null

            val prerequisitesInit =
                if(prerequisitesAutomata != null) {
                    prerequisitesAutomata.finalize(question)
                    prerequisitesAutomata.initState
                }
                else
                    question

            lastState = prerequisitesInit
        }

        return QuestionAutomata(lastState)
    }
}