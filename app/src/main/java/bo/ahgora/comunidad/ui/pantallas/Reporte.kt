package bo.ahgora.comunidad.ui.pantallas

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
import androidx.compose.material.icons.outlined.Agriculture
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.SetMeal
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bo.ahgora.comunidad.data.local.EstadoReporte
import bo.ahgora.comunidad.ui.AhgoraViewModel
import bo.ahgora.comunidad.ui.Formato
import bo.ahgora.comunidad.ui.componentes.Nota
import bo.ahgora.comunidad.ui.componentes.TituloSeccion

private data class TipoReporte(val clave: String, val nombre: String, val icono: ImageVector)

private val TIPOS = listOf(
    TipoReporte("agua_turbia", "Agua turbia o de color extraño", Icons.Outlined.WaterDrop),
    TipoReporte("peces", "Peces muertos o enfermos", Icons.Outlined.SetMeal),
    TipoReporte("actividad_minera", "Actividad minera nueva", Icons.Outlined.Agriculture),
    TipoReporte("residuos", "Residuos o derrame en el río", Icons.Outlined.Delete),
    TipoReporte("otro", "Otra cosa", Icons.Outlined.MoreHoriz),
)

/**
 * Reporte comunitario voluntario (guía, sección 06): con consentimiento, ubicación voluntaria
 * (solo la zona aproximada) y pantalla de revisión antes de enviar. Queda en revisión: no se
 * publica como aviso sin el proceso de aprobación.
 */
@Composable
fun PantallaReporte(vm: AhgoraViewModel, onListo: () -> Unit) {
    val ajustes by vm.ajustes.collectAsStateWithLifecycle()
    val reportes by vm.reportes.collectAsStateWithLifecycle()
    val enLinea by vm.enLinea.collectAsStateWithLifecycle()
    var tipo by rememberSaveable { mutableStateOf<String?>(null) }
    var descripcion by rememberSaveable { mutableStateOf("") }
    var incluirZona by rememberSaveable { mutableStateOf(true) }
    var consiento by rememberSaveable { mutableStateOf(false) }
    var revisando by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!revisando) {
            Text(
                "Cuéntanos qué viste. Ayuda al equipo a decidir dónde tomar muestras. No es una denuncia y no se publica como aviso.",
                style = MaterialTheme.typography.bodyLarge,
            )
            TituloSeccion("¿Qué viste?")
            TIPOS.forEach { t ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp)
                        .selectable(selected = tipo == t.clave, role = Role.RadioButton) { tipo = t.clave },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = tipo == t.clave, onClick = null)
                    Spacer(Modifier.width(8.dp))
                    Icon(t.icono, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Text(t.nombre, style = MaterialTheme.typography.bodyLarge)
                }
            }
            OutlinedTextField(
                value = descripcion, onValueChange = { descripcion = it.take(500) },
                label = { Text("Detalles (opcional)") },
                supportingText = { Text("No escribas nombres de personas. ${descripcion.length}/500") },
                minLines = 3, modifier = Modifier.fillMaxWidth(),
            )
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp).toggleable(incluirZona, role = Role.Switch) { incluirZona = it },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Incluir mi zona aproximada", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Solo el área de unos 11 km (${ajustes.nombreZona ?: "sin zona"}). Nunca tu ubicación exacta.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = incluirZona, onCheckedChange = null)
            }
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp).toggleable(consiento, role = Role.Checkbox) { consiento = it },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = consiento, onCheckedChange = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Acepto enviar este reporte al equipo de Ahgora para su revisión.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Button(
                onClick = { revisando = true }, enabled = tipo != null && consiento,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) { Text("Revisar antes de enviar") }
        } else {
            Text("Revisa tu reporte", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Qué viste", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(TIPOS.first { it.clave == tipo }.nombre, style = MaterialTheme.typography.titleMedium)
                    Text("Detalles", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(descripcion.ifBlank { "(sin detalles)" }, style = MaterialTheme.typography.bodyLarge)
                    Text("Zona", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if (incluirZona) "${ajustes.nombreZona} (área de ~11 km)" else "No se incluye", style = MaterialTheme.typography.bodyLarge)
                }
            }
            Nota(
                "Qué pasa después",
                if (enLinea) "Se enviará ahora. El equipo lo revisa; no se publica como aviso."
                else "Sin conexión: quedará guardado en el teléfono y se enviará solo cuando vuelva la señal.",
            )
            Button(
                onClick = {
                    vm.enviarReporte(tipo!!, descripcion, incluirZona)
                    tipo = null; descripcion = ""; consiento = false; revisando = false
                    onListo()
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) { Text("Enviar reporte") }
            OutlinedButton(onClick = { revisando = false }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Corregir") }
        }

        if (reportes.isNotEmpty()) {
            TituloSeccion("Mis reportes")
            reportes.forEach { r ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp).semantics(mergeDescendants = true) {}) {
                        Text(TIPOS.firstOrNull { it.clave == r.tipo }?.nombre ?: r.tipo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(Formato.fechaHora(r.creadoEn), style = MaterialTheme.typography.bodySmall)
                        Text(
                            when (r.estado) {
                                EstadoReporte.ENVIADO -> "Enviado. En revisión por el equipo."
                                EstadoReporte.EN_COLA -> "Guardado en el teléfono. Se enviará con conexión."
                                EstadoReporte.SERVIDOR_NO_DISPONIBLE -> "Guardado. El servidor todavía no recibe reportes; se reintentará."
                            },
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}
