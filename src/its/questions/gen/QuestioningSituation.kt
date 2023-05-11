package its.questions.gen

import com.github.max_person.templating.InterpretationData
import its.model.nodes.ThoughtBranch
import its.questions.gen.formulations.LocalizedDomainModel
import its.questions.gen.formulations.TemplatingUtils
import its.questions.gen.formulations.TemplatingUtils._static.alias
import its.reasoner.LearningSituation
import org.apache.jena.rdf.model.Model

class QuestioningSituation : LearningSituation{

    val discussedVariables : MutableMap<String, String>
    val givenAnswers: MutableMap<Int, Int>
    val assumedResults: MutableMap<String, Boolean>
    val localizationCode: String

    constructor(
        model: Model,
        variables: MutableMap<String, String> = mutableMapOf(),
        discussedVariables : MutableMap<String, String> = mutableMapOf(),
        givenAnswers: MutableMap<Int, Int> = mutableMapOf(),
        assumedResults: MutableMap<String, Boolean> = mutableMapOf(),
        localizationCode: String = "RU"
    ) : super(model, variables)
    {
        this.discussedVariables = discussedVariables
        this.givenAnswers = givenAnswers
        this.assumedResults = assumedResults
        this.localizationCode = localizationCode
    }

    constructor(model: Model, localizationCode: String = "RU") : super(model){
        this.discussedVariables = mutableMapOf()
        this.givenAnswers = mutableMapOf()
        this.assumedResults = mutableMapOf()
        this.localizationCode = localizationCode
    }

    constructor(filename: String, localizationCode: String = "RU") : super(filename){
        this.discussedVariables = mutableMapOf()
        this.givenAnswers = mutableMapOf()
        this.assumedResults = mutableMapOf()
        this.localizationCode = localizationCode
    }

    private val templatingUtils = TemplatingUtils(this)
    val templating = InterpretationData().withGlobalObj(templatingUtils).withParser(TemplatingUtils.templatingParser)

    fun addGivenAnswer(questionStateId: Int, value: Int){
        givenAnswers[questionStateId] = value
    }
    fun givenAnswer(questionStateId : Int) : Int?{
        return givenAnswers[questionStateId]
    }

    fun addAssumedResult(branch: ThoughtBranch, value: Boolean){
        assumedResults[branch.alias] = value
    }
    fun assumedResult(branch: ThoughtBranch) : Boolean?{
        return assumedResults[branch.alias]
    }

    val localization
        get() = LocalizedDomainModel.localization(this.localizationCode)

    val domainLocalization
        get() = LocalizedDomainModel.domainLocalization(this.localizationCode)
}