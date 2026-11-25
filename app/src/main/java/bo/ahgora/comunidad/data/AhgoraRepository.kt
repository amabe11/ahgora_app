package bo.ahgora.comunidad.data

import androidx.room.withTransaction
import bo.ahgora.comunidad.data.demo.DemoAvisos
import bo.ahgora.comunidad.data.local.AhgoraDatabase
import bo.ahgora.comunidad.data.local.AvisoEntity
import bo.ahgora.comunidad.data.local.EstadoReporte
import bo.ahgora.comunidad.data.local.OrigenAviso
import bo.ahgora.comunidad.data.local.ReporteEntity
import bo.ahgora.comunidad.data.prefs.AjustesStore
import bo.ahgora.comunidad.data.prefs.OrigenZona
import bo.ahgora.comunidad.data.remote.AhgoraApi
import bo.ahgora.comunidad.data.remote.ReporteDto
import bo.ahgora.comunidad.data.remote.ServidorNoRecibeReportes
import bo.ahgora.comunidad.data.remote.ZonaDto
import bo.ahgora.comunidad.data.sync.Fusion
import bo.ahgora.comunidad.data.sync.ResultadoFusion
import bo.ahgora.comunidad.data.zona.Celda
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

/** Resultado de una sincronización, con los cambios que tocan la zona de la persona. */
data class ResultadoSync(
    val ok: Boolean,
    val error: String? = null,
    val cambiosCerca: List<AvisoEntity> = emptyList(),
    val reportesEnviados: Int = 0,
)

/**
 * Única fuente de verdad: la UI lee siempre de Room (prioridad sin conexión). La red solo actualiza
 * la base local; si falla, lo guardado sigue visible con su fecha.
 */
class AhgoraRepository(
    private val db: AhgoraDatabase,
    private val api: AhgoraApi,
    val ajustes: AjustesStore,
) {
    private val mutex = Mutex()

    val avisos: Flow<List<AvisoEntity>> = db.avisos().observarTodos()
    val reportes: Flow<List<ReporteEntity>> = db.reportes().observarTodos()
    fun aviso(id: String): Flow<AvisoEntity?> = db.avisos().observar(id)

    suspend fun marcarLeido(id: String) = db.avisos().marcarLeido(id)

    // ------------------------------------------------------------------ servidor

    suspend fun sincronizar(): ResultadoSync = mutex.withLock {
        val ahora = System.currentTimeMillis()
        val conf = ajustes.actual
        ajustes.actualizar { it.copy(ultimoIntento = ahora) }
        val resultado = runCatching {
            val resp = api.alertas(conf.apiBaseUrl)
            val entrantes = resp.items.mapNotNull { AhgoraApi.aEntrante(it, ahora) }
            val fusion = db.withTransaction {
                val existentes = db.avisos().porOrigen(OrigenAviso.SERVIDOR)
                Fusion.fusionar(
                    existentes, entrantes, OrigenAviso.SERVIDOR, ahora,
                    completo = resp.items.size < LIMITE_SERVIDOR,
                ).also { db.avisos().upsert(it.guardar) }
            }
            ajustes.actualizar { it.copy(ultimaSincronizacion = ahora, ultimoError = null) }
            fusion
        }
        val enviados = if (resultado.isSuccess) enviarReportesPendientesInterno() else 0
        resultado.fold(
            onSuccess = { ResultadoSync(true, cambiosCerca = cerca(it), reportesEnviados = enviados) },
            onFailure = { e ->
                val msg = "No se pudo actualizar: ${e.message ?: e.javaClass.simpleName}"
                ajustes.actualizar { it.copy(ultimoError = msg) }
                ResultadoSync(false, error = msg)
            },
        )
    }

    // ------------------------------------------------------------------ zona

    suspend fun fijarZona(celda: Celda, origen: OrigenZona, nombrePropio: String?) {
        val anterior = ajustes.actual.celda
        ajustes.actualizar {
            it.copy(celda = celda, origenZona = origen, nombrePropio = nombrePropio?.trim()?.takeIf { n -> n.isNotEmpty() })
        }
        if (ajustes.actual.modoDemo && anterior != celda) reiniciarDemo()
    }

    fun renombrarZona(nombre: String?) =
        ajustes.actualizar { it.copy(nombrePropio = nombre?.trim()?.takeIf { n -> n.isNotEmpty() }) }

    // ------------------------------------------------------------------ demostración

    suspend fun activarDemo(activo: Boolean) {
        ajustes.actualizar { it.copy(modoDemo = activo) }
        if (activo) reiniciarDemo() else db.avisos().borrarOrigen(OrigenAviso.DEMO)
    }

    /** Regenera el caso de demostración desde el inicio, alrededor de la zona actual. */
    suspend fun reiniciarDemo() = mutex.withLock {
        val celda = ajustes.actual.celda ?: return@withLock
        val base = System.currentTimeMillis()
        ajustes.actualizar { it.copy(demoPaso = 0, demoBase = base) }
        db.withTransaction {
            db.avisos().borrarOrigen(OrigenAviso.DEMO)
            val f = Fusion.fusionar(emptyList(), DemoAvisos.generar(celda, 0, base), OrigenAviso.DEMO, base, completo = true)
            db.avisos().upsert(f.guardar)
        }
    }

    /** Avanza el caso simulado un paso (como si el servidor publicara una actualización). */
    suspend fun simularSiguientePaso(): ResultadoSync {
        val conf = ajustes.actual
        val celda = conf.celda ?: return ResultadoSync(false, "Primero elige tu zona")
        val siguiente = (conf.demoPaso + 1).mod(DemoAvisos.PASOS)
        if (siguiente == 0 || conf.demoBase == 0L) {
            reiniciarDemo()
            return ResultadoSync(true)
        }
        return mutex.withLock {
            val ahora = System.currentTimeMillis()
            val f = db.withTransaction {
                val existentes = db.avisos().porOrigen(OrigenAviso.DEMO)
                Fusion.fusionar(existentes, DemoAvisos.generar(celda, siguiente, conf.demoBase), OrigenAviso.DEMO, ahora, completo = true)
                    .also { db.avisos().upsert(it.guardar) }
            }
            ajustes.actualizar { it.copy(demoPaso = siguiente) }
            ResultadoSync(true, cambiosCerca = cerca(f))
        }
    }

    // ------------------------------------------------------------------ reportes comunitarios

    suspend fun guardarReporte(tipo: String, descripcion: String, incluirZona: Boolean): Long {
        val celda = if (incluirZona) ajustes.actual.celda else null
        return db.reportes().insertar(
            ReporteEntity(
                tipo = tipo, descripcion = descripcion.trim().take(500),
                latIdx = celda?.latIdx, lonIdx = celda?.lonIdx, creadoEn = System.currentTimeMillis(),
            )
        )
    }

    suspend fun enviarReportesPendientes(): Int = mutex.withLock { enviarReportesPendientesInterno() }

    private suspend fun enviarReportesPendientesInterno(): Int {
        var enviados = 0
        val base = ajustes.actual.apiBaseUrl
        for (r in db.reportes().pendientes()) {
            val celda = if (r.latIdx != null && r.lonIdx != null) Celda(r.latIdx, r.lonIdx) else null
            val dto = ReporteDto(
                tipo = r.tipo, descripcion = r.descripcion,
                zona = celda?.let { ZonaDto(it.latMin, it.lonMin, it.latMax, it.lonMax) },
                creado_en = Instant.ofEpochMilli(r.creadoEn).toString(),
            )
            try {
                api.enviarReporte(base, dto)
                db.reportes().actualizar(r.copy(estado = EstadoReporte.ENVIADO, enviadoEn = System.currentTimeMillis(), intentos = r.intentos + 1))
                enviados++
            } catch (e: ServidorNoRecibeReportes) {
                db.reportes().actualizar(r.copy(estado = EstadoReporte.SERVIDOR_NO_DISPONIBLE, intentos = r.intentos + 1))
                break
            } catch (e: Exception) {
                db.reportes().actualizar(r.copy(intentos = r.intentos + 1))
                break
            }
        }
        return enviados
    }

    /** Cambios en la zona de la persona o en sus vecinas (distancia ≤ 1 celda). */
    private fun cerca(f: ResultadoFusion): List<AvisoEntity> {
        val celda = ajustes.actual.celda ?: return emptyList()
        return f.cambios.filter { it.celda.distancia(celda) <= 1 }
    }

    companion object {
        /** `/api/alertas` devuelve como máximo 200 avisos. */
        const val LIMITE_SERVIDOR = 200
    }
}
