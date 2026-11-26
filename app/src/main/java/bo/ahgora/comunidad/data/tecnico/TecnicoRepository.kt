package bo.ahgora.comunidad.data.tecnico

import bo.ahgora.comunidad.data.prefs.AjustesStore
import bo.ahgora.comunidad.data.remote.AhgoraApi
import bo.ahgora.comunidad.data.remote.EventoCustodiaDto
import bo.ahgora.comunidad.data.remote.LaboratorioDto
import bo.ahgora.comunidad.data.remote.MuestraDto
import bo.ahgora.comunidad.data.zona.Localidades
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.text.Normalizer
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class ResultadoEnvio(val ok: Boolean, val enviadas: Int, val error: String? = null)

/**
 * Datos del modo técnico (guía, secciones 2.3, 6.2 y 7.6): las muestras se guardan primero en el
 * teléfono (funciona sin internet) y se envían a `POST /api/sync` cuando hay señal.
 */
class TecnicoRepository(
    private val dao: MuestraDao,
    private val api: AhgoraApi,
    private val ajustes: AjustesStore,
    val sesion: SesionStore,
) {
    private val mutex = Mutex()
    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false }

    val muestras: Flow<List<MuestraEntity>> = dao.observarTodas()
    fun muestra(codigo: String): Flow<MuestraEntity?> = dao.observar(codigo)
    fun registro(m: MuestraEntity): MuestraDto = json.decodeFromString(m.registroJson)

    // ------------------------------------------------------------------ sesión

    sealed interface Ingreso {
        data object Ok : Ingreso
        data class Error(val mensaje: String) : Ingreso
    }

    suspend fun ingresar(usuario: String, clave: String, enLinea: Boolean): Ingreso {
        val u = usuario.trim()
        if (u.length < 2) return Ingreso.Error("Escribe tu nombre de usuario.")
        if (clave.isBlank()) return Ingreso.Error("Escribe la clave del equipo.")
        if (!enLinea) {
            return if (sesion.yaComprobada(u, clave)) { sesion.iniciar(u, clave); Ingreso.Ok }
            else Ingreso.Error("Sin señal. La primera vez se necesita internet para comprobar la clave.")
        }
        return when (api.verificarClave(ajustes.actual.apiBaseUrl, clave)) {
            AhgoraApi.Clave.VALIDA -> { sesion.iniciar(u, clave); Ingreso.Ok }
            AhgoraApi.Clave.INVALIDA -> Ingreso.Error("La clave del equipo no es correcta.")
            AhgoraApi.Clave.SIN_RESPUESTA ->
                if (sesion.yaComprobada(u, clave)) { sesion.iniciar(u, clave); Ingreso.Ok }
                else Ingreso.Error("El servidor no respondió. Intenta de nuevo con mejor señal.")
        }
    }

    fun ingresarDemostracion(usuario: String): Ingreso {
        val u = usuario.trim()
        if (u.length < 2) return Ingreso.Error("Escribe tu nombre.")
        sesion.iniciar(u, null)
        return Ingreso.Ok
    }

    fun cerrarSesion() = sesion.cerrar()

    // ------------------------------------------------------------------ muestras

    /** Código para la etiqueta del frasco: ALC-SED-261125-AM01 (localidad, matriz, fecha, iniciales y número). */
    suspend fun nuevoCodigo(matriz: String, lat: Double?, lon: Double?, ahora: Long = System.currentTimeMillis()): String {
        val loc = if (lat != null && lon != null) {
            val (l, km) = Localidades.masCercanaA(lat, lon)
            if (km <= 15) sigla(l.nombre) else "AHG"
        } else sigla(Localidades.ALCOCHE.nombre)
        val mat = when (matriz) { "cola" -> "COL"; "pescado" -> "PEZ"; else -> "SED" }
        val fecha = DateTimeFormatter.ofPattern("yyMMdd").withZone(ZONA).format(Instant.ofEpochMilli(ahora))
        val ini = iniciales(sesion.actual?.usuario.orEmpty())
        val prefijo = "$loc-$mat-$fecha-$ini"
        val usados = dao.codigosCon(prefijo).mapNotNull { it.removePrefix(prefijo).toIntOrNull() }
        return prefijo + "%02d".format((usados.maxOrNull() ?: 0) + 1)
    }

    suspend fun existe(codigo: String): Boolean = dao.obtener(codigo) != null

    suspend fun guardarNueva(m: MuestraDto, fotoRuta: String?) = mutex.withLock {
        val usuario = sesion.actual?.usuario ?: m.responsable
        val conCustodia = m.copy(
            custodia = m.custodia + EventoCustodiaDto("registrada", m.fechaIso, usuario, "Muestra registrada y etiquetada en campo"),
        )
        dao.guardar(entidad(conCustodia, fotoRuta, soloLocal = sesion.actual?.demostracion != false))
    }

    /** Envío al laboratorio: pasa a «en verificación» y queda en la cadena de custodia. */
    suspend fun marcarEnviadaLaboratorio(codigo: String, laboratorio: String) = mutex.withLock {
        val actual = dao.obtener(codigo) ?: return@withLock
        val r = registro(actual)
        val ahora = isoAhora()
        val usuario = sesion.actual?.usuario ?: r.responsable
        val nuevo = r.copy(
            certeza = if (r.certeza == "confirmado") r.certeza else "en_verificacion",
            laboratorio = (r.laboratorio ?: LaboratorioDto()).copy(laboratorio = laboratorio.trim(), enviadaEn = ahora),
            custodia = r.custodia + EventoCustodiaDto("enviada_laboratorio", ahora, usuario, "Enviada a ${laboratorio.trim()}"),
        )
        dao.guardar(entidad(nuevo, actual.fotoRuta, actual.soloLocal).copy(sincronizadoEn = actual.sincronizadoEn))
    }

    suspend fun borrarNoEnviada(codigo: String): Boolean = dao.borrarNoEnviada(codigo) > 0

    private fun entidad(m: MuestraDto, fotoRuta: String?, soloLocal: Boolean) = MuestraEntity(
        codigo = m.codigo,
        registroJson = json.encodeToString(MuestraDto.serializer(), m),
        matriz = m.matriz,
        semaforo = m.semaforo,
        certeza = m.certeza,
        fechaMs = runCatching { OffsetDateTime.parse(m.fechaIso).toInstant().toEpochMilli() }.getOrDefault(System.currentTimeMillis()),
        lat = m.lat, lon = m.lon,
        responsable = m.responsable,
        demostracion = m.demostracion,
        fotoRuta = fotoRuta,
        pendienteSync = true,
        soloLocal = soloLocal,
        actualizadoEn = System.currentTimeMillis(),
    )

    // ------------------------------------------------------------------ servidor

    suspend fun enviarPendientes(): ResultadoEnvio = mutex.withLock {
        val s = sesion.actual ?: return@withLock ResultadoEnvio(false, 0, "Sin sesión de técnico")
        if (s.demostracion) return@withLock ResultadoEnvio(false, 0, "Modo demostración: las muestras no se envían")
        val clave = sesion.clave() ?: return@withLock ResultadoEnvio(false, 0, "Vuelve a ingresar la clave del equipo")
        // Las muestras de una sesión de demostración (soloLocal) no salen del teléfono.
        val pendientes = dao.pendientes()
        if (pendientes.isEmpty()) return@withLock ResultadoEnvio(true, 0)
        return@withLock runCatching {
            pendientes.chunked(100).forEach { lote ->
                api.sincronizarMuestras(ajustes.actual.apiBaseUrl, clave, lote.map { registro(it) })
                val cuando = System.currentTimeMillis()
                lote.forEach { dao.marcarEnviada(it.codigo, it.actualizadoEn, cuando) }
            }
            ResultadoEnvio(true, pendientes.size)
        }.getOrElse { ResultadoEnvio(false, 0, it.message ?: "No se pudo enviar") }
    }

    // ------------------------------------------------------------------ exportar

    /** CSV para el laboratorio (UTF-8 con BOM, coma, punto decimal, hora ISO), con la cadena de custodia. */
    fun csv(lista: List<MuestraEntity>): String {
        val cab = listOf(
            "codigo", "matriz", "cabezal", "fecha_iso", "lat", "lon", "precision_m", "responsable", "localidad", "cuenca",
            "porciones", "profundidad_cm", "fino_pct", "especie", "talla_cm", "peso_g",
            "ref_r", "ref_g", "ref_b", "ref_c", "sin_r", "sin_g", "sin_b", "sin_c", "rea_r", "rea_g", "rea_b", "rea_c",
            "delta_e", "semaforo", "motivo_invalido", "blanco_procedimiento", "cloro_libre_cero", "referencia_blanca", "ph",
            "calibracion", "certeza", "laboratorio", "enviada_en", "demostracion", "observaciones", "custodia",
        )
        fun esc(v: Any?): String {
            val t = v?.toString() ?: ""
            return if (t.any { it == ',' || it == '"' || it == '\n' }) "\"" + t.replace("\"", "\"\"") + "\"" else t
        }
        val filas = lista.map { e ->
            val m = registro(e)
            val l = m.lecturas
            listOf(
                m.codigo, m.matriz, m.cabezal, m.fechaIso, m.lat, m.lon, m.precisionM, m.responsable, m.localidad, m.cuenca,
                m.muestreo?.porciones, m.muestreo?.profundidadCm, m.muestreo?.finoPct, m.muestreo?.especie, m.muestreo?.tallaCm, m.muestreo?.pesoG,
                l?.referenciaBlanca?.r, l?.referenciaBlanca?.g, l?.referenciaBlanca?.b, l?.referenciaBlanca?.c,
                l?.casillaSinUsar?.r, l?.casillaSinUsar?.g, l?.casillaSinUsar?.b, l?.casillaSinUsar?.c,
                l?.casillaReaccionada?.r, l?.casillaReaccionada?.g, l?.casillaReaccionada?.b, l?.casillaReaccionada?.c,
                l?.deltaE, m.semaforo, m.motivoInvalido, m.controles?.blancoProcedimiento, m.controles?.cloroLibreCero,
                m.controles?.referenciaBlanca, m.controles?.ph, m.calibracion, m.certeza, m.laboratorio?.laboratorio,
                m.laboratorio?.enviadaEn, m.demostracion, m.muestreo?.observaciones,
                m.custodia.joinToString(" | ") { "${it.fecha} ${it.evento} (${it.responsable})" },
            ).joinToString(",") { esc(it) }
        }
        return "﻿" + (listOf(cab.joinToString(",")) + filas).joinToString("\r\n")
    }

    companion object {
        val ZONA: ZoneId = ZoneId.of("America/La_Paz")
        fun isoAhora(ms: Long = System.currentTimeMillis()): String =
            OffsetDateTime.ofInstant(Instant.ofEpochMilli(ms), ZONA).withNano(0).toString()

        fun sigla(nombre: String): String {
            val sin = Normalizer.normalize(nombre, Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")
            return sin.filter { it.isLetter() }.take(3).uppercase().padEnd(3, 'X')
        }

        fun iniciales(usuario: String): String {
            val partes = Normalizer.normalize(usuario, Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")
                .split(Regex("[^A-Za-z]+")).filter { it.isNotEmpty() }
            val ini = when {
                partes.size >= 2 -> "${partes[0][0]}${partes[1][0]}"
                partes.size == 1 -> partes[0].take(2)
                else -> "TC"
            }
            return ini.uppercase().padEnd(2, 'X')
        }
    }
}
