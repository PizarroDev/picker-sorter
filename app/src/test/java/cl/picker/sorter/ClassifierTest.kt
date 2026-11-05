package cl.picker.sorter

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class ClassifierTest {
    private val c = Classifier(File("src/main/assets/categorias.txt").readText())
    private fun cat(p: String) = c.classify(p).let { if (it < 0) "-" else c.categories[it].name }

    @Test
    fun clasifica() {
        val casos = mapOf(
            "Leche Entera Colun 1L" to "Lácteos y huevos",
            "Yoghurt Soprole Frutilla 155g" to "Lácteos y huevos",
            "Galletas Tritón Chocolate" to "Galletas y dulces",
            "Chocolate en polvo Nesquik" to "Café, té y desayuno",
            "Papas fritas congeladas McCain" to "Congelados",
            "Papa blanca malla 2kg" to "Frutas y verduras",
            "Tomates" to "Frutas y verduras",
            "Caldo de pollo Maggi" to "Despensa",
            "Pechuga de pollo deshuesada" to "Carnes y pollo",
            "Pan de molde Ideal" to "Panadería",
            "Detergente líquido Omo 3L" to "Limpieza",
            "Papel higiénico Confort 12 rollos" to "Papel",
            "Cerveza Cristal lata" to "Licores",
            "Coca Cola 3L" to "Bebidas y jugos",
            "Atún en agua Van Camps" to "Despensa",
            "Botón Siguiente" to "-",
        )
        val malos = casos.filter { (p, e) -> cat(p) != e }.map { (p, e) -> "$p -> ${cat(p)} (esperado $e)" }
        assertEquals(malos.joinToString(" | "), 0, malos.size)
    }
}
