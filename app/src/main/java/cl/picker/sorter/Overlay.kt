package cl.picker.sorter

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlin.math.abs
import kotlin.math.min

class Overlay(private val svc: PickerService) {

    private val wm = svc.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private fun px(v: Number) = Ui.px(svc, v)

    private val match = LinearLayout.LayoutParams.MATCH_PARENT
    private val wrap = LinearLayout.LayoutParams.WRAP_CONTENT

    private val bubbleSize = px(58)
    private val shadowPad = px(8)

    private val bubble = TextView(svc).apply {
        gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
        textSize = 18f
        setTypeface(null, Typeface.BOLD)
        elevation = px(6).toFloat()
    }

    private val bubbleFrame = FrameLayout(svc).apply {
        setPadding(shadowPad, shadowPad, shadowPad, shadowPad)
        clipToPadding = false
        addView(bubble, FrameLayout.LayoutParams(bubbleSize, bubbleSize))
    }

    private val bubbleLp = params(bubbleSize + 2 * shadowPad, bubbleSize + 2 * shadowPad).apply {
        gravity = Gravity.TOP or Gravity.START
        x = 0
        y = px(180)
    }

    private var panel: View? = null
    private var list: LinearLayout? = null
    private var progressText: TextView? = null
    private var progressFill: View? = null
    private var progressRest: View? = null
    private var shown = false

    init {
        setupDrag()
    }

    private fun params(w: Int, h: Int) = WindowManager.LayoutParams(
        w, h,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    )

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDrag() {
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        var moved = false
        bubble.setOnTouchListener { v, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = bubbleLp.x; startY = bubbleLp.y
                    touchX = e.rawX; touchY = e.rawY
                    moved = false
                    v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(90).start()
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - touchX
                    val dy = e.rawY - touchY
                    if (abs(dx) + abs(dy) > px(8)) moved = true
                    if (moved) {
                        bubbleLp.x = startX + dx.toInt()
                        bubbleLp.y = startY + dy.toInt()
                        wm.updateViewLayout(bubbleFrame, bubbleLp)
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                    if (moved) snapToEdge() else if (e.action == MotionEvent.ACTION_UP) togglePanel()
                }
            }
            true
        }
    }

    private fun snapToEdge() {
        val screenW = svc.resources.displayMetrics.widthPixels
        val w = bubbleLp.width
        val to = if (bubbleLp.x + w / 2 < screenW / 2) 0 else screenW - w
        ValueAnimator.ofInt(bubbleLp.x, to).apply {
            duration = 180
            addUpdateListener {
                bubbleLp.x = it.animatedValue as Int
                if (shown) wm.updateViewLayout(bubbleFrame, bubbleLp)
            }
            start()
        }
    }

    fun setVisible(v: Boolean) {
        if (v && !shown) {
            wm.addView(bubbleFrame, bubbleLp)
            shown = true
            refresh()
        } else if (!v && shown) {
            hidePanel()
            wm.removeView(bubbleFrame)
            shown = false
        }
    }

    fun refresh() {
        val total = svc.totalCount()
        val pending = svc.pendingCount()
        val done = total > 0 && pending == 0
        bubble.text = when {
            total == 0 -> "≡"
            done -> "✓"
            else -> pending.toString()
        }
        bubble.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            if (done) intArrayOf(0xFF22C55E.toInt(), 0xFF15803D.toInt()) else intArrayOf(Ui.PRIMARY, Ui.PRIMARY_DARK)
        ).apply {
            shape = GradientDrawable.OVAL
            setStroke(px(2), Color.WHITE)
        }
        if (panel != null) render()
    }

    private fun togglePanel() {
        if (panel != null) hidePanel() else showPanel()
    }

    private fun hidePanel() {
        panel?.let { wm.removeView(it) }
        panel = null
        list = null
        progressText = null
        progressFill = null
        progressRest = null
    }

    private fun showPanel() {
        val dm = svc.resources.displayMetrics
        val card = LinearLayout(svc).apply {
            orientation = LinearLayout.VERTICAL
            background = Ui.rounded(Ui.CARD, px(24).toFloat())
            clipToOutline = true
            elevation = px(10).toFloat()
        }

        val header = LinearLayout(svc).apply {
            orientation = LinearLayout.VERTICAL
            background = Ui.gradient()
            setPadding(px(18), px(16), px(14), px(16))
        }
        val top = LinearLayout(svc).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val titles = LinearLayout(svc).apply { orientation = LinearLayout.VERTICAL }
        titles.addView(text("Por categoría", 19f, Color.WHITE, bold = true))
        progressText = text("", 13f, Ui.alpha(Color.WHITE, 0xD8)).also { titles.addView(it) }
        top.addView(titles, LinearLayout.LayoutParams(0, wrap, 1f))
        top.addView(headerButton("↺  Limpiar") { svc.clearItems() })
        top.addView(headerButton("✕") { hidePanel() },
            LinearLayout.LayoutParams(wrap, wrap).apply { marginStart = px(8) })
        header.addView(top)

        val bar = LinearLayout(svc).apply {
            orientation = LinearLayout.HORIZONTAL
            background = Ui.rounded(Ui.alpha(Color.WHITE, 0x40), px(3).toFloat())
            clipToOutline = true
        }
        progressFill = View(svc).apply { background = Ui.rounded(Color.WHITE, px(3).toFloat()) }
        progressRest = View(svc)
        bar.addView(progressFill, LinearLayout.LayoutParams(0, match, 0f))
        bar.addView(progressRest, LinearLayout.LayoutParams(0, match, 1f))
        header.addView(bar, LinearLayout.LayoutParams(match, px(6)).apply { topMargin = px(12) })
        card.addView(header)

        val l = LinearLayout(svc).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(14), px(4), px(14), px(14))
        }
        card.addView(ScrollView(svc).apply {
            isVerticalScrollBarEnabled = false
            addView(l)
        }, LinearLayout.LayoutParams(match, 0, 1f))

        card.addView(View(svc).apply { setBackgroundColor(Ui.BORDER) }, LinearLayout.LayoutParams(match, px(1)))
        card.addView(text("Toca para marcar  ·  Mantén presionado para ignorar", 12f, Ui.MUTED).apply {
            gravity = Gravity.CENTER
            setPadding(px(12), px(10), px(12), px(12))
        })

        val frame = FrameLayout(svc).apply {
            setPadding(px(10), px(10), px(10), px(10))
            clipToPadding = false
            addView(card, FrameLayout.LayoutParams(match, match))
        }
        val lp = params(min((dm.widthPixels * 0.95f).toInt(), px(470)), (dm.heightPixels * 0.72f).toInt())
        lp.gravity = Gravity.CENTER
        wm.addView(frame, lp)
        panel = frame
        list = l
        render()
    }

    private fun headerButton(label: String, onClick: () -> Unit) = TextView(svc).apply {
        text = label
        textSize = 14f
        setTypeface(null, Typeface.BOLD)
        setTextColor(Color.WHITE)
        val r = px(50).toFloat()
        background = Ui.ripple(Ui.rounded(Ui.alpha(Color.WHITE, 0x30), r), r, 0x40FFFFFF)
        setPadding(px(13), px(8), px(13), px(8))
        isClickable = true
        setOnClickListener { onClick() }
    }

    private fun text(t: String, size: Float, color: Int, bold: Boolean = false) = TextView(svc).apply {
        text = t
        textSize = size
        setTextColor(color)
        if (bold) setTypeface(null, Typeface.BOLD)
    }

    private fun render() {
        val l = list ?: return
        val total = svc.totalCount()
        val picked = total - svc.pendingCount()
        progressText?.text = if (total == 0) "Sin productos todavía" else "$picked de $total recogidos"
        progressFill?.layoutParams = LinearLayout.LayoutParams(0, match, picked.toFloat())
        progressRest?.layoutParams = LinearLayout.LayoutParams(0, match, (total - picked).toFloat().coerceAtLeast(if (total == 0) 1f else 0f))

        l.removeAllViews()
        val groups = svc.grouped()
        if (groups.isEmpty()) {
            l.addView(emptyState())
            return
        }
        for (g in groups) {
            val color = Ui.categoryColor(g.index)
            val pending = g.items.count { !svc.isPicked(it) }

            val head = LinearLayout(svc).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(px(2), px(16), px(2), px(8))
            }
            head.addView(View(svc).apply { background = Ui.rounded(color, px(3).toFloat()) },
                LinearLayout.LayoutParams(px(5), px(18)).apply { marginEnd = px(10) })
            head.addView(text(g.name, 15f, Ui.TEXT, bold = true), LinearLayout.LayoutParams(0, wrap, 1f))
            head.addView(
                if (pending == 0) Ui.pill(svc, "Listo ✓", Ui.OK, Ui.OK_SOFT)
                else Ui.pill(svc, "${g.items.size - pending}/${g.items.size}", color, Ui.alpha(color, 0x22))
            )
            l.addView(head)

            val box = LinearLayout(svc).apply {
                orientation = LinearLayout.VERTICAL
                background = Ui.rounded(Ui.SURFACE, px(14).toFloat(), px(1), Ui.BORDER)
                clipToOutline = true
            }
            g.items.forEachIndexed { i, name ->
                if (i > 0) box.addView(View(svc).apply { setBackgroundColor(Ui.BORDER) },
                    LinearLayout.LayoutParams(match, px(1)).apply { marginStart = px(48) })
                box.addView(itemRow(name, color))
            }
            l.addView(box)
        }
    }

    private fun itemRow(name: String, color: Int): View {
        val picked = svc.isPicked(name)
        val row = LinearLayout(svc).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(px(12), px(12), px(12), px(12))
            background = Ui.ripple(Ui.rounded(Color.TRANSPARENT, 0f), 0f)
            isClickable = true
            setOnClickListener { svc.togglePicked(name) }
            setOnLongClickListener { svc.ignore(name); true }
        }
        row.addView(TextView(svc).apply {
            gravity = Gravity.CENTER
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            text = if (picked) "✓" else ""
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                if (picked) setColor(Ui.OK) else {
                    setColor(Ui.CARD)
                    setStroke(px(2), color)
                }
            }
        }, LinearLayout.LayoutParams(px(24), px(24)))
        row.addView(text(name, 15f, if (picked) Ui.MUTED else Ui.TEXT).apply {
            if (picked) paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        }, LinearLayout.LayoutParams(0, wrap, 1f).apply { marginStart = px(12) })
        return row
    }

    private fun emptyState() = LinearLayout(svc).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(px(16), px(40), px(16), px(24))
        addView(text("🛒", 44f, Ui.TEXT).apply { gravity = Gravity.CENTER })
        addView(text("Aún no hay productos", 17f, Ui.TEXT, bold = true).apply {
            gravity = Gravity.CENTER
            setPadding(0, px(12), 0, px(6))
        })
        addView(text("Recorre la lista completa en tu app de picking y los productos aparecerán aquí, agrupados por categoría.", 14f, Ui.MUTED).apply {
            gravity = Gravity.CENTER
            setLineSpacing(0f, 1.15f)
        })
    }
}
