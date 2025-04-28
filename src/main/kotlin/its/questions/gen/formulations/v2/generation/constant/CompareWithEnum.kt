package its.questions.gen.formulations.v2.generation.constant

import its.model.definition.types.EnumType
import its.model.definition.types.Type
import its.questions.gen.formulations.Localization
import its.reasoner.LearningSituation

class CompareWithEnum(learningSituation: LearningSituation, localization: Localization) : CompareWithConstant(learningSituation, localization) {
    override fun typeFits(type: Type<*>): Boolean {
        return type is EnumType
    }
}