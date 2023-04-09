package its.questions.inputs

import its.model.DomainModel

class EntityInfo(
    val alias: String,
    className: String,
    calculatedClassNames: List<String>,
    val specificName: String,
    variableName: String? = null,
    val errorCategories: List<String>
) {
    val clazz: QClassModel
    val calculatedClasses : List<QClassModel>
    val variable : QVarModel?
    init {
        require(DomainModel.usesQDictionaries())
        require(DomainModel.classesDictionary.exist(className)){
            "Сущность $alias задана с неизвестным классом $className"
        }
        clazz = (DomainModel.classesDictionary as QClassesDictionary).get(className)!!
        calculatedClasses = calculatedClassNames.map{
            require(DomainModel.classesDictionary.exist(it)){
                "Сущность $alias задана с неизвестным вычисляемым классом $it"
            }
            (DomainModel.classesDictionary as QClassesDictionary).get(it)!!
        }
        if(!variableName.isNullOrBlank()){
            require(DomainModel.decisionTreeVarsDictionary.contains(variableName)){
                "Сущность $alias задана как значение неизвестной переменной $variableName"
            }
            variable = (DomainModel.decisionTreeVarsDictionary as QVarsDictionary).get(variableName)!!
        }
        else
            variable = null
    }
}