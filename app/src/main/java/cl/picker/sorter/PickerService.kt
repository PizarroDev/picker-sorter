package cl.picker.sorter

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class PickerService : AccessibilityService() {

    companion object {
        var instance: PickerService? = null
            private set
        val recent = ArrayList<String>()
        var lastNodes: List<Node> = emptyList()
            private set
    }

    private val handler = Handler(Looper.getMainLooper())
    private val scanTask = Runnable { scanNow() }
    private var scanPending = false
    private val spaces = Regex("\\s+")

    private var overlay: Overlay? = null
    private var target = ""
    private var viewId = ""
    private var sig = ""
    private var autoLearn = true
    private var ignored: MutableSet<String> = HashSet()
    lateinit var classifier: Classifier
        private set

    private var stats = WordStats()
    private val items = LinkedHashMap<String, Boolean>()
    private val catCache = HashMap<String, Int>()

    override fun onServiceConnected() {
        instance = this
        stats = Store.learned(this)
        overlay = Overlay(this)
        reload()
    }

    fun reload() {
        target = Store.target(this)
        viewId = Store.viewId(this)
        sig = Store.sig(this)
        autoLearn = Store.autoLearn(this)
        ignored = Store.ignored(this)
        classifier = Store.classifier(this, stats)
        catCache.clear()
        items.keys.removeAll(ignored)
        val active = rootInActiveWindow?.packageName?.toString()
        overlay?.setVisible(target.isNotEmpty() && active == target)
        overlay?.refresh()
    }

    override fun onAccessibilityEvent(e: AccessibilityEvent) {
        val pkg = e.packageName?.toString() ?: return

        if (e.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val active = rootInActiveWindow?.packageName?.toString()
            if (active == null) {
                // sin ventana activa legible: no cambiar nada
            } else if (active == packageName) {
                overlay?.setVisible(false)
            } else if (active != "com.android.systemui") {
                recent.remove(active)
                recent.add(0, active)
                while (recent.size > 6) recent.removeAt(recent.size - 1)
                overlay?.setVisible(target.isNotEmpty() && active == target)
            }
        }

        if (pkg == packageName || target.isEmpty() || pkg != target) return
        if (!scanPending) {
            scanPending = true
            handler.postDelayed(scanTask, 150)
        }
    }

    private fun scanNow() {
        scanPending = false
        val roots = try {
            windows.mapNotNull { it.root }.filter { it.packageName == target }
        } catch (_: Exception) {
            emptyList()
        }.ifEmpty { listOfNotNull(rootInActiveWindow?.takeIf { it.packageName == target }) }
        if (roots.isEmpty()) return

        var nodes = ArrayList<Node>()
        for (r in roots) walk(r, 0, nodes)

        if (viewId.isEmpty() && sig.isEmpty() && autoLearn) {
            val learned = Learner.best(nodes, classifier)
            if (learned != null) {
                sig = learned
                Store.setSig(this, learned)
                nodes = ArrayList(nodes.map { Node(it.id, it.sig, it.text, isProduct(it.text, it.id, it.sig)) })
            }
        }
        lastNodes = if (nodes.size > 2000) nodes.subList(0, 2000) else nodes

        var changed = false
        for (n in nodes) {
            val t = n.text
            if (n.product && !items.containsKey(t) && t !in ignored) {
                items[t] = false
                changed = true
                val (cat, source) = classifier.classifyWithSource(t)
                if (source == Classifier.EXACT) stats.learn(t, classifier.categories[cat].name)
            }
        }
        if (stats.dirty) {
            Store.saveLearned(this, stats)
            stats.dirty = false
            catCache.clear()
        }
        if (changed) overlay?.refresh()
    }

    private fun walk(n: AccessibilityNodeInfo?, depth: Int, out: MutableList<Node>) {
        if (n == null || depth > 150 || out.size > 6000) return
        val raw = n.text ?: n.contentDescription
        if (raw != null) {
            val t = raw.toString().replace(spaces, " ").trim()
            if (t.isNotEmpty()) {
                val id = n.viewIdResourceName
                val s = Learner.signature(id, n.className, depth)
                out.add(Node(id, s, t, isProduct(t, id, s)))
            }
        }
        for (i in 0 until n.childCount) walk(n.getChild(i), depth + 1, out)
    }

    private fun isProduct(t: String, id: String?, s: String): Boolean {
        if (viewId.isNotEmpty()) {
            return id != null && (id == viewId || id.endsWith(":id/$viewId"))
        }
        if (!Learner.looksLikeText(t)) return false
        if (sig.isNotEmpty() && s == sig) return true
        return categoryOf(t) >= 0
    }

    fun preciseMode() = viewId.isNotEmpty() || sig.isNotEmpty()

    private fun categoryOf(name: String): Int = catCache.getOrPut(name) { classifier.classify(name) }

    class Group(val name: String, val index: Int, val items: List<String>)

    fun grouped(): List<Group> {
        val cats = classifier.categories
        val buckets = Array(cats.size + 1) { ArrayList<String>() }
        for (name in items.keys) {
            val i = categoryOf(name)
            buckets[if (i < 0) cats.size else i].add(name)
        }
        return buckets.mapIndexedNotNull { i, b ->
            if (b.isEmpty()) null
            else Group(
                if (i < cats.size) cats[i].name else "Sin categoría",
                if (i < cats.size) i else -1,
                b.sortedWith(compareBy<String>({ items[it] == true }, { it.lowercase() }))
            )
        }
    }

    fun totalCount() = items.size

    fun uncategorized(): List<String> = items.keys.filter { categoryOf(it) < 0 }

    fun isPicked(name: String) = items[name] == true

    fun togglePicked(name: String) {
        items[name] = !(items[name] ?: false)
        overlay?.refresh()
    }

    fun ignore(name: String) {
        items.remove(name)
        ignored.add(name)
        Store.addIgnored(this, name)
        overlay?.refresh()
    }

    fun clearItems() {
        items.clear()
        catCache.clear()
        overlay?.refresh()
        handler.removeCallbacks(scanTask)
        scanPending = true
        handler.postDelayed(scanTask, 100)
    }

    fun pendingCount() = items.values.count { !it }

    fun learnedCount() = stats.size

    fun forgetLearned() {
        stats.clear()
        Store.saveLearned(this, stats)
        stats.dirty = false
        catCache.clear()
        overlay?.refresh()
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        overlay?.setVisible(false)
        overlay = null
        instance = null
        super.onDestroy()
    }
}
