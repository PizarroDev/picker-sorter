package cl.picker.sorter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class LearnerTest {
    private val c = Classifier(File("src/main/assets/categorias.txt").readText())

    private fun n(sig: String, t: String) = Node(null, sig, t, false)

    @Test
    fun aprendeNombresConId() {
        val pantalla = listOf(
            n("id:title", "Pedido #4521"),
            n("id:btn", "Siguiente"),
            n("id:name", "Leche Entera Colun 1L"),
            n("id:price", "$1.090"),
            n("id:name", "Ricolate 1L"),
            n("id:price", "$990"),
            n("id:name", "Detergente Omo 3L"),
            n("id:name", "Kombucha Biota"),
            n("id:name", "Galletas Tritón"),
            n("id:note", "Sustituir por similar si no hay"),
        )
        assertEquals("id:name", Learner.best(pantalla, c))
    }

    @Test
    fun aprendeSinIdPorPosicion() {
        val fila = "c:android.widget.TextView@14"
        val otra = "c:android.widget.TextView@15"
        val pantalla = listOf(
            n("c:android.widget.TextView@6", "Mis pedidos"),
            n(fila, "Pan de molde Ideal"), n(otra, "Cant: 2"),
            n(fila, "Tofu firme 400g"), n(otra, "Cant: 1"),
            n(fila, "Coca Cola 3L"), n(otra, "Cant: 3"),
            n(fila, "Plátano granel"), n(otra, "Cant: 1"),
        )
        assertEquals(fila, Learner.best(pantalla, c))
    }

    @Test
    fun noAprendeConPocosProductos() {
        val pantalla = listOf(
            n("id:title", "Inicio"),
            n("id:name", "Leche Entera"),
            n("id:name", "Pilas AA"),
        )
        assertNull(Learner.best(pantalla, c))
    }
}
