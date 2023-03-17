package its.questions.gen

import com.github.max_person.templating.InterpretationData
import its.model.DomainModel
import its.model.nodes.*
import its.questions.addAllNew
import its.questions.fileToMap
import its.questions.gen.TemplatingUtils._static.description
import its.questions.gen.TemplatingUtils._static.endingCause
import its.questions.gen.TemplatingUtils._static.interpret
import its.questions.gen.TemplatingUtils._static.toCase
import its.questions.gen.visitors.ALIAS_ATR
import its.questions.gen.visitors.AskNextStepQuestions._static.askNextStepQuestions
import its.questions.gen.visitors.AskNodeQuestions._static.askNodeQuestions
import its.questions.gen.visitors.GetConsideredNodes._static.getConsideredNodes
import its.questions.gen.visitors.GetEndingNodes._static.getAllEndingNodes
import its.questions.gen.visitors.GetEndingNodes._static.getCorrectEndingNode
import its.questions.gen.visitors.GetNodesLCA._static.getNodesLCA
import its.questions.gen.visitors.GetNodesLCA._static.getNodesPreLCA
import its.questions.gen.visitors.getUsedVariables
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

    private val templatingUtils = TemplatingUtils(this)
    val templating = InterpretationData().withGlobalObj(templatingUtils).withParser(TemplatingUtils.templatingParser)

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
        val considered = branch.getConsideredNodes(answers)

        if(determineVariableValues(branch, considered) == true){
            println("\nИтак, мы обсудили, почему " + branch.description(templating, !assumedResult))
            return
        }

        val endingNodes = branch.getAllEndingNodes(considered, answers)
        val correctEndingNode = branch.getCorrectEndingNode(considered, answers)
        val q = SingleChoiceQuestion(
            "Почему вы считаете, что " + branch.description(templating, assumedResult) + "?",
            endingNodes
                .map{ AnswerOption(it.endingCause(templating),it == correctEndingNode, "Давайте разберемся.", it) },
            true,
        )

        val endingNodeAnswer = q.askWithInfo()
        var askingNode : DecisionTreeNode? = if(endingNodeAnswer.first) correctEndingNode else branch.getNodesLCA(correctEndingNode, endingNodeAnswer.second as DecisionTreeNode)!!
        if(askingNode != endingNodeAnswer.second){
            val preLCA = branch.getNodesPreLCA(correctEndingNode, endingNodeAnswer.second as DecisionTreeNode)!!
            val stepAnswer = preLCA.askNextStepQuestions(this, branch).first

            if(!stepAnswer && shouldEndBranch(branch)){
                println("\nИтак, мы обсудили, почему " + branch.description(templating, !assumedResult))
                return
            }
        }
        var stepAnswer : Boolean
        do{
            stepAnswer = askingNode!!.askNodeQuestions(this)
            if(askingNode is LogicAggregationNode || !stepAnswer && shouldEndBranch(branch)) //FIXME сделано для демонстрации
                break

            val nextStep = askingNode.askNextStepQuestions(this, branch)
            stepAnswer = nextStep.first
            askingNode = nextStep.second

            if(!stepAnswer && askingNode !is BranchResultNode && shouldEndBranch(branch))
                break

        }
        while (askingNode != null)

        println("\nИтак, мы обсудили, почему " + branch.description(templating, !assumedResult))
        return
    }

    private fun shouldEndBranch(currentBranch : ThoughtBranch) : Boolean{
        val branchAnswer = answers[currentBranch.additionalInfo[ALIAS_ATR]].toBoolean()
        return Prompt(
            "Понятно ли вам, почему " + currentBranch.description(templating, branchAnswer) + "?",
            listOf("Да" to true, "Нет, продолжить рассматривать дальше" to false)
        ).ask()
    }

    //region Вопросы о значениях переменных

    private fun determineVariableValues(currentBranch: ThoughtBranch, consideredNodes: List<DecisionTreeNode>) : Boolean{
        //Определить список переменных и от каких других переменных они зависят
        val variablePrerequisites = mutableMapOf<String, Set<String>>()
        consideredNodes.forEach {
            if(it is DecisionTreeVarDeclaration){
                variablePrerequisites[it.declaredVariable().name] = it.declarationExpression().getUsedVariables()
            }
        }

        //Определить список решающих переменных
        var decidingVariables = mutableSetOf<String>()
        consideredNodes.forEach {
            if(it is QuestionNode){
                decidingVariables.addAll(it.expr.getUsedVariables())
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
            if(entityDictionary.getByVariable(varName) != null)
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
        val correctEntity = entityDictionary.getByVariable(varName)
        val explanation = if(correctEntity != null)
            "Правильный ответ в данном случае - ${correctEntity.specificName}."
        else "В данном случае искомого ${clazz.textName.toCase(TemplatingUtils.Case.Gen)} нет."
        return SingleChoiceQuestion(
            varData.valueSearchTemplate!!.interpret(templating), //TODO переделать это не через словарь а через узел
            entityDictionary
                //Выбрать объекты, которые еще не были присвоены (?) и класс которых подходит под класс искомой переменной
                .filter { !isEntityAssigned(it.alias) &&
                        (it.clazz.name == varData.className || it.calculatedClasses.any { clazz -> clazz.name == varData.className }) &&
                        (it.variable == varData || it.variableErrorExplanations.containsKey(varName))
                }
                .map { AnswerOption(it.specificName, it.variable == varData, it.variableErrorExplanations[varData.name]?.interpret(templating) + " $explanation") }
                .plus(AnswerOption("Такой ${clazz.textName} отсутствует", entityDictionary.none{it.variable == varData}, "Это неверно. $explanation"))
        )
    }
    //endregion

}