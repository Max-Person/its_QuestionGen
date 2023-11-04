import its.model.DomainModel
import its.questions.gen.QuestioningSituation
import its.questions.gen.formulations.LocalizedDomainModel
import its.questions.gen.states.*
import its.questions.gen.strategies.QuestioningStrategy
import java.lang.NumberFormatException
import java.util.*
import javax.swing.SwingUtilities


fun run() {
    val dir = "..\\inputs\\input_examples\\"

    LocalizedDomainModel(dir)


    val endState = object : SkipQuestionState(){
        override fun skip(situation: QuestioningSituation): QuestionStateChange {
            return QuestionStateChange(Explanation("Конец"), null)
        }

        override val reachableStates: Collection<QuestionState>
            get() = emptyList()

    }
    val automata = QuestioningStrategy.defaultFullBranchStrategy.buildAndFinalize(DomainModel.decisionTree, endState)

    println(GeneralQuestionState.stateCount)
    println()

    val input = Prompt(
        "Выберите используемые входные данные:",
        listOf("X + А / B * C + D / K   -   выбран первый + " to 1,
            "X + А / B * C + D / K   -   выбран * " to 2,
            "X * А ^ B + C + D / K   (где A ^ B уже вычислено)  -   выбран *" to 3,
            "А / B * C + D    -   выбран * " to 4,
            "Arr[B + C]   -   выбран [] " to 5,
            "A * (B * C)  -   выбран первый *" to 6,
            "(X + А) [ B + C * D ]  -   выбран второй +" to 7,)
    ).ask()
    println("Далее вопросы генерируются как для студента, выбравшего данный ответ в данной ситуации.\n\n-----")

    val situation = QuestioningSituation(dir + "_$input\\$input.ttl")
    situation.addAssumedResult(DomainModel.decisionTree.main, true)
    var state : QuestionState? = automata.initState
//    state = automata[94]
//    situation.addAssumedResult(DomainModel.decisionTree.getByAlias("right") as ThoughtBranch, true)
    while(state != null){
        val out = state.getQuestion(situation)
        lateinit var change : QuestionStateChange
        var printed = false
        when(out){
            is Question -> {
                val answers = out.ask()
                printed = true
                change = state.proceedWithAnswer(situation, answers)
            }
            is QuestionStateChange ->{
                change = out
            }
        }
        state = change.nextState
        if(change.explanation != null){
            println(change.explanation!!.text)
            printed = true
        }
        if(printed) println()
    }

    //val q = QuestionGenerator(dir + "_$input\\")
    //q.start(DomainModel.decisionTree, true)

}

fun main(args: Array<String>) {
    val useConsole = false || (args.size > 0 && args[0].equals("-c"))
    if(useConsole){
        run()
    }
    else{
        SwingUtilities.invokeLater {
            val c = ConsoleView()
            c.isVisible = true

            //Start other thread that will run Console.run()
            val mainProgram = Thread { run() }
            mainProgram.start()
        }
    }
}

class Prompt<Info>(val text: String, val options : List<Pair<String, Info>>){
    private val scanner = Scanner(System.`in`)

    private fun getAnswers(): List<Int>{
        print("Ваш ответ: ")
        while(true){
            try{
                val input = scanner.nextLine()
                return if(input.isBlank()) emptyList() else input.split(',').map{ str ->str.toInt()}
            }catch (e: NumberFormatException){
                print("Неверный формат ввода. Введите ответ заново: ")
            }
        }
    }

    fun ask(): Info {
        if(options.size == 1)
            return options[0].second
        println()
        println(text)
        println("(Элемент управления программой: укажите номер варианта для выбора дальнейших действий.)")
        options.forEachIndexed {i, option -> println(" ${i+1}. ${option.first}") }

        var answers = getAnswers()
        while(answers.size != 1 || answers.any { it-1 !in options.indices}){
            println("Укажите одно число для ответа")
            answers = getAnswers()
        }
        val answer = answers.single()
        return options[answer-1].second
    }
}