package its.questions.gen

import its.model.DomainModel
import its.model.nodes.*
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
    private val knownVariables = mutableMapOf<String, String>()

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
        knownVariables.putAll(from.initVariables.map { (name, type) -> name to entityDictionary.get { it.variable?.name == name }!!.alias })
        process(from.main)
    }

    fun init(from : StartNode) {
        knownVariables.putAll(from.initVariables.map { (name, type) -> name to entityDictionary.get { it.variable?.name == name }!!.alias })
    }

    fun process(branch: ThoughtBranch){
        val considered = branch.use(GetConsideredNodes(answers))

        val variablePrerequisites = mutableMapOf<String, List<String>>()
        considered.forEach {
            if(it is DecisionTreeVarDeclaration){
                variablePrerequisites[it.declaredVariable().name] = it.declarationExpression().accept(GetUsedVariables())
            }
        }

        var decidingVariables = mutableSetOf<String>()
        considered.forEach {
            if(it is QuestionNode){
                decidingVariables.addAll(it.expr.accept(GetUsedVariables()))
            }
            else if(it is FindActionNode && it.nextIfNone != null){
                decidingVariables.addAll(it.selectorExpr.accept(GetUsedVariables()))
            }
        }
        decidingVariables = decidingVariables.filter { !knownVariables.containsKey(it) }.toMutableSet()

        decidingVariables.forEach {variable ->
            val varData = (DomainModel.decisionTreeVarsDictionary.get(variable) as QVarModel)
            val q = SingleChoiceQuestion(
                false,
                varData.valueSearchTemplate!!.process(),
                entityDictionary.filter { !knownVariables.containsValue(it.alias) &&
                        (it.clazz.name == varData.className || it.calculatedClasses.any { clazz -> clazz.name == varData.className }) }
                    .map { Question.AnswerOption(it.specificName, it.variable == varData, it.variableErrorExplanations[varData.name]?.process()) }
            )
            if(q.ask() == Question.AnswerStatus.INCORRECT_EXPLAINED)
                return
        }
    }

}