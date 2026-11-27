package bo.ahgora.comunidad.data.remote

import bo.ahgora.comunidad.data.local.EstadoAviso
import bo.ahgora.comunidad.data.sync.AvisoEntrante
import bo.ahgora.comunidad.data.zona.Celda
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/** Respuesta de `GET /api/alertas` del Servidor Ahgora (público, sin clave). */
@Serializable
data class AlertasResponse(
    val aviso: String? = null,
    val items: List<AlertaDto> = emptyList(),
)

@Serializable
data class ZonaDto(
    @SerialName("lat_min") val latMin: Double,
    @SerialName("lon_min") val lonMin: Double,
    @SerialName("lat_max") val latMax: Double? = null,
    @SerialName("lon_max") val lonMax: Double? = null,
)

/**
 * Un aviso aprobado. `id`, `version`, `certeza` y `retirado` son opcionales: el servidor actual no los
 * envía, pero si se agregan (contrato propuesto en la guía, sección 09) la app los usa directamente.
 */
@Serializable
data class AlertaDto(
    val id: String? = null,
    val version: Int? = null,
    val matriz: String? = null,
    val zona: ZonaDto? = null,
    val nivel: String? = null,
    val certeza: String? = null,
    val tipo: String? = null,
    val mensaje: String? = null,
    val fecha: String? = null,
    val laboratorio: String? = null,
    val demostracion: Boolean = false,
    val retirado: Boolean = false,
)

@Serializable
data class ReporteDto(
    val tipo: String,
    val descripcion: String,
    val zona: ZonaDto?,
    val creado_en: String,
    val origen: String = "ahgora-comunidad-android",
)

class ServidorNoRecibeReportes : IOException("El servidor todavía no recibe reportes")

class AhgoraApi(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build(),
) {
    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    suspend fun alertas(baseUrl: String): AlertasResponse = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(url(baseUrl, "api/alertas")).get()
            .header("Accept", "application/json").build()
        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) throw IOException("El servidor respondió ${res.code}")
            json.decodeFromString<AlertasResponse>(res.body?.string().orEmpty())
        }
    }

    /** `POST /api/reportes`. Si el servidor aún no tiene la ruta (404/405) se informa aparte. */
    suspend fun enviarReporte(baseUrl: String, reporte: ReporteDto) = withContext(Dispatchers.IO) {
        val body = json.encodeToString(reporte).toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url(url(baseUrl, "api/reportes")).post(body).build()
        client.newCall(req).execute().use { res ->
            if (res.code == 404 || res.code == 405) throw ServidorNoRecibeReportes()
            if (!res.isSuccessful) throw IOException("El servidor respondió ${res.code}")
        }
    }

    // ------------------------------------------------------------------ modo técnico (con clave del equipo)

    /** Resultado de comprobar la clave del equipo contra el servidor. */
    enum class Clave { VALIDA, INVALIDA, SIN_RESPUESTA }

    /** `GET /api/datos?coleccion=muestras&limit=1` con `x-ahg-key`: 200 = clave válida, 401 = no. */
    suspend fun verificarClave(baseUrl: String, clave: String): Clave = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(url(baseUrl, "api/datos?coleccion=muestras&limit=1"))
            .header("x-ahg-key", clave).get().build()
        runCatching {
            client.newCall(req).execute().use { res ->
                when {
                    res.isSuccessful -> Clave.VALIDA
                    res.code == 401 || res.code == 403 -> Clave.INVALIDA
                    else -> Clave.SIN_RESPUESTA
                }
            }
        }.getOrDefault(Clave.SIN_RESPUESTA)
    }

    /** `POST /api/sync` con `x-ahg-key`: inserta o actualiza por `codigo` (repetir no duplica). */
    suspend fun sincronizarMuestras(baseUrl: String, clave: String, muestras: List<MuestraDto>): SyncResponse =
        withContext(Dispatchers.IO) {
            val body = json.encodeToString(SyncRequest(muestras = muestras)).toRequestBody("application/json".toMediaType())
            val req = Request.Builder().url(url(baseUrl, "api/sync")).header("x-ahg-key", clave).post(body).build()
            client.newCall(req).execute().use { res ->
                if (res.code == 401) throw IOException("La clave del equipo ya no es válida")
                if (!res.isSuccessful) throw IOException("El servidor respondió ${res.code}")
                json.decodeFromString<SyncResponse>(res.body?.string().orEmpty())
            }
        }

    /** `GET /api/lecturas` con `x-ahg-key`: lecturas que el lector ESP32 envió por WiFi, más recientes primero. */
    suspend fun lecturasLector(baseUrl: String, clave: String, nodo: String? = null, desde: String? = null, limit: Int = 50): LecturasResponse =
        withContext(Dispatchers.IO) {
            val q = buildList {
                add("limit=$limit")
                nodo?.let { add("node_id=" + java.net.URLEncoder.encode(it, "UTF-8")) }
                desde?.let { add("desde=" + java.net.URLEncoder.encode(it, "UTF-8")) }
            }.joinToString("&")
            val req = Request.Builder().url(url(baseUrl, "api/lecturas?$q")).header("x-ahg-key", clave).get().build()
            client.newCall(req).execute().use { res ->
                if (res.code == 404) throw IOException("El servidor todavía no recibe lecturas del lector (falta /api/lecturas)")
                if (res.code == 401) throw IOException("La clave del equipo ya no es válida")
                if (!res.isSuccessful) throw IOException("El servidor respondió ${res.code}")
                json.decodeFromString<LecturasResponse>(res.body?.string().orEmpty())
            }
        }

    private fun url(base: String, ruta: String) = (if (base.endsWith("/")) base else "$base/") + ruta

    companion object {
        private val ZONA_BOLIVIA: ZoneId = ZoneId.of("America/La_Paz")

        fun aEntrante(dto: AlertaDto, ahora: Long): AvisoEntrante? {
            val zona = dto.zona ?: return null
            val estado = when {
                dto.certeza == "confirmado" -> EstadoAviso.CONFIRMADO
                dto.certeza == "en_verificacion" -> EstadoAviso.EN_ANALISIS
                dto.certeza == "cribado" -> EstadoAviso.PENDIENTE
                dto.nivel?.contains("CONFIRMADO", ignoreCase = true) == true -> EstadoAviso.CONFIRMADO
                dto.nivel?.contains("VERIFICACI", ignoreCase = true) == true -> EstadoAviso.EN_ANALISIS
                else -> EstadoAviso.PENDIENTE
            }
            return AvisoEntrante(
                id = dto.id?.takeIf { it.isNotBlank() },
                version = dto.version,
                matriz = dto.matriz?.lowercase()?.takeIf { it.isNotBlank() } ?: "muestra",
                estado = estado,
                tipo = dto.tipo,
                mensaje = dto.mensaje.orEmpty().trim(),
                celda = Celda.desdeMinimos(zona.latMin, zona.lonMin),
                publicadoEn = parseFecha(dto.fecha) ?: ahora,
                fechaOriginal = dto.fecha,
                laboratorio = dto.laboratorio?.takeIf { it.isNotBlank() },
                demostracion = dto.demostracion,
                retirado = dto.retirado,
            )
        }

        /** Acepta ISO con zona, ISO local (se asume hora de Bolivia) o solo fecha. */
        fun parseFecha(texto: String?): Long? {
            val t = texto?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            return runCatching { Instant.parse(t).toEpochMilli() }
                .recoverCatching { OffsetDateTime.parse(t).toInstant().toEpochMilli() }
                .recoverCatching { LocalDateTime.parse(t).atZone(ZONA_BOLIVIA).toInstant().toEpochMilli() }
                .recoverCatching { LocalDate.parse(t.take(10)).atStartOfDay(ZONA_BOLIVIA).toInstant().toEpochMilli() }
                .getOrNull()
        }
    }
}
