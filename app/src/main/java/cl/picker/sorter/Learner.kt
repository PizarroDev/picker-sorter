package cl.picker.sorter

class Node(val id: String?, val sig: String, val text: String, val product: Boolean)

object Learner {

    fun signature(id: String?, className: CharSequence?, depth: Int): String =
        if (id != null) "id:" + id.substringAfter(":id/") else "c:${className ?: "?"}@$depth"

    fun looksLikeText(t: String): Boolean =
        t.length in 3..160 && t.count { it.isLetter() } >= 3

    fun best(nodes: List<Node>, classifier: Classifier): String? {
        val total = HashMap<String, Int>()
        val matched = HashMap<String, Int>()
        var allMatched = 0
        for (n in nodes) {
            if (!looksLikeText(n.text)) continue
            total[n.sig] = (total[n.sig] ?: 0) + 1
            if (classifier.classify(n.text) >= 0) {
                matched[n.sig] = (matched[n.sig] ?: 0) + 1
                allMatched++
            }
        }
        val (sig, m) = matched.maxByOrNull { it.value } ?: return null
        val t = total[sig] ?: return null
        val enough = m >= 3
        val mostlyProducts = m * 10 >= t * 4
        val holdsMost = m * 10 >= allMatched * 6
        return if (enough && mostlyProducts && holdsMost) sig else null
    }
}
