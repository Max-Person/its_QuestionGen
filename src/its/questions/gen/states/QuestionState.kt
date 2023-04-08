package its.questions.gen.states

import its.questions.inputs.LearningSituation
import java.util.*

interface QuestionState{
    val id: Int
    fun getQuestion(situation: LearningSituation) : QuestionStateResult
    fun proceedWithAnswer(situation: LearningSituation, answer: List<Int>): QuestionStateChange
    val reachableStates: Collection<QuestionState>

    fun runForAllReachable(block: QuestionState.() -> Unit){
        runForAllReachable(block, Stack())
    }
    private fun runForAllReachable(block: QuestionState.() -> Unit, visited: Stack<QuestionState>){
        if(visited.contains(this))
            return

        this.block()
        visited.push(this)
        reachableStates.forEach { it.runForAllReachable(block, visited) }
        visited.pop()
    }
}

class RedirectQuestionState : QuestionState{
    var redir: QuestionState? = null
    override val id
        get() = redir?.id ?: -1

    override fun getQuestion(situation: LearningSituation): QuestionStateResult {
        return redir!!.getQuestion(situation)
    }

    override fun proceedWithAnswer(situation: LearningSituation, answer: List<Int>): QuestionStateChange {
        return redir!!.proceedWithAnswer(situation, answer)
    }

    override val reachableStates: Collection<QuestionState>
        get() = redir?.reachableStates ?: emptyList()
}

abstract class SkipQuestionState : QuestionState{
    override val id = -2

    abstract fun skip(situation: LearningSituation) : QuestionStateChange

    override fun getQuestion(situation: LearningSituation): QuestionStateResult {
        return skip(situation)
    }

    override fun proceedWithAnswer(situation: LearningSituation, answer: List<Int>): QuestionStateChange {
        return skip(situation)
    }
}

abstract class GeneralQuestionState<AnswerInfo>(
    protected val links: Set<QuestionStateLink<AnswerInfo>>
) : QuestionState {
    override val id: Int = nextId()

    data class QuestionStateLink<AnswerInfo>(
        val condition: (situation: LearningSituation, chosenAnswer: AnswerInfo) -> Boolean,
        val nextState: QuestionState

        //TODO конструктор из пары и тп?
    )

    companion object _static {
        var stateCount = 0
        fun nextId() = stateCount++
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