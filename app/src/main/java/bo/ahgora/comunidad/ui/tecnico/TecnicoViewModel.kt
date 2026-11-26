package bo.ahgora.comunidad.ui.tecnico

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import bo.ahgora.comunidad.AhgoraApp
import bo.ahgora.comunidad.data.Ubicador
import bo.ahgora.comunidad.data.prefs.Perfil
import bo.ahgora.comunidad.data.remote.CalculoDto
import bo.ahgora.comunidad.data.remote.ControlesDto
import bo.ahgora.comunidad.data.remote.LecturasDto
import bo.ahgora.comunidad.data.remote.LectorDto
import bo.ahgora.comunidad.data.remote.MuestraDto
import bo.ahgora.comunidad.data.remote.MuestreoDto
import bo.ahgora.comunidad.data.remote.RgbcDto
import bo.ahgora.comunidad.data.tecnico.Cribado
import bo.ahgora.comunidad.data.tecnico.MuestraEntity
import bo.ahgora.comunidad.data.tecnico.ResultadoCribado
import bo.ahgora.comunidad.data.tecnico.Semaforo
import bo.ahgora.comunidad.data.tecnico.Sesion
import bo.ahgora.comunidad.data.tecnico.TecnicoRepository
import bo.ahgora.comunidad.data.zona.Localidades
import bo.ahgora.comunidad.sync.SyncWorker
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import kotlin.random.Random

/** Muestra en preparación (pantalla «Nueva muestra»). Vive en el ViewModel para no perderse al pedir permisos. */
class Borrador {
    var paso by mutableIntStateOf(0)
    var matriz by mutableStateOf("sedimento")
    var codigo by mutableStateOf("")
    var codigoEditado by mutableStateOf(false)
    val fechaMs = System.currentTimeMillis()
    var lat by mutableStateOf(Localidades.ALCOCHE.lat)
    var lon by mutableStateOf(Localidades.ALCOCHE.lon)
    var precisionM by mutableStateOf<Double?>(null)
    var conGps by mutableStateOf(false)
    var cuenca by mutableStateOf("Río Coroico")
    var fotoRuta by mutableStateOf<String?>(null)
    // Muestreo
    var porciones by mutableIntStateOf(8)
    var profundidadCm by mutableStateOf("5")
    var finoPct by mutableStateOf("")
    var especie by mutableStateOf("")
    var tallaCm by mutableStateOf("")
    var pesoG by mutableStateOf("")
    var observaciones by mutableStateOf("")
    // Procedimiento
    val pasosHechos = mutableStateListOf<Int>()
    // Controles
    var blancoProcedimiento by mutableStateOf("no_medido")
    var cloroLibreCero by mutableStateOf("no_medido")
    var referenciaBlanca by mutableStateOf("no_medido")
    var ph by mutableStateOf("no_medido")
    // Lecturas
    var origenLectura by mutableStateOf("bluetooth")
    var refBlanca by mutableStateOf<RgbcDto?>(null)
    var sinUsar by mutableStateOf<RgbcDto?>(null)
    var reaccionada by mutableStateOf<RgbcDto?>(null)
    var lector by mutableStateOf<LectorDto?>(null)

    val esPescado get() = matriz == "pescado"

    fun controles() = ControlesDto(
        blancoProcedimiento = blancoProcedimiento,
        cloroLibreCero = cloroLibreCero,
        referenciaBlanca = referenciaBlanca,
        blancoMatriz = "no_medido",
        ph = ph,
    )

    fun resultado(): ResultadoCribado =
        if (esPescado) ResultadoCribado(Semaforo.INVALIDO, null, "Sin lectura de campo: cabezal PEZ en etapa de concepto")
        else Cribado.evaluar(controles(), refBlanca, sinUsar, reaccionada)
}

class TecnicoViewModel(app: Application) : AndroidViewModel(app) {
    private val c = (app as AhgoraApp).contenedor
    private val repo: TecnicoRepository = c.tecnico
    val lector = c.lector

    val sesion: StateFlow<Sesion?> = c.sesion.sesion
    val enLinea: StateFlow<Boolean> = c.conexion.enLinea
    val ajustes = c.ajustes.estado
    val muestras: StateFlow<List<MuestraEntity>> = repo.muestras.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _enviando = MutableStateFlow(false)
    val enviando: StateFlow<Boolean> = _enviando.asStateFlow()
    private val _ultimoEnvio = MutableStateFlow<Long?>(null)
    val ultimoEnvio: StateFlow<Long?> = _ultimoEnvio.asStateFlow()

    private val _mensajes = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val mensajes: SharedFlow<String> = _mensajes

    var borrador by mutableStateOf(Borrador())
        private set

    init {
        // Al volver la señal se envían las muestras guardadas (guía, sección 2.3, paso 6).
        viewModelScope.launch { enLinea.drop(1).filter { it }.collect { enviar(silencioso = true) } }
        if (sesion.value?.demostracion == false) enviar(silencioso = true)
    }

    fun registro(m: MuestraEntity): MuestraDto = repo.registro(m)
    fun muestra(codigo: String) = repo.muestra(codigo)

    // ------------------------------------------------------------------ sesión y perfil

    suspend fun ingresar(usuario: String, clave: String): TecnicoRepository.Ingreso {
        val r = repo.ingresar(usuario, clave, enLinea.value)
        if (r is TecnicoRepository.Ingreso.Ok) { SyncWorker.programarPeriodica(getApplication()); enviar(silencioso = true) }
        return r
    }

    fun ingresarDemostracion(usuario: String): TecnicoRepository.Ingreso = repo.ingresarDemostracion(usuario)

    fun cerrarSesion() {
        repo.cerrarSesion()
        borrador = Borrador()
    }

    fun irAComunidad() = c.ajustes.actualizar { it.copy(perfil = Perfil.COMUNIDAD) }

    // ------------------------------------------------------------------ nueva muestra

    fun nuevoBorrador() {
        borrador = Borrador()
        actualizarCodigo()
    }

    fun actualizarCodigo() {
        val b = borrador
        if (b.codigoEditado) return
        viewModelScope.launch { b.codigo = repo.nuevoCodigo(b.matriz, b.lat, b.lon, b.fechaMs) }
    }

    suspend fun tomarGps(): String? = when (val r = c.ubicador.obtenerPunto()) {
        is Ubicador.ResultadoPunto.Ok -> {
            borrador.lat = r.punto.lat; borrador.lon = r.punto.lon
            borrador.precisionM = r.punto.precisionM; borrador.conGps = true
            actualizarCodigo(); null
        }
        Ubicador.ResultadoPunto.SinPermiso -> "Sin permiso de ubicación."
        Ubicador.ResultadoPunto.UbicacionApagada -> "La ubicación del teléfono está apagada."
        Ubicador.ResultadoPunto.SinSenal -> "No hay señal de GPS. Sal a cielo abierto e intenta de nuevo."
    }

    fun archivoFotoNuevo(): File {
        val dir = File(getApplication<Application>().filesDir, "fotos").apply { mkdirs() }
        return File(dir, "foto_${System.currentTimeMillis()}.jpg")
    }

    /** Lecturas de demostración: valores verosímiles del TCS34725, rotuladas como demostración. */
    fun llenarLecturaDemostracion() {
        val b = borrador
        val ref = RgbcDto(21480 + Random.nextInt(-300, 300), 23310 + Random.nextInt(-300, 300), 20120 + Random.nextInt(-300, 300), 60200)
        val sin = RgbcDto(15820, 16950, 12730, 45210)
        // Cambio de color al azar para mostrar los tres colores del semáforo.
        val caida = listOf(0.01, 0.05, 0.10).random() + Random.nextDouble(0.0, 0.02)
        val rea = RgbcDto((sin.r * (1 - caida)).toInt(), (sin.g * (1 - caida * 1.6)).toInt(), (sin.b * (1 - caida * 0.6)).toInt(), (sin.c * (1 - caida)).toInt())
        b.refBlanca = ref; b.sinUsar = sin; b.reaccionada = rea
        b.lector = LectorDto(nodeId = "DEMO", testMode = true, sensor = "TCS34725")
        b.origenLectura = "demostracion"
        if (b.referenciaBlanca == "no_medido") b.referenciaBlanca = "ok"
    }

    suspend fun guardar(): Boolean {
        val b = borrador
        val s = sesion.value ?: return false
        val codigo = b.codigo.trim().uppercase()
        if (codigo.length < 4) { _mensajes.tryEmit("Escribe el código de la muestra."); b.paso = 0; return false }
        if (repo.existe(codigo)) { _mensajes.tryEmit("Ya hay una muestra con el código $codigo."); b.paso = 0; return false }
        val res = b.resultado()
        fun num(t: String) = t.replace(',', '.').toDoubleOrNull()
        val pasosTotal = if (b.esPescado) PASOS_PEZ.size else PASOS_SED.size
        val loc = Localidades.masCercanaA(b.lat, b.lon).let { (l, km) -> if (km <= 15) l.nombre else null }
        val dto = MuestraDto(
            codigo = codigo,
            matriz = b.matriz,
            cabezal = if (b.esPescado) "PEZ" else "SED",
            fechaIso = TecnicoRepository.isoAhora(b.fechaMs),
            lat = Math.round(b.lat * 1e6) / 1e6, lon = Math.round(b.lon * 1e6) / 1e6,
            precisionM = b.precisionM?.let { Math.round(it * 10) / 10.0 },
            responsable = s.usuario,
            cuenca = b.cuenca.trim().ifEmpty { null },
            localidad = loc,
            muestreo = if (b.esPescado) MuestreoDto(
                tipo = "musculo_dorsal", especie = b.especie.trim().ifEmpty { null }, tallaCm = num(b.tallaCm), pesoG = num(b.pesoG),
                observaciones = b.observaciones.trim().ifEmpty { null }, pasosHechos = b.pasosHechos.size, pasosTotal = pasosTotal,
            ) else MuestreoDto(
                tipo = "compuesta", porciones = b.porciones, profundidadCm = num(b.profundidadCm), finoPct = num(b.finoPct),
                observaciones = b.observaciones.trim().ifEmpty { null }, pasosHechos = b.pasosHechos.size, pasosTotal = pasosTotal,
            ),
            lecturas = if (b.esPescado) null else LecturasDto(
                nPromedio = 10, referenciaBlanca = b.refBlanca, casillaSinUsar = b.sinUsar, casillaReaccionada = b.reaccionada,
                deltaE = res.deltaE, origen = b.origenLectura,
            ),
            calculo = CalculoDto(base = if (b.esPescado) "fresco" else "seco",
                nota = "Sin conversión a mg/kg: la curva de calibración aún no está validada con laboratorio."),
            semaforo = res.semaforo.servidor,
            motivoInvalido = res.motivoInvalido,
            controles = if (b.esPescado) null else b.controles(),
            calibracion = if (b.esPescado) null else Cribado.CALIBRACION,
            firmware = b.lector?.let { "ahgora-optical-demo-v0.1" },
            lector = b.lector,
            certeza = "cribado",
            visibilidad = "restringido",
            demostracion = s.demostracion || !Cribado.CALIBRACION_VALIDADA || b.origenLectura == "demostracion",
        )
        repo.guardarNueva(dto, b.fotoRuta)
        _mensajes.tryEmit(
            when {
                s.demostracion -> "Muestra $codigo guardada en el teléfono (demostración)."
                enLinea.value -> "Muestra $codigo guardada. Enviando…"
                else -> "Muestra $codigo guardada. Se enviará cuando haya señal."
            }
        )
        if (!s.demostracion) {
            if (enLinea.value) enviar(silencioso = true) else SyncWorker.sincronizarCuandoHayaRed(getApplication())
        }
        borrador = Borrador()
        return true
    }

    // ------------------------------------------------------------------ envío y acciones

    fun enviar(silencioso: Boolean = false) {
        val s = sesion.value ?: return
        if (s.demostracion) { if (!silencioso) _mensajes.tryEmit("Modo demostración: las muestras quedan en el teléfono."); return }
        if (_enviando.value) return
        if (!enLinea.value) {
            SyncWorker.sincronizarCuandoHayaRed(getApplication())
            if (!silencioso) _mensajes.tryEmit("Sin señal. Se enviarán solas cuando vuelva la conexión.")
            return
        }
        viewModelScope.launch {
            _enviando.value = true
            val r = repo.enviarPendientes()
            _enviando.value = false
            if (r.ok) {
                _ultimoEnvio.value = System.currentTimeMillis()
                if (r.enviadas > 0) _mensajes.tryEmit("Se enviaron ${r.enviadas} muestra(s) al servidor.")
                else if (!silencioso) _mensajes.tryEmit("No hay muestras por enviar.")
            } else {
                SyncWorker.sincronizarCuandoHayaRed(getApplication())
                if (!silencioso || r.error?.contains("clave") == true) _mensajes.tryEmit(r.error ?: "No se pudo enviar.")
            }
        }
    }

    fun marcarEnviadaLaboratorio(codigo: String, laboratorio: String) = viewModelScope.launch {
        repo.marcarEnviadaLaboratorio(codigo, laboratorio.ifBlank { "Laboratorio aliado" })
        _mensajes.tryEmit("Registrado: enviada al laboratorio.")
        enviar(silencioso = true)
    }

    fun borrar(codigo: String) = viewModelScope.launch {
        _mensajes.tryEmit(if (repo.borrarNoEnviada(codigo)) "Muestra $codigo borrada." else "Solo se pueden borrar muestras que nunca se enviaron.")
    }

    fun csv(lista: List<MuestraEntity>): String = repo.csv(lista)

    fun avisar(texto: String) { _mensajes.tryEmit(texto) }
}
