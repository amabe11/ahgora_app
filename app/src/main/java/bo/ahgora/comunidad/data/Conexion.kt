package bo.ahgora.comunidad.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Estado de la conexión a internet, observado en vivo. */
class Conexion(context: Context) {
    private val cm = context.getSystemService(ConnectivityManager::class.java)
    private val _enLinea = MutableStateFlow(consultar())
    val enLinea: StateFlow<Boolean> = _enLinea.asStateFlow()

    init {
        val req = NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
        cm.registerNetworkCallback(req, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { _enLinea.value = consultar() }
            override fun onLost(network: Network) { _enLinea.value = consultar() }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { _enLinea.value = consultar() }
        })
    }

    private fun consultar(): Boolean {
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
