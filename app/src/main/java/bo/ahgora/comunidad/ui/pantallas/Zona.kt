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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CheckCircle
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bo.ahgora.comunidad.data.Ubicador
import bo.ahgora.comunidad.data.prefs.OrigenZona
import bo.ahgora.comunidad.data.zona.Celda
import bo.ahgora.comunidad.data.zona.Localidad
import bo.ahgora.comunidad.data.zona.Localidades
import bo.ahgora.comunidad.ui.AhgoraViewModel
import bo.ahgora.comunidad.ui.componentes.Nota
import kotlinx.coroutines.launch

private sealed interface PasoZona {
    data object Elegir : PasoZona
    data object Buscando : PasoZona
    data class ConfirmarGps(val celda: Celda, val sugerido: String) : PasoZona
}

/** Cuántas comunidades cercanas a Alcoche se muestran antes de «Buscar otra». */
private const val CERCANAS = 6

/**
 * Selector de comunidad compartido por la bienvenida y «Cambiar comunidad». Funciona sin internet:
 * la lista viene en la instalación (las más cercanas a Alcoche primero) y el GPS no necesita datos.
 * Un solo toque en la lista guarda la comunidad.
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
    val ajustes by vm.ajustes.collectAsStateWithLifecycle()
    var paso by remember { mutableStateOf<PasoZona>(PasoZona.Elegir) }
    var error by remember { mutableStateOf<String?>(null) }
    var ubicacionApagada by remember { mutableStateOf(false) }
    var buscarOtra by remember { mutableStateOf(false) }

    fun guardar(celda: Celda, origen: OrigenZona, nombre: String?) {
        vm.fijarZona(celda, origen, nombre)
        onGuardado()
    }

    fun buscarGps() {
        error = null; ubicacionApagada = false
        paso = PasoZona.Buscando
        scope.launch {
            paso = when (val r = vm.obtenerZonaGps()) {
                is Ubicador.Resultado.Ok -> PasoZona.ConfirmarGps(r.celda, Localidades.nombreSugerido(r.celda))
                Ubicador.Resultado.SinPermiso -> { error = "Sin permiso de ubicación. Elige tu comunidad de la lista."; PasoZona.Elegir }
                Ubicador.Resultado.UbicacionApagada -> { error = "La ubicación del teléfono está apagada."; ubicacionApagada = true; PasoZona.Elegir }
                Ubicador.Resultado.SinSenal -> { error = "No se encontró tu ubicación. Sal al aire libre o elige de la lista."; PasoZona.Elegir }
            }
        }
    }

    val permisos = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        if (r.values.any { it }) buscarGps() else error = "Sin permiso de ubicación. Elige tu comunidad de la lista."
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when (val p = paso) {
            PasoZona.Elegir -> {
                Text("Toca tu comunidad:", style = MaterialTheme.typography.titleMedium)
                val lista = if (buscarOtra) emptyList() else Localidades.cercaDeAlcoche.take(CERCANAS)
                lista.forEach { loc ->
                    FilaLocalidad(loc, elegida = ajustes.celda == loc.celda) { guardar(loc.celda, OrigenZona.LISTA, null) }
                }
                if (buscarOtra) {
                    BuscarLocalidad(onElegir = { loc -> guardar(loc.celda, OrigenZona.LISTA, null) })
                } else {
                    TextButton(onClick = { buscarOtra = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Icon(Icons.Outlined.Search, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Buscar otra comunidad")
                    }
                }
                OutlinedButton(
                    onClick = { permisos.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) {
                    Icon(Icons.Outlined.MyLocation, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Usar mi ubicación")
                }
                if (error != null) {
                    Text(
                        error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                    if (ubicacionApagada) {
                        TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }) {
                            Text("Encender la ubicación")
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
                    Text("Buscando dónde estás…", style = MaterialTheme.typography.titleSmall)
                    Text("No necesita internet. Al aire libre es más rápido.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            is PasoZona.ConfirmarGps -> {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp).semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.LocationOn, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Estás en", style = MaterialTheme.typography.labelLarge)
                            Text(p.sugerido, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
                Button(onClick = { guardar(p.celda, OrigenZona.GPS, null) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Text(textoGuardar)
                }
                TextButton(onClick = { paso = PasoZona.Elegir }, modifier = Modifier.heightIn(min = 48.dp)) { Text("No, elegir de la lista") }
            }
        }
    }
}

@Composable
private fun FilaLocalidad(loc: Localidad, elegida: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (elegida) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(if (elegida) 2.dp else 1.dp, if (elegida) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(loc.nombre, style = MaterialTheme.typography.titleMedium)
                Text(loc.region, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (elegida) Icon(Icons.Outlined.CheckCircle, contentDescription = "Elegida", tint = MaterialTheme.colorScheme.primary)
            else Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun BuscarLocalidad(onElegir: (Localidad) -> Unit) {
    var filtro by remember { mutableStateOf("") }
    val f = filtro.trim().lowercase()
    val lista = Localidades.cercaDeAlcoche.filter { f.isEmpty() || it.nombre.lowercase().contains(f) || it.region.lowercase().contains(f) }
    OutlinedTextField(
        value = filtro, onValueChange = { filtro = it },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        label = { Text("Nombre de la comunidad") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
    )
    lista.forEach { loc -> FilaLocalidad(loc, elegida = false) { onElegir(loc) } }
    if (lista.isEmpty()) Text("No la encontramos. Prueba otro nombre o usa tu ubicación.", style = MaterialTheme.typography.bodyMedium)
}

/** Pantalla «Cambiar comunidad». */
@Composable
fun PantallaZona(vm: AhgoraViewModel, onListo: () -> Unit) {
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SelectorZona(vm, textoGuardar = "Sí, es mi comunidad", onGuardado = onListo)
        Nota("Tu privacidad", "Solo guardamos tu comunidad en este teléfono. Tu ubicación exacta no se guarda ni se envía.")
    }
}
