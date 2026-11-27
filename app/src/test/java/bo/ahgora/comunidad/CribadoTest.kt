package bo.ahgora.comunidad

import bo.ahgora.comunidad.data.remote.ControlesDto
import bo.ahgora.comunidad.data.remote.MuestraDto
import bo.ahgora.comunidad.data.remote.RgbcDto
import bo.ahgora.comunidad.data.tecnico.Cribado
import bo.ahgora.comunidad.data.tecnico.Semaforo
import bo.ahgora.comunidad.data.tecnico.TecnicoRepository
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CribadoTest {
    private val blanco = RgbcDto(21480, 23310, 20120, 60000)
    private val sinUsar = RgbcDto(15820, 16950, 12730, 45210)
    private val ok = ControlesDto("ok", "ok", "ok", "no_medido", "ok")

    private fun reaccion(caida: Double) =
        RgbcDto((sinUsar.r * (1 - caida)).toInt(), (sinUsar.g * (1 - caida)).toInt(), (sinUsar.b * (1 - caida)).toInt(), sinUsar.c)

    @Test
    fun sin_cambio_de_color_es_prioridad_baja() {
        val r = Cribado.evaluar(ok, blanco, sinUsar, sinUsar)
        assertEquals(Semaforo.VERDE, r.semaforo)
        assertEquals(0.0, r.deltaE!!, 0.001)
    }

    @Test
    fun cambio_grande_es_prioridad_alta_y_medio_es_verificar() {
        assertEquals(Semaforo.ROJO, Cribado.evaluar(ok, blanco, sinUsar, reaccion(0.15)).semaforo)
        val medio = Cribado.evaluar(ok, blanco, sinUsar, reaccion(0.06))
        assertEquals(Semaforo.AMARILLO, medio.semaforo)
        assertTrue(medio.deltaE!! >= Cribado.UMBRAL_VERIFICAR && medio.deltaE!! < Cribado.UMBRAL_ALTA)
    }

    @Test
    fun control_fallido_o_lectura_faltante_es_invalido() {
        val cloro = Cribado.evaluar(ok.copy(cloroLibreCero = "falla"), blanco, sinUsar, reaccion(0.2))
        assertEquals(Semaforo.INVALIDO, cloro.semaforo)
        assertTrue(cloro.motivoInvalido!!.contains("cloro"))
        assertEquals(Semaforo.INVALIDO, Cribado.evaluar(ok, blanco, sinUsar, null).semaforo)
        assertEquals(Semaforo.INVALIDO, Cribado.evaluar(ok, RgbcDto(100, 100, 100, 200), sinUsar, sinUsar).semaforo)
        assertEquals(Semaforo.INVALIDO, Cribado.evaluar(ok, blanco, sinUsar, RgbcDto(65535, 1, 1, 65535)).semaforo)
    }

    @Test
    fun el_semaforo_usa_los_valores_que_acepta_el_servidor() {
        assertEquals(setOf("rojo", "amarillo", "verde", "invalido"), Semaforo.entries.map { it.servidor }.toSet())
        assertEquals(Semaforo.INVALIDO, Semaforo.deServidor("otra cosa"))
    }

    @Test
    fun promedio_de_lecturas() {
        assertEquals(RgbcDto(2, 3, 4, 5), Cribado.promedio(listOf(RgbcDto(1, 2, 3, 4), RgbcDto(3, 4, 5, 6))))
    }

    @Test
    fun codigo_de_muestra() {
        assertEquals("ALC", TecnicoRepository.sigla("Alcoche"))
        assertEquals("POT", TecnicoRepository.sigla("Potosí"))
        assertEquals("AM", TecnicoRepository.iniciales("Alvin Mamani"))
        assertEquals("LO", TecnicoRepository.iniciales("loana"))
        assertEquals("TC", TecnicoRepository.iniciales(""))
    }

    @Test
    fun entiende_la_respuesta_de_api_lecturas() {
        // Forma real devuelta por /api/lecturas (probada con la API local y MongoDB).
        val texto = """{"coleccion":"lecturas","total":1,"items":[{"node_id":"AHG-SED-01","seq":49,"r":1862,"g":611,"b":529,"c":3052,
            "r_norm":155.57,"g_norm":51.05,"b_norm":44.2,"medido_en":"2026-11-27T00:23:56.000Z","recibido_en":"2026-11-27T00:23:58.218Z",
            "test_mode":true,"sensor":"TCS34725","firmware":"ahgora-wifi-0.2","rssi":-57}],
            "nodos":[{"node_id":"AHG-SED-01","total_24h":51,"ultima":{"node_id":"AHG-SED-01","seq":50,"r":1411,"g":1506,"b":1287,"c":4297,
            "medido_en":null,"recibido_en":"2026-11-27T00:23:58.218Z","rssi":null}}],"ahora":"2026-11-27T00:24:00.000Z"}"""
        val r = Json { ignoreUnknownKeys = true }.decodeFromString(bo.ahgora.comunidad.data.remote.LecturasResponse.serializer(), texto)
        assertEquals(1862, r.items.single().r)
        assertEquals("AHG-SED-01", r.nodos.single().nodeId)
        assertEquals(51, r.nodos.single().total24h)
        assertNull(r.nodos.single().ultima!!.rssi)
        assertEquals("2026-11-27T00:24:00.000Z", r.ahora)
    }

    @Test
    fun el_registro_sigue_el_contrato_del_servidor() {
        val json = Json { encodeDefaults = true; explicitNulls = false }
        val m = MuestraDto(codigo = "ALC-SED-261126-AM01", matriz = "sedimento", cabezal = "SED", fechaIso = "2026-11-26T10:00:00-04:00",
            lat = -15.75, lon = -67.67, responsable = "Alvin", semaforo = "rojo")
        val o = Json.parseToJsonElement(json.encodeToString(MuestraDto.serializer(), m)).jsonObject
        assertEquals("ALC-SED-261126-AM01", o["codigo"]!!.jsonPrimitive.content)
        assertEquals("2026-11-26T10:00:00-04:00", o["fecha_iso"]!!.jsonPrimitive.content)
        assertEquals("restringido", o["visibilidad"]!!.jsonPrimitive.content)
        assertEquals("cribado", o["certeza"]!!.jsonPrimitive.content)
        assertNull(o["lecturas"])
    }
}
