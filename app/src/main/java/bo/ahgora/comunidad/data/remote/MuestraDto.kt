package bo.ahgora.comunidad.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Registro de muestra del modo técnico: contrato del Servidor Ahgora (`POST /api/sync`, repo `taku`,
 * web/README.md «Registro de muestra») y campos de la guía, sección 7.6. Idempotente por `codigo`.
 * Lo agregado por la app (lector, custodia, motivo_invalido) el servidor lo conserva tal cual.
 */
@Serializable
data class MuestraDto(
    val codigo: String,
    /** sedimento | cola | pescado */
    val matriz: String,
    /** SED | PEZ */
    val cabezal: String,
    @SerialName("fecha_iso") val fechaIso: String,
    val lat: Double? = null,
    val lon: Double? = null,
    @SerialName("precision_m") val precisionM: Double? = null,
    val responsable: String,
    val cuenca: String? = null,
    val localidad: String? = null,
    val muestreo: MuestreoDto? = null,
    val lecturas: LecturasDto? = null,
    val calculo: CalculoDto? = null,
    /** rojo | amarillo | verde | invalido */
    val semaforo: String,
    @SerialName("motivo_invalido") val motivoInvalido: String? = null,
    val controles: ControlesDto? = null,
    val calibracion: String? = null,
    val firmware: String? = null,
    val lector: LectorDto? = null,
    /** cribado | en_verificacion | confirmado */
    val certeza: String = "cribado",
    val laboratorio: LaboratorioDto? = null,
    /** publico | restringido. El técnico siempre registra «restringido»: publicar lo decide un responsable. */
    val visibilidad: String = "restringido",
    val custodia: List<EventoCustodiaDto> = emptyList(),
    val demostracion: Boolean = true,
)

@Serializable
data class MuestreoDto(
    val tipo: String? = null,
    val porciones: Int? = null,
    @SerialName("profundidad_cm") val profundidadCm: Double? = null,
    @SerialName("fino_pct") val finoPct: Double? = null,
    val especie: String? = null,
    @SerialName("talla_cm") val tallaCm: Double? = null,
    @SerialName("peso_g") val pesoG: Double? = null,
    val observaciones: String? = null,
    @SerialName("pasos_hechos") val pasosHechos: Int? = null,
    @SerialName("pasos_total") val pasosTotal: Int? = null,
)

@Serializable
data class RgbcDto(val r: Int, val g: Int, val b: Int, val c: Int)

@Serializable
data class LecturasDto(
    @SerialName("n_promedio") val nPromedio: Int? = null,
    @SerialName("referencia_blanca") val referenciaBlanca: RgbcDto? = null,
    @SerialName("casilla_sin_usar") val casillaSinUsar: RgbcDto? = null,
    @SerialName("casilla_reaccionada") val casillaReaccionada: RgbcDto? = null,
    @SerialName("blanco_matriz_delta") val blancoMatrizDelta: Double? = null,
    @SerialName("delta_e") val deltaE: Double? = null,
    /** manual | bluetooth | demostracion */
    val origen: String? = null,
)

@Serializable
data class CalculoDto(
    @SerialName("c_estimada_mg_kg") val cEstimadaMgKg: Double? = null,
    val base: String? = null,
    val nota: String? = null,
)

/** Cada control: "ok" | "falla" | "no_medido" (blanco_matriz: "restado" | "no_medido"). */
@Serializable
data class ControlesDto(
    @SerialName("blanco_procedimiento") val blancoProcedimiento: String = "no_medido",
    @SerialName("cloro_libre_cero") val cloroLibreCero: String = "no_medido",
    @SerialName("referencia_blanca") val referenciaBlanca: String = "no_medido",
    @SerialName("blanco_matriz") val blancoMatriz: String = "no_medido",
    val ph: String = "no_medido",
)

@Serializable
data class LectorDto(
    @SerialName("node_id") val nodeId: String? = null,
    @SerialName("test_mode") val testMode: Boolean? = null,
    val sensor: String? = null,
)

@Serializable
data class LaboratorioDto(
    val laboratorio: String = "",
    @SerialName("enviada_en") val enviadaEn: String = "",
    @SerialName("fecha_resultado") val fechaResultado: String = "",
    val metodo: String = "",
    @SerialName("resultado_mg_kg") val resultadoMgKg: Double? = null,
    val base: String = "seco",
    val incertidumbre: String = "",
)

/** Cadena de custodia (guía, sección 6.2): quién hizo qué y cuándo con la muestra. */
@Serializable
data class EventoCustodiaDto(
    val evento: String,
    val fecha: String,
    val responsable: String,
    val detalle: String? = null,
)

@Serializable
data class SyncRequest(
    @SerialName("origen_web") val origenWeb: String = "ahgora-android-tecnico",
    val muestras: List<MuestraDto>,
)

@Serializable
data class SyncResponse(
    val ok: Boolean = false,
    val muestras: Int = 0,
    @SerialName("sincronizado_en") val sincronizadoEn: String? = null,
)

/**
 * Lo que envía el nodo ESP32 + TCS34725 por línea (guía de Loana, sección 05):
 * `{"node_id":"AHG-SED-01","seq":12,"test_mode":true,"sensor":"TCS34725","r":1010,"g":625,"b":420,"c":2440,...}`
 */
@Serializable
data class LecturaNodoDto(
    @SerialName("node_id") val nodeId: String? = null,
    val seq: Long? = null,
    @SerialName("test_mode") val testMode: Boolean? = null,
    val sensor: String? = null,
    val r: Int,
    val g: Int,
    val b: Int,
    val c: Int,
)

/** Una lectura guardada por el servidor (GET /api/lecturas). */
@Serializable
data class LecturaServidorDto(
    @SerialName("node_id") val nodeId: String,
    val seq: Long = 0,
    val r: Int,
    val g: Int,
    val b: Int,
    val c: Int,
    @SerialName("medido_en") val medidoEn: String? = null,
    @SerialName("recibido_en") val recibidoEn: String? = null,
    @SerialName("test_mode") val testMode: Boolean? = null,
    val sensor: String? = null,
    val firmware: String? = null,
    val rssi: Int? = null,
)

@Serializable
data class NodoLectorDto(
    @SerialName("node_id") val nodeId: String,
    val ultima: LecturaServidorDto? = null,
    @SerialName("total_24h") val total24h: Int = 0,
)

@Serializable
data class LecturasResponse(
    val items: List<LecturaServidorDto> = emptyList(),
    val nodos: List<NodoLectorDto> = emptyList(),
    /** Hora del servidor: se usa como punto de partida para no depender del reloj del teléfono. */
    val ahora: String? = null,
)
