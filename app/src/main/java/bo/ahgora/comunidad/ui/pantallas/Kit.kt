package bo.ahgora.comunidad.ui.pantallas

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Factory
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.OndemandVideo
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bo.ahgora.comunidad.BuildConfig
import bo.ahgora.comunidad.data.remote.KitDto
import bo.ahgora.comunidad.data.tecnico.Cribado
import bo.ahgora.comunidad.ui.AhgoraViewModel
import bo.ahgora.comunidad.ui.componentes.Nota
import bo.ahgora.comunidad.ui.tecnico.Temporizador
import bo.ahgora.comunidad.ui.theme.Paleta

/**
 * Medición comunitaria con el kit Ahgora. Primero el video tutorial (obligatorio); después cuatro pasos
 * sencillos: qué se mide, los pasos del kit con temporizadores, comparar la tira con la carta de colores
 * y el resultado. El resultado es una primera señal que se envía al equipo como reporte «en revisión»:
 * nunca se publica como aviso ni reemplaza al laboratorio.
 */

/** Pasos del kit en palabras sencillas (guía de la solución, sección 4.3). Textos para validar con el laboratorio aliado. */
private data class PasoKit(val texto: String, val segundos: Int? = null, val nota: String? = null)

private val PASOS_KIT = listOf(
    PasoKit("Ponte los guantes del kit.", nota = "Trabaja al aire libre, lejos de niños y de la comida."),
    PasoKit("Junta un poco de barro de 5 lugares cercanos del río. Mézclalo en la bolsa."),
    PasoKit("Cuela el barro con el colador del kit."),
    PasoKit("Pon una cucharita del barro colado en el tubo (hasta la marca)."),
    PasoKit("Echa el líquido del frasco 1 hasta la marca. Tapa y agita.", 15 * 60, "Agita un poco cada minuto."),
    PasoKit("Deja el tubo quieto para que el barro baje.", 5 * 60),
    PasoKit("Pasa el líquido de arriba por el papel filtro al vaso.", nota = "Que el líquido quede limpio, sin barro."),
    PasoKit("Echa el polvo del sobre 2 y espera.", 2 * 60, "Moja la tira de cloro: debe quedar SIN color."),
    PasoKit("Moja la tira de colores 2 segundos y espera.", 30),
    PasoKit("Todo lo usado va al frasco de residuos, bien cerrado. Nunca al río."),
)

/** Lo que la persona ve en la casilla de mercurio, comparada con la carta de colores del kit. */
private enum class ColorTira(val clave: String, val nombre: String, val detalle: String, val muestra: Color?) {
    SIN_CAMBIO("sin_cambio", "Igual que la tira nueva", "La casilla no cambió de color.", Color(0xFFF3EEDF)),
    LEVE("cambio_leve", "Cambio leve", "La casilla cambió un poco.", Color(0xFFF0C9A0)),
    FUERTE("cambio_fuerte", "Cambio fuerte", "La casilla cambió mucho.", Color(0xFFC97A4A)),
    NO_SE("no_seguro", "No estoy seguro", "No puedo decir si cambió.", null),
}

private data class Resultado(val titulo: String, val queHacer: String, val color: Color, val fondo: Color)

private fun resultado(cloroSinColor: Boolean?, color: ColorTira?): Resultado = when {
    cloroSinColor == false -> Resultado("La prueba no sirvió",
        "La tira de cloro tenía color. Repite desde el paso 8 o pide ayuda al promotor.", Color(0xFF4F5B62), Color(0xFFECEFF1))
    color == ColorTira.FUERTE -> Resultado("Hay que llevarla al laboratorio",
        "Guarda la muestra en su frasco, con la fecha y el lugar escritos, y avisa al equipo Ahgora o al promotor. No es un diagnóstico.",
        Color(0xFFB3261E), Color(0xFFFBE3E1))
    color == ColorTira.LEVE -> Resultado("Hay que revisarla",
        "Guarda la muestra en su frasco, con la fecha y el lugar escritos, y entrégala al promotor o al equipo Ahgora.",
        Color(0xFF8A5A00), Color(0xFFFFF1CC))
    color == ColorTira.SIN_CAMBIO -> Resultado("No se vio cambio",
        "Esto no garantiza que no haya mercurio. El equipo Ahgora decide si hace falta el laboratorio.",
        Color(0xFF1E6B3A), Color(0xFFE1F2E6))
    else -> Resultado("Pide ayuda", "Muestra la tira al promotor o al equipo Ahgora antes de que pasen 10 minutos.",
        Color(0xFF1D5B99), Color(0xFFE2EEF9))
}

@Composable
fun PantallaKit(vm: AhgoraViewModel, onListo: () -> Unit) {
    val ajustes by vm.ajustes.collectAsStateWithLifecycle()
    var verTutorial by rememberSaveable { mutableStateOf(false) }

    if (!ajustes.tutorialKitVisto || verTutorial) {
        PasoTutorial(
            yaVisto = ajustes.tutorialKitVisto,
            onVisto = { vm.marcarTutorialKitVisto(); verTutorial = false },
        )
        return
    }

    var paso by rememberSaveable { mutableStateOf(0) }
    var matriz by rememberSaveable { mutableStateOf("sedimento") }
    var rio by rememberSaveable { mutableStateOf("Río Coroico") }
    val hechos = remember { mutableStateListOf<Int>() }
    var cloro by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var color by rememberSaveable { mutableStateOf<ColorTira?>(null) }
    var incluirZona by rememberSaveable { mutableStateOf(true) }
    var consiento by rememberSaveable { mutableStateOf(false) }
    val titulos = listOf("Qué vas a medir", "Pasos del kit", "Mira la tira", "Resultado")

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Paso ${paso + 1} de 4 · ${titulos[paso]}", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).semantics { heading() })
                TextButton(onClick = { verTutorial = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(Icons.Outlined.OndemandVideo, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Video")
                }
            }
            LinearProgressIndicator(progress = { (paso + 1) / 4f }, modifier = Modifier.fillMaxWidth())
        }
        Column(Modifier.weight(1f).verticalScroll(key(paso) { rememberScrollState() }).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (paso) {
                0 -> {
                    Text("¿Qué vas a medir?", style = MaterialTheme.typography.headlineSmall)
                    OpcionKit("Barro del río", "Sedimento del fondo o de la orilla", Icons.Outlined.Landscape, matriz == "sedimento") { matriz = "sedimento" }
                    OpcionKit("Residuo de la mina", "Cola o lodo que sale de la planta", Icons.Outlined.Factory, matriz == "cola") { matriz = "cola" }
                    Text("¿De dónde?", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Río Coroico", "Quebrada", "Otro lugar").forEach { r ->
                            FilterChip(selected = rio == r, onClick = { rio = r }, label = { Text(r) }, modifier = Modifier.heightIn(min = 48.dp))
                        }
                    }
                }
                1 -> {
                    Text("Haz cada paso como en el video y márcalo. ${hechos.size} de ${PASOS_KIT.size} listos.", style = MaterialTheme.typography.bodyLarge)
                    PASOS_KIT.forEachIndexed { i, p ->
                        val hecho = i in hechos
                        Card(
                            onClick = { if (hecho) hechos.remove(i) else hechos.add(i) },
                            colors = CardDefaults.cardColors(containerColor = if (hecho) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(36.dp).background(if (hecho) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                                    contentAlignment = Alignment.Center) {
                                    if (hecho) Icon(Icons.Outlined.CheckCircle, null, tint = Color.White)
                                    else Text("${i + 1}", style = MaterialTheme.typography.titleMedium)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(p.texto, style = MaterialTheme.typography.bodyLarge)
                                    p.nota?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                }
                                p.segundos?.let { Temporizador(it, onTermina = { if (i !in hechos) hechos.add(i) }) }
                            }
                        }
                    }
                }
                2 -> {
                    Text("¿La tira de cloro quedó sin color?", style = MaterialTheme.typography.titleLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BotonSiNo("Sí, sin color", cloro == true, Modifier.weight(1f)) { cloro = true }
                        BotonSiNo("No, tiene color", cloro == false, Modifier.weight(1f)) { cloro = false }
                    }
                    if (cloro != false) {
                        Text("Compara la casilla de mercurio con la carta de colores del kit:", style = MaterialTheme.typography.titleLarge)
                        ColorTira.entries.forEach { c -> OpcionColor(c, color == c) { color = c } }
                        Text("Los colores de la pantalla son de referencia: usa la carta impresa del kit.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> {
                    val r = resultado(cloro, color)
                    Card(colors = CardDefaults.cardColors(containerColor = r.fondo), border = BorderStroke(2.dp, r.color), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(r.titulo, style = MaterialTheme.typography.headlineSmall, color = r.color)
                            Text(r.queHacer, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    Nota("Importante", Cribado.MENSAJE_COMUNIDAD)
                    Text("Enviar al equipo Ahgora", style = MaterialTheme.typography.titleMedium)
                    Text("Le ayuda a decidir dónde tomar muestras para el laboratorio. No se publica como aviso.",
                        style = MaterialTheme.typography.bodyMedium)
                    FilaInterruptor("Incluir mi zona aproximada (${ajustes.nombreZona ?: "mi comunidad"})", incluirZona) { incluirZona = it }
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = consiento, onCheckedChange = { consiento = it })
                        Text("Acepto enviar esta medición al equipo Ahgora.", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
        Surface(shadowElevation = 8.dp) {
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (paso > 0) OutlinedButton(onClick = { paso-- }, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) { Text("Atrás") }
                when (paso) {
                    in 0..1 -> Button(onClick = { paso++ }, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) { Text("Siguiente") }
                    2 -> Button(onClick = { paso = 3 }, enabled = cloro == false || (cloro == true && color != null),
                        modifier = Modifier.weight(1f).heightIn(min = 56.dp)) { Text("Ver resultado") }
                    else -> Button(
                        onClick = {
                            val r = when {
                                cloro == false -> "invalido"
                                else -> color?.clave ?: "no_seguro"
                            }
                            val kit = KitDto(resultado = r, matriz = matriz, cloroSinColor = cloro, pasosHechos = hechos.size,
                                pasosTotal = PASOS_KIT.size, tutorialVisto = true, rio = rio)
                            vm.enviarMedicionKit(kit, "Medición con el kit: ${resultado(cloro, color).titulo}", incluirZona)
                            onListo()
                        },
                        enabled = consiento, modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                    ) { Text("Enviar") }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------- tutorial

/** Video del kit: res/raw/tutorial_kit.mp4 si existe; si no, BuildConfig.TUTORIAL_KIT_URL. */
private fun fuenteVideo(context: android.content.Context): Uri? {
    @Suppress("DiscouragedApi")
    val raw = context.resources.getIdentifier("tutorial_kit", "raw", context.packageName)
    if (raw != 0) return Uri.parse("android.resource://${context.packageName}/$raw")
    return BuildConfig.TUTORIAL_KIT_URL.takeIf { it.isNotBlank() }?.let(Uri::parse)
}

@Composable
private fun PasoTutorial(yaVisto: Boolean, onVisto: () -> Unit) {
    val context = LocalContext.current
    val fuente = remember { fuenteVideo(context) }
    var terminado by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Primero mira el video", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        Text("Antes de usar el kit, mira el video completo. Enseña cada paso y cómo cuidarte.", style = MaterialTheme.typography.bodyLarge)
        if (fuente != null) {
            VideoTutorial(fuente, onTermina = { terminado = true })
            Text(if (terminado) "¡Listo! Ya viste el video." else "El botón se activa cuando termina el video.",
                style = MaterialTheme.typography.bodyMedium, color = if (terminado) Paleta.Verde else MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            // Todavía no hay video en la app: se muestra su lugar y se puede seguir (por ejemplo, con el promotor).
            Box(
                Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color(0xFF1B2B30), RoundedCornerShape(16.dp))
                    .semantics { contentDescription = "Lugar del video tutorial del kit" },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.OndemandVideo, null, tint = Color.White, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Aquí va el video tutorial del kit", color = Color.White, style = MaterialTheme.typography.titleMedium)
                }
            }
            Text("Mientras el video no esté en la app, mira el tutorial con el promotor o el equipo Ahgora.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = terminado, onCheckedChange = { terminado = it })
                Text("Ya vi el tutorial completo.", style = MaterialTheme.typography.bodyLarge)
            }
        }
        Button(onClick = onVisto, enabled = terminado || yaVisto, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Text(if (yaVisto) "Volver a medir" else "Empezar a medir")
        }
        if (fuente != null && yaVisto && !terminado) Text("Ya viste el video antes: puedes volver a medir cuando quieras.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun VideoTutorial(fuente: Uri, onTermina: () -> Unit) {
    val context = LocalContext.current
    val video = remember {
        VideoView(context).apply {
            setMediaController(MediaController(context).also { it.setAnchorView(this) })
            setVideoURI(fuente)
            setOnCompletionListener { onTermina() }
            setOnErrorListener { _, _, _ -> true } // sin señal o archivo dañado: no se cierra la app
        }
    }
    DisposableEffect(video) { onDispose { video.stopPlayback() } }
    AndroidView(factory = { video }, modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black))
}

// ---------------------------------------------------------------------------------- piezas

@Composable
private fun OpcionKit(titulo: String, detalle: String, icono: ImageVector, elegida: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = if (elegida) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
        border = BorderStroke(if (elegida) 2.dp else 1.dp, if (elegida) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp).semantics { selected = elegida },
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icono, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.titleLarge)
                Text(detalle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(if (elegida) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked, null,
                tint = if (elegida) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun OpcionColor(c: ColorTira, elegida: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = if (elegida) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
        border = BorderStroke(if (elegida) 2.dp else 1.dp, if (elegida) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp).semantics { selected = elegida },
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (c.muestra != null) Box(Modifier.size(48.dp).background(c.muestra, RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)))
            else Box(Modifier.size(48.dp).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Text("?", style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(c.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(c.detalle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun BotonSiNo(texto: String, elegido: Boolean, modifier: Modifier, onClick: () -> Unit) {
    if (elegido) Button(onClick = onClick, modifier = modifier.heightIn(min = 56.dp)) { Text(texto) }
    else OutlinedButton(onClick = onClick, modifier = modifier.heightIn(min = 56.dp)) { Text(texto) }
}

@Composable
private fun FilaInterruptor(texto: String, valor: Boolean, onCambio: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(texto, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = valor, onCheckedChange = onCambio)
    }
}
