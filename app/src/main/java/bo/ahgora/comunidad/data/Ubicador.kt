package bo.ahgora.comunidad.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import android.os.CancellationSignal
import bo.ahgora.comunidad.data.zona.Celda
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Obtiene la ubicación UNA vez y la convierte de inmediato en la celda de 0,1°. La coordenada exacta
 * no sale de esta función: no se guarda ni se envía. El GPS funciona sin internet.
 */
class Ubicador(private val context: Context) {

    sealed interface Resultado {
        data class Ok(val celda: Celda) : Resultado
        data object SinPermiso : Resultado
        data object UbicacionApagada : Resultado
        data object SinSenal : Resultado
    }

    fun tienePermiso(): Boolean = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    private fun tieneFino() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    suspend fun obtenerZona(timeoutMs: Long = 45_000): Resultado {
        if (!tienePermiso()) return Resultado.SinPermiso
        val lm = context.getSystemService(LocationManager::class.java)
        if (!LocationManagerCompat.isLocationEnabled(lm)) return Resultado.UbicacionApagada

        val proveedores = buildList {
            if (tieneFino()) add(LocationManager.GPS_PROVIDER)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
        }.filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }

        // 1) Una ubicación reciente (≤ 30 min) basta: la zona mide ~11 km.
        val reciente = proveedores.mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .filter { System.currentTimeMillis() - it.time < 30 * 60_000 }
            .minByOrNull { it.accuracy }
        if (reciente != null) return Resultado.Ok(Celda.de(reciente.latitude, reciente.longitude))

        // 2) Pedir una lectura nueva a cada proveedor, en orden, hasta agotar el tiempo.
        val porProveedor = timeoutMs / proveedores.size.coerceAtLeast(1)
        for (p in proveedores) {
            val loc = withTimeoutOrNull(porProveedor) { lecturaActual(lm, p) }
            if (loc != null) return Resultado.Ok(Celda.de(loc.latitude, loc.longitude))
        }
        return Resultado.SinSenal
    }

    @SuppressLint("MissingPermission")
    private suspend fun lecturaActual(lm: LocationManager, proveedor: String): Location? =
        suspendCancellableCoroutine { cont ->
            val cancel = CancellationSignal()
            cont.invokeOnCancellation { cancel.cancel() }
            runCatching {
                LocationManagerCompat.getCurrentLocation(lm, proveedor, cancel, ContextCompat.getMainExecutor(context)) { loc ->
                    if (cont.isActive) cont.resume(loc)
                }
            }.onFailure { if (cont.isActive) cont.resume(null) }
        }
}
