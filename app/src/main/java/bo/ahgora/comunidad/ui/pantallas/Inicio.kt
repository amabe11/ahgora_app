package bo.ahgora.comunidad.ui.pantallas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.EditLocationAlt
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bo.ahgora.comunidad.data.local.AvisoEntity
import bo.ahgora.comunidad.ui.AhgoraViewModel
import bo.ahgora.comunidad.ui.Formato
import bo.ahgora.comunidad.ui.Presentacion
import bo.ahgora.comunidad.ui.Textos
import bo.ahgora.comunidad.ui.componentes.EstadoChip
import bo.ahgora.comunidad.ui.componentes.EstadoDatos
import bo.ahgora.comunidad.ui.componentes.FilaAccion
import bo.ahgora.comunidad.ui.componentes.SelloEjemplo
import bo.ahgora.comunidad.ui.componentes.TituloSeccion
import bo.ahgora.comunidad.ui.theme.Paleta

/** Inicio: qué pasó cerca de su zona, en qué estado está y a quién consultar (guía, sección 04). */
@Composable
fun PantallaInicio(
    vm: AhgoraViewModel,
    onAbrirAviso: (String) -> Unit,
    onVerAvisos: () -> Unit,
    onCambiarZona: () -> Unit,
    onAyuda: () -> Unit,
    onReportar: () -> Unit,
) {
    val ajustes by vm.ajustes.collectAsStateWithLifecycle()
    val avisos by vm.avisos.collectAsStateWithLifecycle()
    val enLinea by vm.enLinea.collectAsStateWithLifecycle()
    val sincronizando by vm.sincronizando.collectAsStateWithLifecycle()
    val celda = ajustes.celda

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        EstadoDatos(enLinea, ajustes.ultimaSincronizacion, sincronizando, onActualizar = { vm.sincronizar() })

        if (ajustes.modoDemo) {
            Surface(color = Paleta.AmbarFondo, contentColor = Paleta.Ambar, shape = RoundedCornerShape(12.dp)) {
                Text(
                    "Modo demostración: los avisos con el sello EJEMPLO no son reales.",
                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                )
            }
        }

        // Mi zona
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
                    Text("Mi zona", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(ajustes.nombreZona ?: "Sin zona", style = MaterialTheme.typography.titleLarge)
                    celda?.let { Text(it.rangoLegible(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                OutlinedButton(onClick = onCambiarZona, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(Icons.Outlined.EditLocationAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Cambiar")
                }
            }
        }

        val lista = avisos
        if (lista == null) {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Column
        }
        val vigentes = lista.filter { !it.retirado }
        val enMiZona = vigentes.filter { celda != null && it.celda == celda }
        val vecinas = vigentes.filter { celda != null && it.celda.distancia(celda) == 1 }
        val destacado = enMiZona.sortedWith(
            compareBy<AvisoEntity> { it.leido }.thenByDescending { Presentacion.de(it).prioridad }.thenByDescending { it.actualizadoEn }
        ).firstOrNull()

        TituloSeccion("Aviso vigente en tu zona")
        if (destacado != null) AvisoDestacado(destacado, onAbrir = { onAbrirAviso(destacado.id) })
        else SinAvisos(ajustes.ultimaSincronizacion)

        // Resumen
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Resumen("En tu zona", enMiZona.size, Modifier.weight(1f), onVerAvisos)
            Resumen("Zonas vecinas", vecinas.size, Modifier.weight(1f), onVerAvisos)
        }
        val sinLeer = vigentes.count { !it.leido && celda != null && it.celda.distancia(celda) <= 1 }
        if (sinLeer > 0) {
            Text(
                "Tienes $sinLeer aviso(s) sin leer cerca de tu zona.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold,
            )
        }

        TituloSeccion("Ayuda rápida")
        FilaAccion(Icons.AutoMirrored.Outlined.HelpOutline, "¿Qué significa cada aviso?", "Cribado, análisis y resultado de laboratorio", onAyuda)
        FilaAccion(Icons.Outlined.SupportAgent, "¿Dónde pregunto?", "Contacto oficial de tu zona", onAyuda)
        FilaAccion(Icons.Outlined.Campaign, "Reportar algo que viste", "Agua turbia, peces muertos, nueva actividad…", onReportar)

        Text(
            Textos.PRIMERA_SENAL, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun AvisoDestacado(a: AvisoEntity, onAbrir: () -> Unit) {
    val p = Presentacion.de(a)
    Card(
        onClick = onAbrir,
        colors = CardDefaults.cardColors(containerColor = p.fondo),
        border = BorderStroke(1.dp, p.color.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EstadoChip(p, grande = true)
                Spacer(Modifier.weight(1f))
                if (a.esEjemplo) SelloEjemplo()
            }
            Column(Modifier.semantics(mergeDescendants = true) {}) {
                Text(Textos.titulo(a), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.size(4.dp))
                Text(
                    a.mensaje.ifBlank { Textos.quePaso(a) }, style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface, maxLines = 4, overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    "Publicado: ${Formato.fechaHora(a.publicadoEn)}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = onAbrir, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Ver detalle y qué hacer") }
        }
    }
}

@Composable
fun SinAvisos(ultimaSync: Long?, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp).semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Inbox, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(Textos.SIN_AVISOS, style = MaterialTheme.typography.titleSmall)
                Text(
                    if (ultimaSync != null) "Revisado con el servidor el ${Formato.fechaHora(ultimaSync)}."
                    else "Revisa tu zona y vuelve a intentar cuando tengas señal.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Resumen(titulo: String, n: Int, modifier: Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.heightIn(min = 72.dp).semantics(mergeDescendants = true) {
            contentDescription = "$titulo: $n avisos vigentes. Abrir avisos."
        },
    ) {
        Column(Modifier.padding(14.dp)) {
            Text("$n", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.secondary)
            Text(titulo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
