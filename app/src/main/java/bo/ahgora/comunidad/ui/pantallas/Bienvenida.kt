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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import bo.ahgora.comunidad.ui.AhgoraViewModel
import bo.ahgora.comunidad.ui.Presentacion
import bo.ahgora.comunidad.ui.componentes.EstadoChip
import bo.ahgora.comunidad.ui.componentes.Nota
import bo.ahgora.comunidad.ui.theme.Paleta

/** Primer uso (guía, secciones 06 y 07). Sin cuenta: la comunidad consulta avisos aprobados. */
@Composable
fun PantallaBienvenida(vm: AhgoraViewModel) {
    var paso by rememberSaveable { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).navigationBarsPadding().imePadding()) {
        // Encabezado azul petróleo como la portada de la guía
        Column(
            Modifier.fillMaxWidth().background(Paleta.Petroleo).statusBarsPadding().padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            Text("Ahgora", style = MaterialTheme.typography.displaySmall, color = Color.White, modifier = Modifier.semantics { heading() })
            Text("Comunidad", style = MaterialTheme.typography.titleMedium, color = Paleta.Menta)
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { (paso + 1) / 3f }, color = Color(0xFFF2C14E), trackColor = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Paso ${paso + 1} de 3", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 6.dp))
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (paso) {
                0 -> PasoQueEs(onSeguir = { paso = 1 })
                1 -> PasoZona(vm, onSeguir = { paso = 2 })
                else -> PasoAvisos(onTerminar = { notif -> vm.completarBienvenida(notif) })
            }
        }
    }
}

@Composable
private fun PasoQueEs(onSeguir: () -> Unit) {
    Text("Avisos revisados sobre mercurio en tu zona", style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.semantics { heading() })
    Text(
        "Ahgora te cuenta qué muestras se tomaron cerca de donde vives, en qué estado está cada caso y a quién consultar.",
        style = MaterialTheme.typography.bodyLarge,
    )
    Punto(Icons.Outlined.Science, "Primero una prueba rápida, después el laboratorio",
        "En el sitio se hace un cribado. Solo el laboratorio confirma un resultado.")
    Punto(Icons.Outlined.CloudOff, "Funciona sin señal",
        "Los avisos se guardan en el teléfono y se actualizan cuando vuelve la conexión.")
    Punto(Icons.Outlined.Lock, "Sin cuenta y sin tu ubicación exacta",
        "Solo guardamos una zona aproximada de unos 11 km.")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        EstadoChip(Presentacion.PENDIENTE)
        EstadoChip(Presentacion.CONFIRMADO)
    }
    Nota("Importante", "Un aviso es una primera señal, no un diagnóstico ni una prueba legal.")
    Button(onClick = onSeguir, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Empezar") }
}

@Composable
private fun PasoZona(vm: AhgoraViewModel, onSeguir: () -> Unit) {
    Text("¿Cuál es tu zona?", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
    Text(
        "La preguntamos una sola vez para mostrarte los avisos cercanos. Puedes cambiarla cuando quieras desde Inicio.",
        style = MaterialTheme.typography.bodyLarge,
    )
    Nota(
        "Tu ubicación queda en el teléfono",
        "Usamos el GPS, que funciona sin internet, y guardamos solo un área de unos 11 km. Tu posición exacta no se guarda ni se envía.",
    )
    SelectorZona(vm, textoGuardar = "Usar esta zona", onGuardado = onSeguir)
}

@Composable
private fun PasoAvisos(onTerminar: (Boolean) -> Unit) {
    val pedir = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        onTerminar(concedido)
    }
    Text("¿Te avisamos de las novedades?", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
    Text(
        "Si hay un aviso nuevo o un cambio en tu zona, el teléfono mostrará un mensaje corto: «Hay una actualización de Ahgora para tu zona». " +
            "No muestra detalles en la pantalla bloqueada.",
        style = MaterialTheme.typography.bodyLarge,
    )
    Punto(Icons.Outlined.NotificationsActive, "Es tu elección", "Si dices que no, igual puedes ver todos los avisos en la app.")
    Button(
        onClick = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) pedir.launch(Manifest.permission.POST_NOTIFICATIONS)
            else onTerminar(true)
        },
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
    ) { Text("Sí, avisarme") }
    OutlinedButton(onClick = { onTerminar(false) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Ahora no") }
}

@Composable
private fun Punto(icono: ImageVector, titulo: String, texto: String) {
    Row(Modifier.semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(titulo, style = MaterialTheme.typography.titleSmall)
            Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
