package its.questions.gen.states

import its.questions.gen.QuestioningSituation

sealed class QuestionState{
    abstract val id: Int
    abstract fun getQuestion(situation: QuestioningSituation) : QuestionStateResult
    abstract fun proceedWithAnswer(situation: QuestioningSituation, answer: List<Int>): QuestionStateChange
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

    override fun getQuestion(situation: QuestioningSituation): QuestionStateResult {
        return redir!!.getQuestion(situation)
    }

    override fun proceedWithAnswer(situation: QuestioningSituation, answer: List<Int>): QuestionStateChange {
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

    abstract fun skip(situation: QuestioningSituation) : QuestionStateChange

    override fun getQuestion(situation: QuestioningSituation): QuestionStateResult {
        return skip(situation)
    }

    override fun proceedWithAnswer(situation: QuestioningSituation, answer: List<Int>): QuestionStateChange {
        return skip(situation)
    }

    companion object _static {
        @JvmStatic
        var stateCount = -1
        @JvmStatic
        private fun nextId() = stateCount--
    }
}

class EndQuestionState: SkipQuestionState() {
    override val reachableStates: Collection<QuestionState> = emptyList()
    override fun skip(situation: QuestioningSituation): QuestionStateChange = QuestionStateChange(null, null)
}

abstract class GeneralQuestionState<AnswerInfo> : QuestionState() {
    private val links: MutableSet<QuestionStateLink<AnswerInfo>> = mutableSetOf()

    override val id: Int = nextId()

    protected fun String.prependId(): String {
        return if(PREPEND_ID_TO_TEXT) "$id. $this" else this
    }

    data class QuestionStateLink<AnswerInfo>(
        val condition: (situation: QuestioningSituation, chosenAnswer: AnswerInfo) -> Boolean,
        val nextState: QuestionState,

        //TODO конструктор из пары и тп?
    )

    /**
     * Связать текущее состояние с некоторым другим.
     * Предполагается, что это безусловный переход, и в связи с этим он должен быть единственным -
     * вызов данного метода очищает массив переходов.
     */
    internal fun linkTo(nextState: QuestionState) {
        links.clear()
        links.add(QuestionStateLink({ _, _ -> true }, nextState))
    }

    /**
     * Связать текущее состояние с некоторым другим - переход будет осуществляться при выполнении условия [condition]
     */
    internal fun linkTo(
        nextState: QuestionState,
        condition: (situation: QuestioningSituation, chosenAnswer: AnswerInfo) -> Boolean,
    ) {
        links.add(QuestionStateLink(condition, nextState))
    }

    /**
     * Получить из массива переходов состояние, чье условие перехода выполняется в данной конкретной ситуации
     */
    internal fun getStateFromLinks(situation: QuestioningSituation, chosenAnswer: AnswerInfo): QuestionState {
        return links.first { it.condition(situation, chosenAnswer) }.nextState
    }

    companion object _static {
        @JvmStatic
        var PREPEND_ID_TO_TEXT: Boolean = false
        @JvmStatic
        internal var stateCount = 1
        @JvmStatic
        private fun nextId() = stateCount++
    }

    override val reachableStates: Collection<QuestionState>
        get() = links.map { it.nextState }
}

enum class ExplanationType{
    Error, Success, Continue,
}

data class Explanation(
    val text : String,
    val type: ExplanationType = ExplanationType.Continue,
    val shouldPause: Boolean = true
)

data class QuestionStateChange(
    val explanation: Explanation?,
    val nextState: QuestionState?
) : QuestionStateResult

sealed interface QuestionStateResult //в обычном случае вопрос (Question), но если состояние может быть пропущено, то QuestionStateChange