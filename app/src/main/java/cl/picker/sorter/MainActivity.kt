package cl.picker.sorter

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.util.Collections

class MainActivity : Activity() {

    private lateinit var statusDot: View
    private lateinit var statusTitle: TextView
    private lateinit var statusSub: TextView
    private lateinit var statusAction: TextView
    private lateinit var stepsBox: LinearLayout
    private lateinit var catBox: LinearLayout
    private lateinit var autoBox: LinearLayout
    private lateinit var testInput: EditText
    private lateinit var testResult: LinearLayout
    private lateinit var uncatCard: LinearLayout
    private lateinit var uncatBox: LinearLayout
    private lateinit var troubleBox: LinearLayout

    private lateinit var catList: CategoryList
    private lateinit var classifier: Classifier
    private var detectMsg: Pair<Boolean, String>? = null
    private var readOpen = false
    private var advancedOpen = false

    private val act: MainActivity get() = this
    private val match = LinearLayout.LayoutParams.MATCH_PARENT
    private val wrap = LinearLayout.LayoutParams.WRAP_CONTENT
    private val dialogTheme = android.R.style.Theme_DeviceDefault_Light_Dialog_Alert
    private val warnSoft = 0xFFFEF3C7.toInt()

    private fun px(v: Number) = Ui.px(this, v)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Ui.PRIMARY
        loadCategories()

        val column = vbox().apply { setPadding(0, 0, 0, px(32)) }
        column.addView(header())
        val body = vbox().apply { setPadding(px(16), px(4), px(16), 0) }
        column.addView(body)

        card(body, "Primeros pasos", null).apply {
            stepsBox = vbox().also { addView(it, lp(6)) }
        }

        card(body, "Categorías",
            "En la burbuja los productos aparecen en este orden. Ordénalas igual que tu recorrido " +
                "por la tienda. Toca una para editarla.").apply {
            autoBox = vbox().also { addView(it, lp(14)) }
            catBox = vbox().also { addView(it, lp(14)) }
            addView(Ui.button(act, "＋  Nueva categoría", false) { editCategory(null) }, lp(12))
            addDivider(this, 20)
            addView(text("Probar un producto", 16f, Ui.TEXT, bold = true), lp(18))
            addView(text("Escribe el nombre de un producto y te digo en qué categoría queda.", 14f, Ui.MUTED), lp(4))
            testInput = Ui.input(act, "ej: Leche descremada 1L").apply {
                setSingleLine()
                addTextChangedListener(watcher { updateTest() })
            }
            addView(testInput, lp(10))
            testResult = vbox().also { addView(it, lp(0)) }
            addView(link("Restaurar las categorías originales") { confirmRestore() }, lp(16))
        }

        uncatCard = card(body, "Productos sin categoría",
            "Mini Picker no pudo reconocer estos productos todavía. Muchos se categorizan solos " +
                "a medida que aprende de tus pedidos, pero también puedes asignarlos tú.").apply {
            uncatBox = vbox().also { addView(it, lp(10)) }
        }

        card(body, "¿La burbuja no se ve bien?", null).apply {
            troubleBox = vbox().also { addView(it, lp(4)) }
        }

        column.addView(text("Mini Picker 1.4 · Funciona sin internet y no envía datos a ningún lado", 12f, Ui.MUTED).apply {
            gravity = Gravity.CENTER
            setPadding(px(24), px(24), px(24), 0)
        })

        setContentView(ScrollView(this).apply {
            setBackgroundColor(Ui.BG)
            isFillViewport = true
            addView(column)
        })
        renderCategories()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        classifier = Store.classifier(this)
        renderAuto()
        renderStatus()
        renderSteps()
        renderUncategorized()
        renderTrouble()
    }

    // ---------- Encabezado y estado ----------

    private fun header(): View {
        val h = vbox().apply {
            setPadding(px(20), px(18), px(20), px(22))
            background = Ui.gradient().apply {
                val r = px(28).toFloat()
                cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, r, r, r, r)
            }
        }
        val top = hbox()
        top.addView(ImageView(this).apply {
            setImageResource(R.drawable.ic_launcher_fg)
            background = Ui.rounded(Ui.alpha(Color.WHITE, 0x2A), px(16).toFloat())
        }, LinearLayout.LayoutParams(px(52), px(52)))
        val titles = vbox()
        titles.addView(text("Mini Picker", 24f, Color.WHITE, bold = true))
        titles.addView(text("Tus productos ordenados por categoría", 14f, Ui.alpha(Color.WHITE, 0xD0)))
        top.addView(titles, weighted().apply { marginStart = px(14) })
        h.addView(top)

        val status = hbox().apply {
            background = Ui.rounded(Ui.CARD, px(18).toFloat())
            setPadding(px(16), px(14), px(12), px(14))
        }
        statusDot = View(this)
        status.addView(statusDot, LinearLayout.LayoutParams(px(12), px(12)))
        val st = vbox()
        statusTitle = text("", 15f, Ui.TEXT, bold = true).also { st.addView(it) }
        statusSub = text("", 13f, Ui.MUTED).also { st.addView(it) }
        status.addView(st, weighted().apply { marginStart = px(12) })
        statusAction = Ui.button(this, "Activar", true) { openAccessibility() }.small()
        status.addView(statusAction)
        h.addView(status, lp(20))
        return h
    }

    private fun renderStatus() {
        val on = PickerService.instance != null
        val t = Store.target(this)
        statusDot.background = oval(if (on) Ui.OK else Ui.WARN)
        if (on) {
            statusTitle.text = "Servicio activo"
            statusSub.text = if (t.isEmpty()) "Falta elegir tu app de picking" else "Leyendo ${appLabel(t)}"
            statusAction.visibility = View.GONE
        } else {
            statusTitle.text = "Servicio apagado"
            statusSub.text = "Actívalo para ver la burbuja"
            statusAction.visibility = View.VISIBLE
        }
    }

    // ---------- Primeros pasos ----------

    private fun renderSteps() {
        stepsBox.removeAllViews()
        val on = PickerService.instance != null
        val t = Store.target(this)
        val captured = (PickerService.instance?.totalCount() ?: 0) > 0

        stepsBox.addView(step(1, on, "Activa el servicio",
            if (on) "Listo. Mini Picker ya puede leer tu app."
            else "Permite que Mini Picker lea la pantalla. Solo lee: no toca nada ni envía datos.",
            if (!on) Ui.button(this, "Activar servicio", true) { openAccessibility() }.small() else null))

        val apps = PickerService.recent.filter {
            it != packageName && packageManager.getLaunchIntentForPackage(it) != null
        }
        val sub2 = when {
            !on -> "Primero activa el servicio."
            t.isNotEmpty() -> "Elegida: ${appLabel(t)}." + if (apps.any { it != t }) " Toca otra para cambiarla." else ""
            apps.isEmpty() -> "Abre tu app de picking y vuelve aquí: aparecerá para elegirla."
            else -> "Toca tu app de picking:"
        }
        val chips = if (on && apps.isNotEmpty()) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            for (p in apps) row.addView(chip(appLabel(p), p == t) { chooseTarget(p) },
                LinearLayout.LayoutParams(wrap, wrap).apply { marginEnd = px(8) })
            HorizontalScrollView(this).apply {
                isHorizontalScrollBarEnabled = false
                addView(row)
            }
        } else null
        stepsBox.addView(step(2, t.isNotEmpty(), "Elige tu app de picking", sub2, chips), lp(16))

        stepsBox.addView(step(3, captured, "Abre un pedido y recórrelo",
            "Haz scroll hasta el final de la lista y toca la burbuja azul para ver todo ordenado.",
            if (t.isNotEmpty()) Ui.button(this, "Abrir ${appLabel(t)}", false) { launch(t) }.small() else null),
            lp(16))
    }

    private fun step(n: Int, done: Boolean, title: String, sub: String, extra: View?): View {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(text(if (done) "✓" else "$n", 14f, if (done) Color.WHITE else Ui.PRIMARY, bold = true).apply {
            gravity = Gravity.CENTER
            background = oval(if (done) Ui.OK else Ui.PRIMARY_SOFT)
        }, LinearLayout.LayoutParams(px(30), px(30)))
        val col = vbox()
        col.addView(text(title, 15f, if (done) Ui.MUTED else Ui.TEXT, bold = true))
        col.addView(text(sub, 13f, Ui.MUTED).spaced(), lp(2))
        if (extra != null) col.addView(extra, LinearLayout.LayoutParams(wrap, wrap).apply { topMargin = px(10) })
        row.addView(col, weighted().apply { marginStart = px(14); topMargin = px(4) })
        return row
    }

    private fun chooseTarget(p: String) {
        Store.setTarget(this, p)
        Store.setSig(this, "")
        Store.setAutoLearn(this, true)
        PickerService.instance?.clearItems()
        PickerService.instance?.reload()
        refresh()
        Toast.makeText(this, "Ahora Mini Picker leerá ${appLabel(p)}", Toast.LENGTH_SHORT).show()
    }

    // ---------- Categorías ----------

    private fun loadCategories() {
        catList = CategoryList(Store.categories(this))
        classifier = Store.classifier(this)
    }

    private fun persist() {
        Store.saveCategories(this, catList.serialize())
        classifier = Store.classifier(this)
        PickerService.instance?.reload()
        renderCategories()
        renderUncategorized()
        updateTest()
    }

    private fun renderAuto() {
        autoBox.removeAllViews()
        val learned = PickerService.instance?.learnedCount() ?: Store.learned(this).size
        autoBox.addView(notice(true, "Categorización automática",
            "Además de tus palabras, Mini Picker trae un diccionario con cientos de productos y marcas, " +
                "reconoce palabras parecidas (\"galletitas\" → galleta) y aprende solo de cada pedido " +
                "qué marcas van en cada categoría." +
                if (learned > 0) " Ya aprendió $learned palabras." else ""), lp(0))
        if (learned > 0) {
            autoBox.addView(link("Olvidar lo aprendido") {
                AlertDialog.Builder(this, dialogTheme)
                    .setTitle("¿Olvidar lo aprendido?")
                    .setMessage("Mini Picker volverá a aprender desde cero con tus próximos pedidos.")
                    .setPositiveButton("Olvidar") { _, _ ->
                        PickerService.instance?.forgetLearned() ?: Store.saveLearned(this, WordStats())
                        classifier = Store.classifier(this)
                        renderAuto()
                        renderUncategorized()
                        updateTest()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }, lp(6))
        }
    }

    private fun categoryName(i: Int) =
        if (i >= 0 && i < classifier.categories.size) classifier.categories[i].name else "Sin categoría"

    private fun renderCategories() {
        catBox.removeAllViews()
        if (catList.cats.isEmpty()) {
            catBox.addView(text("No hay categorías. Agrega una o restaura las originales.", 14f, Ui.MUTED))
            return
        }
        val last = catList.cats.size - 1
        catList.cats.forEachIndexed { i, c ->
            val color = Ui.categoryColor(i)
            val r = px(14).toFloat()
            val row = hbox().apply {
                background = Ui.ripple(Ui.rounded(Ui.SURFACE, r, px(1), Ui.BORDER), r)
                setPadding(px(12), px(10), px(4), px(10))
                isClickable = true
                setOnClickListener { editCategory(i) }
            }
            row.addView(text("${i + 1}", 13f, color, bold = true).apply {
                gravity = Gravity.CENTER
                background = oval(Ui.alpha(color, 0x24))
            }, LinearLayout.LayoutParams(px(30), px(30)))
            val col = vbox()
            col.addView(text(c.name, 15f, Ui.TEXT, bold = true))
            col.addView(text(
                if (c.keywords.isEmpty()) "Sin palabras: toca para agregar"
                else "${c.keywords.size} palabras · " + c.keywords.joinToString(", "),
                12f, Ui.MUTED
            ).apply {
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            })
            row.addView(col, weighted().apply { marginStart = px(12) })
            row.addView(arrow("↑", i > 0) { move(i, -1) })
            row.addView(arrow("↓", i < last) { move(i, 1) })
            catBox.addView(row, lp(if (i == 0) 0 else 8))
        }
    }

    private fun arrow(label: String, enabled: Boolean, onClick: () -> Unit) = text(label, 18f,
        if (enabled) Ui.PRIMARY else Ui.BORDER, bold = true).apply {
        gravity = Gravity.CENTER
        layoutParams = LinearLayout.LayoutParams(px(40), px(40))
        if (enabled) {
            val r = px(20).toFloat()
            background = Ui.ripple(Ui.rounded(Color.TRANSPARENT, r), r)
            isClickable = true
            setOnClickListener { onClick() }
        }
    }

    private fun move(i: Int, d: Int) {
        Collections.swap(catList.cats, i, i + d)
        persist()
    }

    private fun editCategory(index: Int?) {
        val cat = if (index == null) CategoryList.Cat("", ArrayList()) else catList.cats[index]
        val box = vbox().apply { setPadding(px(22), px(8), px(22), 0) }
        box.addView(text("Nombre de la categoría", 13f, Ui.MUTED, bold = true))
        val name = Ui.input(this, "ej: Lácteos").apply {
            setSingleLine()
            setText(cat.name)
        }
        box.addView(name, lp(6))
        box.addView(text("Palabras que la identifican", 13f, Ui.MUTED, bold = true), lp(16))
        box.addView(text("Si el nombre de un producto contiene alguna de estas palabras, irá a esta categoría. " +
            "Sepáralas con comas. No importan mayúsculas, tildes ni plurales.", 12f, Ui.MUTED).spaced(), lp(4))
        val kws = Ui.input(this, "ej: leche, yogur, queso").apply {
            gravity = Gravity.TOP
            minLines = 4
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setText(cat.keywords.joinToString(", "))
        }
        box.addView(kws, lp(6))

        val b = AlertDialog.Builder(this, dialogTheme)
            .setTitle(if (index == null) "Nueva categoría" else "Editar categoría")
            .setView(ScrollView(this).apply { addView(box) })
            .setPositiveButton("Guardar") { _, _ ->
                val n = CategoryList.cleanName(name.text.toString())
                if (n.isEmpty()) {
                    Toast.makeText(this, "Ponle un nombre a la categoría", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                cat.name = n
                cat.keywords.clear()
                cat.keywords.addAll(CategoryList.splitKeywords(kws.text.toString()))
                if (index == null) catList.cats.add(cat)
                persist()
            }
            .setNegativeButton("Cancelar", null)
        if (index != null) {
            b.setNeutralButton("Eliminar") { _, _ ->
                AlertDialog.Builder(this, dialogTheme)
                    .setTitle("¿Eliminar \"${cat.name}\"?")
                    .setMessage("Los productos de esta categoría pasarán a \"Sin categoría\".")
                    .setPositiveButton("Eliminar") { _, _ ->
                        catList.cats.removeAt(index)
                        persist()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        }
        b.show()
    }

    private fun confirmRestore() {
        AlertDialog.Builder(this, dialogTheme)
            .setTitle("¿Restaurar las categorías originales?")
            .setMessage("Se perderán los cambios que hiciste en las categorías.")
            .setPositiveButton("Restaurar") { _, _ ->
                catList = CategoryList(Store.defaultCategories(this))
                persist()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun updateTest() {
        testResult.removeAllViews()
        val p = testInput.text.toString().trim()
        if (p.isEmpty()) return
        val (i, source) = classifier.classifyWithSource(p)
        if (i >= 0) {
            val how = when (source) {
                Classifier.SIMILAR -> "Reconocido por una palabra parecida."
                Classifier.LEARNED -> "Reconocido por lo que aprendió de tus pedidos."
                else -> "Reconocido por palabra clave o por el diccionario."
            }
            testResult.addView(notice(true, "Va a: ${categoryName(i)}", how), lp(10))
        } else {
            testResult.addView(notice(false, "Sin categoría",
                "No lo reconozco todavía. Si aparece en tus pedidos junto a productos de la misma marca, " +
                    "lo aprenderé solo; o puedes asignarlo ahora."), lp(10))
            testResult.addView(Ui.button(this, "Asignarle una categoría", false) { assign(p) }.small(),
                LinearLayout.LayoutParams(wrap, wrap).apply { topMargin = px(8) })
        }
    }

    private fun assign(product: String) {
        if (catList.cats.isEmpty()) {
            Toast.makeText(this, "Primero crea una categoría", Toast.LENGTH_SHORT).show()
            return
        }
        val names = catList.cats.map { it.name }.toTypedArray()
        AlertDialog.Builder(this, dialogTheme)
            .setTitle("¿En qué categoría va \"$product\"?")
            .setItems(names) { _, which -> confirmKeyword(product, which) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmKeyword(product: String, which: Int) {
        val cat = catList.cats[which]
        val box = vbox().apply { setPadding(px(22), px(8), px(22), 0) }
        box.addView(text("Mini Picker usará esta palabra para reconocer este producto y otros parecidos:",
            14f, Ui.MUTED).spaced())
        val kw = Ui.input(this, "").apply {
            setSingleLine()
            setText(CategoryList.suggestKeyword(product))
        }
        box.addView(kw, lp(10))
        AlertDialog.Builder(this, dialogTheme)
            .setTitle("Agregar a ${cat.name}")
            .setView(box)
            .setPositiveButton("Agregar") { _, _ ->
                val k = kw.text.toString().trim().trim(',')
                if (k.isEmpty()) return@setPositiveButton
                if (cat.keywords.none { it.equals(k, ignoreCase = true) }) cat.keywords.add(k)
                persist()
                Toast.makeText(this, "Listo: \"$k\" ahora va a ${cat.name}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ---------- Sin categoría ----------

    private fun renderUncategorized() {
        uncatBox.removeAllViews()
        val list = PickerService.instance?.uncategorized() ?: emptyList()
        uncatCard.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
        list.take(30).forEachIndexed { i, p ->
            val row = hbox().apply { setPadding(0, px(6), 0, px(6)) }
            row.addView(text(p, 14f, Ui.TEXT), weighted())
            row.addView(Ui.button(this, "Asignar", false) { assign(p) }.small(),
                LinearLayout.LayoutParams(wrap, wrap).apply { marginStart = px(10) })
            if (i > 0) addDivider(uncatBox, 0)
            uncatBox.addView(row)
        }
    }

    // ---------- Problemas ----------

    private fun renderTrouble() {
        troubleBox.removeAllViews()
        val precise = Store.viewId(this).isNotEmpty() || Store.sig(this).isNotEmpty()
        val auto = Store.autoLearn(this)

        if (precise) {
            troubleBox.addView(notice(true, "Lectura precisa activada",
                "Mini Picker aprendió dónde están los nombres de producto en tu app: captura todos los " +
                    "productos, y los que no calzan con ninguna palabra aparecen en \"Sin categoría\"."), lp(4))
            troubleBox.addView(text("Si ves textos raros o faltan productos, vuelve a aprender con un pedido abierto.",
                14f, Ui.MUTED).spaced(), lp(12))
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            row.addView(Ui.button(this, "Volver a aprender", false) { relearn() }, weighted())
            row.addView(Ui.button(this, "Desactivar", false) {
                Store.setViewId(this, "")
                Store.setSig(this, "")
                Store.setAutoLearn(this, false)
                detectMsg = null
                PickerService.instance?.clearItems()
                PickerService.instance?.reload()
                refresh()
            }, weighted().apply { marginStart = px(10) })
            troubleBox.addView(row, lp(10))
        } else if (auto) {
            troubleBox.addView(notice(false, "Aprendiendo tu app…",
                "Abre un pedido con varios productos a la vista. Cuando Mini Picker reconozca dónde están " +
                    "los nombres, empezará a capturar todos los productos, incluso los que no tienen categoría."), lp(4))
            troubleBox.addView(text("Si ya tienes un pedido abierto, puedes intentarlo ahora:", 14f, Ui.MUTED).spaced(), lp(12))
            troubleBox.addView(Ui.button(this, "Detectar productos ahora", true) { detect() }, lp(10))
        } else {
            troubleBox.addView(text("La lectura precisa está desactivada: solo se capturan los productos que " +
                "calzan con alguna palabra clave. Si faltan productos, actívala.", 14f, Ui.MUTED).spaced(), lp(4))
            troubleBox.addView(Ui.button(this, "Activar lectura precisa", true) {
                Store.setAutoLearn(this, true)
                detect()
            }, lp(10))
        }
        detectMsg?.let { (ok, msg) ->
            troubleBox.addView(notice(ok, if (ok) "¡Listo!" else "No pude detectar los productos", msg), lp(12))
        }

        addDivider(troubleBox, 20)
        troubleBox.addView(Ui.button(this, if (readOpen) "Ocultar lo que se leyó" else "Ver qué está leyendo", false) {
            readOpen = !readOpen
            renderTrouble()
        }, lp(20))
        if (readOpen) troubleBox.addView(readView(), lp(10))

        val ignored = Store.ignored(this).size
        if (ignored > 0) {
            troubleBox.addView(link("Volver a mostrar $ignored texto(s) que ignoraste") {
                Store.clearIgnored(this)
                PickerService.instance?.reload()
                renderTrouble()
                Toast.makeText(this, "Listo", Toast.LENGTH_SHORT).show()
            }, lp(14))
        }

        troubleBox.addView(link(if (advancedOpen) "Opciones avanzadas  ▴" else "Opciones avanzadas  ▾") {
            advancedOpen = !advancedOpen
            renderTrouble()
        }, lp(14))
        if (advancedOpen) troubleBox.addView(advancedView(), lp(8))
    }

    private fun relearn() {
        Store.setViewId(this, "")
        Store.setSig(this, "")
        Store.setAutoLearn(this, true)
        detect()
    }

    private fun detect() {
        val nodes = PickerService.lastNodes
        val best = Learner.best(nodes, classifier)
        detectMsg = if (PickerService.instance == null) {
            false to "Primero activa el servicio (paso 1)."
        } else if (Store.target(this).isEmpty()) {
            false to "Primero elige tu app de picking (paso 2)."
        } else if (best == null) {
            PickerService.instance?.reload()
            false to "En la última pantalla leída no vi suficientes productos. Abre un pedido con varios " +
                "productos a la vista, vuelve aquí y toca el botón otra vez. También se aprende solo mientras recorres el pedido."
        } else {
            Store.setSig(this, best)
            PickerService.instance?.clearItems()
            PickerService.instance?.reload()
            val count = nodes.count { it.sig == best && Learner.looksLikeText(it.text) }
            true to "Encontré $count nombres de producto en la última pantalla. " +
                "Vuelve a tu pedido, toca Limpiar en la burbuja y recórrelo de nuevo."
        }
        refresh()
    }

    private fun readView(): View {
        val box = vbox().apply {
            background = Ui.rounded(Ui.SURFACE, px(14).toFloat(), px(1), Ui.BORDER)
            setPadding(px(14), px(12), px(14), px(12))
        }
        val nodes = PickerService.lastNodes.distinctBy { it.text }
        if (nodes.isEmpty()) {
            box.addView(text("Todavía no se ha leído nada. Abre un pedido en tu app de picking y vuelve aquí.",
                13f, Ui.MUTED).spaced())
            return box
        }
        val products = nodes.count { it.product }
        box.addView(text("En la última pantalla se leyeron ${nodes.size} textos. " +
            "$products se reconocieron como productos.", 13f, Ui.TEXT, bold = true).spaced())
        for (n in nodes.take(80)) {
            val row = hbox().apply { setPadding(0, px(7), 0, 0) }
            row.addView(text(if (n.product) "✓" else "·", 14f, if (n.product) Ui.OK else Ui.MUTED, bold = true),
                LinearLayout.LayoutParams(px(20), wrap))
            row.addView(text(n.text, 13f, if (n.product) Ui.TEXT else Ui.MUTED).apply {
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
            }, weighted())
            if (n.product) {
                val i = classifier.classify(n.text)
                val color = Ui.categoryColor(i)
                row.addView(Ui.pill(this, categoryName(i),
                    color, Ui.alpha(color, 0x22)), LinearLayout.LayoutParams(wrap, wrap).apply { marginStart = px(8) })
            }
            box.addView(row)
        }
        if (nodes.size > 80) box.addView(text("… y ${nodes.size - 80} más", 12f, Ui.MUTED), lp(6))
        return box
    }

    private fun advancedView(): View {
        val box = vbox().apply {
            background = Ui.rounded(Ui.SURFACE, px(14).toFloat(), px(1), Ui.BORDER)
            setPadding(px(14), px(12), px(14), px(14))
        }
        box.addView(text("Solo si sabes lo que haces. Normalmente no necesitas tocar esto.", 13f, Ui.MUTED).spaced())
        box.addView(text("Paquete de la app de picking", 13f, Ui.TEXT, bold = true), lp(12))
        val target = Ui.input(this, "ej: com.empresa.picker").apply {
            setSingleLine()
            setText(Store.target(act))
        }
        box.addView(target, lp(6))
        box.addView(text("Identificador del nombre de producto", 13f, Ui.TEXT, bold = true), lp(12))
        val vid = Ui.input(this, "vacío = usar palabras clave").apply {
            setSingleLine()
            setText(Store.viewId(act))
        }
        box.addView(vid, lp(6))
        box.addView(Ui.button(this, "Guardar", true) {
            Store.setTarget(this, target.text.toString().trim())
            Store.setViewId(this, vid.text.toString().trim())
            PickerService.instance?.reload()
            refresh()
            Toast.makeText(this, "Guardado ✓", Toast.LENGTH_SHORT).show()
        }.small(), LinearLayout.LayoutParams(wrap, wrap).apply { topMargin = px(12) })
        return box
    }

    // ---------- Utilidades ----------

    private fun card(parent: LinearLayout, title: String, desc: String?): LinearLayout {
        val card = vbox().apply {
            background = Ui.rounded(Ui.CARD, px(20).toFloat(), px(1), Ui.BORDER)
            setPadding(px(18), px(18), px(18), px(18))
        }
        parent.addView(card, lp(16))
        card.addView(text(title, 18f, Ui.TEXT, bold = true))
        if (desc != null) card.addView(text(desc, 14f, Ui.MUTED).spaced(), lp(6))
        return card
    }

    private fun notice(ok: Boolean, title: String, msg: String): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = Ui.rounded(if (ok) Ui.OK_SOFT else warnSoft, px(14).toFloat())
            setPadding(px(12), px(12), px(14), px(12))
        }
        row.addView(text(if (ok) "✓" else "!", 13f, Color.WHITE, bold = true).apply {
            gravity = Gravity.CENTER
            background = oval(if (ok) Ui.OK else Ui.WARN)
        }, LinearLayout.LayoutParams(px(22), px(22)))
        val col = vbox()
        col.addView(text(title, 14f, Ui.TEXT, bold = true))
        col.addView(text(msg, 13f, 0xFF4B5563.toInt()).spaced(), lp(2))
        row.addView(col, weighted().apply { marginStart = px(10) })
        return row
    }

    private fun chip(label: String, selected: Boolean, onClick: () -> Unit) = text(label, 14f,
        if (selected) Color.WHITE else Ui.PRIMARY, bold = true).apply {
        val r = px(50).toFloat()
        background = Ui.ripple(Ui.rounded(if (selected) Ui.PRIMARY else Ui.PRIMARY_SOFT, r), r)
        setPadding(px(14), px(8), px(14), px(8))
        isClickable = true
        setOnClickListener { onClick() }
    }

    private fun link(label: String, onClick: () -> Unit) = text(label, 14f, Ui.PRIMARY, bold = true).apply {
        setPadding(0, px(6), 0, px(6))
        isClickable = true
        setOnClickListener { onClick() }
    }

    private fun addDivider(parent: LinearLayout, top: Int) {
        parent.addView(View(this).apply { setBackgroundColor(Ui.BORDER) },
            LinearLayout.LayoutParams(match, px(1)).apply { topMargin = px(top) })
    }

    private fun appLabel(p: String): String = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(p, 0)).toString()
    } catch (_: Exception) {
        p
    }

    private fun launch(p: String) {
        val i = packageManager.getLaunchIntentForPackage(p)
        if (i != null) startActivity(i)
        else Toast.makeText(this, "No pude abrir esa app", Toast.LENGTH_SHORT).show()
    }

    private fun openAccessibility() = startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

    private fun oval(color: Int) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
    }

    private fun vbox() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

    private fun hbox() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    private fun text(t: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = t
        textSize = size
        setTextColor(color)
        if (bold) setTypeface(null, Typeface.BOLD)
    }

    private fun TextView.spaced() = apply { setLineSpacing(0f, 1.15f) }

    private fun TextView.small() = apply {
        textSize = 14f
        setPadding(px(16), px(9), px(16), px(9))
    }

    private fun lp(top: Int = 0) = LinearLayout.LayoutParams(match, LinearLayout.LayoutParams.WRAP_CONTENT)
        .apply { topMargin = px(top) }

    private fun weighted() = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

    private fun watcher(f: () -> Unit) = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) = f()
        override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
    }
}
