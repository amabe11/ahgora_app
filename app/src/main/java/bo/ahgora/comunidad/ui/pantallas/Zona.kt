package bo.ahgora.comunidad.ui.pantallas

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bo.ahgora.comunidad.data.Ubicador
import bo.ahgora.comunidad.data.prefs.OrigenZona
import bo.ahgora.comunidad.data.zona.Celda
import bo.ahgora.comunidad.data.zona.Localidades
import bo.ahgora.comunidad.ui.AhgoraViewModel
import bo.ahgora.comunidad.ui.componentes.Nota
import kotlinx.coroutines.launch

private sealed interface PasoZona {
    data object Elegir : PasoZona
    data object Buscando : PasoZona
    data object Lista : PasoZona
    data class Confirmar(val celda: Celda, val origen: OrigenZona, val sugerido: String) : PasoZona
}

/**
 * Selector de zona compartido por la bienvenida y «Cambiar zona». Funciona sin internet:
 * el GPS no necesita datos y la lista de localidades viene en la instalación.
 */
@Composable
fun SelectorZona(
    vm: AhgoraViewModel,
    textoGuardar: String,
    onGuardado: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var paso by remember { mutableStateOf<PasoZona>(PasoZona.Elegir) }
    var error by remember { mutableStateOf<String?>(null) }
    var ubicacionApagada by remember { mutableStateOf(false) }

    fun buscar() {
        error = null; ubicacionApagada = false
        paso = PasoZona.Buscando
        scope.launch {
            paso = when (val r = vm.obtenerZonaGps()) {
                is Ubicador.Resultado.Ok -> PasoZona.Confirmar(r.celda, OrigenZona.GPS, Localidades.nombreSugerido(r.celda))
                Ubicador.Resultado.SinPermiso -> { error = "Sin permiso de ubicación. Puedes elegir tu zona de la lista."; PasoZona.Elegir }
                Ubicador.Resultado.UbicacionApagada -> { error = "La ubicación del teléfono está apagada."; ubicacionApagada = true; PasoZona.Elegir }
                Ubicador.Resultado.SinSenal -> { error = "No se encontró señal. Sal al aire libre e intenta de nuevo, o elige de la lista."; PasoZona.Elegir }
            }
        }
    }

    val permisos = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        if (r.values.any { it }) buscar() else error = "Sin permiso de ubicación. Puedes elegir tu zona de la lista."
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (val p = paso) {
            PasoZona.Elegir -> {
                Button(
                    onClick = {
                        permisos.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) {
                    Icon(Icons.Outlined.MyLocation, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Usar mi ubicación")
                }
                OutlinedButton(onClick = { paso = PasoZona.Lista }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Icon(Icons.AutoMirrored.Outlined.List, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Elegir de una lista")
                }
                if (error != null) {
                    Text(
                        error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                    if (ubicacionApagada) {
                        TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }) {
                            Text("Abrir ajustes de ubicación")
                        }
                    }
                }
            }

            PasoZona.Buscando -> Row(
                Modifier.fillMaxWidth().padding(vertical = 16.dp).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(Modifier.size(28.dp))
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Buscando tu zona…", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Funciona sin internet. Al aire libre suele tardar menos de un minuto.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            PasoZona.Lista -> ListaLocalidades(
                onElegir = { loc -> paso = PasoZona.Confirmar(loc.celda, OrigenZona.LISTA, loc.nombre) },
                onVolver = { paso = PasoZona.Elegir },
            )

            is PasoZona.Confirmar -> {
                // Si se eligió de la lista, el nombre de la localidad queda como nombre de la zona.
                var nombre by remember(p) { mutableStateOf(if (p.origen == OrigenZona.LISTA) p.sugerido else "") }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(16.dp).semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Tu zona", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(p.sugerido, style = MaterialTheme.typography.titleLarge)
                            Text(
                                "Área de unos 11 km · ${p.celda.rangoLegible()}",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = nombre, onValueChange = { nombre = it.take(40) },
                    label = { Text("Nombre de tu comunidad (opcional)") },
                    supportingText = { Text("Solo se guarda en este teléfono.") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = {
                        vm.fijarZona(p.celda, p.origen, nombre.ifBlank { null })
                        onGuardado()
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) { Text(textoGuardar) }
                TextButton(onClick = { paso = PasoZona.Elegir }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Elegir otra zona") }
            }
        }
    }
}

@Composable
private fun ListaLocalidades(onElegir: (bo.ahgora.comunidad.data.zona.Localidad) -> Unit, onVolver: () -> Unit) {
    var filtro by remember { mutableStateOf("") }
    val lista = remember(filtro) {
        val f = filtro.trim().lowercase()
        Localidades.todas.filter { f.isEmpty() || it.nombre.lowercase().contains(f) || it.region.lowercase().contains(f) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = filtro, onValueChange = { filtro = it },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            label = { Text("Buscar localidad") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "Elige la localidad más cercana a donde vives.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        lista.forEach { loc ->
            Card(
                onClick = { onElegir(loc) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(loc.nombre, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(loc.region, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (lista.isEmpty()) Text("No hay coincidencias. Prueba con otro nombre o usa tu ubicación.")
        TextButton(onClick = onVolver, modifier = Modifier.heightIn(min = 48.dp)) { Text("Volver") }
    }
}

/** Pantalla «Cambiar zona». */
@Composable
fun PantallaZona(vm: AhgoraViewModel, onListo: () -> Unit) {
    val ajustes by vm.ajustes.collectAsStateWithLifecycle()
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ajustes.celda?.let { c ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp).semantics(mergeDescendants = true) {}) {
                    Text("Zona actual", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(ajustes.nombreZona.orEmpty(), style = MaterialTheme.typography.titleLarge)
                    Text(c.rangoLegible(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        if (ajustes.origenZona == OrigenZona.GPS) "Elegida con la ubicación del teléfono" else "Elegida de la lista",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Text("Elegir una nueva zona", style = MaterialTheme.typography.titleMedium)
        SelectorZona(vm, textoGuardar = "Guardar nueva zona", onGuardado = onListo)
        Nota(
            "Tu privacidad",
            "Ahgora guarda solo una zona aproximada de unos 11 km. Tu ubicación exacta no se guarda ni se envía.",
        )
    }
}
