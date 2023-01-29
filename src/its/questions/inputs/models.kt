package its.questions.inputs

import its.model.expressions.types.DataType
import its.model.models.*

class QClassModel(
    name: String,
    parent: String? = null,
    calcExprXML: String? = null,
    val textName: String,
): ClassModel(name, parent, calcExprXML) {
}

class QEnumModel(
    name: String,
    values: List<String>,
    isLinear: Boolean,
    val textName: String,
    val textValues: List<String>,
): EnumModel(name, values, isLinear) {
}

class QPropertyModel(
    name: String,
    dataType: DataType?,
    enumName: String? = null,
    isStatic: Boolean,
    owners: List<String>? = null,
    valueRange: Range? = null,
    val valueTemplate : String,
    val questionTemplate : String,
    val descriptionTemplate : String,
) : PropertyModel(name, dataType, enumName, isStatic, owners, valueRange)

class QRelationshipModel(
    name: String,
    parent: String? = null,
    argsClasses: List<String>,
    scaleType: ScaleType? = null,
    scaleRelationshipsNames: List<String>? = null,
    relationType: RelationType? = null,
    flags: Int,
    val descriptionTemplate : String,
    val scaleDescriptionTemplates : List<String>? = null,
) : RelationshipModel(name, parent, argsClasses, scaleType, scaleRelationshipsNames, relationType, flags) {
    override fun scaleRelationships(): List<RelationshipModel> {
        when (scaleType) {
            ScaleType.Linear -> {
                scaleDescriptionTemplates!!
                return listOf(
                    QRelationshipModel(
                        name = scaleRelationshipsNames!![0],
                        argsClasses = argsClasses,
                        flags = flags,
                        descriptionTemplate = scaleDescriptionTemplates[0]
                    ),
                    QRelationshipModel(
                        name = scaleRelationshipsNames!![1],
                        argsClasses = argsClasses,
                        flags = 16,
                        descriptionTemplate = scaleDescriptionTemplates[1]
                    ),
                    QRelationshipModel(
                        name = scaleRelationshipsNames!![2],
                        argsClasses = argsClasses,
                        flags = 16,
                        descriptionTemplate = scaleDescriptionTemplates[2]
                    ),
                    QRelationshipModel(
                        name = scaleRelationshipsNames!![3],
                        argsClasses = argsClasses.plus(argsClasses[0]),
                        flags = 0,
                        descriptionTemplate = scaleDescriptionTemplates[3]
                    ),
                    QRelationshipModel(
                        name = scaleRelationshipsNames!![4],
                        argsClasses = argsClasses.plus(argsClasses[0]),
                        flags = 0,
                        descriptionTemplate = scaleDescriptionTemplates[4]
                    ),
                    QRelationshipModel(
                        name = scaleRelationshipsNames!![5],
                        argsClasses = argsClasses.plus(argsClasses[0]),
                        flags = 0,
                        descriptionTemplate = scaleDescriptionTemplates[5]
                    )
                )
            }

            ScaleType.Partial -> {
                TODO("Отношения частичного порядка")
                return emptyList()
            }

            else -> {
                return emptyList()
            }

        }
    }
}

class QVarModel(
    name: String,
    className: String,
    val valueSearchTemplate : String?
) : DecisionTreeVarModel(name, className)