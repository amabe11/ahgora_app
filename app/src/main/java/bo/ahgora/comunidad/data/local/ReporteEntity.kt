package bo.ahgora.comunidad.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class EstadoReporte { EN_COLA, ENVIADO, SERVIDOR_NO_DISPONIBLE }

/**
 * Reporte comunitario voluntario (guía, sección 06 «Ayuda y participación»). Se guarda en el
 * teléfono y se envía cuando hay conexión. Solo lleva la zona generalizada si la persona lo autoriza.
 * Queda «en revisión»: no se convierte en aviso público sin el proceso de aprobación.
 */
@Entity(tableName = "reportes")
data class ReporteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tipo: String,
    val descripcion: String,
    val latIdx: Int?,
    val lonIdx: Int?,
    val creadoEn: Long,
    val estado: EstadoReporte = EstadoReporte.EN_COLA,
    val intentos: Int = 0,
    val enviadoEn: Long? = null,
    /** Medición con el kit comunitario (JSON de KitDto), si el reporte la trae. */
    val kitJson: String? = null,
)
