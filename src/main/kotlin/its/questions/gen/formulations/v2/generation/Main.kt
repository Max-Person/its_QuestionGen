package its.questions.gen.formulations.v2.generation

import its.model.definition.loqi.DomainLoqiBuilder
import its.model.definition.loqi.OperatorLoqiBuilder
import its.questions.gen.formulations.LocalizationRU
import its.reasoner.LearningSituation
import java.io.StringReader

fun main() {
    val loqiStr = """
        //класс "Питомец"  
        class Pet {  
        	//"Возраст" - Объектное свойство данного класса  
        	obj prop age: int [RU.name = "красота";];  
        }  

        //класс "Человек"  
        class Human {  
        	//"Возраст" - Объектное свойство данного класса  
        	obj prop age: int;  
        	//"Имеет питомца" - отношение между объектами класса "Человек"  
        	// и объектами класса "Питомец"  
        	rel hasPet(Pet) : {1 -> *} ;  
        }  

        //Объект класса "Питомец", представляющий конкретного питомца "Мурзик"  
        obj murzik : Pet {  
        	//Утверждение о значении свойства "Возраст" данного объекта  
        	age = 2;  
        }  [RU.localizedName = "мурзик";]

        //Объект класса "Человек", представляющий конкретного человека "Алиса"  
        obj alice : Human {  
        	//Утверждение о значении свойства "Возраст" данного объекта  
        	age = 22;  
        	//Утверждение о связи данного объекта 
        	//с другим объектом "Мурзик" по отношению "имеет питомца" 
        	hasPet(murzik);  
        }  
        
    """.trimIndent()

    val domainModel = DomainLoqiBuilder.buildDomain(StringReader(loqiStr))
    domainModel.validateAndThrow()

    val expr = OperatorLoqiBuilder.buildExp("""
        obj:murzik.age == 2
    """.trimIndent())

    println(QuestionGeneratorFabric(
        LearningSituation(domainModel, HashMap()),
        LocalizationRU
    ).generateQuestion(expr))
}