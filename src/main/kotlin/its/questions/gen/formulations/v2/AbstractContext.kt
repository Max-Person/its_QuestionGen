package its.questions.gen.formulations.v2

import its.questions.gen.formulations.Localization
import its.questions.gen.visitors.ValueToAnswerString.toLocalizedString
import its.reasoner.LearningSituation

interface AbstractContext {
    fun generate(learningSituation: LearningSituation, localization: Localization): String?

    fun generateAnswer(learningSituation: LearningSituation, localization: Localization, value : Any) : String? {
        return value.toLocalizedString(learningSituation, localization.codePrefix)
    }

    fun generateExplanation(learningSituation: LearningSituation, localization: Localization, correctAnswer : Any) : String? {
        return null
    }
}