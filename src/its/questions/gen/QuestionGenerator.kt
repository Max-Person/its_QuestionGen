package its.questions.gen

import its.model.DomainModel
import its.model.nodes.*
import its.questions.addAllNew
import its.questions.fileToMap
import its.questions.gen.TemplatingUtils._static.replaceAlternatives
import its.questions.gen.visitors.*
import its.questions.gen.visitors.GetNodesLCA._static.getNodesLCA
import its.questions.inputs.EntityDictionary
import its.questions.inputs.QClassModel
import its.questions.inputs.QVarModel
import its.questions.inputs.usesQDictionaries
import its.questions.questiontypes.AnswerOption
import its.questions.questiontypes.AnswerStatus
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

    fun process(branch: ThoughtBranch, assumedResult : Boolean) : AnswerStatus{
        val considered = branch.use(GetConsideredNodes(answers))

        if(determineVariableValues(considered) == AnswerStatus.INCORRECT_EXPLAINED){
            println("Итак, мы обсудили, почему " + branch.additionalInfo["description"]!!.replaceAlternatives(!assumedResult).process())
            return AnswerStatus.INCORRECT_EXPLAINED
        }

        val endingSearch = GetEndingNodes(considered)
        branch.use(endingSearch)
        val endingNodes = endingSearch.set
        val correctEndingNode = endingSearch.correct
        val q = SingleChoiceQuestion(
            false,
            "Почему вы считаете, что " + branch.additionalInfo["description"]!!.replaceAlternatives(assumedResult).process() + "?",
            endingNodes
                .map{ AnswerOption((it.additionalInfo["endingCause"]?:"").replaceAlternatives(assumedResult).process(),it == correctEndingNode, "", it) }
        )

        val answer = q.askWithInfo()
        var askingNode : DecisionTreeNode? = if(answer.first == AnswerStatus.CORRECT) correctEndingNode else branch.getNodesLCA(correctEndingNode, answer.second as DecisionTreeNode)!!
        lateinit var status : AnswerStatus
        do{
            status = askingNode!!.use(AskNodeQuestions(this))
            if(status != AnswerStatus.INCORRECT_EXPLAINED){
                val nextStep = askingNode.use(AskNextStepQuestions(this, branch))
                status = nextStep.first
                askingNode = nextStep.second
            }

        }
        while (status != AnswerStatus.INCORRECT_EXPLAINED && askingNode != null)

        println("Итак, мы обсудили, почему " + branch.additionalInfo["description"]!!.replaceAlternatives(!assumedResult).process())
        return if(askingNode == null) AnswerStatus.CORRECT else AnswerStatus.INCORRECT_EXPLAINED
    }

    //region Вопросы о значениях переменных

    private fun determineVariableValues(consideredNodes: List<DecisionTreeNode>) : AnswerStatus{
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

        var answered = AnswerStatus.CORRECT
        //задать вопросы
        order.forEach { varName ->
            val q = variableValueQuestion(varName, false)
            answered = q.ask()
            knownVariables.add(varName)
            if(answered == AnswerStatus.INCORRECT_EXPLAINED)
                return answered
        }

        return answered
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

    private fun variableValueQuestion(varName : String, hasUncheckedPrerequisites : Boolean) : SingleChoiceQuestion {
        val varData = (DomainModel.decisionTreeVarsDictionary.get(varName) as QVarModel)
        val clazz = DomainModel.classesDictionary.get(varData.className) as QClassModel
        return SingleChoiceQuestion(
            !hasUncheckedPrerequisites,
            varData.valueSearchTemplate!!.process(),
            entityDictionary
                //Выбрать объекты, которые еще не были присвоены (?) и класс которых подходит под класс искомой переменной
                .filter { !isEntityAssigned(it.alias) && (it.clazz.name == varData.className || it.calculatedClasses.any { clazz -> clazz.name == varData.className }) }
                .map { AnswerOption(it.specificName, it.variable == varData, it.variableErrorExplanations[varData.name]?.process()) }
                .plus(AnswerOption("Такой ${clazz.textName} отсутствует", entityDictionary.none{it.variable == varData}, ""))
        )
    }
    //endregion

}