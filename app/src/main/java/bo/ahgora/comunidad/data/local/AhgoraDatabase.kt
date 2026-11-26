package bo.ahgora.comunidad.data.local

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Dao
import bo.ahgora.comunidad.data.tecnico.MuestraDao
import bo.ahgora.comunidad.data.tecnico.MuestraEntity
import androidx.room.Database
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Upsert
import androidx.room.Insert
import kotlinx.coroutines.flow.Flow

@Dao
interface AvisoDao {
    @Query("SELECT * FROM avisos ORDER BY retirado ASC, publicadoEn DESC")
    fun observarTodos(): Flow<List<AvisoEntity>>

    @Query("SELECT * FROM avisos WHERE id = :id")
    fun observar(id: String): Flow<AvisoEntity?>

    @Query("SELECT * FROM avisos WHERE origen = :origen")
    suspend fun porOrigen(origen: String): List<AvisoEntity>

    @Upsert
    suspend fun upsert(avisos: List<AvisoEntity>)

    @Query("DELETE FROM avisos WHERE origen = :origen")
    suspend fun borrarOrigen(origen: String)

    @Query("UPDATE avisos SET leido = 1 WHERE id = :id")
    suspend fun marcarLeido(id: String)
}

@Dao
interface ReporteDao {
    @Query("SELECT * FROM reportes ORDER BY creadoEn DESC")
    fun observarTodos(): Flow<List<ReporteEntity>>

    @Query("SELECT * FROM reportes WHERE estado != 'ENVIADO' ORDER BY creadoEn ASC")
    suspend fun pendientes(): List<ReporteEntity>

    @Insert
    suspend fun insertar(reporte: ReporteEntity): Long

    @Upsert
    suspend fun actualizar(reporte: ReporteEntity)
}

class Convertidores {
    @TypeConverter fun estadoAviso(v: EstadoAviso): String = v.name
    @TypeConverter fun estadoAviso(v: String): EstadoAviso = EstadoAviso.valueOf(v)
    @TypeConverter fun estadoReporte(v: EstadoReporte): String = v.name
    @TypeConverter fun estadoReporte(v: String): EstadoReporte = EstadoReporte.valueOf(v)
}

// v2: tabla «muestras» del modo técnico. La migración automática solo agrega la tabla nueva:
// los avisos y reportes guardados se conservan.
@Database(
    entities = [AvisoEntity::class, ReporteEntity::class, MuestraEntity::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
@TypeConverters(Convertidores::class)
abstract class AhgoraDatabase : RoomDatabase() {
    abstract fun avisos(): AvisoDao
    abstract fun reportes(): ReporteDao
    abstract fun muestras(): MuestraDao

    companion object {
        fun crear(context: Context): AhgoraDatabase =
            Room.databaseBuilder(context, AhgoraDatabase::class.java, "ahgora.db")
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
