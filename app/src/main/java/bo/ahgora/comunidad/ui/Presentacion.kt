package bo.ahgora.comunidad.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import bo.ahgora.comunidad.data.local.AvisoEntity
import bo.ahgora.comunidad.data.local.EstadoAviso
import bo.ahgora.comunidad.data.local.TipoAviso
import bo.ahgora.comunidad.data.zona.Celda
import bo.ahgora.comunidad.data.zona.Localidades
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Cómo se presenta cada estado (guía, secciones 05 y 06): siempre texto + ícono + color, nunca el
 * color solo. Los textos son propuestas de interfaz para validar con personas de la zona.
 */
enum class Presentacion(
    val etiqueta: String,
    val icono: ImageVector,
    val color: Color,
    val fondo: Color,
    val queSignifica: String,
    /** Orden para el mapa: cuál destacar si una zona tiene varios avisos. */
    val prioridad: Int,
) {
    PENDIENTE(
        "Confirmación pendiente", Icons.Outlined.HourglassTop, Color(0xFF7A5100), Color(0xFFFFF1CC),
        "Una prueba rápida en el sitio (cribado) indicó que una muestra debe revisarse en laboratorio. " +
            "Todavía no hay resultado. No es un diagnóstico.", 3,
    ),
    EN_ANALISIS(
        "En análisis", Icons.Outlined.Science, Color(0xFF1D5B99), Color(0xFFE2EEF9),
        "La muestra ya está en el laboratorio. El resultado todavía está pendiente.", 2,
    ),
    CONFIRMADO_INDICACIONES(
        "Resultado con indicaciones", Icons.Outlined.ReportProblem, Color(0xFF9C3D10), Color(0xFFFCE9DF),
        "El laboratorio confirmó el resultado y la autoridad aprobó indicaciones para esta zona. " +
            "Sigue solo las indicaciones del aviso.", 4,
    ),
    CONFIRMADO(
        "Resultado confirmado", Icons.Outlined.TaskAlt, Color(0xFF1E6B3A), Color(0xFFE1F2E6),
        "El laboratorio analizó la muestra y emitió un resultado revisado.", 1,
    ),
    RETIRADO(
        "Actualizado o retirado", Icons.Outlined.History, Color(0xFF4F5B62), Color(0xFFECEFF1),
        "Este aviso cambió o ya no está vigente. Consulta la fecha y el motivo.", 0,
    );

    companion object {
        fun de(a: AvisoEntity): Presentacion = when {
            a.retirado -> RETIRADO
            a.estado == EstadoAviso.PENDIENTE -> PENDIENTE
            a.estado == EstadoAviso.EN_ANALISIS -> EN_ANALISIS
            a.tipo == TipoAviso.ALERTA_CONFIRMADA -> CONFIRMADO_INDICACIONES
            else -> CONFIRMADO
        }
    }
}

object Textos {
    const val PRIMERA_SENAL = "Primera señal, no un diagnóstico ni una prueba legal."
    const val SIN_AVISOS = "Por ahora no hay avisos para tu comunidad."

    fun matriz(m: String): String = when (m.lowercase()) {
        "sedimento" -> "sedimento"
        "cola", "colas" -> "cola minera"
        "pescado", "pez" -> "pescado"
        "suelo" -> "suelo"
        "cabello" -> "cabello"
        else -> m
    }

    fun queSeAnalizo(m: String): String = when (m.lowercase()) {
        "sedimento" -> "Material del fondo del río o de la ribera."
        "cola", "colas" -> "Residuo que deja el procesamiento del mineral en la planta o el campamento."
        "pescado", "pez" -> "Tejido de pescado de la zona."
        "suelo" -> "Tierra de la zona."
        else -> "El tipo de muestra que indica el aviso."
    }

    fun titulo(a: AvisoEntity): String = "Muestra de ${matriz(a.matriz)}"

    /** «Qué pasó» cuando el responsable no escribió un texto propio. */
    fun quePaso(a: AvisoEntity): String = when (Presentacion.de(a)) {
        Presentacion.PENDIENTE -> "Una muestra de ${matriz(a.matriz)} de esta zona necesita análisis de laboratorio. " +
            "Publicaremos una actualización cuando se revise el resultado."
        Presentacion.EN_ANALISIS -> "La muestra fue enviada al laboratorio. El resultado todavía está pendiente."
        Presentacion.CONFIRMADO, Presentacion.CONFIRMADO_INDICACIONES ->
            "El laboratorio emitió un resultado para una muestra de ${matriz(a.matriz)}. Consulta el aviso revisado y su fecha."
        Presentacion.RETIRADO -> "Este aviso cambió. Consulta la fecha y el motivo de la actualización."
    }

    fun queHacer(a: AvisoEntity): String = when (Presentacion.de(a)) {
        Presentacion.PENDIENTE -> "Todavía no hay resultado. Te avisaremos cuando el laboratorio responda. " +
            "Si tienes dudas, pregunta al contacto oficial."
        Presentacion.EN_ANALISIS -> "Espera: te avisaremos cuando haya resultado. Si tienes dudas, pregunta al contacto oficial."
        Presentacion.CONFIRMADO -> "Se seguirá revisando. Si tienes dudas, pregunta al contacto oficial."
        Presentacion.CONFIRMADO_INDICACIONES -> "Sigue solo las indicaciones de la autoridad de salud que están en este aviso. " +
            "Si tienes dudas, pregunta al contacto oficial."
        Presentacion.RETIRADO -> "Abre la versión vigente si está disponible."
    }

    fun alcance(a: AvisoEntity): String = when (a.matriz.lowercase()) {
        "pescado", "pez" -> "Este resultado describe el pescado analizado. Las indicaciones sobre su consumo las " +
            "determina la autoridad de salud."
        else -> "El análisis de una muestra de ${matriz(a.matriz)} describe ese material. No es un análisis del agua " +
            "que bebes ni de tu salud."
    }

    fun entidad(a: AvisoEntity): String = when {
        a.estado == EstadoAviso.CONFIRMADO && a.laboratorio != null ->
            "Resultado de ${a.laboratorio}. Aviso revisado y aprobado por el responsable de Ahgora."
        else -> "Aviso revisado y aprobado por el responsable de Ahgora para la zona."
    }
}

object Formato {
    private val es = Locale.forLanguageTag("es-BO")
    private val fechaHora = SimpleDateFormat("d MMM yyyy · HH:mm", es)
    private val fechaCorta = SimpleDateFormat("d MMM · HH:mm", es)

    fun fechaHora(ms: Long): String = fechaHora.format(Date(ms)).replace(".", "")
    fun fechaCorta(ms: Long): String = fechaCorta.format(Date(ms)).replace(".", "")

    fun haceCuanto(ms: Long, ahora: Long = System.currentTimeMillis()): String {
        val min = ((ahora - ms) / 60_000).coerceAtLeast(0)
        return when {
            min < 1 -> "hace un momento"
            min < 60 -> "hace $min min"
            min < 24 * 60 -> "hace ${min / 60} h"
            min < 48 * 60 -> "ayer"
            else -> "hace ${min / (24 * 60)} días"
        }
    }

    /** «Publicado hace 2 h» o «Publicado el 24 nov» (más fácil de leer que fecha y hora). */
    fun haceCuantoPublicado(ms: Long, ahora: Long = System.currentTimeMillis()): String =
        if (ahora - ms < 48 * 3_600_000L) "Publicado ${haceCuanto(ms, ahora)}"
        else "Publicado el " + SimpleDateFormat("d MMM", es).format(Date(ms)).replace(".", "")

    /** Relación entre la zona del aviso y la de la persona. */
    fun relacion(celda: Celda, mia: Celda?): String {
        if (mia == null) return nombreZona(celda, null, null)
        return when (celda.distancia(mia)) {
            0 -> "En tu comunidad"
            1 -> nombreZona(celda, null, null).let { if (it.startsWith("Cerca de")) it else "Cerca · $it" }
            else -> {
                val km = Localidades.distanciaKm(celda.latCentro, celda.lonCentro, mia.latCentro, mia.lonCentro)
                "A unos ${(km / 5).roundToInt() * 5} km · ${nombreZona(celda, null, null)}"
            }
        }
    }

    fun nombreZona(celda: Celda, mia: Celda?, nombreMio: String?): String =
        if (mia != null && celda == mia && nombreMio != null) nombreMio else Localidades.nombreSugerido(celda)
}
