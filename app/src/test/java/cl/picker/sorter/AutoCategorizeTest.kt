package cl.picker.sorter

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class AutoCategorizeTest {
    private val cats = File("src/main/assets/categorias.txt").readText()
    private val dict = File("src/main/assets/diccionario.txt").readText()

    private fun name(c: Classifier, p: String) = c.classify(p).let { if (it < 0) "-" else c.categories[it].name }

    @Test
    fun diccionarioReconoceProductosSinPalabraClave() {
        val c = Classifier(cats, dict)
        val casos = mapOf(
            "Ricolate Soprole 1L" to "Lácteos y huevos",
            "Pilas AA Duracell 4 un" to "Hogar y bazar",
            "Velas aromáticas vainilla" to "Hogar y bazar",
            "Kombucha Biota jengibre" to "Bebidas y jugos",
            "Tofu firme 400g" to "Fiambres y quesos",
            "Papas fritas Lays corte americano" to "Snacks",
            "Papas fritas congeladas McCain" to "Congelados",
            "Pasta dental Colgate" to "Cuidado personal",
            "Agua oxigenada 120ml" to "Cuidado personal",
            "Crema de leche Colun" to "Lácteos y huevos",
            "Lomo vetado" to "Carnes y pollo",
            "Pisco Mistral 35°" to "Licores",
            "Huggies Natural Care XG" to "Bebés",
            "Whiskas pouch salmón" to "Mascotas",
        )
        val malos = casos.filter { (p, e) -> name(c, p) != e }.map { (p, e) -> "$p -> ${name(c, p)} (esperado $e)" }
        assertEquals(malos.joinToString(" | "), 0, malos.size)
    }

    @Test
    fun reconocePalabrasParecidas() {
        val c = Classifier(cats, dict)
        assertEquals("Galletas y dulces", name(c, "Galletitas surtidas"))
        assertEquals(Classifier.SIMILAR, c.classifyWithSource("Galletitas surtidas").second)
    }

    @Test
    fun aprendeMarcasDeLosPedidos() {
        val s = WordStats()
        val c = Classifier("[Lácteos y huevos]\nleche, mantequilla\n[Limpieza]\ncloro", "", s)
        assertEquals("-", name(c, "Quillayes Ricotta 250g"))
        s.learn("Leche entera Quillayes 1L", "Lácteos y huevos")
        s.learn("Mantequilla Quillayes 250g", "Lácteos y huevos")
        assertEquals("Lácteos y huevos", name(c, "Quillayes Ricotta 250g"))
        assertEquals(Classifier.LEARNED, c.classifyWithSource("Quillayes Ricotta 250g").second)
        assertEquals("-", name(c, "Producto 250g"))
    }

    @Test
    fun guardaYCargaLoAprendido() {
        val s = WordStats()
        s.learn("Leche Colun", "Lácteos y huevos")
        s.learn("Manjar Colun", "Lácteos y huevos")
        val r = WordStats.parse(s.serialize())
        assertEquals("Lácteos y huevos", r.guess(listOf("colun")))
    }
}
