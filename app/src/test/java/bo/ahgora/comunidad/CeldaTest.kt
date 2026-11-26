package bo.ahgora.comunidad

import bo.ahgora.comunidad.data.remote.AhgoraApi
import bo.ahgora.comunidad.data.zona.Celda
import bo.ahgora.comunidad.data.zona.Localidades
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.floor

class CeldaTest {

    /** Réplica de `zonaPublica` del servidor (JS): floor(v / 0.1) * 0.1, redondeado a 2 decimales. */
    private fun servidor(v: Double): Double = "%.2f".format(java.util.Locale.US, floor(v / 0.1) * 0.1).toDouble()

    @Test
    fun la_celda_de_la_app_coincide_con_la_zona_del_servidor() {
        val puntos = listOf(-15.498 to -67.883, -16.5 to -68.15, -14.4413 to -67.5278, -11.0 to -66.07, -15.0 to -67.0)
        for ((lat, lon) in puntos) {
            val app = Celda.de(lat, lon)
            val desdeServidor = Celda.desdeMinimos(servidor(lat), servidor(lon))
            assertEquals("lat=$lat lon=$lon", app, desdeServidor)
        }
    }

    @Test
    fun distancia_en_celdas() {
        val a = Celda(-155, -679)
        assertEquals(0, a.distancia(a))
        assertEquals(1, a.distancia(Celda(-154, -678)))
        assertEquals(3, a.distancia(Celda(-158, -679)))
    }

    @Test
    fun clave_ida_y_vuelta() {
        val c = Celda(-155, -679)
        assertEquals(c, Celda.desdeClave(c.clave()))
    }

    @Test
    fun nombre_sugerido_usa_la_localidad_cercana() {
        assertTrue(Localidades.nombreSugerido(Celda.de(-15.498, -67.883)).contains("Guanay"))
        assertEquals("Caranavi", Localidades.nombreSugerido(Celda.de(-15.833, -67.567)))
        assertTrue(Localidades.nombreSugerido(Celda.de(-15.45, -67.70)).startsWith("Cerca de"))
    }

    @Test
    fun alcoche_es_la_zona_por_defecto() {
        val alcoche = Localidades.ALCOCHE
        assertEquals(alcoche.celda, bo.ahgora.comunidad.data.prefs.ZONA_POR_DEFECTO)
        assertEquals("Alcoche", Localidades.nombreSugerido(alcoche.celda))
        // Alcoche y Caranavi quedan en zonas distintas, y la lista empieza por Alcoche y sus vecinas.
        assertTrue(alcoche.celda != Celda.de(-15.833, -67.567))
        assertEquals("Alcoche", Localidades.cercaDeAlcoche.first().nombre)
        assertEquals("Caranavi", Localidades.cercaDeAlcoche[1].nombre)
    }

    @Test
    fun contexto_del_mapa_es_coherente() {
        val ctx = bo.ahgora.comunidad.data.mapa.ContextoAlcoche
        // Todos los peces de los tramos existen y el tramo medido tiene los 10 del estudio.
        ctx.tramos.flatMap { it.peces }.forEach { assertNotNull(it, ctx.pez(it)) }
        assertEquals(10, ctx.tramos.single { it.medido }.peces.size)
        // El primer lugar es el río Coroico junto a Alcoche.
        assertEquals(Localidades.ALCOCHE.celda, Celda.de(ctx.lugares.first().lat, ctx.lugares.first().lon))
    }

    @Test
    fun fechas_del_servidor() {
        assertNotNull(AhgoraApi.parseFecha("2026-11-24T16:20:00.000Z"))
        assertNotNull(AhgoraApi.parseFecha("2026-11-24T16:20"))
        assertNotNull(AhgoraApi.parseFecha("2026-11-24"))
        assertEquals(null, AhgoraApi.parseFecha("ayer"))
    }
}
