package its.questions.gen.states

import its.model.DomainModel
import its.model.nodes.*
import its.questions.gen.TemplatingUtils
import its.questions.gen.TemplatingUtils._static.explanation
import its.questions.gen.TemplatingUtils._static.question
import its.questions.gen.TemplatingUtils._static.toCase
import its.questions.gen.visitors.ALIAS_ATR
import its.questions.gen.visitors.getUsedVariables
import its.questions.inputs.EntityInfo
import its.questions.inputs.QClassModel
import its.questions.inputs.QVarModel

class VariableQuestionAutomataCreation {


    companion object _static{
        private data class VariableInfo(
            val name : String,
            val declarationNode: FindActionNode,
            val prerequisites: List<String>,
            var isDeciding : Boolean = false,
        )

        private fun DecisionTreeNode.getVariableInfo(map: MutableMap<String, VariableInfo>) : Map<String, VariableInfo>{
            if(this is DecisionTreeVarDeclaration){
                require(this is FindActionNode){"Don't know how to collect variable info from ${javaClass.simpleName}"}

                val info = VariableInfo(declaredVariable().name, this, declarationExpression().getUsedVariables().toList())
                map[info.name] = info
            }

            if(this is QuestionNode){
                expr.getUsedVariables().forEach { variable -> map[variable]?.isDeciding = true }
            }
            else if(this is FindActionNode && nextIfNone != null){
                map[variable.name]?.isDeciding = true
            }

            if(this is LinkNode<*>){
                children.forEach { it.getVariableInfo(map) }
            }
            return map
        }

        @JvmStatic
        fun create(branch: ThoughtBranch) : QuestionAutomata{
            val infoMap = branch.start.getVariableInfo(mutableMapOf())
            val automata = create(infoMap.values.filter { it.isDeciding }.map{it.name}, infoMap)
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

        @JvmStatic
        private fun create(variables: List<String>, infoMap: Map<String, VariableInfo>): QuestionAutomata{
            var lastState : QuestionState = RedirectQuestionState()
            for(v in variables.reversed()){
                val varInfo = infoMap[v]!!

                val question = object : CorrectnessCheckQuestionState<EntityInfo?>(setOf(
                    QuestionStateLink({_, _ -> true }, lastState),
                ))
                {
                    override fun text(situation: ILearningSituation): String {
                        return varInfo.declarationNode.question(situation.templating)
                    }

                    override fun options(situation: ILearningSituation): List<SingleChoiceOption<Pair<EntityInfo?, Boolean>>> {
                        val varData = (DomainModel.decisionTreeVarsDictionary.get(varInfo.name) as QVarModel)
                        val clazz = DomainModel.classesDictionary.get(varData.className) as QClassModel
                        val correctEntity = situation.entityDictionary.getByVariable(varData.name)
                        val explanation = if(correctEntity != null)
                            "Правильный ответ в данном случае - ${correctEntity.specificName}."
                        else "В данном случае искомого ${clazz.textName.toCase(TemplatingUtils.Case.Gen)} нет."

                        val options = situation.entityDictionary
                            //Выбрать объекты, которые еще не были присвоены (?) и класс которых подходит под класс искомой переменной
                            .filter {entity -> !situation.knownVariables.containsValue(entity.alias) &&
                                    (entity.clazz.name == varData.className || entity.calculatedClasses.any { clazz -> clazz.name == varData.className }) &&
                                    (entity.variable == varData || varInfo.declarationNode.errorCategories.any{ varErr -> entity.errorCategories.contains(varErr.additionalInfo[ALIAS_ATR])})
                            }
                            .map {
                                val error = varInfo.declarationNode.errorCategories.firstOrNull { varErr -> it.errorCategories.contains(varErr.additionalInfo[ALIAS_ATR])}
                                SingleChoiceOption<Pair<EntityInfo?, Boolean>>(
                                it.specificName,
                                Explanation("${error?.explanation(situation.templating, it.alias)} $explanation"),
                                it to (it.variable == varData),
                                ) }
                            .plus(SingleChoiceOption<Pair<EntityInfo?, Boolean>>(
                                "Такой ${clazz.textName} отсутствует",
                                Explanation("Это неверно. $explanation"),
                                null to (situation.entityDictionary.none{it.variable == varData})
                            ))

                        return options
                    }

                    override fun additionalActions(situation: ILearningSituation, chosenAnswer: Pair<EntityInfo?, Boolean>) {
                        super.additionalActions(situation, chosenAnswer)
                        situation.knownVariables.put(varInfo.name, chosenAnswer.first?.alias ?: "")
                    }

                    override fun shouldBeSkipped(situation: ILearningSituation): QuestionStateChange? {
                        if(situation.knownVariables.containsKey(varInfo.name)){ //TODO убедиться что корректно работает в ситуациях, когда переменная не нужна вовсе. Если не работает - скипать
                            return QuestionStateChange(null, lastState)
                        }
                        return super.shouldBeSkipped(situation)
                    }

                }


                val prerequisites = varInfo.prerequisites.filter { infoMap.containsKey(it) }
                val prerequisitesAutomata = if(!prerequisites.isEmpty()) create(prerequisites, infoMap) else null

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
}