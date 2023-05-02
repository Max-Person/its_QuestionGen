package its.questions.gen

import com.github.max_person.templating.InterpretationData
import its.model.nodes.ThoughtBranch
import its.questions.gen.formulations.LocalizedDomainModel
import its.questions.gen.formulations.TemplatingUtils
import its.questions.gen.formulations.TemplatingUtils._static.alias
import its.reasoner.LearningSituation

class QuestioningSituation(
    file: String,
    val localizationCode: String = "RU"
) : LearningSituation(file){

    val discussedVariables : MutableMap<String, String> = mutableMapOf()
    val givenAnswers: MutableMap<Int, Int> = mutableMapOf()
    val assumedResults: MutableMap<String, Boolean> = mutableMapOf()

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