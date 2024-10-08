package its.questions.gen.formulations

import com.github.drapostolos.typeparser.ParserHelper
import com.github.drapostolos.typeparser.TypeParser
import com.github.jsonldjava.utils.Obj
import com.github.max_person.templating.InterpretationData
import com.github.max_person.templating.TemplatingSafeMethod
import its.model.nodes.*
import its.questions.gen.QuestioningSituation
import padeg.lib.Padeg

internal object TemplatingUtils {

    @JvmStatic
    internal val templatingParser  = TypeParser.newBuilder()
        .registerParser(Case::class.java) { s: String, h: ParserHelper -> Case.fromString(s) }
        .build()

    @JvmStatic
    fun String.toCase(case: Case?) : String{
        return Padeg.getAppointmentPadeg(this, (case?: Case.Nom).ordinal+1).replace(Regex("\\s+"), " ")
    }

    @JvmStatic
    fun String.capitalize() : String{
        return this.replaceFirstChar { it.uppercaseChar() }
    }


    @JvmStatic
    private fun String.cleanup() : String{
        return this.replace(Regex("\\s+"), " ")
    }

    @JvmStatic
    internal fun String.interpret(interpretationData: InterpretationData) : String{
        return interpretationData.interpret(this).cleanup()
    }

    //region Получение и шаблонизация текстовой информации

    private const val ALIAS_ATR = "alias"
    internal val DecisionTreeElement.alias get() = metadata[ALIAS_ATR]!! as String

    private fun Any?.stringCheck(message: String): String {
        if(this !is String) throw IllegalArgumentException(message)
        return this
    }
    
    private fun DecisionTreeElement.getMeta(localizationCode: String, metaName: String) : Any? {
        //вынесено для возможности переопределить получение шаблона для всех - для тестирования и т.п.
        return metadata[localizationCode, metaName]
    }

    //Узлы
    @JvmStatic
    internal fun DecisionTreeNode.asNextStep(localizationCode: String, interpretationData: InterpretationData) : String {
        return getMeta(localizationCode, "asNextStep")
            .stringCheck("Node '$this' doesn't have a $localizationCode associated 'as next step' description")
            .interpret(interpretationData)
    }

    @JvmStatic
    internal fun DecisionTreeNode.question(localizationCode: String, interpretationData: InterpretationData) : String {
        return getMeta(localizationCode, "question")
            .stringCheck("Node '$this' doesn't have a $localizationCode associated question")
            .interpret(interpretationData) //TODO Если такого нет - у агрегаций, например.
    }

    @JvmStatic
    internal fun TupleQuestionNode.TupleQuestionPart.question(
        localizationCode: String,
        interpretationData: InterpretationData
    ) : String {
        return getMeta(localizationCode, "question")
            .stringCheck("'$this' doesn't have a $localizationCode associated question")
            .interpret(interpretationData)
    }

    @JvmStatic
    internal fun DecisionTreeNode.endingCause(localizationCode: String, interpretationData: InterpretationData) : String {
        return getMeta(localizationCode, "endingCause")
            .stringCheck("Node '$this' doesn't have a $localizationCode ending cause")
            .interpret(interpretationData) //TODO Если такого нет - т.е. у не-конечных узлов
    }

    @JvmStatic
    internal fun AggregationNode.description(localizationCode: String, interpretationData: InterpretationData, result : Boolean) : String {
        return getMeta(localizationCode, "description")
            .stringCheck("Aggregation node '$this' doesn't have a $localizationCode description")
            .interpret(interpretationData.usingVar("result", result))
    }

    @JvmStatic
    internal fun QuestionNode.trivialityExplanation(localizationCode: String, interpretationData: InterpretationData) : String? {
        return getMeta(localizationCode, "triviality")?.let{it as String}?.interpret(interpretationData)
    }

    //Выходы (стрелки)
    @JvmStatic
    internal fun Outcome<*>.text(localizationCode: String, interpretationData: InterpretationData) : String? {
        return getMeta(localizationCode, "text")?.let{it as String}?.interpret(interpretationData)
    }

    @JvmStatic
    internal fun Outcome<*>.explanation(localizationCode: String, interpretationData: InterpretationData) : String? { //TODO? если это PredeterminingOutcome то использовать другую функцию
        return getMeta(localizationCode, "explanation")?.let{it as String}?.interpret(interpretationData)
    }

    @JvmStatic
    internal fun TupleQuestionNode.TupleQuestionOutcome.text(
        localizationCode: String,
        interpretationData: InterpretationData
    ) : String? {
        return getMeta(localizationCode, "text")?.let{it as String}?.interpret(interpretationData)
    }

    @JvmStatic
    internal fun TupleQuestionNode.TupleQuestionOutcome.explanation(
        localizationCode: String,
        interpretationData: InterpretationData
    ) : String? {
        return getMeta(localizationCode, "explanation")?.let{it as String}?.interpret(interpretationData)
    }

    @JvmStatic
    internal fun FindErrorCategory.explanation(localizationCode: String, interpretationData: InterpretationData, entityAlias : String) : String {
        return getMeta(localizationCode, "explanation")!!
            .stringCheck("FindErrorCategory '$this' doesn't have a $localizationCode explanation")
            .interpret(interpretationData.usingVar("checked", entityAlias))
    }

    @JvmStatic
    internal fun Outcome<ThoughtBranch?>.predeterminingExplanation(localizationCode: String, interpretationData: InterpretationData, result: Boolean) : String {
        return getMeta(localizationCode, "explanation")
            .stringCheck("Predetermining outcome leading to node '$node' doesn't have a $localizationCode explanation")
            .interpret(interpretationData.usingVar("result", result))
    }

    @JvmStatic
    internal fun Outcome<*>.nextStepQuestion(localizationCode: String, interpretationData: InterpretationData) : String? {
        return getMeta(localizationCode, "nextStepQuestion")
            ?.let{it as String}
            ?.interpret(interpretationData)
    }

    @JvmStatic
    internal fun Outcome<*>.nextStepBranchResult(localizationCode: String, interpretationData: InterpretationData, branchResult : Boolean) : String? {
        return getMeta(localizationCode, "nextStepBranchResult")
            ?.let{it as String}
            ?.interpret(interpretationData.usingVar("branchResult", branchResult))
    }

    @JvmStatic
    internal fun Outcome<*>.nextStepExplanation(localizationCode: String, interpretationData: InterpretationData) : String? {
        return getMeta(localizationCode, "nextStepExplanation")
            ?.let{it as String}
            ?.interpret(interpretationData)
    }

    //Ветки
    @JvmStatic
    internal fun ThoughtBranch.description(localizationCode: String, interpretationData: InterpretationData, result : Boolean) : String {
        return getMeta(localizationCode, "description")
            .stringCheck("Branch '$this' doesn't have a $localizationCode description")
            .interpret(interpretationData.usingVar("result", result))
    }

    @JvmStatic
    internal fun ThoughtBranch.nextStepQuestion(localizationCode: String, interpretationData: InterpretationData) : String? {
        return getMeta(localizationCode, "nextStepQuestion")
            ?.let{it as String}
            ?.interpret(interpretationData)
    }

    @JvmStatic
    internal fun ThoughtBranch.nextStepBranchResult(localizationCode: String, interpretationData: InterpretationData, branchResult : Boolean) : String? {
        return getMeta(localizationCode, "nextStepBranchResult")
            ?.let{it as String}
            ?.interpret(interpretationData.usingVar("branchResult", branchResult))
    }

    @JvmStatic
    internal fun ThoughtBranch.nextStepExplanation(localizationCode: String, interpretationData: InterpretationData) : String {
        return getMeta(localizationCode, "nextStepExplanation")
            .stringCheck("Branch '$this' doesn't have a $localizationCode next step explanation")
            .interpret(interpretationData)
    }
}


enum class Case{
    Nom, //именительный (кто? что?)
    Gen, //родительный (кого? чего?)
    Dat, //дательный (кому? чему?)
    Acc, //винительный (кого? что?)
    Ins, //творительный (кем? чем?)
    Pre, //предложный (о ком? о чем?)
    ;

    companion object {
        @JvmStatic
        fun fromString(str: String) : Case? {
            return when(str.lowercase()){
                "и.п.", "им.п.", "и", "им", "и.", "им.", "n", "nom", "nom." -> Nom
                "р.п.", "род.п.", "р", "род", "р.", "род.", "g", "gen", "gen." -> Gen
                "д.п.", "дат.п.", "д", "дат", "д.", "дат.", "d", "dat", "dat." -> Dat
                "в.п.", "вин.п.", "в", "вин", "в.", "вин.", "a", "acc", "acc." -> Acc
                "т.п.", "тв.п.", "т", "тв", "т.", "тв.", "i", "ins", "ins." -> Ins
                "п.п.", "пр.п.", "п", "пр", "п.", "пр.", "p", "pre", "pre." -> Pre
                else -> null
            }
        }
    }
}