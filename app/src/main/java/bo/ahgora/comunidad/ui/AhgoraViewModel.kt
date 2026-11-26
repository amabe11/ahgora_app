package bo.ahgora.comunidad.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import bo.ahgora.comunidad.AhgoraApp
import bo.ahgora.comunidad.data.Ubicador
import bo.ahgora.comunidad.data.demo.DemoAvisos
import bo.ahgora.comunidad.data.local.AvisoEntity
import bo.ahgora.comunidad.data.local.ReporteEntity
import bo.ahgora.comunidad.data.prefs.Ajustes
import bo.ahgora.comunidad.data.prefs.OrigenZona
import bo.ahgora.comunidad.data.prefs.Perfil
import bo.ahgora.comunidad.data.zona.Celda
import bo.ahgora.comunidad.notif.Notificador
import bo.ahgora.comunidad.sync.SyncWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AhgoraViewModel(app: Application) : AndroidViewModel(app) {
    private val c = (app as AhgoraApp).contenedor
    private val repo = c.repositorio

    val ajustes: StateFlow<Ajustes> = c.ajustes.estado
    val enLinea: StateFlow<Boolean> = c.conexion.enLinea

    /** null mientras Room carga por primera vez. */
    val avisos: StateFlow<List<AvisoEntity>?> = repo.avisos
        .map<List<AvisoEntity>, List<AvisoEntity>?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val reportes: StateFlow<List<ReporteEntity>> = repo.reportes
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _sincronizando = MutableStateFlow(false)
    val sincronizando: StateFlow<Boolean> = _sincronizando.asStateFlow()

    private val _mensajes = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val mensajes: SharedFlow<String> = _mensajes

    init {
        if (ajustes.value.bienvenidaHecha) sincronizar(silencioso = true)
        // Al volver la conexión: descargar nuevas versiones y retiros.
        viewModelScope.launch {
            enLinea.drop(1).filter { it }.collect {
                if (ajustes.value.bienvenidaHecha) sincronizar(silencioso = true)
            }
        }
    }

    fun aviso(id: String): Flow<AvisoEntity?> = repo.aviso(id)
    fun marcarLeido(id: String) = viewModelScope.launch { repo.marcarLeido(id) }

    fun sincronizar(silencioso: Boolean = false) {
        if (_sincronizando.value || ajustes.value.celda == null) return
        if (!enLinea.value) {
            SyncWorker.sincronizarCuandoHayaRed(getApplication())
            if (!silencioso) _mensajes.tryEmit("Sin conexión. Se actualizará solo cuando vuelva la señal.")
            return
        }
        viewModelScope.launch {
            _sincronizando.value = true
            val r = repo.sincronizar()
            _sincronizando.value = false
            when {
                !r.ok -> {
                    SyncWorker.sincronizarCuandoHayaRed(getApplication())
                    if (!silencioso) _mensajes.tryEmit("No se pudo actualizar. Se muestran los avisos guardados.")
                }
                r.cambiosCerca.isNotEmpty() -> _mensajes.tryEmit("Hay ${r.cambiosCerca.size} novedad(es) cerca de tu zona.")
                !silencioso -> _mensajes.tryEmit("Avisos actualizados.")
            }
            if (r.reportesEnviados > 0) _mensajes.tryEmit("Se enviaron ${r.reportesEnviados} reporte(s).")
        }
    }

    // ------------------------------------------------------------------ zona

    suspend fun obtenerZonaGps(): Ubicador.Resultado = c.ubicador.obtenerZona()

    fun fijarZona(celda: Celda, origen: OrigenZona, nombre: String?) = viewModelScope.launch {
        repo.fijarZona(celda, origen, nombre)
        if (ajustes.value.bienvenidaHecha) {
            _mensajes.tryEmit("Zona guardada.")
            sincronizar(silencioso = true)
        }
    }

    fun completarBienvenida(notificaciones: Boolean) {
        c.ajustes.actualizar { it.copy(bienvenidaHecha = true, notificaciones = notificaciones) }
        SyncWorker.programarPeriodica(getApplication())
        // La zona ya viene fijada (Alcoche): si nadie la cambió, los ejemplos todavía no existen.
        viewModelScope.launch {
            if (ajustes.value.modoDemo) repo.reiniciarDemo()
            sincronizar(silencioso = true)
        }
    }

    // ------------------------------------------------------------------ perfil

    /** Abre el perfil técnico (pide usuario y clave si no hay sesión). */
    fun irATecnico() = c.ajustes.actualizar { it.copy(perfil = Perfil.TECNICO) }

    // ------------------------------------------------------------------ preferencias

    fun setNotificaciones(activas: Boolean) = c.ajustes.actualizar { it.copy(notificaciones = activas) }
    fun setEscala(escala: Float) = c.ajustes.actualizar { it.copy(escalaTexto = escala) }
    fun setServidor(url: String) {
        val limpio = url.trim().ifEmpty { return }
        c.ajustes.actualizar { it.copy(apiBaseUrl = if (limpio.endsWith("/")) limpio else "$limpio/") }
        _mensajes.tryEmit("Servidor guardado.")
    }

    fun setModoDemo(activo: Boolean) = viewModelScope.launch {
        repo.activarDemo(activo)
        _mensajes.tryEmit(if (activo) "Modo demostración activado." else "Se quitaron los avisos de ejemplo.")
    }

    fun simularActualizacion() = viewModelScope.launch {
        val r = repo.simularSiguientePaso()
        Notificador.notificarCambios(getApplication(), r.cambiosCerca, ajustes.value.notificaciones)
        _mensajes.tryEmit("Simulado: ${DemoAvisos.descripcionPaso(ajustes.value.demoPaso)}")
    }

    // ------------------------------------------------------------------ reportes

    fun enviarReporte(tipo: String, descripcion: String, incluirZona: Boolean) = viewModelScope.launch {
        repo.guardarReporte(tipo, descripcion, incluirZona)
        if (enLinea.value) {
            val n = repo.enviarReportesPendientes()
            _mensajes.tryEmit(if (n > 0) "Reporte enviado. Queda en revisión." else "Reporte guardado. Se enviará más tarde.")
        } else {
            SyncWorker.sincronizarCuandoHayaRed(getApplication())
            _mensajes.tryEmit("Reporte guardado. Se enviará cuando vuelva la señal.")
        }
    }
}
