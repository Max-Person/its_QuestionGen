package its.questions.inputs

import com.github.max_person.templating.InterpretationData
import its.model.DomainModel
import its.model.nodes.ThoughtBranch
import its.questions.fileToMap
import its.questions.gen.visitors.ALIAS_ATR

class LearningSituation private constructor(
    val entityDictionary : EntityDictionary = EntityDictionary(),
    val answers: Map<String, String> = mapOf(),
    val knownVariables : MutableMap<String, String> = mutableMapOf(),
    val questioningInfo: MutableMap<String, String> = mutableMapOf()
) {

    constructor(dir: String) : this(answers = fileToMap(dir + "answers.txt", ':')){
        require(DomainModel.usesQDictionaries())
        entityDictionary.fromCSV(dir + "entities.csv")
    }

    private val templatingUtils = TemplatingUtils(this)
    val templating = InterpretationData().withGlobalObj(templatingUtils).withParser(TemplatingUtils.templatingParser)

    fun addGivenAnswer(questionStateId: Int, value: Int){
        questioningInfo[questionStateId.toString()] = value.toString()
    }
    fun givenAnswer(questionStateId : Int) : Int?{
        return questioningInfo[questionStateId.toString()]?.toInt()
    }

    fun addAssumedResult(branch: ThoughtBranch, value: Boolean){
        questioningInfo[branch.additionalInfo[ALIAS_ATR]!!] = value.toString()
    }
    fun assumedResult(branch: ThoughtBranch) : Boolean?{
        return questioningInfo[branch.additionalInfo[ALIAS_ATR]!!]?.toBoolean()
    }
}