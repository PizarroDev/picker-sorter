package cl.picker.sorter

import java.text.Normalizer

class Classifier(text: String, dictionary: String = "", private val learned: WordStats? = null) {

    class Category(val name: String, val keywords: List<String>)

    val categories: List<Category>
    private val stems: List<List<String>>
    private val byName = HashMap<String, Int>()

    init {
        val names = ArrayList<String>()
        val kws = ArrayList<MutableList<String>>()
        for ((n, k) in parse(text)) {
            val key = normalize(n)
            val i = byName[key] ?: names.size.also {
                byName[key] = it
                names.add(n)
                kws.add(ArrayList())
            }
            kws[i].addAll(k)
        }
        for ((n, k) in parse(dictionary)) {
            val key = normalize(n)
            val i = byName[key] ?: names.size.also {
                byName[key] = it
                names.add(n)
                kws.add(ArrayList())
            }
            kws[i].addAll(k)
        }
        categories = names.indices.map { Category(names[it], kws[it].distinct()) }
        stems = categories.map { c ->
            c.keywords.filter { !it.endsWith("*") && !it.contains(' ') && it.length >= 6 }
                .map { it.dropLast(1) }
                .distinct()
        }
    }

    fun classify(product: String): Int = classifyWithSource(product).first

    fun classifyWithSource(product: String): Pair<Int, Int> {
        val p = " " + normalize(product) + " "
        exact(p).let { if (it >= 0) return it to EXACT }
        similar(p).let { if (it >= 0) return it to SIMILAR }
        val guess = learned?.guess(WordStats.words(product))
        if (guess != null) byName[normalize(guess)]?.let { return it to LEARNED }
        return -1 to NONE
    }

    fun indexOf(name: String): Int = byName[normalize(name)] ?: -1

    private fun exact(p: String): Int {
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

    private fun similar(p: String): Int {
        var best = -1
        var bestPos = Int.MAX_VALUE
        var bestLen = 0
        for (i in stems.indices) {
            for (s in stems[i]) {
                val pos = p.indexOf(" $s")
                if (pos < 0) continue
                if (pos < bestPos || (pos == bestPos && s.length > bestLen)) {
                    best = i
                    bestPos = pos
                    bestLen = s.length
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
        const val NONE = 0
        const val EXACT = 1
        const val SIMILAR = 2
        const val LEARNED = 3

        private val SUFFIXES = arrayOf("", "s", "es")
        private val MARKS = Regex("\\p{M}+")
        private val OTHER = Regex("[^a-z0-9* ]")
        private val SPACES = Regex("\\s+")

        fun normalize(s: String): String {
            val n = Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(MARKS, "")
            return n.replace(OTHER, " ").replace(SPACES, " ").trim()
        }

        private fun parse(text: String): List<Pair<String, List<String>>> {
            val out = ArrayList<Pair<String, MutableList<String>>>()
            for (raw in text.lines()) {
                val line = raw.trim()
                if (line.isEmpty() || line.startsWith("#")) continue
                if (line.startsWith("[") && line.endsWith("]")) {
                    out.add(line.substring(1, line.length - 1).trim() to ArrayList())
                } else if (out.isNotEmpty()) {
                    for (k in line.split(',')) {
                        val n = normalize(k)
                        if (n.isNotEmpty()) out.last().second.add(n)
                    }
                }
            }
            return out
        }
    }
}
