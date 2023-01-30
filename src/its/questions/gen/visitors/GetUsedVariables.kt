package its.questions.gen.visitors

import its.model.expressions.Literal
import its.model.expressions.Variable
import its.model.expressions.literals.DecisionTreeVarLiteral
import its.model.expressions.operators.BaseOperator
import its.model.expressions.visitors.SimpleOperatorVisitor

class GetUsedVariables() : SimpleOperatorVisitor<List<String>>(){
    val list = mutableListOf<String>()
    override fun process(literal: Literal): List<String> {
        if(literal is DecisionTreeVarLiteral && !list.contains(literal.value)){
            list.add(literal.value)
        }
        return list
    }
    override fun process(variable: Variable): List<String> {return list}
    override fun process(op: BaseOperator): List<String> {return list}
    override fun process(op: BaseOperator, currentInfo: List<String>, argInfo: List<List<String>> ): List<String> {return list}
}