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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import bo.ahgora.comunidad.ui.componentes.SelloEjemplo
import bo.ahgora.comunidad.ui.theme.Paleta

/**
 * Inicio (guía, sección 04): responde en pocos segundos qué pasó en mi comunidad, en qué estado está
 * y qué hago. Poco texto, botones grandes.
 */
@Composable
fun PantallaInicio(
    vm: AhgoraViewModel,
    onAbrirAviso: (String) -> Unit,
    onVerAvisos: () -> Unit,
    onVerMapa: () -> Unit,
    onCambiarZona: () -> Unit,
    onAyuda: () -> Unit,
    onReportar: () -> Unit,
) {
    val ajustes by vm.ajustes.collectAsStateWithLifecycle()
    val avisos by vm.avisos.collectAsStateWithLifecycle()
    val enLinea by vm.enLinea.collectAsStateWithLifecycle()
    val sincronizando by vm.sincronizando.collectAsStateWithLifecycle()
    val celda = ajustes.celda
    val nombre = ajustes.nombreZona ?: "tu comunidad"

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Mi comunidad
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                nombre, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f).semantics { heading() },
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            TextButton(onClick = onCambiarZona, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cambiar") }
        }

        if (ajustes.modoDemo) {
            Surface(color = Paleta.AmbarFondo, contentColor = Paleta.Ambar, shape = RoundedCornerShape(12.dp)) {
                Text(
                    "Modo de prueba: los avisos marcados EJEMPLO no son reales.",
                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                )
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

        if (destacado != null) AvisoDestacado(destacado, otros = enMiZona.size - 1, onAbrir = { onAbrirAviso(destacado.id) }, onVerTodos = onVerAvisos)
        else SinAvisos(ajustes.ultimaSincronizacion)

        if (vecinas.isNotEmpty()) {
            Card(
                onClick = onVerAvisos,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (vecinas.size == 1) "1 aviso en comunidades vecinas" else "${vecinas.size} avisos en comunidades vecinas",
                        style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f),
                    )
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.outline)
                }
            }
        }

        // Tres botones grandes
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            BotonGrande(Icons.Outlined.Map, "Mapa", Modifier.weight(1f), onVerMapa)
            BotonGrande(Icons.Outlined.SupportAgent, "Preguntar", Modifier.weight(1f), onAyuda)
            BotonGrande(Icons.Outlined.Campaign, "Reportar", Modifier.weight(1f), onReportar)
        }

        EstadoDatos(enLinea, ajustes.ultimaSincronizacion, sincronizando, onActualizar = { vm.sincronizar() })
    }
}

@Composable
private fun AvisoDestacado(a: AvisoEntity, otros: Int, onAbrir: () -> Unit, onVerTodos: () -> Unit) {
    val p = Presentacion.de(a)
    Card(
        onClick = onAbrir,
        colors = CardDefaults.cardColors(containerColor = p.fondo),
        border = BorderStroke(2.dp, p.color.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EstadoChip(p, grande = true)
                Spacer(Modifier.weight(1f))
                if (a.esEjemplo) SelloEjemplo()
            }
            Column(Modifier.semantics(mergeDescendants = true) {}) {
                Text(
                    a.mensaje.ifBlank { Textos.quePaso(a) }, style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface, maxLines = 4, overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.size(4.dp))
                Text(Formato.haceCuantoPublicado(a.publicadoEn), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onAbrir, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("¿Qué hago?", style = MaterialTheme.typography.titleSmall) }
            if (otros > 0) {
                TextButton(onClick = onVerTodos, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(if (otros == 1) "Hay 1 aviso más aquí" else "Hay $otros avisos más aquí")
                }
            }
        }
    }
}

@Composable
fun SinAvisos(ultimaSync: Long?, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Presentacion.CONFIRMADO.fondo),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp).semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.TaskAlt, contentDescription = null, tint = Presentacion.CONFIRMADO.color, modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(Textos.SIN_AVISOS, style = MaterialTheme.typography.titleMedium)
                if (ultimaSync == null) {
                    Text("Se revisará cuando haya señal.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun BotonGrande(icono: ImageVector, texto: String, modifier: Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.heightIn(min = 92.dp).semantics(mergeDescendants = true) { contentDescription = texto },
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
            Text(texto, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}
