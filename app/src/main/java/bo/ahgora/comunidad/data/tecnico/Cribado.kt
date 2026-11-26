package bo.ahgora.comunidad.data.tecnico

import bo.ahgora.comunidad.data.remote.ControlesDto
import bo.ahgora.comunidad.data.remote.RgbcDto
import kotlin.math.sqrt

/** Semáforo del modo técnico (guía, sección 2.4). El valor `servidor` es el que acepta `/api/sync`. */
enum class Semaforo(val servidor: String, val etiqueta: String, val accion: String) {
    ROJO("rojo", "Prioridad alta", "Enviar primero al laboratorio y priorizar el sitio."),
    AMARILLO("amarillo", "Verificar", "Repetir la prueba y enviar al laboratorio."),
    VERDE("verde", "Prioridad baja", "Registrar y seguir el plan de muestreo de control. No significa «libre de mercurio»."),
    INVALIDO("invalido", "Inválido", "No clasificar: repetir la prueba.");

    companion object {
        fun deServidor(v: String?): Semaforo = entries.firstOrNull { it.servidor == v } ?: INVALIDO
    }
}

data class ResultadoCribado(
    val semaforo: Semaforo,
    /** Cambio de color entre la casilla sin usar y la reaccionada, en puntos de reflectancia (%). */
    val deltaE: Double?,
    val motivoInvalido: String?,
)

/**
 * Cálculo del cribado colorimétrico (guía, secciones 4.4 y 4.5) con un criterio PROVISIONAL.
 *
 * 1. Cada casilla se normaliza con la referencia blanca: reflectancia = canal / canal_blanco × 100.
 * 2. ΔE = distancia entre la casilla de Hg sin usar y la reaccionada en (R, G, B) normalizados,
 *    menos el blanco de matriz si se midió.
 * 3. Umbrales bajos a propósito (sesgo hacia la seguridad, sección 6.3).
 *
 * Mientras la curva no esté validada con el laboratorio aliado, todo resultado se rotula como
 * demostración (guía del proyecto, sección 03) y no se convierte en mg/kg.
 */
object Cribado {
    const val CALIBRACION = "SED-CAL-v0.1-demo"
    const val CALIBRACION_VALIDADA = false
    const val UMBRAL_VERIFICAR = 4.0
    const val UMBRAL_ALTA = 8.0
    /** Por debajo, la referencia blanca está demasiado oscura (LED, caja abierta o sensor tapado). */
    const val CLARO_MINIMO = 500
    /** Lectura saturada del TCS34725 (16 bits). */
    const val SATURACION = 65_000

    const val ETIQUETA = "CRIBADO — NO ES DIAGNÓSTICO NI EVIDENCIA LEGAL"
    const val MENSAJE_COMUNIDAD = "Este resultado es una primera señal, no un diagnóstico ni una prueba legal. Un " +
        "resultado rojo significa que la muestra debe confirmarse en laboratorio con prioridad. Un resultado verde " +
        "no garantiza que no haya mercurio."

    fun reflectancia(x: RgbcDto, blanco: RgbcDto): Triple<Double, Double, Double> = Triple(
        x.r * 100.0 / blanco.r.coerceAtLeast(1),
        x.g * 100.0 / blanco.g.coerceAtLeast(1),
        x.b * 100.0 / blanco.b.coerceAtLeast(1),
    )

    fun deltaE(blanco: RgbcDto, sinUsar: RgbcDto, reaccionada: RgbcDto): Double {
        val a = reflectancia(sinUsar, blanco)
        val b = reflectancia(reaccionada, blanco)
        val dr = a.first - b.first
        val dg = a.second - b.second
        val db = a.third - b.third
        return sqrt(dr * dr + dg * dg + db * db)
    }

    fun evaluar(
        controles: ControlesDto,
        blanco: RgbcDto?,
        sinUsar: RgbcDto?,
        reaccionada: RgbcDto?,
        blancoMatrizDelta: Double? = null,
    ): ResultadoCribado {
        // Un control fallido produce inválido (guía, sección 4.4).
        val fallas = buildList {
            if (controles.blancoProcedimiento == "falla") add("blanco de procedimiento")
            if (controles.cloroLibreCero == "falla") add("cloro libre distinto de 0")
            if (controles.referenciaBlanca == "falla") add("referencia blanca")
            if (controles.ph == "falla") add("pH fuera de rango")
        }
        if (fallas.isNotEmpty()) return ResultadoCribado(Semaforo.INVALIDO, null, "Falló: " + fallas.joinToString(", "))
        if (blanco == null || sinUsar == null || reaccionada == null) {
            return ResultadoCribado(Semaforo.INVALIDO, null, "Faltan lecturas del lector")
        }
        if (blanco.c < CLARO_MINIMO) return ResultadoCribado(Semaforo.INVALIDO, null, "Referencia blanca muy oscura: revisar LED y caja")
        val todas = listOf(blanco, sinUsar, reaccionada).flatMap { listOf(it.r, it.g, it.b, it.c) }
        if (todas.any { it >= SATURACION }) return ResultadoCribado(Semaforo.INVALIDO, null, "Sensor saturado: bajar ganancia o luz")

        val de = (deltaE(blanco, sinUsar, reaccionada) - (blancoMatrizDelta ?: 0.0)).coerceAtLeast(0.0)
        val s = when {
            de >= UMBRAL_ALTA -> Semaforo.ROJO
            de >= UMBRAL_VERIFICAR -> Semaforo.AMARILLO
            else -> Semaforo.VERDE
        }
        return ResultadoCribado(s, Math.round(de * 10) / 10.0, null)
    }

    /** Promedio de varias lecturas del nodo (la guía pide 10 por medición). */
    fun promedio(lecturas: List<RgbcDto>): RgbcDto {
        require(lecturas.isNotEmpty())
        fun m(f: (RgbcDto) -> Int) = Math.round(lecturas.map(f).average()).toInt()
        return RgbcDto(m { it.r }, m { it.g }, m { it.b }, m { it.c })
    }
}
