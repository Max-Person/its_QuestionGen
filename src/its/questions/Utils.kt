package its.questions

import kotlin.io.path.Path

internal fun fileToMap(filePath : String, delimiter: Char): Map<String, String> {
    val data = mutableMapOf<String, String>()

    Path(filePath).toFile().reader().use { reader ->
        reader.forEachLine { line ->
            val trim = line.trim()
            if(trim.isNotBlank() && !trim.startsWith("//")){
                val (key, value) = trim.split(delimiter)
                data[key] = value
            }
        }
    }

    return data
}

internal fun <T> MutableCollection<T>.addAllNew(o : Collection<T>){
    o.forEach { if(!this.contains(it)) this.add(it) }
}