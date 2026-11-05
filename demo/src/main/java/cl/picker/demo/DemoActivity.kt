package cl.picker.demo

import android.app.Activity
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import kotlin.random.Random

class DemoActivity : Activity() {

    private val green = 0xFF0E9F6E.toInt()
    private val greenDark = 0xFF046C4E.toInt()
    private val greenSoft = 0xFFDEF7EC.toInt()
    private val bg = 0xFFF2F5F3.toInt()
    private val ink = 0xFF1A1D23.toInt()
    private val muted = 0xFF6B7280.toInt()
    private val border = 0xFFE3E7EE.toInt()
    private val amber = 0xFFB45309.toInt()

    private class Item(val name: String, val qty: Int, val sku: String, val aisle: Int, val note: String?)

    private val catalogo = listOf(
        "Leche Entera Colun 1L", "Leche Descremada Soprole 1L", "Yoghurt Soprole Frutilla 155g", "Yoghurt griego natural 1kg",
        "Mantequilla con sal 250g", "Huevos blancos 12 un", "Crema de leche 200ml", "Manjar Nestlé 400g",
        "Queso Gouda laminado 250g", "Queso mantecoso 500g", "Jamón de pavo 200g", "Vienesas de pollo 20 un", "Salame ahumado 100g",
        "Pan de molde blanco Ideal", "Marraqueta granel", "Hallulla granel", "Tortillas de trigo 10 un",
        "Pechuga de pollo deshuesada 1kg", "Trutro entero de pollo", "Carne molida 10% grasa 500g", "Chuleta de cerdo vetada", "Lomo vetado",
        "Salmón filete congelado", "Merluza austral filete", "Helado Savory chocolate 1L", "Papas fritas congeladas McCain 1kg", "Nuggets de pollo 500g",
        "Arroz grado 1 Tucapel 1kg", "Fideos spaghetti Carozzi 400g", "Aceite maravilla Belmont 1L", "Atún en agua Van Camps",
        "Salsa de tomate Pomarola 200g", "Azúcar Iansa 1kg", "Harina sin polvos 1kg", "Caldo de pollo Maggi", "Lentejas 1kg", "Mayonesa Hellmann's 400g",
        "Café Nescafé Tradición 170g", "Té Supremo 100 bolsitas", "Avena tradicional Quaker 900g", "Cereal Chocapic 400g",
        "Galletas Tritón vainilla", "Chocolate Sahne-Nuss 160g", "Alfajor Costa", "Gomitas Ambrosoli",
        "Ramitas Evercrisp queso", "Maní salado 200g", "Papas Lays corte americano",
        "Coca Cola Original 3L", "Jugo Watts durazno 1.5L", "Agua mineral Cachantun sin gas 1.6L", "Bebida Fanta 2L",
        "Cerveza Cristal lata 470cc", "Vino tinto Casillero del Diablo",
        "Detergente líquido Omo 3L", "Cloro Clorinda 1L", "Lavaloza Quix limón 750ml", "Esponja doble uso", "Suavizante Fuzol 3L",
        "Papel higiénico Confort 12 rollos", "Toalla de papel Nova 3 un", "Servilletas 100 un",
        "Shampoo Head & Shoulders 375ml", "Pasta dental Colgate 90g", "Desodorante Rexona spray", "Jabón Dove 3 un",
        "Pañales Babysec G 40 un", "Toallitas húmedas 80 un", "Alimento perro adulto Master Dog 3kg", "Arena sanitaria 4kg",
        "Plátano granel", "Manzana Fuji granel", "Tomate larga vida", "Palta Hass malla 1kg", "Lechuga escarola", "Cebolla malla 1kg", "Limón granel",
    )

    private val sinCategoria = listOf(
        "Kombucha Biota jengibre", "Tofu firme 400g", "Pilas AA Duracell 4 un", "Velas aromáticas vainilla",
    )

    private val notas = listOf(
        "Sustituir por similar si no hay", "Cliente prefiere fecha larga", "Revisar que no venga abollado",
    )

    private var size = 25
    private var pedido: List<Item> = emptyList()
    private val picked = HashSet<Int>()
    private val adapter = Adapter()
    private lateinit var titulo: TextView
    private lateinit var progreso: TextView
    private lateinit var sizeRow: LinearLayout
    private lateinit var list: ListView

    private fun px(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun rounded(c: Int, r: Int, sw: Int = 0, sc: Int = 0) = GradientDrawable().apply {
        setColor(c)
        cornerRadius = px(r).toFloat()
        if (sw > 0) setStroke(px(sw), sc)
    }

    private fun text(t: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = t
        textSize = size
        setTextColor(color)
        if (bold) setTypeface(null, Typeface.BOLD)
    }

    private fun button(label: String, filled: Boolean, onClick: () -> Unit) = text(label, 14f,
        if (filled) Color.WHITE else green, bold = true).apply {
        gravity = Gravity.CENTER
        background = if (filled) rounded(green, 12) else rounded(Color.WHITE, 12, 1, border)
        setPadding(px(14), px(9), px(14), px(9))
        isClickable = true
        setOnClickListener { onClick() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = greenDark

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }

        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(green, greenDark))
            setPadding(px(18), px(14), px(18), px(16))
        }
        bar.addView(text("Picker Demo", 20f, Color.WHITE, bold = true))
        bar.addView(text("App de práctica · los pedidos son de mentira", 13f, 0xD0FFFFFF.toInt()))
        val info = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, px(12), 0, 0)
        }
        titulo = text("", 16f, Color.WHITE, bold = true)
        info.addView(titulo, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        progreso = text("", 13f, Color.WHITE, bold = true).apply {
            background = rounded(0x33FFFFFF, 50)
            setPadding(px(10), px(4), px(10), px(4))
        }
        info.addView(progreso)
        bar.addView(info)
        root.addView(bar)

        list = ListView(this).apply {
            divider = null
            dividerHeight = 0
            selector = GradientDrawable().apply { setColor(Color.TRANSPARENT) }
            setPadding(px(12), px(12), px(12), px(24))
            clipToPadding = false
            addHeaderView(header(), null, false)
            addFooterView(footer(), null, false)
            adapter = this@DemoActivity.adapter
        }
        root.addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
        nuevoPedido()
    }

    private fun header(): View {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        val guide = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(Color.WHITE, 16, 1, border)
            setPadding(px(16), px(14), px(16), px(16))
        }
        guide.addView(text("Cómo practicar con Mini Picker", 16f, ink, bold = true))
        val pasos = listOf(
            "En Mini Picker, elige \"Picker Demo\" como tu app de picking.",
            "Vuelve aquí y recorre el pedido hasta el final.",
            "Toca la burbuja azul: verás todo ordenado por categoría.",
            "Algunos productos no tienen categoría a propósito, para que practiques asignarlas.",
        )
        pasos.forEachIndexed { i, p ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, px(8), 0, 0)
            }
            row.addView(text("${i + 1}", 12f, green, bold = true).apply {
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(greenSoft)
                }
            }, LinearLayout.LayoutParams(px(22), px(22)))
            row.addView(text(p, 14f, 0xFF374151.toInt()).apply { setLineSpacing(0f, 1.1f) },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = px(10) })
            guide.addView(row)
        }
        guide.addView(button("Abrir Mini Picker", false) { abrirMiniPicker() },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = px(14)
            })
        col.addView(guide)

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(px(2), px(16), 0, px(10))
        }
        controls.addView(text("Tamaño:", 14f, muted, bold = true))
        sizeRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        controls.addView(sizeRow, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = px(8)
        })
        controls.addView(button("Nuevo pedido", true) { nuevoPedido() })
        col.addView(controls)
        renderSizes()
        return col
    }

    private fun renderSizes() {
        sizeRow.removeAllViews()
        for (n in listOf(10, 25, 50)) {
            val sel = n == size
            sizeRow.addView(text("$n", 14f, if (sel) Color.WHITE else ink, bold = true).apply {
                gravity = Gravity.CENTER
                background = if (sel) rounded(ink, 50) else rounded(Color.WHITE, 50, 1, border)
                setPadding(px(12), px(6), px(12), px(6))
                isClickable = true
                setOnClickListener {
                    size = n
                    renderSizes()
                    nuevoPedido()
                }
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginEnd = px(6)
            })
        }
    }

    private fun footer(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        layoutParams = AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        setPadding(px(16), px(16), px(16), px(8))
        addView(text("Fin del pedido", 15f, ink, bold = true))
        addView(text("¿Viste todos los productos en la burbuja? Toca \"Limpiar\" en la burbuja y luego " +
            "\"Nuevo pedido\" para practicar otra vez.", 13f, muted).apply {
            gravity = Gravity.CENTER
            setPadding(0, px(4), 0, 0)
        })
    }

    private fun nuevoPedido() {
        val extra = sinCategoria.shuffled().take(if (size >= 25) 2 else 1)
        val nombres = (catalogo.shuffled().take(size - extra.size) + extra).shuffled()
        pedido = nombres.map {
            Item(
                name = it,
                qty = Random.nextInt(1, 4),
                sku = "78" + Random.nextInt(10000000, 99999999),
                aisle = Random.nextInt(1, 15),
                note = if (Random.nextInt(6) == 0) notas.random() else null,
            )
        }
        picked.clear()
        titulo.text = "Pedido #${Random.nextInt(10000, 99999)}"
        adapter.notifyDataSetChanged()
        actualizarProgreso()
        if (::list.isInitialized) list.setSelection(0)
    }

    private fun actualizarProgreso() {
        progreso.text = "✓ ${picked.size}/${pedido.size}"
    }

    private fun abrirMiniPicker() {
        val i = packageManager.getLaunchIntentForPackage("cl.picker.sorter")
        if (i != null) startActivity(i)
        else Toast.makeText(this, "Mini Picker no está instalado", Toast.LENGTH_SHORT).show()
    }

    private inner class Adapter : BaseAdapter() {
        override fun getCount() = pedido.size
        override fun getItem(i: Int) = pedido[i]
        override fun getItemId(i: Int) = i.toLong()

        override fun getView(i: Int, convert: View?, parent: ViewGroup): View {
            val frame = convert as? FrameLayout ?: crearFila()
            val card = frame.getChildAt(0) as LinearLayout
            val check = card.getChildAt(0) as TextView
            val col = card.getChildAt(1) as LinearLayout
            val nombre = col.getChildAt(0) as TextView
            val meta = col.getChildAt(1) as TextView
            val nota = col.getChildAt(2) as TextView
            val cant = card.getChildAt(2) as TextView

            val item = pedido[i]
            val ok = i in picked
            nombre.text = item.name
            nombre.setTextColor(if (ok) muted else ink)
            nombre.paintFlags = if (ok) nombre.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                                else nombre.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            meta.text = "SKU ${item.sku}  ·  Pasillo ${item.aisle}"
            nota.visibility = if (item.note != null) View.VISIBLE else View.GONE
            nota.text = "⚠ ${item.note ?: ""}"
            cant.text = "x${item.qty}"
            check.text = if (ok) "✓" else ""
            check.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                if (ok) setColor(green) else {
                    setColor(Color.WHITE)
                    setStroke(px(2), border)
                }
            }
            card.alpha = if (ok) 0.7f else 1f
            card.setOnClickListener {
                if (!picked.remove(i)) picked.add(i)
                actualizarProgreso()
                notifyDataSetChanged()
            }
            return frame
        }

        private fun crearFila(): FrameLayout {
            val card = LinearLayout(this@DemoActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = rounded(Color.WHITE, 14, 1, border)
                setPadding(px(14), px(12), px(14), px(12))
            }
            card.addView(text("", 13f, Color.WHITE, bold = true).apply { gravity = Gravity.CENTER },
                LinearLayout.LayoutParams(px(26), px(26)))
            val col = LinearLayout(this@DemoActivity).apply { orientation = LinearLayout.VERTICAL }
            col.addView(text("", 16f, ink).apply { id = R.id.product_name })
            col.addView(text("", 12f, muted).apply { setPadding(0, px(2), 0, 0) })
            col.addView(text("", 12f, amber, bold = true).apply { setPadding(0, px(4), 0, 0) })
            card.addView(col, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = px(12)
            })
            card.addView(text("", 14f, green, bold = true).apply {
                gravity = Gravity.CENTER
                background = rounded(greenSoft, 50)
                setPadding(px(10), px(4), px(10), px(4))
            })
            return FrameLayout(this@DemoActivity).apply {
                setPadding(0, 0, 0, px(8))
                addView(card)
            }
        }
    }
}
