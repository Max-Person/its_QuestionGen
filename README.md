its_QuestionGen  - компонент системы, служащий для построения структуры наводящих вопросов на основе деревьев решений, а также для реализации текстового взаимодействия с пользователем на основе этой структуры  
## Функционал в этом модуле  
- Построение структуры (последовательностей) наводящих вопросов на основе графа мыслительных процессов.  
- Генерация текстовых формулировок наводящих вопросов (а также ответов и объяснений для них) для данной структуры в конкретной ситуации.  
	- Введение выражений в стороннюю систему текстовых шаблонов (см. [JavaStringTemplating](https://github.com/Max-Person/JavaStringTemplating)) для построения формулировок на основе данных о предметной области.  
	- Динамическое построение формулировок на основе структуры выражений в конкретных узлах ГМП.

**Подробнее о функционале этого и других модулей читайте на [вики](https://max-person.github.io/Compprehensive_ITS_wiki/3.-%D0%BD%D0%B0%D0%B2%D0%BE%D0%B4%D1%8F%D1%89%D0%B8%D0%B5-%D0%B2%D0%BE%D0%BF%D1%80%D0%BE%D1%81%D1%8B-(its_questiongen)/%D0%BE%D0%B1-its_questiongen.html).**

## Установка зависимостей

Как автор данных проектов, **я рекомендую использовать Maven + [JitPack](https://jitpack.io/)  для подключения проектов its_\* как зависимостей в ваших собственных проектах**.
Для этого необходимо:

1\. В pom.xml своего проекта указать репозиторий JitPack:
```xml
<repositories>
	<repository>
		<id>jitpack.io</id>
		<url>https://jitpack.io</url>
	</repository>
</repositories>
```
2\. Также в pom.xml указать репозиторий как зависимость:
```xml
<dependency>
	<groupId>com.github.Max-Person</groupId>
	<artifactId>its_QuestionGen</artifactId>
	<version>...</version>
</dependency>
```
- В качестве версии JitPack может принимать название ветки, тег (release tag), или хэш коммита. Для данных проектов я рекомендую указывать тег последней версии (см. [tags](https://github.com/Max-Person/its_QuestionGen/tags)), чтобы ваш проект не сломался с обновлением библиотек.


3\. В IntelliJ IDEA надо обновить зависимости Maven (Maven -> Reload All Maven Projects), и все, данный проект настроен для использования в качестве библиотеки.
> [!note]
Обратите внимание, что JitPack собирает нужные артефакты только по запросу - т.е. когда вы подтягиваете зависимость. Это значит, что первое подобное подтягивание скорее всего займет несколько минут - JitPack-у нужно будет время на билд.  
После завершения такого долгого билда, в IDEA может отобразиться надпись "Couldn't aqcuire locks", как будто произошла ошибка - в этом случае просто обновитесь еще раз, это будет быстро.

4\. Вместе с артефактами данной библиотеки всегда доступен ее исходный код, а в нем и документация (kotlindoc/javadoc). **Проект на 90% задокументирован, поэтому смотрите на документацию к используемым вами методам!**  
Для того, чтобы исходный код и документация тоже подтянулись, нужно в IntelliJ IDEA сделать Maven -> Download Sources and/or Documentation -> Download Sources and Documentation

## Примеры использования  
Примеры использования описаны на Java, т.к. я думаю, что вы с большей вероятностью будете использовать именно ее (использование на Kotlin в принципе аналогично, и более просто).
  
Данные примеры также полагаются на код из its_DomainModel, подробнее см. их примеры использования.  
#### Построение автомата наводящих вопросов  
```java  
DomainSolvingModel model = ... ;
    
QuestionAutomata questionAutomata = FullBranchStrategy.INSTANCE.buildAndFinalize(  
    model.getDecisionTree().getMainBranch(),  
    new EndQuestionState()  
);
```  
#### Построение диалоговой ситуации  
```java
DomainModel situationModel = ... ;

String localizationCode = "RU";
QuestioningSituation questioningSituation = new QuestioningSituation(situationModel, localizationCode)
```  
#### Ведение диалога в конкретной ситуации  
```java
QuestionAutomata questionAutomata = ... ;
QuestioningSituation = ... ;

QuestionState currentState = questionAutomata.getInitState();  
while (currentState != null) {  
    QuestionStateResult stateResult = currentState.getQuestion(questioningSituation);  
    //Если переход к следующему вопросу  
    if(stateResult instanceof QuestionStateChange stateChange){  
        //<Как-то обрабатываем объяснение о переходе к след. вопросу>  
        handleExplanation(stateChange.getExplanation());  
        currentState = stateChange.getNextState();  
    }  
    //Иначе если вопрос  
    else if (stateResult instanceof Question question) {  
        //<Как-то получаем ответы пользователя на вопрос>  
        //например, question.ask()List<Integer> answers = handleQuestion(question);  
        QuestionStateChange stateChange = currentState.proceedWithAnswer(questioningSituation, answers);  
        //<Как-то обрабатываем объяснение о переходе к след. вопросу>  
        handleExplanation(stateChange.getExplanation());  
        currentState = stateChange.getNextState();  
    }  
}
```


