package cl.picker.sorter

class CategoryList(text: String) {

    class Cat(var name: String, val keywords: MutableList<String>)

    val cats = ArrayList<Cat>()

    init {
        var cur: Cat? = null
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            if (line.startsWith("[") && line.endsWith("]")) {
                cur = Cat(line.substring(1, line.length - 1).trim(), ArrayList())
                cats.add(cur)
            } else {
                cur?.keywords?.addAll(splitKeywords(line))
            }
        }
    }

    fun serialize(): String = buildString {
        append("# Categorías de Mini Picker, en el orden del recorrido.\n")
        append("# [Categoría] y debajo sus palabras separadas por coma. Un * al final = \"empieza con\".\n")
        for (c in cats) {
            append("\n[").append(c.name).append("]\n")
            append(c.keywords.joinToString(", ")).append('\n')
        }
    }

    companion object {
        private val STOP = setOf("de", "del", "con", "sin", "para", "la", "el", "los", "las", "en", "y", "al", "pack", "un", "una")

        fun splitKeywords(s: String): List<String> =
            s.split(',', '\n').map { it.trim() }.filter { it.isNotEmpty() }

        fun cleanName(s: String) = s.replace("[", "").replace("]", "").trim()

        fun suggestKeyword(product: String): String {
            val words = Classifier.normalize(product).split(' ')
            return words.firstOrNull { w -> w.length >= 3 && w !in STOP && w.any { it.isLetter() } }
                ?: Classifier.normalize(product)
        }
    }
}
