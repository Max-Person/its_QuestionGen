package its.questions.gen

import its.model.DomainModel
import its.model.nodes.*
import its.questions.addAllNew
import its.questions.fileToMap
import its.questions.gen.TemplatingUtils._static.replaceAlternatives
import its.questions.gen.visitors.*
import its.questions.gen.visitors.GetNodesLCA._static.getNodesLCA
import its.questions.gen.visitors.GetNodesLCA._static.getNodesPreLCA
import its.questions.inputs.EntityDictionary
import its.questions.inputs.QClassModel
import its.questions.inputs.QVarModel
import its.questions.inputs.usesQDictionaries
import its.questions.questiontypes.AnswerOption
import its.questions.questiontypes.Prompt
import its.questions.questiontypes.SingleChoiceQuestion

class QuestionGenerator(dir : String) {
    internal val entityDictionary : EntityDictionary = EntityDictionary()
    internal val answers: Map<String, String>
    internal val knownVariables = mutableSetOf<String>()

    val templating = TemplatingUtils(this)
    private fun String.process() : String {
        return templating.process(this)
    }

    init{
        require(DomainModel.usesQDictionaries())
        entityDictionary.fromCSV(dir + "entities.csv")
        answers = fileToMap(dir + "answers.txt", ':')
    }

    fun start(from : StartNode, assumedResult : Boolean){
        knownVariables.addAll(from.initVariables.keys)
        process(from.main, assumedResult)
    }

    fun init(from : StartNode) {
        knownVariables.addAll(from.initVariables.keys)
    }

    fun process(branch: ThoughtBranch, assumedResult : Boolean){
        val considered = branch.use(GetConsideredNodes(answers))

        if(determineVariableValues(branch, considered) == true){
            println("\nИтак, мы обсудили, почему " + branch.additionalInfo["description"]!!.replaceAlternatives(!assumedResult).process())
            return
        }

        val endingSearch = GetEndingNodes(considered)
        branch.use(endingSearch)
        val endingNodes = endingSearch.set
        val correctEndingNode = endingSearch.correct
        val q = SingleChoiceQuestion(
            "Почему вы считаете, что " + branch.additionalInfo["description"]!!.replaceAlternatives(assumedResult).process() + "?",
            endingNodes
                .map{ AnswerOption((it.additionalInfo["endingCause"]?:"").replaceAlternatives(assumedResult).process(),it == correctEndingNode, "Это неверно. Давайте разберемся.", it) }
        )

        val endingNodeAnswer = q.askWithInfo()
        var askingNode : DecisionTreeNode? = if(endingNodeAnswer.first) correctEndingNode else branch.getNodesLCA(correctEndingNode, endingNodeAnswer.second as DecisionTreeNode)!!
        if(askingNode != endingNodeAnswer.second){
            val preLCA = branch.getNodesPreLCA(correctEndingNode, endingNodeAnswer.second as DecisionTreeNode)!!
            val stepAnswer = preLCA.use(AskNextStepQuestions(this, branch)).first

            if(!stepAnswer && shouldEndBranch(branch)){
                println("\nИтак, мы обсудили, почему " + branch.additionalInfo["description"]!!.replaceAlternatives(!assumedResult).process())
                return
            }
        }
        var stepAnswer : Boolean
        do{
            stepAnswer = askingNode!!.use(AskNodeQuestions(this))
            if(!stepAnswer && shouldEndBranch(branch))
                break

            val nextStep = askingNode.use(AskNextStepQuestions(this, branch))
            stepAnswer = nextStep.first
            askingNode = nextStep.second

            if(!stepAnswer && askingNode !is BranchResultNode && shouldEndBranch(branch))
                break

        }
        while (stepAnswer && askingNode != null)

        println("\nИтак, мы обсудили, почему " + branch.additionalInfo["description"]!!.replaceAlternatives(!assumedResult).process())
        return
    }

    private fun shouldEndBranch(currentBranch : ThoughtBranch) : Boolean{
        val branchAnswer = answers[currentBranch.additionalInfo[ALIAS_ATR]].toBoolean()
        return Prompt(
            "Понятно ли вам, почему " + currentBranch.additionalInfo["description"]!!.replaceAlternatives(branchAnswer).process() + "?",
            listOf("Да" to true, "Нет, продолжить рассматривать дальше" to false)
        ).ask()
    }

    //region Вопросы о значениях переменных

    private fun determineVariableValues(currentBranch: ThoughtBranch, consideredNodes: List<DecisionTreeNode>) : Boolean{
        //Определить список переменных и от каких других переменных они зависят
        val variablePrerequisites = mutableMapOf<String, Set<String>>()
        consideredNodes.forEach {
            if(it is DecisionTreeVarDeclaration){
                variablePrerequisites[it.declaredVariable().name] = it.declarationExpression().accept(GetUsedVariables())
            }
        }

        //Определить список решающих переменных
        var decidingVariables = mutableSetOf<String>()
        consideredNodes.forEach {
            if(it is QuestionNode){
                decidingVariables.addAll(it.expr.accept(GetUsedVariables()))
            }
            else if(it is FindActionNode && it.nextIfNone != null){
                decidingVariables.add(it.variable.name)
            }
        }
        decidingVariables = decidingVariables.filter { !knownVariables.contains(it) }.toMutableSet()

        //Определить порядок, в котором переменные стоит рассматривать, с учетом зависимостей
        val order = mutableListOf<String>()
        decidingVariables.forEach {order.addAllNew(variableOrder(it, variablePrerequisites))}

        //задать вопросы
        order.forEach { varName ->
            val q = variableValueQuestion(varName)
            val answer = q.ask()
            knownVariables.add(varName)
            if(!answer && shouldEndBranch(currentBranch))
                return true
        }

        return false
    }

    private fun variableOrder(varName: String, variablePrerequisites : Map<String, Set<String>>) : List<String>{
        val list = mutableListOf<String>()
        variablePrerequisites[varName]?.forEach { if(!knownVariables.contains(it)) list.addAllNew(variableOrder(it, variablePrerequisites)) }
        list.add(varName)
        return list
    }

    private fun isEntityAssigned(entityAlias : String) : Boolean {
        return knownVariables.any { entityDictionary.getByVariable(it)?.alias == entityAlias }
    }

    private fun variableValueQuestion(varName : String) : SingleChoiceQuestion {
        val varData = (DomainModel.decisionTreeVarsDictionary.get(varName) as QVarModel)
        val clazz = DomainModel.classesDictionary.get(varData.className) as QClassModel
        return SingleChoiceQuestion(
            varData.valueSearchTemplate!!.process(),
            entityDictionary
                //Выбрать объекты, которые еще не были присвоены (?) и класс которых подходит под класс искомой переменной
                .filter { !isEntityAssigned(it.alias) &&
                        (it.clazz.name == varData.className || it.calculatedClasses.any { clazz -> clazz.name == varData.className }) &&
                        (it.variable == varData || it.variableErrorExplanations.containsKey(varName))
                }
                .map { AnswerOption(it.specificName, it.variable == varData, it.variableErrorExplanations[varData.name]?.process() + " Правильный ответ в данном случае - ${entityDictionary.getByVariable(varName)!!.specificName}.") }
                .plus(AnswerOption("Такой ${clazz.textName} отсутствует", entityDictionary.none{it.variable == varData}, "Правильный ответ в данном случае - ${entityDictionary.getByVariable(varName)!!.specificName}."))
        )
    }
    //endregion

}