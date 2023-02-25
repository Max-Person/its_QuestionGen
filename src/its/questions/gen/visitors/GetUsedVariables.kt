package its.questions.gen.visitors

import its.model.expressions.Literal
import its.model.expressions.Operator
import its.model.expressions.Variable
import its.model.expressions.literals.DecisionTreeVarLiteral
import its.model.expressions.operators.BaseOperator
import its.model.expressions.visitors.SimpleOperatorVisitor

class GetUsedVariables private constructor() : SimpleOperatorVisitor<Set<String>>(){
    val set = mutableSetOf<String>()

    // ---------------------- Удобства ---------------------------

    companion object _static{
        @JvmStatic
        fun Operator.getUsedVariables() : Set<String>{
            return this.accept(GetUsedVariables())
        }
    }

    // ---------------------- Функции поведения ---------------------------

    override fun process(literal: Literal): Set<String> {
        if(literal is DecisionTreeVarLiteral){
            set.add(literal.value)
        }
        return set
    }
    override fun process(variable: Variable): Set<String> {return set}
    override fun process(op: BaseOperator): Set<String> {return set}
    override fun process(op: BaseOperator, currentInfo: Set<String>, argInfo: List<Set<String>> ): Set<String> {return set}
}