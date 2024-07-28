package its.questions.gen.strategies

import its.questions.gen.states.GeneralQuestionState
import its.questions.gen.states.QuestionState
import its.questions.gen.states.RedirectQuestionState

class QuestionAutomata(
    val initState: QuestionState,
): Collection<QuestionState> {

    val states : Collection<QuestionState>
    init{
        states = mutableSetOf()
        initState.runForAllReachable { states.add(this) }
    }

    override val size: Int
        get() = states.size
    override fun isEmpty(): Boolean = states.isEmpty()
    override fun iterator(): Iterator<QuestionState> = states.iterator()
    override fun containsAll(elements: Collection<QuestionState>): Boolean = states.containsAll(elements)
    override fun contains(element: QuestionState): Boolean = states.contains(element)
    operator fun get(id: Int): QuestionState {
        return states.first { it.id == id }
    }

    fun hasQuestions() : Boolean{
        return this.any { state ->
            state is GeneralQuestionState<*>
                || (state is RedirectQuestionState && state.redirectsTo() is GeneralQuestionState<*>)
        }
    }

    private fun nonFinalizedEnds() : List<RedirectQuestionState>{
        return states.filter { it is RedirectQuestionState && !it.isFinalized() } as List<RedirectQuestionState>
    }
    fun isFinalized() : Boolean {
        return states.none { it is RedirectQuestionState && !it.isFinalized() }
    }
    fun finalize(nextState: QuestionState){
        require(!isFinalized())

        nonFinalizedEnds().forEach { it.redir = nextState }
    }
}