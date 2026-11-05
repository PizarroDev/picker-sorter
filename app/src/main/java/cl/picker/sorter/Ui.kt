package cl.picker.sorter

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.view.Gravity
import android.widget.EditText
import android.widget.TextView

object Ui {
    const val PRIMARY = 0xFF1E5EFF.toInt()
    const val PRIMARY_DARK = 0xFF1442C2.toInt()
    const val PRIMARY_SOFT = 0xFFE8EEFF.toInt()
    const val BG = 0xFFF3F5F9.toInt()
    const val SURFACE = 0xFFF7F8FB.toInt()
    const val CARD = 0xFFFFFFFF.toInt()
    const val TEXT = 0xFF1A1D23.toInt()
    const val MUTED = 0xFF6B7280.toInt()
    const val BORDER = 0xFFE3E7EE.toInt()
    const val OK = 0xFF16A34A.toInt()
    const val OK_SOFT = 0xFFDCFCE7.toInt()
    const val WARN = 0xFFD97706.toInt()

    private val CATEGORY_COLORS = intArrayOf(
        0xFF16A34A.toInt(),
        0xFFD97706.toInt(),
        0xFFDC2626.toInt(),
        0xFF0891B2.toInt(),
        0xFFDB2777.toInt(),
        0xFF2563EB.toInt(),
        0xFF7C3AED.toInt(),
        0xFF65A30D.toInt(),
        0xFFEA580C.toInt(),
        0xFF0D9488.toInt(),
    )

    fun categoryColor(i: Int) = if (i < 0) MUTED else CATEGORY_COLORS[i % CATEGORY_COLORS.size]

    fun alpha(color: Int, a: Int) = (color and 0x00FFFFFF) or (a shl 24)

    fun px(c: Context, v: Number) = (v.toFloat() * c.resources.displayMetrics.density).toInt()

    fun rounded(color: Int, radius: Float, strokeW: Int = 0, strokeColor: Int = 0) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
            if (strokeW > 0) setStroke(strokeW, strokeColor)
        }

    fun gradient(radius: Float = 0f) = GradientDrawable(
        GradientDrawable.Orientation.TL_BR, intArrayOf(PRIMARY, PRIMARY_DARK)
    ).apply { cornerRadius = radius }

    fun ripple(base: Drawable, radius: Float, rippleColor: Int = 0x22000000): Drawable =
        RippleDrawable(ColorStateList.valueOf(rippleColor), base, rounded(Color.WHITE, radius))

    fun button(c: Context, label: String, filled: Boolean, onClick: () -> Unit) = TextView(c).apply {
        text = label
        gravity = Gravity.CENTER
        textSize = 15f
        setTypeface(null, Typeface.BOLD)
        val r = px(c, 14).toFloat()
        if (filled) {
            setTextColor(Color.WHITE)
            background = ripple(rounded(PRIMARY, r), r, 0x33FFFFFF)
        } else {
            setTextColor(PRIMARY)
            background = ripple(rounded(CARD, r, px(c, 1), BORDER), r)
        }
        setPadding(px(c, 16), px(c, 13), px(c, 16), px(c, 13))
        isClickable = true
        isFocusable = true
        setOnClickListener { onClick() }
    }

    fun input(c: Context, hintText: String) = EditText(c).apply {
        hint = hintText
        textSize = 15f
        setTextColor(TEXT)
        setHintTextColor(0xFF9CA3AF.toInt())
        val r = px(c, 12).toFloat()
        background = StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_focused), rounded(CARD, r, px(c, 2), PRIMARY))
            addState(intArrayOf(), rounded(SURFACE, r, px(c, 1), BORDER))
        }
        setPadding(px(c, 14), px(c, 12), px(c, 14), px(c, 12))
    }

    fun pill(c: Context, label: String, fg: Int, bg: Int) = TextView(c).apply {
        text = label
        textSize = 12f
        setTypeface(null, Typeface.BOLD)
        setTextColor(fg)
        background = rounded(bg, px(c, 50).toFloat())
        setPadding(px(c, 9), px(c, 3), px(c, 9), px(c, 3))
    }
}
