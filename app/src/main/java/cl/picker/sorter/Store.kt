package cl.picker.sorter

import android.content.Context
import java.io.File

object Store {
    private fun prefs(c: Context) = c.getSharedPreferences("cfg", Context.MODE_PRIVATE)

    fun target(c: Context): String = prefs(c).getString("target", "") ?: ""
    fun setTarget(c: Context, v: String) = prefs(c).edit().putString("target", v).apply()

    fun viewId(c: Context): String = prefs(c).getString("viewId", "") ?: ""
    fun setViewId(c: Context, v: String) = prefs(c).edit().putString("viewId", v).apply()

    fun ignored(c: Context): MutableSet<String> =
        HashSet(prefs(c).getStringSet("ignored", emptySet()) ?: emptySet())

    fun addIgnored(c: Context, s: String) {
        val set = ignored(c)
        set.add(s)
        prefs(c).edit().putStringSet("ignored", set).apply()
    }

    fun clearIgnored(c: Context) = prefs(c).edit().remove("ignored").apply()

    private fun catFile(c: Context) = File(c.filesDir, "categorias.txt")

    fun categories(c: Context): String {
        val f = catFile(c)
        return if (f.exists()) f.readText() else defaultCategories(c)
    }

    fun defaultCategories(c: Context): String =
        c.assets.open("categorias.txt").bufferedReader().use { it.readText() }

    fun saveCategories(c: Context, text: String) = catFile(c).writeText(text)
}
