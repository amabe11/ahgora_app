package bo.ahgora.comunidad.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import bo.ahgora.comunidad.data.local.AvisoEntity
import bo.ahgora.comunidad.data.zona.Celda
import bo.ahgora.comunidad.ui.Formato
import bo.ahgora.comunidad.ui.Presentacion
import bo.ahgora.comunidad.ui.Textos
import bo.ahgora.comunidad.ui.theme.Paleta

/** Estado con ícono + texto + color. Se lee como una sola frase en TalkBack. */
@Composable
fun EstadoChip(p: Presentacion, modifier: Modifier = Modifier, grande: Boolean = false) {
    Surface(
        color = p.fondo, contentColor = p.color, shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, p.color.copy(alpha = 0.35f)),
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = "Estado: ${p.etiqueta}" },
    ) {
        Row(
            Modifier.padding(horizontal = if (grande) 14.dp else 10.dp, vertical = if (grande) 8.dp else 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(p.icono, contentDescription = null, modifier = Modifier.size(if (grande) 22.dp else 18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                p.etiqueta,
                style = if (grande) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun SelloEjemplo(modifier: Modifier = Modifier) {
    Surface(color = Paleta.AmbarFondo, contentColor = Paleta.Ambar, shape = RoundedCornerShape(6.dp), modifier = modifier) {
        Text(
            "EJEMPLO", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                .semantics { contentDescription = "Aviso de ejemplo, no es real" },
        )
    }
}

@Composable
fun PuntoNuevo(modifier: Modifier = Modifier) {
    Row(modifier.semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
        Spacer(Modifier.width(4.dp))
        Text("Nuevo", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TituloSeccion(texto: String, modifier: Modifier = Modifier) {
    Text(
        texto, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary,
        modifier = modifier.padding(top = 8.dp, bottom = 4.dp).semantics { heading() },
    )
}

/** Tarjeta de aviso para listas. Área táctil completa (≥ 48 dp). */
@Composable
fun AvisoTarjeta(aviso: AvisoEntity, miCelda: Celda?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = Presentacion.de(aviso)
    val relacion = Formato.relacion(aviso.celda, miCelda)
    val fecha = Formato.fechaCorta(aviso.publicadoEn)
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (!aviso.leido) p.color.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = buildString {
                    append("${p.etiqueta}. ${Textos.titulo(aviso)}. $relacion. Publicado el $fecha.")
                    if (!aviso.leido) append(" Nuevo.")
                    if (aviso.esEjemplo) append(" Es un ejemplo.")
                }
            },
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EstadoChip(p)
                Spacer(Modifier.weight(1f))
                if (aviso.esEjemplo) SelloEjemplo()
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(Textos.titulo(aviso), style = MaterialTheme.typography.titleMedium)
                    Text(relacion, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (aviso.retirado) "Cambió el ${Formato.fechaCorta(aviso.retiradoEn ?: aviso.actualizadoEn)}" else "Publicado: $fecha",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (!aviso.leido) { Spacer(Modifier.width(10.dp)); PuntoNuevo() }
                    }
                }
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

/**
 * Línea de estado de los datos (guía, sección 06): distingue copia guardada de publicación reciente.
 */
@Composable
fun EstadoDatos(
    enLinea: Boolean,
    ultimaSync: Long?,
    sincronizando: Boolean,
    onActualizar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (icono, titulo, color, fondo) = when {
        sincronizando -> Cuatro(Icons.Outlined.Sync, "Actualizando avisos…", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
        !enLinea -> Cuatro(Icons.Outlined.CloudOff, "Sin conexión", Paleta.Ambar, Paleta.AmbarFondo)
        else -> Cuatro(Icons.Outlined.CloudDone, "Conectado", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
    }
    val detalle = when {
        ultimaSync == null && !enLinea -> "Todavía faltan avisos por descargar. Se intentará al volver la señal."
        ultimaSync == null -> "Aún no se descargaron avisos del servidor."
        !enLinea -> "Mostrando avisos guardados el ${Formato.fechaHora(ultimaSync)}."
        else -> "Última actualización: ${Formato.fechaHora(ultimaSync)} (${Formato.haceCuanto(ultimaSync)})."
    }
    Surface(color = fondo, shape = RoundedCornerShape(12.dp), modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (sincronizando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = color)
            else Icon(icono, contentDescription = null, tint = color)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f).clearAndSetSemantics { contentDescription = "$titulo. $detalle" }) {
                Text(titulo, style = MaterialTheme.typography.labelLarge, color = color)
                Text(detalle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            }
            if (enLinea && !sincronizando) TextButton(onClick = onActualizar, modifier = Modifier.heightIn(min = 48.dp)) { Text("Actualizar") }
        }
    }
}

private data class Cuatro<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

/** Recuadro informativo (menta) como en la guía. */
@Composable
fun Nota(titulo: String, texto: String, modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(14.dp), modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp).semantics(mergeDescendants = true) {}) {
            Text(titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.size(4.dp))
            Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

/** Fila de menú con ícono, ≥ 48 dp. */
@Composable
fun FilaAccion(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    detalle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (detalle != null) Text(detalle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}
