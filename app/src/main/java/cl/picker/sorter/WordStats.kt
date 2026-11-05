package cl.picker.sorter

class WordStats {

    private val map = HashMap<String, HashMap<String, Int>>()
    var dirty = false

    val size: Int get() = map.size

    fun learn(product: String, category: String) {
        for (w in words(product)) {
            val m = map.getOrPut(w) { HashMap() }
            m[category] = (m[category] ?: 0) + 1
            dirty = true
        }
    }

    fun guess(words: List<String>): String? {
        val votes = HashMap<String, Double>()
        for (w in words) {
            val m = map[w] ?: continue
            val total = m.values.sum()
            val top = m.maxByOrNull { it.value } ?: continue
            if (total >= 2 && top.value * 4 >= total * 3) {
                votes[top.key] = (votes[top.key] ?: 0.0) + top.value.toDouble() / total
            }
        }
        return votes.maxByOrNull { it.value }?.key
    }

    fun clear() {
        map.clear()
        dirty = true
    }

    fun serialize(): String = buildString {
        for ((w, m) in map) for ((c, n) in m) append(w).append('\t').append(c).append('\t').append(n).append('\n')
    }

    companion object {
        private val UNIT = Regex("^[0-9]+[a-z]{0,4}$")
        private val STOP = setOf(
            "de", "del", "con", "sin", "para", "por", "los", "las", "una", "uno", "unidad", "unidades",
            "und", "pack", "gr", "grs", "gramos", "kg", "kilo", "ml", "cc", "lt", "lts", "litro", "litros",
            "bolsa", "caja", "botella", "lata", "sobre", "frasco", "malla", "granel", "light", "natural",
            "original", "clasico", "clasica", "tradicional", "sabor", "variedad", "surtido", "tipo",
            "grande", "chico", "mediano", "familiar", "oferta", "nuevo", "nueva", "premium", "extra",
            "rojo", "verde", "blanco", "blanca", "negro", "azul",
        )

        fun useful(w: String) = w.length >= 3 && w !in STOP && !UNIT.matches(w) && w.any { it.isLetter() }

        fun words(product: String): List<String> =
            Classifier.normalize(product).split(' ').filter(::useful).distinct()

        fun parse(text: String): WordStats {
            val s = WordStats()
            for (line in text.lines()) {
                val p = line.split('\t')
                if (p.size != 3) continue
                val n = p[2].toIntOrNull() ?: continue
                s.map.getOrPut(p[0]) { HashMap() }[p[1]] = n
            }
            return s
        }
    }
}
