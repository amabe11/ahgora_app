package bo.ahgora.comunidad.ui.pantallas

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bo.ahgora.comunidad.ui.AhgoraViewModel
import bo.ahgora.comunidad.ui.Textos
import bo.ahgora.comunidad.ui.theme.Paleta

/**
 * Primer uso en UNA pantalla (guía, secciones 06 y 07). La comunidad ya viene elegida (Alcoche);
 * la persona solo toca «Empezar». Sin cuenta.
 */
@Composable
fun PantallaBienvenida(vm: AhgoraViewModel) {
    val ajustes by vm.ajustes.collectAsStateWithLifecycle()
    var cambiando by rememberSaveable { mutableStateOf(false) }
    val pedir = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        vm.completarBienvenida(concedido)
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).navigationBarsPadding().imePadding()) {
        Column(Modifier.fillMaxWidth().background(Paleta.Petroleo).statusBarsPadding().padding(horizontal = 24.dp, vertical = 24.dp)) {
            Text("Ahgora", style = MaterialTheme.typography.displaySmall, color = Color.White, modifier = Modifier.semantics { heading() })
            Text("Avisos sobre mercurio en tu comunidad", style = MaterialTheme.typography.titleMedium, color = Paleta.Menta)
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (cambiando) {
                Text("¿Dónde vives?", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                SelectorZona(vm, textoGuardar = "Usar esta comunidad", onGuardado = { cambiando = false })
                TextButton(onClick = { cambiando = false }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Volver") }
                return@Column
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocationOn, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
                        Text("Tu comunidad", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(ajustes.nombreZona ?: "Alcoche", style = MaterialTheme.typography.headlineSmall)
                    }
                    TextButton(onClick = { cambiando = true }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cambiar") }
                }
            }

            Punto(Icons.Outlined.Science, "Te contamos qué muestras se tomaron cerca y qué dijo el laboratorio.")
            Punto(Icons.Outlined.NotificationsActive, "Te avisamos con un mensaje corto cuando hay novedades.")
            Punto(Icons.Outlined.CloudOff, "Funciona sin señal.")

            Text(Textos.PRIMERA_SENAL, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) pedir.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else vm.completarBienvenida(true)
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
            ) { Text("Empezar", style = MaterialTheme.typography.titleMedium) }
            OutlinedButton(onClick = { vm.completarBienvenida(false) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text("Empezar sin mensajes")
            }
            TextButton(onClick = { vm.irATecnico() }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Soy técnico de campo")
            }
        }
    }
}

@Composable
private fun Punto(icono: ImageVector, texto: String) {
    Row(Modifier.semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(44.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
        Spacer(Modifier.width(14.dp))
        Text(texto, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}
