package its.questions.gen

import its.model.DomainModel
import its.model.nodes.*
import its.questions.addAllNew
import its.questions.fileToMap
import its.questions.gen.visitors.GetConsideredNodes
import its.questions.gen.visitors.GetUsedVariables
import its.questions.inputs.EntityDictionary
import its.questions.inputs.QVarModel
import its.questions.inputs.usesQDictionaries
import its.questions.questiontypes.Question
import its.questions.questiontypes.SingleChoiceQuestion

class QuestionGenerator(dir : String) {
    internal val entityDictionary : EntityDictionary = EntityDictionary()
    internal val answers: Map<String, String>
    private val knownVariables = mutableSetOf<String>()

    val templating = TemplatingUtils(this)
    private fun String.process() : String {
        return templating.process(this)
    }

    init{
        require(DomainModel.usesQDictionaries())
        entityDictionary.fromCSV(dir + "entities.csv")
        answers = fileToMap(dir + "answers.txt", ':')
    }

    fun start(from : StartNode){
        knownVariables.addAll(from.initVariables.keys)
        process(from.main)
    }

    fun init(from : StartNode) {
        knownVariables.addAll(from.initVariables.keys)
    }

    fun process(branch: ThoughtBranch){
        val considered = branch.use(GetConsideredNodes(answers))

        if(!determineVariableValues(considered)) return

    }

    //region Вопросы о значениях переменных

    private fun determineVariableValues(consideredNodes: List<DecisionTreeNode>) : Boolean{
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
                decidingVariables.addAll(it.selectorExpr.accept(GetUsedVariables()))
            }
        }
        decidingVariables = decidingVariables.filter { !knownVariables.contains(it) }.toMutableSet()

        //Определить порядок, в котором переменные стоит рассматривать, с учетом зависимостей
        val order = mutableListOf<String>()
        decidingVariables.forEach {order.addAllNew(variableOrder(it, variablePrerequisites))}

        //задать вопросы
        order.forEach { varName ->
            val q = variableValueQuestion(varName, false)
            val answered = q.ask()
            if(answered == Question.AnswerStatus.CORRECT){
                knownVariables.add(varName)
            }
            else
                return false //TODO обработка нескольких ошибок подряд
        }

        return true
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

    private fun variableValueQuestion(varName : String, hasUncheckedPrerequisites : Boolean) : Question {
        val varData = (DomainModel.decisionTreeVarsDictionary.get(varName) as QVarModel)
        return SingleChoiceQuestion(
            hasUncheckedPrerequisites,
            varData.valueSearchTemplate!!.process(),
            entityDictionary
                //Выбрать объекты, которые еще не были присвоены (?) и класс которых подходит под класс искомой переменной
                .filter { !isEntityAssigned(it.alias) && (it.clazz.name == varData.className || it.calculatedClasses.any { clazz -> clazz.name == varData.className }) }
                .map { Question.AnswerOption(it.specificName, it.variable == varData, it.variableErrorExplanations[varData.name]?.process()) }
                .plus(Question.AnswerOption("Отсутствует", entityDictionary.none{it.variable == varData}, "Это неверно."))
        )
    }
    //endregion

}