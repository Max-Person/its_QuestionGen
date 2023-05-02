package its.questions.inputs

import its.model.DomainModel
import its.model.dictionaries.DictionaryBase
import its.model.models.ClassModel
import its.model.models.DecisionTreeVarModel

class EntityInfo(
    val alias: String,
    className: String,
    calculatedClassNames: List<String>,
    val specificName: String,
    variableName: String? = null,
    val errorCategories: List<String>
) {
    val clazz: ClassModel
    val calculatedClasses : List<ClassModel>
    val variable : DecisionTreeVarModel?
    init {
        require(DomainModel.classesDictionary.exist(className)){
            "Сущность $alias задана с неизвестным классом $className"
        }
        clazz = DomainModel.classesDictionary.get(className)!!
        calculatedClasses = calculatedClassNames.map{
            require(DomainModel.classesDictionary.exist(it)){
                "Сущность $alias задана с неизвестным вычисляемым классом $it"
            }
            DomainModel.classesDictionary.get(it)!!
        }
        if(!variableName.isNullOrBlank()){
            require(DomainModel.decisionTreeVarsDictionary.contains(variableName)){
                "Сущность $alias задана как значение неизвестной переменной $variableName"
            }
            variable = DomainModel.decisionTreeVarsDictionary.get(variableName)!!
        }
        else
            variable = null
    }
}

class EntityDictionary : DictionaryBase<EntityInfo>(EntityInfo::class) {
    override fun onAddActions(added: EntityInfo) {}

    override fun onAddValidation(value: EntityInfo) {
        require(!contains(value.alias)) {
            "Сущность ${value.alias} уже объявлена в словаре."
        }
    }

    override fun get(name: String): EntityInfo? {
        return values.firstOrNull { it.alias == name }
    }

    fun getByVariable(varName: String): EntityInfo? {
        return values.firstOrNull { it.variable?.name == varName }
    }

    override fun validate() {/*Не нужно т.к EntityInfo валидируют сами себя при создании*/}
}