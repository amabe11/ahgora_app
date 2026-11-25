package bo.ahgora.comunidad.ui.pantallas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bo.ahgora.comunidad.data.local.AvisoEntity
import bo.ahgora.comunidad.ui.AhgoraViewModel
import bo.ahgora.comunidad.ui.Formato
import bo.ahgora.comunidad.ui.Presentacion
import bo.ahgora.comunidad.ui.Textos
import bo.ahgora.comunidad.ui.componentes.AvisoTarjeta
import bo.ahgora.comunidad.ui.componentes.EstadoChip
import bo.ahgora.comunidad.ui.componentes.EstadoDatos
import bo.ahgora.comunidad.ui.componentes.Nota
import bo.ahgora.comunidad.ui.componentes.SelloEjemplo
import bo.ahgora.comunidad.ui.componentes.TituloSeccion
import bo.ahgora.comunidad.ui.theme.Paleta

enum class FiltroZona(val etiqueta: String) { MI_ZONA("Mi zona"), CERCA("Mi zona y vecinas"), TODOS("Todos") }

/** Avisos: historial y cambios de estado fechados (guía, sección 04). */
@Composable
fun PantallaAvisos(vm: AhgoraViewModel, onAbrirAviso: (String) -> Unit) {
    val ajustes by vm.ajustes.collectAsStateWithLifecycle()
    val avisos by vm.avisos.collectAsStateWithLifecycle()
    val enLinea by vm.enLinea.collectAsStateWithLifecycle()
    val sincronizando by vm.sincronizando.collectAsStateWithLifecycle()
    var filtro by rememberSaveable { mutableStateOf(FiltroZona.CERCA) }
    var verHistorial by rememberSaveable { mutableStateOf(false) }
    val celda = ajustes.celda

    val lista = avisos
    if (lista == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val enFiltro = lista.filter { a ->
        when (filtro) {
            FiltroZona.MI_ZONA -> celda != null && a.celda == celda
            FiltroZona.CERCA -> celda != null && a.celda.distancia(celda) <= 1
            FiltroZona.TODOS -> true
        }
    }
    val vigentes = enFiltro.filter { !it.retirado }
        .sortedWith(compareBy<AvisoEntity> { it.leido }.thenByDescending { it.publicadoEn })
    val historial = enFiltro.filter { it.retirado }.sortedByDescending { it.retiradoEn ?: it.actualizadoEn }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { EstadoDatos(enLinea, ajustes.ultimaSincronizacion, sincronizando, onActualizar = { vm.sincronizar() }) }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FiltroZona.entries.forEach { f ->
                    FilterChip(
                        selected = filtro == f, onClick = { filtro = f }, label = { Text(f.etiqueta) },
                        leadingIcon = if (filtro == f) { { Icon(Icons.Outlined.Check, null, Modifier.size(18.dp)) } } else null,
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }
        }
        item { TituloSeccion("Vigentes (${vigentes.size})") }
        if (vigentes.isEmpty()) item { SinAvisos(ajustes.ultimaSincronizacion) }
        items(vigentes, key = { it.id }) { a -> AvisoTarjeta(a, celda, onClick = { onAbrirAviso(a.id) }) }

        item {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.History, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Actualizados o retirados (${historial.size})", style = MaterialTheme.typography.titleSmall)
                    Text("Avisos que cambiaron o ya no están vigentes.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = verHistorial, onCheckedChange = { verHistorial = it })
            }
        }
        if (verHistorial) items(historial, key = { "h-" + it.id }) { a -> AvisoTarjeta(a, celda, onClick = { onAbrirAviso(a.id) }) }
    }
}

/** Detalle del aviso (guía, sección 05 «Qué lleva cada detalle»). */
@Composable
fun PantallaDetalleAviso(vm: AhgoraViewModel, id: String, onAbrirAviso: (String) -> Unit, onContacto: () -> Unit) {
    val flujo = remember(id) { vm.aviso(id) }
    val aviso by flujo.collectAsStateWithLifecycle(initialValue = null)
    val ajustes by vm.ajustes.collectAsStateWithLifecycle()
    LaunchedEffect(id) { vm.marcarLeido(id) }

    val a = aviso ?: run {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("Este aviso ya no está guardado en el teléfono.", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }
    val p = Presentacion.de(a)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EstadoChip(p, grande = true)
            Spacer(Modifier.weight(1f))
            if (a.esEjemplo) SelloEjemplo()
        }
        Text(Textos.titulo(a), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        Text(
            Formato.relacion(a.celda, ajustes.celda) +
                if (a.celda == ajustes.celda && ajustes.nombreZona != null) " · ${ajustes.nombreZona}" else "",
            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (a.retirado) {
            Card(colors = CardDefaults.cardColors(containerColor = Presentacion.RETIRADO.fondo)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Este aviso cambió el ${Formato.fechaHora(a.retiradoEn ?: a.actualizadoEn)}",
                        style = MaterialTheme.typography.titleSmall, color = Presentacion.RETIRADO.color,
                    )
                    Text("Motivo: ${a.motivoRetiro ?: "actualización del responsable"}", style = MaterialTheme.typography.bodyMedium)
                    a.reemplazadoPor?.let { nuevo ->
                        OutlinedButton(onClick = { onAbrirAviso(nuevo) }, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text("Ver la versión vigente")
                        }
                    }
                }
            }
        }
        a.reemplazaA?.let { viejo ->
            Nota("Actualización de un aviso", "Este aviso reemplaza a uno anterior de la misma zona.")
            TextButton(onClick = { onAbrirAviso(viejo) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Ver aviso anterior") }
        }

        Seccion("Qué pasó") {
            Text(Textos.quePaso(a), style = MaterialTheme.typography.bodyLarge)
            if (a.mensaje.isNotBlank() && a.mensaje.trim() != Textos.quePaso(a)) {
                Spacer(Modifier.size(8.dp))
                Text("Mensaje aprobado para la zona:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("«${a.mensaje}»", style = MaterialTheme.typography.bodyLarge, fontStyle = FontStyle.Italic)
            }
        }
        Seccion("Qué puedes hacer") {
            Text(Textos.queHacer(a), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.size(8.dp))
            OutlinedButton(onClick = onContacto, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Icon(Icons.Outlined.SupportAgent, null)
                Spacer(Modifier.width(8.dp))
                Text("Ver contacto oficial de la zona")
            }
        }
        Seccion("Qué se analizó") {
            Text(Textos.matriz(a.matriz).replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleSmall)
            Text(Textos.queSeAnalizo(a.matriz), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.size(6.dp))
            Text(Textos.alcance(a), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Seccion("Fechas") {
            Dato("Publicado", Formato.fechaHora(a.publicadoEn))
            Dato("Última actualización", Formato.fechaHora(a.actualizadoEn))
            Dato("Guardado en este teléfono", Formato.fechaHora(a.descargadoEn))
            Dato("Versión del aviso", a.version.toString())
        }
        Seccion("Fuente del aviso") {
            Text(Textos.entidad(a), style = MaterialTheme.typography.bodyMedium)
            if (a.esEjemplo) {
                Spacer(Modifier.size(6.dp))
                Text(
                    "Este es un aviso de EJEMPLO para demostración. No describe un lugar ni una muestra real.",
                    style = MaterialTheme.typography.bodyMedium, color = Paleta.Ambar, fontWeight = FontWeight.Bold,
                )
            }
        }
        Text(Textos.PRIMERA_SENAL, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Seccion(titulo: String, contenido: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() }.padding(bottom = 6.dp))
            contenido()
        }
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp).semantics(mergeDescendants = true) {}) {
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(valor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}
