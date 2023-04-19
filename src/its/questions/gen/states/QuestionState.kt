package its.questions.gen.states

import its.questions.inputs.LearningSituation

sealed class QuestionState{
    abstract val id: Int
    abstract fun getQuestion(situation: LearningSituation) : QuestionStateResult
    abstract fun proceedWithAnswer(situation: LearningSituation, answer: List<Int>): QuestionStateChange
    abstract val reachableStates: Collection<QuestionState>

    fun runForAllReachable(block: QuestionState.() -> Unit){
        runForAllReachable(block, mutableSetOf())
    }
    private fun runForAllReachable(block: QuestionState.() -> Unit, visited: MutableSet<QuestionState> = mutableSetOf()){
        if(visited.contains(this))
            return

        this.block()
        visited.add(this)
        reachableStates.map{if(it is RedirectQuestionState && it.redirectsTo() != null) it.redirectsTo()!! else it}.forEach { it.runForAllReachable(block, visited) }
    }
}

//TODO продумать правильное взаимодействие редиректов со сравнениями
class RedirectQuestionState : QuestionState(){
    var redir: QuestionState? = null
        set(value){
            var r = value
            while(r is RedirectQuestionState){
                r = r.redirectsTo()
            }
            field = r
        }

    override val id
        get() = redir?.id ?: 0

    override fun getQuestion(situation: LearningSituation): QuestionStateResult {
        return redir!!.getQuestion(situation)
    }

    override fun proceedWithAnswer(situation: LearningSituation, answer: List<Int>): QuestionStateChange {
        return redir!!.proceedWithAnswer(situation, answer)
    }

    override val reachableStates: Collection<QuestionState>
        get() = redir?.reachableStates ?: emptyList()

    fun isFinalized() : Boolean{
        return this.redirectsTo() != null
    }

    fun redirectsTo() : QuestionState?{
        return if(redir == null) null
        else if(redir is RedirectQuestionState) (redir!! as RedirectQuestionState).redirectsTo()
        else redir
    }
}

abstract class SkipQuestionState : QuestionState(){
    override val id = nextId()

    abstract fun skip(situation: LearningSituation) : QuestionStateChange

    override fun getQuestion(situation: LearningSituation): QuestionStateResult {
        return skip(situation)
    }

    override fun proceedWithAnswer(situation: LearningSituation, answer: List<Int>): QuestionStateChange {
        return skip(situation)
    }

    companion object _static {
        @JvmStatic
        var stateCount = -1
        @JvmStatic
        private fun nextId() = stateCount--
    }
}

abstract class GeneralQuestionState<AnswerInfo>(
    protected val links: Set<QuestionStateLink<AnswerInfo>>
) : QuestionState() {
    override val id: Int = nextId()

    data class QuestionStateLink<AnswerInfo>(
        val condition: (situation: LearningSituation, chosenAnswer: AnswerInfo) -> Boolean,
        val nextState: QuestionState

        //TODO конструктор из пары и тп?
    )

    companion object _static {
        @JvmStatic
        var stateCount = 1
        @JvmStatic
        private fun nextId() = stateCount++
    }

    override val reachableStates: Collection<QuestionState>
        get() = links.map { it.nextState }
}

data class Explanation(
    val text : String,
    val shouldPause: Boolean = true
)

data class QuestionStateChange(
    val explanation: Explanation?,
    val nextState: QuestionState?
) : QuestionStateResult

sealed interface QuestionStateResult //в обычном случае вопрос (Question), но если состояние может быть пропущено, то QuestionStateChange