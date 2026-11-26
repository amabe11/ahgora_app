package bo.ahgora.comunidad.data.tecnico

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import bo.ahgora.comunidad.data.remote.LecturaNodoDto
import bo.ahgora.comunidad.data.remote.RgbcDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.IOException
import java.util.UUID

/**
 * Enlace con el lector Ahgora (ESP32 + TCS34725) por Bluetooth clásico (puerto serie, SPP).
 * El nodo envía una línea JSON por lectura, el mismo formato que hoy sale por USB (guía de Loana, sección 05).
 * Del lado del ESP32 basta `BluetoothSerial SerialBT; SerialBT.begin("AHG-SED-01"); SerialBT.println(json);`.
 * El teléfono debe estar emparejado antes con el lector desde los ajustes de Bluetooth.
 */
class LectorBluetooth(private val context: Context) {

    data class Dispositivo(val nombre: String, val direccion: String)

    class ErrorLector(mensaje: String) : IOException(mensaje)

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    val permisoNecesario: String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Manifest.permission.BLUETOOTH_CONNECT else null

    fun tienePermiso(): Boolean = permisoNecesario == null ||
        ContextCompat.checkSelfPermission(context, permisoNecesario) == PackageManager.PERMISSION_GRANTED

    private val adaptador get() = context.getSystemService(BluetoothManager::class.java)?.adapter

    fun disponible(): Boolean = adaptador != null
    fun encendido(): Boolean = adaptador?.isEnabled == true

    /** Equipos ya emparejados; los que parecen lectores Ahgora van primero. */
    @SuppressLint("MissingPermission")
    fun emparejados(): List<Dispositivo> {
        if (!tienePermiso()) return emptyList()
        return adaptador?.bondedDevices.orEmpty()
            .map { Dispositivo(it.name ?: it.address, it.address) }
            .sortedByDescending { it.nombre.contains("AHG", ignoreCase = true) || it.nombre.contains("ESP32", ignoreCase = true) }
    }

    /**
     * Se conecta, junta [n] lecturas válidas y devuelve su promedio. [onProgreso] recibe cuántas van.
     */
    @SuppressLint("MissingPermission")
    suspend fun leer(
        direccion: String,
        n: Int = 10,
        timeoutMs: Long = 30_000,
        onProgreso: (Int) -> Unit = {},
    ): Pair<RgbcDto, LecturaNodoDto> = withContext(Dispatchers.IO) {
        if (!tienePermiso()) throw ErrorLector("Falta el permiso de Bluetooth")
        val ad = adaptador ?: throw ErrorLector("Este teléfono no tiene Bluetooth")
        if (!ad.isEnabled) throw ErrorLector("Enciende el Bluetooth")
        val equipo = ad.getRemoteDevice(direccion)
        val socket = equipo.createRfcommSocketToServiceRecord(SPP)
        // connect() y readLine() bloquean y no responden a la cancelación: un guardián cierra el socket
        // al vencer el tiempo, y eso los destraba con una excepción.
        var vencido = false
        coroutineScope {
            val guardian = launch { delay(timeoutMs); vencido = true; runCatching { socket.close() } }
            try {
                ad.cancelDiscovery()
                runCatching { socket.connect() }.onFailure {
                    throw ErrorLector(if (vencido) "El lector no respondió a tiempo" else "No se pudo conectar con el lector. ¿Está encendido y emparejado?")
                }
                val entrada = socket.inputStream.bufferedReader()
                val lecturas = mutableListOf<LecturaNodoDto>()
                while (lecturas.size < n) {
                    val linea = try { entrada.readLine() } catch (e: IOException) {
                        throw ErrorLector(if (vencido) "El lector no envió $n lecturas a tiempo" else "Se cortó la conexión con el lector")
                    } ?: throw ErrorLector("El lector cerró la conexión")
                    val l = linea.trim()
                    if (!l.startsWith("{")) continue // «LISTO: lector Ahgora…» y otras líneas de texto
                    val dato = runCatching { json.decodeFromString<LecturaNodoDto>(l) }.getOrNull() ?: continue
                    lecturas += dato
                    withContext(Dispatchers.Main) { onProgreso(lecturas.size) }
                }
                Cribado.promedio(lecturas.map { RgbcDto(it.r, it.g, it.b, it.c) }) to lecturas.last()
            } finally {
                guardian.cancel()
                runCatching { socket.close() }
            }
        }
    }

    companion object {
        /** UUID estándar del puerto serie por Bluetooth (SPP). */
        val SPP: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
}
