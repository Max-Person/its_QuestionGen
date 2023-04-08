package its.questions.inputs

import com.github.drapostolos.typeparser.ParserHelper
import com.github.drapostolos.typeparser.TypeParser
import com.github.max_person.templating.InterpretationData
import com.github.max_person.templating.TemplatingSafeMethod
import its.model.nodes.*
import padeg.lib.Padeg

class TemplatingUtils(val q : ILearningSituation) {
    enum class Case{
        Nom, //именительный (кто? что?)
        Gen, //родительный (кого? чего?)
        Dat, //дательный (кому? чему?)
        Acc, //винительный (кого? что?)
        Ins, //творительный (кем? чем?)
        Pre, //предложный (о ком? о чем?)
        ;

        companion object _static{
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

    companion object _static{
        @JvmStatic
        internal val templatingParser  = TypeParser.newBuilder()
            .registerParser(Case::class.java) { s: String, h: ParserHelper -> Case.fromString(s) }
            .build()

        @JvmStatic
        fun String.toCase(case: Case?) : String{
            return Padeg.getAppointmentPadeg(this, (case?: Case.Nom).ordinal+1).replace(Regex("\\s+"), " ")
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

        //Узлы
        @JvmStatic
        internal fun DecisionTreeNode.asNextStep(interpretationData: InterpretationData) : String {
            return additionalInfo["asNextStep"]!!.interpret(interpretationData)
        }

        @JvmStatic
        internal fun DecisionTreeNode.question(interpretationData: InterpretationData) : String {
            return additionalInfo["question"]!!.interpret(interpretationData) //TODO Если такого нет - у агрегаций, например.
        }

        @JvmStatic
        internal fun DecisionTreeNode.endingCause(interpretationData: InterpretationData) : String {
            return additionalInfo["endingCause"]!!.interpret(interpretationData) //TODO Если такого нет - т.е. у не-конечных узлов
        }

        @JvmStatic
        internal fun LogicAggregationNode.description(interpretationData: InterpretationData, result : Boolean) : String {
            return additionalInfo["description"]!!.interpret(interpretationData.usingVar("result", result))
        }

        //Выходы (стрелки)
        @JvmStatic
        internal fun Outcome<*>.text(interpretationData: InterpretationData) : String? {
            return additionalInfo["text"]?.interpret(interpretationData)
        }

        @JvmStatic
        internal fun Outcome<*>.explanation(interpretationData: InterpretationData) : String? { //TODO? если это PredeterminingOutcome то использовать другую функцию
            return additionalInfo["explanation"]?.interpret(interpretationData)
        }

        @JvmStatic
        internal fun FindActionNode.FindErrorCategory.explanation(interpretationData: InterpretationData, entityAlias : String) : String {
            return additionalInfo["explanation"]!!.interpret(interpretationData.usingVar("current", entityAlias))
        }

        @JvmStatic
        internal fun PredeterminingOutcome.explanation(interpretationData: InterpretationData, result: Boolean) : String {
            return additionalInfo["explanation"]!!.interpret(interpretationData.usingVar("result", result))
        }

        @JvmStatic
        internal fun Outcome<*>.nextStepQuestion(interpretationData: InterpretationData) : String? {
            return additionalInfo["nextStepQuestion"]?.interpret(interpretationData)
        }

        @JvmStatic
        internal fun Outcome<*>.nextStepBranchResult(interpretationData: InterpretationData, branchResult : Boolean) : String? {
            return additionalInfo["nextStepBranchResult"]?.interpret(interpretationData.usingVar("branchResult", branchResult))
        }

        @JvmStatic
        internal fun Outcome<*>.nextStepExplanation(interpretationData: InterpretationData) : String? {
            return additionalInfo["nextStepExplanation"]?.interpret(interpretationData)
        }

        //Ветки
        @JvmStatic
        internal fun ThoughtBranch.description(interpretationData: InterpretationData, result : Boolean) : String {
            return additionalInfo["description"]!!.interpret(interpretationData.usingVar("result", result))
        }

        @JvmStatic
        internal fun ThoughtBranch.nextStepQuestion(interpretationData: InterpretationData) : String? {
            return additionalInfo["nextStepQuestion"]?.interpret(interpretationData)
        }

        @JvmStatic
        internal fun ThoughtBranch.nextStepBranchResult(interpretationData: InterpretationData, branchResult : Boolean) : String? {
            return additionalInfo["nextStepBranchResult"]?.interpret(interpretationData.usingVar("branchResult", branchResult))
        }

        @JvmStatic
        internal fun ThoughtBranch.nextStepExplanation(interpretationData: InterpretationData) : String? {
            return additionalInfo["nextStepExplanation"]?.interpret(interpretationData)
        }

        //endregion
    }

    @TemplatingSafeMethod("val")
    fun getVariableValue(varName: String, case: Case) : String{
        return q.entityDictionary.getByVariable(varName)!!.specificName.toCase(case)
    }

    @TemplatingSafeMethod("obj")
    fun getEntity(alias: String, case: Case) : String{
        return q.entityDictionary.get(alias)!!.specificName.toCase(case)
    }

    @TemplatingSafeMethod("class")
    fun getVariableClassname(varName: String, case: Case) : String{
        return q.entityDictionary.getByVariable(varName)!!.clazz.textName.toCase(case)
    }
}