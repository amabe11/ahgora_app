package bo.ahgora.comunidad.data.tecnico

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Muestra registrada por el técnico. El registro completo (contrato de `/api/sync`) se guarda como JSON;
 * las columnas sueltas sirven para listar, ordenar y saber qué falta enviar. Datos de acceso
 * restringido (coordenadas exactas, lecturas crudas): nunca se muestran en el perfil comunitario.
 */
@Entity(tableName = "muestras", indices = [Index("fechaMs"), Index("pendienteSync")])
data class MuestraEntity(
    @PrimaryKey val codigo: String,
    val registroJson: String,
    val matriz: String,
    val semaforo: String,
    val certeza: String,
    val fechaMs: Long,
    val lat: Double?,
    val lon: Double?,
    val responsable: String,
    val demostracion: Boolean,
    /** Ruta local de la foto (no se envía al servidor). */
    val fotoRuta: String? = null,
    val pendienteSync: Boolean = true,
    /** Registrada en una sesión de demostración (sin clave): nunca se envía al servidor. */
    val soloLocal: Boolean = false,
    val sincronizadoEn: Long? = null,
    val actualizadoEn: Long,
)

@Dao
interface MuestraDao {
    @Query("SELECT * FROM muestras ORDER BY fechaMs DESC")
    fun observarTodas(): Flow<List<MuestraEntity>>

    @Query("SELECT * FROM muestras WHERE codigo = :codigo")
    fun observar(codigo: String): Flow<MuestraEntity?>

    @Query("SELECT * FROM muestras WHERE codigo = :codigo")
    suspend fun obtener(codigo: String): MuestraEntity?

    @Query("SELECT * FROM muestras WHERE pendienteSync = 1 AND soloLocal = 0 ORDER BY fechaMs ASC")
    suspend fun pendientes(): List<MuestraEntity>

    @Query("SELECT codigo FROM muestras WHERE codigo LIKE :prefijo || '%'")
    suspend fun codigosCon(prefijo: String): List<String>

    @Upsert
    suspend fun guardar(m: MuestraEntity)

    @Query("UPDATE muestras SET pendienteSync = 0, sincronizadoEn = :cuando WHERE codigo = :codigo AND actualizadoEn = :version")
    suspend fun marcarEnviada(codigo: String, version: Long, cuando: Long)

    @Query("DELETE FROM muestras WHERE codigo = :codigo AND pendienteSync = 1 AND sincronizadoEn IS NULL")
    suspend fun borrarNoEnviada(codigo: String): Int
}
