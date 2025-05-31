package its.questions.gen.formulations.v2.generation

import its.model.definition.loqi.DomainLoqiBuilder
import its.model.definition.loqi.OperatorLoqiBuilder
import its.model.definition.types.BooleanType
import its.questions.gen.formulations.LocalizationRU
import its.reasoner.LearningSituation
import java.io.StringReader

fun main() {

    val domainModel = DomainLoqiBuilder.buildDomain(StringReader(loqiStr))
    domainModel.validateAndThrow()

    val exprs = listOf(

        "obj:murzik.age",
        "obj:murzik.age.compare(2)",
        "obj:murzik.age == 2",
        "obj:murzik.age != 2",
        "obj:murzik.age > 2",
        "obj:murzik.age < 2",
        "obj:murzik.age >= 2",

        "obj:murzik.age.compare(obj:basya.age)",
        "obj:murzik.age == obj:basya.age",
        "obj:murzik.age != obj:basya.age",
        "obj:murzik.age > obj:basya.age",
        "obj:murzik.age < obj:basya.age",
        "obj:murzik.age >= obj:basya.age",

        "obj:murzik.isCool",
        "obj:murzik.isCool == true",
        "obj:murzik.isCool == obj:basya.isCool",

        "obj:alice=>hasPet(obj:murzik)",

        "obj:murzik.class()",
        "obj:murzik is class:Pet",
    ).associateWith { OperatorLoqiBuilder.buildExp(it) }

    val learningSituation = LearningSituation(domainModel, HashMap())
    exprs.forEach { (string, expr) ->
        val context = QuestionGeneratorFabric(learningSituation, LocalizationRU).getContext(expr)
        println(
            string + "\t\t"
            + context?.generate(learningSituation, LocalizationRU)
            + if(expr.resolvedType(domainModel) == BooleanType)
                "\t\t" + context?.generateAnswer(learningSituation, LocalizationRU, true)
            else ""
        )
    }

}


val loqiStr = """
        //класс "Питомец"  
        class Pet {  
        	//"Возраст" - Объектное свойство данного класса  
        	obj prop age: int [
                RU.question = "${   "Сколько лет \${\$`obj`}[case='д']?"   }";
                RU.compareValueQuestion = "${   "\${\$`obj`}[case='д'] \${\$value} \${\$value == 1 ? 'год' : \$value < 5 ? 'года' : 'лет'}?"   }";
                RU.assertion = "${   "\${\$`obj`}[case='д'] \${\$value} \${\$value == 1 ? 'год' : \$value < 5 ? 'года' : 'лет'}"   }";
                RU.localizedName = "возраст";
            ];  
            
            obj prop isCool: bool [
                RU.question = "${   "Крутой ли \${\$`obj`}?"   }";
                RU.assertion = "${   "\${\$`obj`} \${\$value ? '' : 'не'} крутой"   }";
                RU.localizedName = "крутость";
            ];
        }  [
            RU.localizedName = "питомец";
        ]

        //класс "Человек"  
        class Human {  
        	//"Возраст" - Объектное свойство данного класса  
        	obj prop age: int;  
        	//"Имеет питомца" - отношение между объектами класса "Человек"  
        	// и объектами класса "Питомец"  
        	rel hasPet(Pet) : {1 -> *} [
                RU.question = "${   "Является ли \${\$obj1} питомцем \${\$subj}[case='р']?"   }";
                RU.assertion = "${   "\${\$subj} имеет питомца \${\$obj1}"   }";
            ];  
        }  [
            RU.localizedName = "человек";
        ]

        //Объект класса "Питомец", представляющий конкретного питомца "Мурзик"  
        obj murzik : Pet {  
        	//Утверждение о значении свойства "Возраст" данного объекта  
        	age = 2;  
            isCool = false;
        }  [
            RU.localizedName = "Мурзик";
        ]
        
        obj basya : Pet {  
        	age = 5;  
            isCool = true;
        }  [
            RU.localizedName = "Бася";
        ]

        //Объект класса "Человек", представляющий конкретного человека "Алиса"  
        obj alice : Human {  
        	//Утверждение о значении свойства "Возраст" данного объекта  
        	age = 22;  
        	//Утверждение о связи данного объекта 
        	//с другим объектом "Мурзик" по отношению "имеет питомца" 
        	hasPet(murzik);  
        }  [
            RU.localizedName = "Алиса";
        ]
        
    """.trimIndent()