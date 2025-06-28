package its.questions.gen.visitors

import its.model.nodes.*


internal fun ThoughtBranch.canHaveNullResult() : Boolean {
    return this.start.checkCanEndWithNullResult()
}

private fun DecisionTreeNode.checkCanEndWithNullResult(): Boolean {
    return this is BranchResultNode && this.value == BranchResult.NULL
           //Вне зависимости от типа агрегации, узел агрегации завершает выполнение с NULL, если его ветви могут выдать NULL,
           // и если у него нет переходов по этому ключу
           || (this is AggregationNode && !this.outcomes.containsKey(BranchResult.NULL) && this.canHaveNullResult())
           || (this is LinkNode<*> && this.children.any { it.checkCanEndWithNullResult() })
}

/**
 * Может ли у узла агрегации быть получен NULL-результат.
 * В зависимости от конфигурации ветви, наличие такого результата не обязательно приводит к выдаче NULL из ветви.
 */
internal fun AggregationNode.canHaveNullResult(): Boolean {
    return when(this){
        is BranchAggregationNode -> this.thoughtBranches.all { it.canHaveNullResult() }
        is CycleAggregationNode -> true //всегда могут быть не найдены удовлетворяющие условию объекты
    }
}