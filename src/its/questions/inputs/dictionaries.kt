package its.questions.inputs

import its.model.DomainModel
import its.model.dictionaries.*

class QClassesDictionary : ClassesDictionaryBase<QClassModel>(QClassModel::class)
class QEnumsDictionary : EnumsDictionaryBase<QEnumModel>(QEnumModel::class)
class QPropertiesDictionary : PropertiesDictionaryBase<QPropertyModel>(QPropertyModel::class)
class QRelationshipsDictionary : RelationshipsDictionaryBase<QRelationshipModel>(QRelationshipModel::class)
class QVarsDictionary : DecisionTreeVarsDictionaryBase<QVarModel>(QVarModel::class)
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

fun DomainModel._static.usesQDictionaries() : Boolean{
    return classesDictionary is QClassesDictionary &&
            enumsDictionary is QEnumsDictionary &&
            propertiesDictionary is QPropertiesDictionary &&
            relationshipsDictionary is QRelationshipsDictionary &&
            decisionTreeVarsDictionary is QVarsDictionary
}