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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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

/** Avisos: primero los de mi comunidad, luego los de cerca. Los antiguos, a un toque (guía, sección 04). */
@Composable
fun PantallaAvisos(vm: AhgoraViewModel, onAbrirAviso: (String) -> Unit) {
    val ajustes by vm.ajustes.collectAsStateWithLifecycle()
    val avisos by vm.avisos.collectAsStateWithLifecycle()
    val enLinea by vm.enLinea.collectAsStateWithLifecycle()
    val sincronizando by vm.sincronizando.collectAsStateWithLifecycle()
    var verAntiguos by rememberSaveable { mutableStateOf(false) }
    val celda = ajustes.celda

    val lista = avisos
    if (lista == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val orden = compareBy<AvisoEntity> { it.leido }.thenByDescending { it.publicadoEn }
    val vigentes = lista.filter { !it.retirado }
    val mios = vigentes.filter { celda != null && it.celda == celda }.sortedWith(orden)
    val cerca = vigentes.filter { celda != null && it.celda.distancia(celda) == 1 }.sortedWith(orden)
    val antiguos = lista.filter { it.retirado && celda != null && it.celda.distancia(celda) <= 1 }
        .sortedByDescending { it.retiradoEn ?: it.actualizadoEn }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { TituloSeccion("En ${ajustes.nombreZona ?: "tu comunidad"}") }
        if (mios.isEmpty()) item { SinAvisos(ajustes.ultimaSincronizacion) }
        items(mios, key = { it.id }) { a -> AvisoTarjeta(a, celda, onClick = { onAbrirAviso(a.id) }) }

        if (cerca.isNotEmpty()) {
            item { TituloSeccion("Comunidades vecinas") }
            items(cerca, key = { it.id }) { a -> AvisoTarjeta(a, celda, onClick = { onAbrirAviso(a.id) }) }
        }

        if (antiguos.isNotEmpty()) {
            item {
                TextButton(onClick = { verAntiguos = !verAntiguos }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(Icons.Outlined.History, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (verAntiguos) "Ocultar avisos antiguos" else "Ver avisos antiguos o cambiados (${antiguos.size})")
                }
            }
            if (verAntiguos) items(antiguos, key = { "h-" + it.id }) { a -> AvisoTarjeta(a, celda, onClick = { onAbrirAviso(a.id) }) }
        }
        item { EstadoDatos(enLinea, ajustes.ultimaSincronizacion, sincronizando, onActualizar = { vm.sincronizar() }) }
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
            // Si el responsable escribió un mensaje, ese es el que se lee; si no, el texto estándar.
            Text(a.mensaje.ifBlank { Textos.quePaso(a) }, style = MaterialTheme.typography.bodyLarge)
        }
        Seccion("Qué puedes hacer") {
            Text(Textos.queHacer(a), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.size(8.dp))
            OutlinedButton(onClick = onContacto, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Icon(Icons.Outlined.SupportAgent, null)
                Spacer(Modifier.width(8.dp))
                Text("Preguntar al contacto oficial")
            }
        }
        Seccion("Qué se analizó") {
            Text(Textos.queSeAnalizo(a.matriz), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.size(6.dp))
            Text(Textos.alcance(a), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (a.esEjemplo) {
            Text(
                "Este es un aviso de EJEMPLO. No es real.",
                style = MaterialTheme.typography.bodyLarge, color = Paleta.Ambar, fontWeight = FontWeight.Bold,
            )
        }
        // Fuente y fechas en letra pequeña: están, pero no distraen.
        Text(
            Textos.entidad(a) + " " + Formato.haceCuantoPublicado(a.publicadoEn) + "." +
                (if (a.version > 1) " Cambió el ${Formato.fechaCorta(a.actualizadoEn)}." else ""),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
