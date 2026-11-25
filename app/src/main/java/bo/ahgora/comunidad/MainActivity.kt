package bo.ahgora.comunidad

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bo.ahgora.comunidad.notif.Notificador
import bo.ahgora.comunidad.ui.AhgoraViewModel
import bo.ahgora.comunidad.ui.RaizApp
import bo.ahgora.comunidad.ui.theme.AhgoraTema
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val vm: AhgoraViewModel by viewModels()
    /** Aviso a abrir cuando se toca una notificación. */
    private val avisoPendiente = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Íconos claros sobre el encabezado azul petróleo.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        leerIntent(intent)
        setContent {
            val ajustes by vm.ajustes.collectAsStateWithLifecycle()
            val d = LocalDensity.current
            // Texto ampliable además del tamaño de fuente del sistema (guía, sección 06).
            CompositionLocalProvider(LocalDensity provides Density(d.density, d.fontScale * ajustes.escalaTexto)) {
                AhgoraTema {
                    RaizApp(vm, avisoPendiente, onAvisoAbierto = { avisoPendiente.value = null })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        leerIntent(intent)
    }

    private fun leerIntent(intent: Intent?) {
        intent?.getStringExtra(Notificador.EXTRA_AVISO_ID)?.let { avisoPendiente.value = it }
        intent?.removeExtra(Notificador.EXTRA_AVISO_ID)
    }
}
