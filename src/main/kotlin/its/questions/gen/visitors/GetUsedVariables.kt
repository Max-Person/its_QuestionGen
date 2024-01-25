package its.questions.gen.visitors

import its.model.expressions.Operator
import its.model.expressions.literals.DecisionTreeVarLiteral
import its.model.expressions.literals.Literal

fun Operator.getUsedVariables() : Set<String>{
    val set = mutableSetOf<String>()
    if(this is DecisionTreeVarLiteral){
        set.add(this.name)
    }
    else if(this !is Literal){
        this.children.forEach{set.addAll(it.getUsedVariables())}
    }
    return set
}