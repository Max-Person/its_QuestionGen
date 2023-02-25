package its.questions.gen.visitors

import its.model.expressions.Operator
import its.model.expressions.literals.DecisionTreeVarLiteral
import its.model.expressions.operators.BaseOperator

fun Operator.getUsedVariables() : Set<String>{
    val set = mutableSetOf<String>()
    if(this is DecisionTreeVarLiteral){
        set.add(this.value)
    }
    else if(this is BaseOperator){
        this.args.forEach{set.addAll(it.getUsedVariables())}
    }
    return set
}