package bo.ahgora.comunidad.data.zona

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Zona pública de Ahgora: celda de 0,1° (~11 km), la misma que calcula el servidor
 * (`zonaPublica` en Ahgora Web y `/api/alertas`). Se identifica por índices enteros para evitar
 * errores de coma flotante: `latIdx = floor(lat / 0.1)`.
 *
 * La app nunca guarda la coordenada exacta de la persona: solo esta celda.
 */
data class Celda(val latIdx: Int, val lonIdx: Int) {

    val latMin: Double get() = latIdx * PASO
    val lonMin: Double get() = lonIdx * PASO
    val latMax: Double get() = (latIdx + 1) * PASO
    val lonMax: Double get() = (lonIdx + 1) * PASO
    val latCentro: Double get() = (latIdx + 0.5) * PASO
    val lonCentro: Double get() = (lonIdx + 0.5) * PASO

    /** Distancia en celdas (Chebyshev): 0 = misma zona, 1 = zona vecina. */
    fun distancia(otra: Celda): Int = max(abs(latIdx - otra.latIdx), abs(lonIdx - otra.lonIdx))

    fun clave(): String = "$latIdx:$lonIdx"

    /** Texto corto con el rango de la celda, p. ej. «15,5–15,4° S · 67,9–67,8° O». */
    fun rangoLegible(): String {
        fun lat(v: Double) = "%.1f".format(abs(v)).replace('.', ',')
        val ns = if (latCentro < 0) "S" else "N"
        val eo = if (lonCentro < 0) "O" else "E"
        return "${lat(latMax)}–${lat(latMin)}° $ns · ${lat(lonMax)}–${lat(lonMin)}° $eo"
    }

    companion object {
        const val PASO = 0.1

        fun de(lat: Double, lon: Double): Celda =
            Celda(floor(lat / PASO).toInt(), floor(lon / PASO).toInt())

        /** Celda a partir de la zona que entrega el servidor (lat_min / lon_min ya redondeados). */
        fun desdeMinimos(latMin: Double, lonMin: Double): Celda =
            Celda((latMin / PASO).roundToInt(), (lonMin / PASO).roundToInt())

        fun desdeClave(clave: String?): Celda? {
            val partes = clave?.split(":") ?: return null
            if (partes.size != 2) return null
            val a = partes[0].toIntOrNull() ?: return null
            val b = partes[1].toIntOrNull() ?: return null
            return Celda(a, b)
        }
    }
}
