package its.questions.gen

import com.github.max_person.templating.InterpretationData
import its.model.definition.DomainModel
import its.model.definition.types.Obj
import its.model.nodes.BranchResult
import its.model.nodes.ThoughtBranch
import its.questions.gen.formulations.Localization
import its.questions.gen.formulations.SituationTextGenerator
import its.questions.gen.formulations.TemplatingUtils
import its.questions.gen.formulations.TemplatingUtils.alias
import its.reasoner.LearningSituation

class QuestioningSituation : LearningSituation{

    val discussedVariables : MutableMap<String, String>
    val givenAnswers: MutableMap<Int, Int>
    val assumedResults: MutableMap<String, BranchResult>
    val localizationCode: String

    constructor(
        model: DomainModel,
        variables: MutableMap<String, Obj> = mutableMapOf(),
        discussedVariables : MutableMap<String, String> = mutableMapOf(),
        givenAnswers: MutableMap<Int, Int> = mutableMapOf(),
        assumedResults: MutableMap<String, BranchResult> = mutableMapOf(),
        localizationCode: String = "RU"
    ) : super(model, variables)
    {
        this.discussedVariables = discussedVariables
        this.givenAnswers = givenAnswers
        this.assumedResults = assumedResults
        this.localizationCode = localizationCode
    }

    constructor(model: DomainModel, localizationCode: String = "RU") : super(model){
        this.discussedVariables = mutableMapOf()
        this.givenAnswers = mutableMapOf()
        this.assumedResults = mutableMapOf()
        this.localizationCode = localizationCode
    }

    internal val formulations = SituationTextGenerator(this)
    val templating = InterpretationData().withGlobalObj(formulations).withParser(TemplatingUtils.templatingParser)

    fun addGivenAnswer(questionStateId: Int, value: Int){
        givenAnswers[questionStateId] = value
    }
    fun givenAnswer(questionStateId : Int) : Int?{
        return givenAnswers[questionStateId]
    }

    fun addAssumedResult(branch: ThoughtBranch, value: BranchResult){
        assumedResults[branch.alias] = value
    }
    fun assumedResult(branch: ThoughtBranch) : BranchResult?{
        return assumedResults[branch.alias]
    }

    /**
     * Создать учебную ситуацию-копию из текущей, чтобы вычисления не поменяли состояние текущей
     */
    fun forEval(): LearningSituation {
        return LearningSituation(
            domainModel.copy(),
            decisionTreeVariables.toMutableMap()
        )
    }

    val localization
        get() = Localization.localizations[this.localizationCode]!!
}