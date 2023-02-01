package its.questions.gen

import com.github.drapostolos.typeparser.ParserHelper
import com.github.drapostolos.typeparser.TypeParser
import com.github.max_person.templating.StrSubMethod
import com.github.max_person.templating.StrSubstitutionCore
import padeg.lib.Padeg

class TemplatingUtils(val q : QuestionGenerator) {
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
        private val templatingParser  = TypeParser.newBuilder()
            .registerParser(Case::class.java) { s: String, h: ParserHelper -> Case.fromString(s) }
            .build()
    }

    private fun String.toCase(case: Case?) : String{
        return Padeg.getAppointmentPadeg(this, (case?: Case.Nom).ordinal+1)
    }

    fun process(str: String) : String{
        return sub.process(str)
    }

    private val sub = StrSubstitutionCore(this, templatingParser)

    @StrSubMethod("val")
    fun getVariableValue(varName: String, case: Case) : String{
        return q.entityDictionary.getByVariable(varName)!!.specificName.toCase(case)
    }

    @StrSubMethod("obj")
    fun getEntity(alias: String, case: Case) : String{
        return q.entityDictionary.get(alias)!!.specificName.toCase(case)
    }

    @StrSubMethod("class")
    fun getVariableClassname(varName: String, case: Case) : String{
        return q.entityDictionary.getByVariable(varName)!!.clazz.textName.toCase(case)
    }
}