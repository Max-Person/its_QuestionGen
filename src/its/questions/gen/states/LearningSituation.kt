package its.questions.gen.states

import com.github.max_person.templating.InterpretationData
import its.model.DomainModel
import its.model.nodes.ThoughtBranch
import its.questions.fileToMap
import its.questions.gen.TemplatingUtils
import its.questions.gen.visitors.ALIAS_ATR
import its.questions.inputs.EntityDictionary
import its.questions.inputs.usesQDictionaries

interface ILearningSituation{
    val entityDictionary : EntityDictionary
    val answers: Map<String, String>
    val templating : InterpretationData
    val questioningInfo : MutableMap<String, String>

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

class LearningSituation private constructor(
    override val entityDictionary : EntityDictionary = EntityDictionary(),
    override val answers: Map<String, String> = mapOf(),
    val knownVariables : MutableSet<String> = mutableSetOf(),
    override val questioningInfo: MutableMap<String, String> = mutableMapOf()
) : ILearningSituation{

    constructor(dir: String) : this(answers = fileToMap(dir + "answers.txt", ':')){
        require(DomainModel.usesQDictionaries())
        entityDictionary.fromCSV(dir + "entities.csv")
    }

    private val templatingUtils = TemplatingUtils(this)
    override val templating = InterpretationData().withGlobalObj(templatingUtils).withParser(TemplatingUtils.templatingParser)
}