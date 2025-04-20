package its.questions.gen.formulations

import com.github.max_person.templating.InterpretationData
import com.github.max_person.templating.Template
import com.github.max_person.templating.TemplatingModifier
import its.model.definition.*
import its.model.definition.build.DomainBuilderUtils.newClass
import its.model.definition.build.DomainBuilderUtils.newObject
import its.model.definition.build.DomainBuilderUtils.setEnumProperty
import its.model.definition.types.EnumType
import its.model.definition.types.Obj
import its.model.nodes.*
import its.questions.gen.QuestioningSituation
import its.reasoner.LearningSituation
import padeg.lib.Padeg

object TemplatingUtils {

    @JvmStatic
    fun DomainDefWithMeta<*>.getLocalizedName(localizationCode : String) : String {
        return  this.metadata[localizationCode, "localizedName"].toString()
    }

    @JvmStatic
    fun <T : DomainDefWithMeta<T>> DomainRef<T>.getLocalizedName(domainModel : DomainModel, localizationCode : String) : String {
        return this.findInOrUnkown(domainModel).getLocalizedName(localizationCode)
    }

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

    private object TemplateModifier{
        @TemplatingModifier
        fun String.case(case : String) : String {
            return this.toCase(Case.fromString(case))
        }
    }

    private fun addParamsObj(model: DomainModel, branchResult: BranchResult) : Obj {
        val enumName = "BranchResult"
        val enumDef = model.enums.added(EnumDef(enumName))
        BranchResult.values().forEach { br ->  enumDef.values.add(EnumValueDef(enumName, br.name))}

        val classDef = model.newClass("TemplateParams")
        classDef.declaredProperties.add(PropertyDef("TemplateParams", "branchResult",
            EnumType("BranchResult"), PropertyDef.PropertyKind.OBJECT, ParamsDecl()))

        val obj = model.newObject("params", "TemplateParams")
        obj.setEnumProperty("branchResult", "BranchResult", branchResult.name)
        return obj.reference
    }

    private fun deleteParamsObj(model: DomainModel) {
        model.enums.remove("BranchResult")
        model.objects.remove("params")
        model.classes.remove("TemplateParams")
    }

    @JvmStatic
    fun String.interpret(
        learningSituation: LearningSituation,
        localizationCode: String,
        contextVars : Map<String, Obj> = emptyMap()
    ) : String{
        val parse = Template(this, OperatorTemplateParser)
        return parse.interpret(
            InterpretationData()
                .withModifierObj(TemplateModifier)
                .withVar(OperatorTemplateParser.LEARNING_SITUATION, learningSituation)
                .withVar(OperatorTemplateParser.LOCALIZATION_CODE, localizationCode)
                .withVar(OperatorTemplateParser.CONTEXT_VARS, contextVars)
        ).cleanup()
    }

    //region Получение и шаблонизация текстовой информации

    private const val ALIAS_ATR = "alias"
    internal val DecisionTreeElement.alias get() = metadata[ALIAS_ATR]!! as String

    private fun Any?.stringCheck(message: String): String {
        if (this !is String) throw IllegalArgumentException(message)
        return this
    }
    
    private fun DecisionTreeElement.getMeta(localizationCode: String, metaName: String) : Any? {
        //вынесено для возможности переопределить получение шаблона для всех - для тестирования и т.п.
        return metadata[localizationCode, metaName]
    }

    private fun DecisionTreeElement.getAndInterpretWithBranchResult(
        metaName: String,
        situation: QuestioningSituation,
        branchResult: BranchResult,
    ): String? {
        val localizationCode = situation.localizationCode
        val template = getMeta(localizationCode, metaName) ?: getMeta(
            localizationCode,
            metaName + "_" + branchResult.toString().lowercase()
        )
        return template?.toString()?.let {
            val paramsObj = addParamsObj(situation.domainModel, branchResult)
            val result = it.interpret(situation, localizationCode, mapOf("params" to paramsObj))
            deleteParamsObj(situation.domainModel)
            result
        }
    }

    //Узлы
    @JvmStatic
    internal fun DecisionTreeNode.asNextStep(situation: QuestioningSituation) : String {
        val localizationCode = situation.localizationCode
        return getMeta(localizationCode, "asNextStep")
            .stringCheck("Node '$this' doesn't have a $localizationCode associated 'as next step' description")
            .interpret(situation, localizationCode)
    }

    @JvmStatic
    internal fun DecisionTreeNode.question(situation: QuestioningSituation) : String {
        val localizationCode = situation.localizationCode
        return getMeta(localizationCode, "question")
            .stringCheck("Node '$this' doesn't have a $localizationCode associated question")
            .interpret(situation, localizationCode)
    }

    @JvmStatic
    internal fun TupleQuestionNode.TupleQuestionPart.question(situation: QuestioningSituation) : String {
        val localizationCode = situation.localizationCode
        return getMeta(localizationCode, "question")
            .stringCheck("'$this' doesn't have a $localizationCode associated question")
            .interpret(situation, localizationCode)
    }

    @JvmStatic
    internal fun DecisionTreeNode.endingCause(situation: QuestioningSituation) : String {
        val localizationCode = situation.localizationCode
        return getMeta(localizationCode, "endingCause")
            .stringCheck("Node '$this' doesn't have a $localizationCode ending cause")
            .interpret(situation, localizationCode) //TODO Если такого нет - т.е. у не-конечных узлов
    }

    @JvmStatic
    internal fun AggregationNode.description(situation: QuestioningSituation, result : BranchResult) : String {
        val localizationCode = situation.localizationCode
        return getAndInterpretWithBranchResult("description", situation, result)
            .stringCheck("Aggregation node '$this' doesn't have a $localizationCode description")
    }

    @JvmStatic
    internal fun AggregationNode.nullFormulation(situation: QuestioningSituation): String {
        val localizationCode = situation.localizationCode
        return getMeta(localizationCode, "nullFormulation").let { it as? String }
                   ?.interpret(situation, localizationCode) ?: situation.localization.NO_EFFECT
    }

    @JvmStatic
    internal fun QuestionNode.trivialityExplanation(situation: QuestioningSituation) : String? {
        val localizationCode = situation.localizationCode
        return getMeta(localizationCode, "triviality")?.let{it as String}?.interpret(situation, localizationCode)
    }

    //Выходы (стрелки)
    @JvmStatic
    internal fun Outcome<*>.text(situation: QuestioningSituation) : String? {
        val localizationCode = situation.localizationCode
        return getMeta(localizationCode, "text")?.let{it as String}?.interpret(situation, localizationCode)
    }

    @JvmStatic
    internal fun Outcome<*>.explanation(situation: QuestioningSituation) : String? {
        val localizationCode = situation.localizationCode
        return getMeta(localizationCode, "explanation")?.let{it as String}?.interpret(situation, localizationCode)
    }

    @JvmStatic
    internal fun TupleQuestionNode.TupleQuestionOutcome.text(situation: QuestioningSituation) : String? {
        val localizationCode = situation.localizationCode
        return getMeta(localizationCode, "text")?.let{it as String}?.interpret(situation, localizationCode)
    }

    @JvmStatic
    internal fun TupleQuestionNode.TupleQuestionOutcome.explanation(situation: QuestioningSituation) : String? {
        val localizationCode = situation.localizationCode
        return getMeta(localizationCode, "explanation")?.let{it as String}?.interpret(situation, localizationCode)
    }

    @JvmStatic
    internal fun FindErrorCategory.explanation(situation: QuestioningSituation, entityAlias : String) : String {
        val localizationCode = situation.localizationCode
        return getMeta(localizationCode, "explanation")!!
            .stringCheck("FindErrorCategory '$this' doesn't have a $localizationCode explanation")
            .interpret(situation, localizationCode, mapOf("checked" to Obj(entityAlias)))
    }

    @JvmStatic
    internal fun Outcome<*>.nextStepQuestion(situation: QuestioningSituation) : String? {
        val localizationCode = situation.localizationCode
        return getMeta(localizationCode, "nextStepQuestion")
            ?.let{it as String}
            ?.interpret(situation, localizationCode)
    }

    @JvmStatic
    internal fun Outcome<*>.nextStepBranchResult(situation: QuestioningSituation, branchResult : BranchResult) : String? {
        val localizationCode = situation.localizationCode
        return getAndInterpretWithBranchResult("nextStepBranchResult", situation, branchResult)
    }

    @JvmStatic
    internal fun Outcome<*>.nextStepExplanation(situation: QuestioningSituation) : String? {
        val localizationCode = situation.localizationCode
        return getMeta(localizationCode, "nextStepExplanation")
            ?.let{it as String}
            ?.interpret(situation, localizationCode)
    }

    //Ветки
    @JvmStatic
    internal fun ThoughtBranch.description(situation: QuestioningSituation, result : BranchResult) : String {
        val localizationCode = situation.localizationCode
        return getAndInterpretWithBranchResult("description", situation, result)
            .stringCheck("Branch '$this' doesn't have a $localizationCode description")
    }

    @JvmStatic
    internal fun ThoughtBranch.nextStepQuestion(situation: QuestioningSituation) : String? {
        val localizationCode = situation.localizationCode
        return getMeta(localizationCode, "nextStepQuestion")
            ?.let{it as String}
            ?.interpret(situation, localizationCode)
    }

    @JvmStatic
    internal fun ThoughtBranch.nextStepBranchResult(situation: QuestioningSituation, branchResult : BranchResult) : String? {
        return getAndInterpretWithBranchResult("nextStepBranchResult", situation, branchResult)
    }

    @JvmStatic
    internal fun ThoughtBranch.nextStepExplanation(situation: QuestioningSituation) : String {
        val localizationCode = situation.localizationCode
        return getMeta(localizationCode, "nextStepExplanation")
            .stringCheck("Branch '$this' doesn't have a $localizationCode next step explanation")
            .interpret(situation ,localizationCode)
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