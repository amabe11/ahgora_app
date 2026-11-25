package bo.ahgora.comunidad.data.sync

import bo.ahgora.comunidad.data.local.AvisoEntity
import bo.ahgora.comunidad.data.local.EstadoAviso
import bo.ahgora.comunidad.data.zona.Celda
import java.security.MessageDigest

/** Aviso tal como llega de una fuente (servidor o demostración), ya normalizado. */
data class AvisoEntrante(
    /** Si el servidor no entrega id, se deriva uno estable del contenido. */
    val id: String?,
    val version: Int?,
    val matriz: String,
    val estado: EstadoAviso,
    val tipo: String?,
    val mensaje: String,
    val celda: Celda,
    val publicadoEn: Long,
    /** Texto original de la fecha (se usa para el id derivado). */
    val fechaOriginal: String?,
    val laboratorio: String?,
    val demostracion: Boolean,
    val retirado: Boolean = false,
    val motivoRetiro: String? = null,
)

data class ResultadoFusion(
    val guardar: List<AvisoEntity>,
    val nuevos: List<AvisoEntity>,
    val actualizados: List<AvisoEntity>,
    val retirados: List<AvisoEntity>,
) {
    val cambios: List<AvisoEntity> get() = nuevos + actualizados + retirados
}

/**
 * Reglas de sincronización (guía, sección 09):
 *  - una respuesta repetida no crea duplicados (id estable + huella de contenido);
 *  - una versión más antigua no reemplaza a una nueva;
 *  - lo que deja de publicarse se retira de la lista vigente, pero se conserva con su fecha y motivo;
 *  - si en la misma zona y matriz aparece un aviso nuevo, se enlaza como actualización del anterior.
 */
object Fusion {

    const val MOTIVO_RETIRO = "El responsable retiró este aviso de la publicación."
    const val MOTIVO_REEMPLAZO = "Hay una versión más reciente de este aviso."

    fun idDerivado(e: AvisoEntrante, origen: String): String =
        "$origen-" + sha1("${e.matriz}|${e.celda.clave()}|${e.tipo}|${e.fechaOriginal ?: e.publicadoEn}").take(16)

    fun huella(e: AvisoEntrante): String =
        sha1("${e.matriz}|${e.estado}|${e.tipo}|${e.mensaje}|${e.celda.clave()}|${e.laboratorio}|${e.publicadoEn}|${e.demostracion}|${e.retirado}")

    /**
     * @param existentes avisos guardados de ESTA fuente.
     * @param completo la respuesta es la lista completa de lo publicado: lo ausente se retira.
     *   (El servidor devuelve como máximo 200; si llega el tope no se retira nada por ausencia.)
     */
    fun fusionar(
        existentes: List<AvisoEntity>,
        entrantes: List<AvisoEntrante>,
        origen: String,
        ahora: Long,
        completo: Boolean,
    ): ResultadoFusion {
        val porId = existentes.associateBy { it.id }
        // Respuesta con repetidos: nos quedamos con la versión más alta de cada id.
        val unicos = entrantes
            .map { (it.id ?: idDerivado(it, origen)) to it }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, lista) -> lista.maxBy { it.version ?: 0 } }

        val nuevos = mutableListOf<AvisoEntity>()
        val actualizados = mutableListOf<AvisoEntity>()
        val retirados = mutableListOf<AvisoEntity>()
        val guardar = mutableListOf<AvisoEntity>()

        for ((id, e) in unicos) {
            val previo = porId[id]
            val h = huella(e)
            when {
                previo == null -> {
                    val nuevo = AvisoEntity(
                        id = id, version = e.version ?: 1, origen = origen, matriz = e.matriz,
                        estado = e.estado, tipo = e.tipo, mensaje = e.mensaje,
                        latIdx = e.celda.latIdx, lonIdx = e.celda.lonIdx,
                        publicadoEn = e.publicadoEn, descargadoEn = ahora, actualizadoEn = ahora,
                        laboratorio = e.laboratorio, demostracion = e.demostracion,
                        retirado = e.retirado, retiradoEn = if (e.retirado) e.publicadoEn else null,
                        motivoRetiro = if (e.retirado) e.motivoRetiro ?: MOTIVO_RETIRO else null,
                        leido = e.retirado, huella = h,
                    )
                    guardar += nuevo
                    if (!e.retirado) nuevos += nuevo
                }
                // Una versión más antigua no reemplaza a una más nueva.
                e.version != null && e.version < previo.version -> Unit
                e.retirado && !previo.retirado -> {
                    val r = previo.copy(
                        retirado = true, retiradoEn = ahora, leido = false, actualizadoEn = ahora,
                        motivoRetiro = e.motivoRetiro ?: MOTIVO_RETIRO, huella = h,
                    )
                    guardar += r; retirados += r
                }
                previo.huella != h || previo.retirado != e.retirado ||
                    (e.version != null && e.version > previo.version) -> {
                    val a = previo.copy(
                        version = e.version ?: (previo.version + 1), matriz = e.matriz, estado = e.estado,
                        tipo = e.tipo, mensaje = e.mensaje, latIdx = e.celda.latIdx, lonIdx = e.celda.lonIdx,
                        publicadoEn = e.publicadoEn, actualizadoEn = ahora, laboratorio = e.laboratorio,
                        demostracion = e.demostracion, retirado = e.retirado,
                        retiradoEn = if (e.retirado) ahora else null,
                        motivoRetiro = if (e.retirado) e.motivoRetiro ?: MOTIVO_RETIRO else null,
                        reemplazadoPor = if (e.retirado) previo.reemplazadoPor else null,
                        leido = false, huella = h,
                    )
                    guardar += a; actualizados += a
                }
                else -> Unit // sin cambios: respuesta repetida
            }
        }

        if (completo) {
            val vigentesAusentes = existentes.filter { !it.retirado && it.id !in unicos.keys }
            for (viejo in vigentesAusentes) {
                // ¿Lo reemplaza un aviso nuevo de la misma zona y matriz?
                val idx = nuevos.indexOfFirst { it.celda == viejo.celda && it.matriz == viejo.matriz && it.reemplazaA == null }
                val reemplazo = nuevos.getOrNull(idx)
                val r = viejo.copy(
                    retirado = true, retiradoEn = ahora, actualizadoEn = ahora, leido = false,
                    motivoRetiro = if (reemplazo != null) MOTIVO_REEMPLAZO else MOTIVO_RETIRO,
                    reemplazadoPor = reemplazo?.id,
                )
                guardar += r; retirados += r
                if (reemplazo != null) {
                    val enlazado = reemplazo.copy(reemplazaA = viejo.id, version = maxOf(reemplazo.version, viejo.version + 1))
                    nuevos[idx] = enlazado
                    guardar[guardar.indexOfFirst { it.id == enlazado.id }] = enlazado
                }
            }
        }
        return ResultadoFusion(guardar, nuevos, actualizados, retirados)
    }

    private fun sha1(s: String): String =
        MessageDigest.getInstance("SHA-1").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
}
