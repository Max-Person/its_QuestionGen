package its.questions.gen.formulations.v2.generation

import its.model.definition.loqi.DomainLoqiBuilder
import its.model.definition.loqi.OperatorLoqiBuilder
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
        "obj:murzik is Pet",
    ).associateWith { OperatorLoqiBuilder.buildExp(it) }

    exprs.forEach { (string, expr) ->
        println(
            string + "\t\t" + QuestionGeneratorFabric(
                LearningSituation(domainModel, HashMap()),
                LocalizationRU
            ).getContext(expr)?.generate(LearningSituation(domainModel, HashMap()), LocalizationRU)
        )
    }

}


val loqiStr = """
        //класс "Питомец"  
        class Pet {  
        	//"Возраст" - Объектное свойство данного класса  
        	obj prop age: int [
                RU.localizedName = "возраст";
            ];  
            
            obj prop isCool: bool [
                RU.localizedName = "крутой";
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
                RU.assertion = "${'$'}{${'$'}subj} имеет питомца ${'$'}{${'$'}obj1}";
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