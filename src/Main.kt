import its.model.DomainModel
import its.questions.gen.QuestionGenerator
import its.questions.inputs.*
import its.questions.questiontypes.Prompt
import javax.swing.SwingUtilities


fun run() {
    val dir = "inputs\\"

    DomainModel.collect(
        QClassesDictionary(),
        QVarsDictionary(),
        QEnumsDictionary(),
        QPropertiesDictionary(),
        QRelationshipsDictionary(),
    ).initFrom(dir)

    val input = Prompt(
        "Выберите используемые входные данные:",
        listOf("X + А / B * C + D / K   -   выбран первый + " to 1,
            "X + А / B * C + D / K   -   выбран * " to 2,
            "X ^ А / B * C + D / K   (где A / B уже вычислено)  -   выбран *" to 3,
            "А / B * C + D    -   выбран * " to 4,
            "Arr[B + C]   -   выбран [] " to 5,
            "A * (B * C)  -   выбран первый *" to 6,)
    ).ask()
    println("Далее вопросы генерируются как для студента, выбравшего данный ответ в данной ситуации.\n\n-----")

    val q = QuestionGenerator(dir + "_$input\\")
    q.start(DomainModel.decisionTree, true)
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