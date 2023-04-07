package its.questions.gen.states

class QuestionAutomata(
    val initState: QuestionState,
): Collection<QuestionState> {

    val states : Collection<QuestionState>
    init{
        states = mutableListOf()
        initState.runForAllReachable { states.add(this) }
    }

    override val size: Int
        get() = states.size
    override fun isEmpty(): Boolean = states.isEmpty()
    override fun iterator(): Iterator<QuestionState> = states.iterator()
    override fun containsAll(elements: Collection<QuestionState>): Boolean = states.containsAll(elements)
    override fun contains(element: QuestionState): Boolean = states.contains(element)
    operator fun get(id: Int): QuestionState{
        return states.first { it.id == id }
    }

    private fun nonFinalizedEnds() : List<RedirectQuestionState>{
        return states.filter { it is RedirectQuestionState && it.redir == null }.toList() as List<RedirectQuestionState>
    }
    fun isFinalized() : Boolean {
        return states.none { it is RedirectQuestionState && it.redir == null }
    }
    fun finalize(nextState: QuestionState){
        require(!isFinalized())

        nonFinalizedEnds().forEach { it.redir = nextState }
    }
}