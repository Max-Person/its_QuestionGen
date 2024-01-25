package its.questions.gen.strategies

import its.model.nodes.ThoughtBranch
import its.questions.gen.states.QuestionState

interface QuestioningStrategy {
    companion object _static {
        @JvmStatic
        var defaultFullBranchStrategy : QuestioningStrategy = FullBranchStrategy
    }

    fun build(branch: ThoughtBranch) : QuestionAutomata

    fun buildAndFinalize(branch: ThoughtBranch, endState: QuestionState): QuestionAutomata{
        val automata = build(branch)
        automata.finalize(endState)
        return automata
    }
}

interface QuestioningStrategyWithInfo<Info> : QuestioningStrategy {
    data class StrategyOutput<Info>(
        val automata: QuestionAutomata,
        val info: Info,
    )

    override fun build(branch: ThoughtBranch): QuestionAutomata {
        return buildWithInfo(branch).automata
    }

    fun buildWithInfo(branch: ThoughtBranch) : StrategyOutput<Info>
}