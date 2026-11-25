package bo.ahgora.comunidad

import bo.ahgora.comunidad.data.local.EstadoAviso
import bo.ahgora.comunidad.data.local.OrigenAviso
import bo.ahgora.comunidad.data.remote.AhgoraApi
import bo.ahgora.comunidad.data.remote.AlertasResponse
import bo.ahgora.comunidad.data.sync.AvisoEntrante
import bo.ahgora.comunidad.data.sync.Fusion
import bo.ahgora.comunidad.data.zona.Celda
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FusionTest {
    private val S = OrigenAviso.SERVIDOR
    private val zona = Celda(-155, -679)

    private fun e(
        id: String? = null, version: Int? = null, estado: EstadoAviso = EstadoAviso.PENDIENTE,
        mensaje: String = "m", fecha: String = "2026-11-24T10:00:00Z", celda: Celda = zona, matriz: String = "sedimento",
    ) = AvisoEntrante(id, version, matriz, estado, "aviso_cribado", mensaje, celda, 1_000L, fecha, null, false)

    @Test
    fun respuesta_repetida_no_duplica() {
        val r1 = Fusion.fusionar(emptyList(), listOf(e(), e()), S, 10, completo = true)
        assertEquals(1, r1.guardar.size)
        val r2 = Fusion.fusionar(r1.guardar, listOf(e()), S, 20, completo = true)
        assertTrue(r2.guardar.isEmpty())
        assertTrue(r2.cambios.isEmpty())
    }

    @Test
    fun cambio_de_contenido_es_actualizacion_y_no_se_marca_leido() {
        val r1 = Fusion.fusionar(emptyList(), listOf(e(id = "a", version = 1)), S, 10, true)
        val leido = r1.guardar.map { it.copy(leido = true) }
        val r2 = Fusion.fusionar(leido, listOf(e(id = "a", version = 2, estado = EstadoAviso.EN_ANALISIS)), S, 20, true)
        assertEquals(1, r2.actualizados.size)
        assertEquals(EstadoAviso.EN_ANALISIS, r2.actualizados[0].estado)
        assertEquals(2, r2.actualizados[0].version)
        assertFalse(r2.actualizados[0].leido)
    }

    @Test
    fun version_antigua_no_reemplaza_a_la_nueva() {
        val r1 = Fusion.fusionar(emptyList(), listOf(e(id = "a", version = 3, mensaje = "nuevo")), S, 10, true)
        val r2 = Fusion.fusionar(r1.guardar, listOf(e(id = "a", version = 2, mensaje = "viejo")), S, 20, true)
        assertTrue(r2.guardar.isEmpty())
    }

    @Test
    fun aviso_ausente_se_retira_y_se_enlaza_con_su_reemplazo() {
        val r1 = Fusion.fusionar(emptyList(), listOf(e(fecha = "2026-11-20T10:00:00Z")), S, 10, true)
        val viejo = r1.guardar.single()
        // El servidor ya no publica el anterior y sí uno nuevo de la misma zona y matriz.
        val r2 = Fusion.fusionar(r1.guardar, listOf(e(fecha = "2026-11-24T10:00:00Z", estado = EstadoAviso.CONFIRMADO)), S, 20, true)
        val retirado = r2.retirados.single()
        val nuevo = r2.nuevos.single()
        assertEquals(viejo.id, retirado.id)
        assertTrue(retirado.retirado)
        assertEquals(nuevo.id, retirado.reemplazadoPor)
        assertEquals(viejo.id, nuevo.reemplazaA)
        assertEquals(Fusion.MOTIVO_REEMPLAZO, retirado.motivoRetiro)
    }

    @Test
    fun respuesta_incompleta_no_retira_por_ausencia() {
        val r1 = Fusion.fusionar(emptyList(), listOf(e(id = "a"), e(id = "b")), S, 10, true)
        val r2 = Fusion.fusionar(r1.guardar, listOf(e(id = "a")), S, 20, completo = false)
        assertTrue(r2.retirados.isEmpty())
    }

    @Test
    fun retirado_que_vuelve_a_publicarse_se_restaura() {
        val r1 = Fusion.fusionar(emptyList(), listOf(e(id = "a")), S, 10, true)
        val r2 = Fusion.fusionar(r1.guardar, emptyList(), S, 20, true)
        assertTrue(r2.retirados.single().retirado)
        val r3 = Fusion.fusionar(r2.guardar, listOf(e(id = "a")), S, 30, true)
        assertFalse(r3.actualizados.single().retirado)
    }

    @Test
    fun contrato_con_id_version_y_retiro_del_servidor() {
        val api = AhgoraApi()
        fun leer(json: String) = api.json.decodeFromString(AlertasResponse.serializer(), json)
            .items.mapNotNull { AhgoraApi.aEntrante(it, 0L) }
        val zona = """"zona":{"lat_min":-15.5,"lon_min":-67.9,"lat_max":-15.4,"lon_max":-67.8}"""
        val v1 = leer("""{"items":[{"id":"av-1","version":1,"matriz":"sedimento",$zona,"certeza":"cribado","tipo":"aviso_cribado","mensaje":"m","fecha":"2026-11-24T16:20:00Z"}]}""")
        val v2 = leer("""{"items":[{"id":"av-1","version":2,"matriz":"sedimento",$zona,"certeza":"confirmado","tipo":"aviso_cribado","mensaje":"m","fecha":"2026-11-24T16:20:00Z","laboratorio":"Lab"}]}""")
        val v3 = leer("""{"items":[{"id":"av-1","version":3,"matriz":"sedimento",$zona,"retirado":true,"fecha":"2026-11-25T01:21:13Z"}]}""")

        val r1 = Fusion.fusionar(emptyList(), v1, S, 10, true)
        assertEquals("av-1", r1.nuevos.single().id)
        val r2 = Fusion.fusionar(r1.guardar, v2, S, 20, true)
        assertEquals(EstadoAviso.CONFIRMADO, r2.actualizados.single().estado)
        assertEquals(2, r2.actualizados.single().version)
        // Respuesta vieja que llega tarde: no reemplaza a la versión 2.
        assertTrue(Fusion.fusionar(r2.guardar, v1, S, 25, true).guardar.isEmpty())
        val r3 = Fusion.fusionar(r2.guardar, v3, S, 30, true)
        val retirado = r3.retirados.single()
        assertTrue(retirado.retirado)
        assertEquals(EstadoAviso.CONFIRMADO, retirado.estado) // conserva el último estado conocido
    }

    @Test
    fun json_real_de_api_alertas() {
        val api = AhgoraApi()
        val texto = """
            {"aviso":"Primera señal, no un diagnóstico ni una prueba legal.","items":[
              {"matriz":"sedimento","zona":{"lat_min":-15.5,"lon_min":-67.9,"lat_max":-15.4,"lon_max":-67.8},
               "nivel":"CONFIRMADO POR LABORATORIO","tipo":"tranquilizador","mensaje":"El monitoreo continúa.",
               "fecha":"2026-11-24T16:20:00.000Z","laboratorio":"Lab X","demostracion":true,"extra":1},
              {"matriz":"cola","zona":{"lat_min":-15.5,"lon_min":-67.9},"nivel":"CRIBADO — requiere confirmación",
               "tipo":"aviso_cribado","mensaje":"m","fecha":"2026-11-23"}
            ]}
        """.trimIndent()
        val resp = api.json.decodeFromString(AlertasResponse.serializer(), texto)
        val entrantes = resp.items.mapNotNull { AhgoraApi.aEntrante(it, 0L) }
        assertEquals(2, entrantes.size)
        assertEquals(EstadoAviso.CONFIRMADO, entrantes[0].estado)
        assertEquals(EstadoAviso.PENDIENTE, entrantes[1].estado)
        assertEquals(Celda.desdeMinimos(-15.5, -67.9), entrantes[0].celda)
        assertTrue(entrantes[0].demostracion)
    }
}
