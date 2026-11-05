package cl.picker.sorter

import java.text.Normalizer

class Classifier(text: String) {

    class Category(val name: String, val keywords: List<String>)

    val categories: List<Category>

    init {
        val cats = ArrayList<Category>()
        var name: String? = null
        val kws = ArrayList<String>()
        fun flush() {
            name?.let { cats.add(Category(it, kws.toList())) }
            kws.clear()
        }
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            if (line.startsWith("[") && line.endsWith("]")) {
                flush()
                name = line.substring(1, line.length - 1).trim()
            } else if (name != null) {
                for (k in line.split(',')) {
                    val n = normalize(k)
                    if (n.isNotEmpty()) kws.add(n)
                }
            }
        }
        flush()
        categories = cats
    }

    fun classify(product: String): Int {
        val p = " " + normalize(product) + " "
        var best = -1
        var bestPos = Int.MAX_VALUE
        var bestLen = 0
        for (i in categories.indices) {
            for (kw in categories[i].keywords) {
                val pos = find(p, kw)
                if (pos < 0) continue
                if (pos < bestPos || (pos == bestPos && kw.length > bestLen)) {
                    best = i
                    bestPos = pos
                    bestLen = kw.length
                }
            }
        }
        return best
    }

    private fun find(p: String, kw: String): Int {
        if (kw.endsWith("*")) return p.indexOf(" " + kw.dropLast(1))
        for (suf in SUFFIXES) {
            val i = p.indexOf(" $kw$suf ")
            if (i >= 0) return i
        }
        return -1
    }

    companion object {
        private val SUFFIXES = arrayOf("", "s", "es")
        private val MARKS = Regex("\\p{M}+")
        private val OTHER = Regex("[^a-z0-9* ]")
        private val SPACES = Regex("\\s+")

        fun normalize(s: String): String {
            val n = Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(MARKS, "")
            return n.replace(OTHER, " ").replace(SPACES, " ").trim()
        }
    }
}
