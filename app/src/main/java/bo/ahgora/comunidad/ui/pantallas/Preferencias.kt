package bo.ahgora.comunidad.ui.pantallas

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EditLocationAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bo.ahgora.comunidad.BuildConfig
import bo.ahgora.comunidad.data.demo.DemoAvisos
import bo.ahgora.comunidad.notif.Notificador
import bo.ahgora.comunidad.ui.AhgoraViewModel
import bo.ahgora.comunidad.ui.Formato
import bo.ahgora.comunidad.ui.componentes.FilaAccion
import bo.ahgora.comunidad.ui.componentes.TituloSeccion

@Composable
fun PantallaPreferencias(vm: AhgoraViewModel, onCambiarZona: () -> Unit) {
    val context = LocalContext.current
    val ajustes by vm.ajustes.collectAsStateWithLifecycle()
    val enLinea by vm.enLinea.collectAsStateWithLifecycle()
    var permisoNotif by remember { mutableStateOf(Notificador.permisoConcedido(context)) }
    val pedirNotif = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        permisoNotif = ok
        vm.setNotificaciones(ok)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TituloSeccion("Avisos en el teléfono")
        Bloque {
            FilaSwitch(
                "Recibir avisos de mi zona",
                "Mensaje corto cuando hay novedades. La bandeja de la app funciona igual si lo desactivas.",
                ajustes.notificaciones && permisoNotif,
            ) { activar ->
                if (activar && !permisoNotif && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pedirNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else vm.setNotificaciones(activar)
            }
            if (ajustes.notificaciones && !permisoNotif) {
                TextButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                }) { Text("El permiso está desactivado. Abrir ajustes del teléfono") }
            }
        }

        TituloSeccion("Tamaño del texto")
        Bloque {
            listOf(1f to "Normal", 1.2f to "Grande", 1.4f to "Muy grande").forEach { (escala, nombre) ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .selectable(selected = ajustes.escalaTexto == escala, role = Role.RadioButton) { vm.setEscala(escala) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = ajustes.escalaTexto == escala, onClick = null)
                    Spacer(Modifier.width(10.dp))
                    Text(nombre, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        TituloSeccion("Idioma")
        Bloque {
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = true, onClick = null)
                Spacer(Modifier.width(10.dp))
                Text("Castellano", style = MaterialTheme.typography.bodyLarge)
            }
            Text(
                "Más adelante: audio y las lenguas locales que se acuerden con la comunidad, con el mismo texto equivalente.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        TituloSeccion("Mi zona")
        FilaAccion(Icons.Outlined.EditLocationAlt, ajustes.nombreZona ?: "Sin zona", "Cambiar zona o su nombre", onCambiarZona)

        TituloSeccion("Datos guardados")
        Bloque {
            Text(
                ajustes.ultimaSincronizacion?.let { "Última actualización: ${Formato.fechaHora(it)}" } ?: "Todavía no se descargaron avisos.",
                style = MaterialTheme.typography.bodyMedium,
            )
            ajustes.ultimoError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            Button(onClick = { vm.sincronizar() }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(if (enLinea) "Actualizar ahora" else "Actualizar al volver la señal")
            }
        }

        TituloSeccion("Demostración")
        Bloque {
            FilaSwitch(
                "Modo demostración",
                "Agrega avisos de EJEMPLO alrededor de tu zona para mostrar la app sin servidor ni señal.",
                ajustes.modoDemo,
            ) { vm.setModoDemo(it) }
            if (ajustes.modoDemo) {
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                Text("Caso simulado: ${DemoAvisos.descripcionPaso(ajustes.demoPaso)}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Simula que el responsable publica una actualización. Si los avisos están activados, llega una notificación.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(onClick = { vm.simularActualizacion() }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Simular siguiente actualización")
                }
            }
        }

        var avanzado by rememberSaveable { mutableStateOf(false) }
        TextButton(onClick = { avanzado = !avanzado }, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(if (avanzado) "Ocultar opciones avanzadas" else "Opciones avanzadas")
        }
        if (avanzado) {
            Bloque {
                var url by rememberSaveable { mutableStateOf(ajustes.apiBaseUrl) }
                OutlinedTextField(
                    value = url, onValueChange = { url = it }, singleLine = true,
                    label = { Text("Servidor Ahgora") },
                    supportingText = { Text("La app solo lee avisos aprobados (GET /api/alertas).") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.setServidor(url) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Guardar") }
                    TextButton(onClick = { url = BuildConfig.API_BASE_URL; vm.setServidor(url) }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("Restablecer")
                    }
                }
            }
        }
        Text(
            "Ahgora Comunidad ${BuildConfig.VERSION_NAME} · Gold4Good Bolivia 2026 · Desafío ORO-4.1",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Bloque(contenido: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { contenido() } }
}

@Composable
private fun FilaSwitch(titulo: String, detalle: String, valor: Boolean, onCambio: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).toggleable(value = valor, role = Role.Switch, onValueChange = onCambio),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(detalle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(10.dp))
        Switch(checked = valor, onCheckedChange = null)
    }
}
