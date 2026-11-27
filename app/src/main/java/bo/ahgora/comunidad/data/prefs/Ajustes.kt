package bo.ahgora.comunidad.data.prefs

import android.content.Context
import androidx.core.content.edit
import bo.ahgora.comunidad.BuildConfig
import bo.ahgora.comunidad.data.zona.Celda
import bo.ahgora.comunidad.data.zona.Localidades
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class OrigenZona { GPS, LISTA }

/** Las dos personas que usan la app (guía, sección 7.6: una sola app con dos perfiles). */
enum class Perfil { COMUNIDAD, TECNICO }

data class Ajustes(
    val bienvenidaHecha: Boolean = false,
    val perfil: Perfil = Perfil.COMUNIDAD,
    /**
     * Zona elegida: SOLO la celda de 0,1°, nunca la coordenada exacta. Viene fijada en Alcoche
     * (Caranavi), la localidad del piloto; la persona puede cambiarla por lista o por GPS.
     */
    val celda: Celda? = ZONA_POR_DEFECTO,
    val nombrePropio: String? = null,
    val origenZona: OrigenZona? = OrigenZona.LISTA,
    val notificaciones: Boolean = false,
    /** Ya vio el video tutorial del kit: recién entonces puede medir. */
    val tutorialKitVisto: Boolean = false,
    val escalaTexto: Float = 1f,
    val modoDemo: Boolean = BuildConfig.DEMO_MODE_DEFAULT,
    val demoPaso: Int = 0,
    val demoBase: Long = 0L,
    val apiBaseUrl: String = BuildConfig.API_BASE_URL,
    val ultimaSincronizacion: Long? = null,
    val ultimoIntento: Long? = null,
    val ultimoError: String? = null,
) {
    /** Nombre visible de la zona: el que puso la persona o «Cerca de …». */
    val nombreZona: String?
        get() = nombrePropio?.takeIf { it.isNotBlank() } ?: celda?.let { Localidades.nombreSugerido(it) }
}

/** Alcoche, municipio de Caranavi: la comunidad del piloto. */
val ZONA_POR_DEFECTO: Celda = Localidades.ALCOCHE.celda

/** Preferencias en SharedPreferences expuestas como StateFlow (sin dependencias extra). */
class AjustesStore(context: Context) {
    private val sp = context.getSharedPreferences("ahgora_ajustes", Context.MODE_PRIVATE)
    private val _estado = MutableStateFlow(leer())
    val estado: StateFlow<Ajustes> = _estado.asStateFlow()
    val actual: Ajustes get() = _estado.value

    @Synchronized
    fun actualizar(cambio: (Ajustes) -> Ajustes) {
        val nuevo = cambio(_estado.value)
        escribir(nuevo)
        _estado.value = nuevo
    }

    private fun leer() = Ajustes(
        bienvenidaHecha = sp.getBoolean(K_BIENVENIDA, false),
        perfil = sp.getString(K_PERFIL, null)?.let { runCatching { Perfil.valueOf(it) }.getOrNull() } ?: Perfil.COMUNIDAD,
        celda = Celda.desdeClave(sp.getString(K_CELDA, null)) ?: ZONA_POR_DEFECTO,
        nombrePropio = sp.getString(K_NOMBRE, null),
        origenZona = sp.getString(K_ORIGEN, null)?.let { runCatching { OrigenZona.valueOf(it) }.getOrNull() } ?: OrigenZona.LISTA,
        notificaciones = sp.getBoolean(K_NOTIF, false),
        tutorialKitVisto = sp.getBoolean(K_TUTORIAL_KIT, false),
        escalaTexto = sp.getFloat(K_ESCALA, 1f),
        modoDemo = sp.getBoolean(K_DEMO, BuildConfig.DEMO_MODE_DEFAULT),
        demoPaso = sp.getInt(K_DEMO_PASO, 0),
        demoBase = sp.getLong(K_DEMO_BASE, 0L),
        apiBaseUrl = sp.getString(K_API, null) ?: BuildConfig.API_BASE_URL,
        ultimaSincronizacion = sp.getLong(K_SYNC_OK, 0L).takeIf { it > 0 },
        ultimoIntento = sp.getLong(K_SYNC_INTENTO, 0L).takeIf { it > 0 },
        ultimoError = sp.getString(K_SYNC_ERROR, null),
    )

    private fun escribir(a: Ajustes) = sp.edit {
        putBoolean(K_BIENVENIDA, a.bienvenidaHecha)
        putString(K_PERFIL, a.perfil.name)
        putString(K_CELDA, a.celda?.clave())
        putString(K_NOMBRE, a.nombrePropio)
        putString(K_ORIGEN, a.origenZona?.name)
        putBoolean(K_NOTIF, a.notificaciones)
        putBoolean(K_TUTORIAL_KIT, a.tutorialKitVisto)
        putFloat(K_ESCALA, a.escalaTexto)
        putBoolean(K_DEMO, a.modoDemo)
        putInt(K_DEMO_PASO, a.demoPaso)
        putLong(K_DEMO_BASE, a.demoBase)
        putString(K_API, a.apiBaseUrl)
        putLong(K_SYNC_OK, a.ultimaSincronizacion ?: 0L)
        putLong(K_SYNC_INTENTO, a.ultimoIntento ?: 0L)
        putString(K_SYNC_ERROR, a.ultimoError)
    }

    private companion object {
        const val K_BIENVENIDA = "bienvenida"
        const val K_PERFIL = "perfil"
        const val K_CELDA = "celda"
        const val K_NOMBRE = "nombre_zona"
        const val K_ORIGEN = "origen_zona"
        const val K_NOTIF = "notificaciones"
        const val K_TUTORIAL_KIT = "tutorial_kit_visto"
        const val K_ESCALA = "escala_texto"
        const val K_DEMO = "modo_demo"
        const val K_DEMO_PASO = "demo_paso"
        const val K_DEMO_BASE = "demo_base"
        const val K_API = "api_base_url"
        const val K_SYNC_OK = "sync_ok"
        const val K_SYNC_INTENTO = "sync_intento"
        const val K_SYNC_ERROR = "sync_error"
    }
}
