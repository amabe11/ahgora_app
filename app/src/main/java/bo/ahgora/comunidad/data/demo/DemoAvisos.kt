package bo.ahgora.comunidad.data.demo

import bo.ahgora.comunidad.data.local.EstadoAviso
import bo.ahgora.comunidad.data.local.TipoAviso
import bo.ahgora.comunidad.data.sync.AvisoEntrante
import bo.ahgora.comunidad.data.zona.Celda

/**
 * Avisos de DEMOSTRACIÓN generados alrededor de la zona elegida, para que la app se pueda mostrar
 * sin servidor ni señal. Todos van rotulados «Ejemplo»: no son mediciones reales ni se atribuyen a
 * un lugar real. Los textos siguen los mensajes de Ahgora Web (`ALERTAS` en model.js) y de la guía.
 *
 * `paso` recorre el caso de la guía (sección 07): pendiente → en análisis → resultado confirmado →
 * nuevo aviso en la zona. Cada paso es un «informe claramente simulado».
 */
object DemoAvisos {

    const val PASOS = 4
    private const val HORA = 3_600_000L
    private const val DIA = 24 * HORA

    const val TEXTO_CRIBADO = "Se tomaron muestras en tu zona y una necesita confirmación de laboratorio. " +
        "No es un diagnóstico. Te avisaremos cuando haya resultado."
    const val TEXTO_EN_ANALISIS = "La muestra fue enviada al laboratorio. El resultado todavía está pendiente."
    const val TEXTO_TRANQUILIZADOR = "El laboratorio analizó la muestra de tu zona y el resultado está por debajo " +
        "del umbral de referencia. El monitoreo continúa."
    const val TEXTO_CONFIRMADA = "Texto de demostración. En un caso real, las recomendaciones (consumo de pescado, " +
        "grupos vulnerables) las redacta y aprueba la autoridad de salud."
    private const val LAB = "Laboratorio aliado (ejemplo)"

    fun generar(celda: Celda, paso: Int, base: Long): List<AvisoEntrante> {
        fun c(dLat: Int, dLon: Int) = Celda(celda.latIdx + dLat, celda.lonIdx + dLon)
        fun e(
            id: String, version: Int, matriz: String, estado: EstadoAviso, tipo: String?, msg: String,
            zona: Celda, haceMs: Long, lab: String? = null, retirado: Boolean = false, motivo: String? = null,
        ) = AvisoEntrante(
            id = id, version = version, matriz = matriz, estado = estado, tipo = tipo, mensaje = msg,
            celda = zona, publicadoEn = base - haceMs, fechaOriginal = null, laboratorio = lab,
            demostracion = true, retirado = retirado, motivoRetiro = motivo,
        )

        val lista = mutableListOf(
            e("demo-2", 1, "cola", EstadoAviso.EN_ANALISIS, TipoAviso.AVISO_CRIBADO,
                "Una muestra de cola de una zona vecina fue enviada al laboratorio. El resultado todavía está pendiente.",
                c(1, 0), 3 * DIA),
            e("demo-3", 1, "sedimento", EstadoAviso.CONFIRMADO, TipoAviso.TRANQUILIZADOR,
                TEXTO_TRANQUILIZADOR, c(0, -1), 10 * DIA, lab = LAB),
            e("demo-4", 1, "pescado", EstadoAviso.CONFIRMADO, TipoAviso.ALERTA_CONFIRMADA,
                TEXTO_CONFIRMADA, c(-2, 1), 20 * DIA, lab = LAB),
            e("demo-5", 1, "sedimento", EstadoAviso.PENDIENTE, TipoAviso.AVISO_CRIBADO,
                TEXTO_CRIBADO, celda, 30 * DIA, retirado = true,
                motivo = "Rectificado por el responsable: la muestra se registró en otra zona."),
        )
        // El caso principal en la zona de la persona avanza con cada paso simulado.
        lista += when (paso.mod(PASOS)) {
            0 -> e("demo-1", 1, "sedimento", EstadoAviso.PENDIENTE, TipoAviso.AVISO_CRIBADO, TEXTO_CRIBADO, celda, 2 * HORA)
            1 -> e("demo-1", 2, "sedimento", EstadoAviso.EN_ANALISIS, TipoAviso.AVISO_CRIBADO, TEXTO_EN_ANALISIS, celda, HORA)
            else -> e("demo-1", 3, "sedimento", EstadoAviso.CONFIRMADO, TipoAviso.TRANQUILIZADOR, TEXTO_TRANQUILIZADOR,
                celda, HORA / 2, lab = LAB)
        }
        if (paso.mod(PASOS) == 3) {
            lista += e("demo-6", 1, "cola", EstadoAviso.PENDIENTE, TipoAviso.AVISO_CRIBADO,
                "Se tomó una muestra de cola en tu zona que requiere análisis de laboratorio. " +
                    "Publicaremos una actualización cuando se revise el resultado.", celda, 0)
        }
        return lista
    }

    fun descripcionPaso(paso: Int): String = when (paso.mod(PASOS)) {
        0 -> "Aviso de tu zona: confirmación pendiente"
        1 -> "La muestra pasó a «En análisis»"
        2 -> "El laboratorio confirmó el resultado"
        else -> "Llegó un aviso nuevo para tu zona"
    }
}
