package bo.ahgora.comunidad.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import bo.ahgora.comunidad.data.zona.Celda

/** Estado público de un aviso (guía, sección 05). */
enum class EstadoAviso { PENDIENTE, EN_ANALISIS, CONFIRMADO }

/** Tipo de mensaje que aprueba el responsable en Ahgora Web (`alerta.tipo`). */
object TipoAviso {
    const val AVISO_CRIBADO = "aviso_cribado"
    const val ALERTA_CONFIRMADA = "alerta_confirmada"
    const val TRANQUILIZADOR = "tranquilizador"
}

object OrigenAviso {
    const val SERVIDOR = "servidor"
    const val DEMO = "demo"
}

/**
 * Copia local de un aviso APROBADO. Solo contiene lo que el servidor publica para la comunidad:
 * zona generalizada (celda de 0,1°), estado, fechas, texto aprobado y entidad. Nunca coordenadas
 * exactas, lecturas crudas ni responsables.
 */
@Entity(
    tableName = "avisos",
    indices = [Index("origen"), Index(value = ["latIdx", "lonIdx"])],
)
data class AvisoEntity(
    @PrimaryKey val id: String,
    val version: Int,
    val origen: String,
    /** sedimento | cola | pescado | suelo | … */
    val matriz: String,
    val estado: EstadoAviso,
    val tipo: String?,
    /** Texto aprobado por el responsable. */
    val mensaje: String,
    val latIdx: Int,
    val lonIdx: Int,
    /** Fecha de publicación o revisión que informa el servidor (epoch ms). */
    val publicadoEn: Long,
    /** Primera vez que este teléfono lo guardó. */
    val descargadoEn: Long,
    /** Última vez que cambió su contenido en este teléfono. */
    val actualizadoEn: Long,
    /** Laboratorio que emitió el resultado (solo si está confirmado). */
    val laboratorio: String?,
    /** El servidor lo marca como dato de demostración. */
    val demostracion: Boolean,
    val retirado: Boolean = false,
    val retiradoEn: Long? = null,
    val motivoRetiro: String? = null,
    /** Si este aviso actualiza a otro anterior (misma zona y matriz). */
    val reemplazaA: String? = null,
    val reemplazadoPor: String? = null,
    val leido: Boolean = false,
    /** Huella del contenido para detectar cambios aunque el servidor no informe versión. */
    val huella: String,
) {
    val celda: Celda get() = Celda(latIdx, lonIdx)
    val esEjemplo: Boolean get() = demostracion || origen == OrigenAviso.DEMO
}
