package bo.ahgora.comunidad.ui.tecnico

import android.Manifest
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.SetMeal
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Factory
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import bo.ahgora.comunidad.data.mapa.ContextoAlcoche
import bo.ahgora.comunidad.data.remote.LectorDto
import bo.ahgora.comunidad.data.remote.RgbcDto
import bo.ahgora.comunidad.data.tecnico.Cribado
import bo.ahgora.comunidad.data.tecnico.LectorBluetooth
import bo.ahgora.comunidad.data.zona.Localidades
import bo.ahgora.comunidad.ui.componentes.Nota
import bo.ahgora.comunidad.ui.theme.Paleta
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/** Un paso del procedimiento guiado, con temporizador opcional (segundos). */
data class PasoGuiado(val texto: String, val segundos: Int? = null, val nota: String? = null)

/** Procedimiento SED/colas (guía de la solución, sección 4.3). */
val PASOS_SED = listOf(
    PasoGuiado("Muestra compuesta: 5 a 10 porciones de 0–5 cm en un radio de 5 m, mezcladas en una bolsa."),
    PasoGuiado("Tamizar a 2 mm y mezclar bien."),
    PasoGuiado("Humedad: pesar 10 g aparte y secar a la sombra, a menos de 40 °C.", nota = "Nunca con calor fuerte: el mercurio se evapora."),
    PasoGuiado("Pesar 5,00 g de sedimento húmedo en un tubo de 15 mL."),
    PasoGuiado("Añadir 5 mL de extractante (lejía ~2,5 % + sal 5 %) y agitar.", 15 * 60, "Agitar a mano cada minuto."),
    PasoGuiado("Dejar decantar.", 5 * 60),
    PasoGuiado("Filtrar hasta tener unos 3 mL de extracto limpio."),
    PasoGuiado("Neutralizar el cloro con ~0,22 g de ascorbato de sodio y esperar.", 2 * 60, "La tira de cloro libre debe marcar 0."),
    PasoGuiado("Medir el pH con la tira de pH."),
    PasoGuiado("Sumergir la tira de metales 2 segundos y esperar.", 30),
    PasoGuiado("Colocar la tira en el soporte del lector."),
    PasoGuiado("Residuos al frasco cerrado y etiquetado. Nunca al río."),
)

/** Procedimiento PEZ, conceptual (guía de la solución, sección 5.3). */
val PASOS_PEZ = listOf(
    PasoGuiado("Registrar especie, talla y peso del pez."),
    PasoGuiado("Cortar músculo dorsal, sin piel ni espinas."),
    PasoGuiado("Triturar la porción de músculo."),
    PasoGuiado("Pesar unos 0,5–1 g de tejido fresco."),
    PasoGuiado("Guardar en frío y etiquetado para el laboratorio aliado.", nota = "La digestión y la medición ASV las hace el laboratorio."),
    PasoGuiado("Residuos al frasco cerrado y etiquetado."),
)

private val TITULOS = listOf("Identificar", "Muestreo", "Procedimiento", "Controles y lectura", "Resultado")

@Composable
fun PantallaNuevaMuestra(vm: TecnicoViewModel, onGuardada: () -> Unit) {
    val b = vm.borrador
    val scope = rememberCoroutineScope()
    var guardando by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { if (b.codigo.isEmpty()) vm.actualizarCodigo() }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text("Paso ${b.paso + 1} de ${TITULOS.size} · ${TITULOS[b.paso]}", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(progress = { (b.paso + 1f) / TITULOS.size }, modifier = Modifier.fillMaxWidth())
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (b.paso) {
                0 -> PasoIdentificar(vm)
                1 -> PasoMuestreo(b)
                2 -> PasoProcedimiento(b)
                3 -> PasoLectura(vm)
                else -> PasoResultado(vm)
            }
        }
        Surface(shadowElevation = 8.dp) {
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (b.paso > 0) OutlinedButton(onClick = { b.paso-- }, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) { Text("Atrás") }
                if (b.paso < TITULOS.lastIndex) {
                    Button(onClick = { b.paso++ }, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) { Text("Siguiente") }
                } else {
                    Button(
                        onClick = { scope.launch { guardando = true; val ok = vm.guardar(); guardando = false; if (ok) onGuardada() } },
                        enabled = !guardando, modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                    ) { Text(if (guardando) "Guardando…" else "Guardar muestra") }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------- paso 1

@Composable
private fun PasoIdentificar(vm: TecnicoViewModel) {
    val b = vm.borrador
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var buscandoGps by remember { mutableStateOf(false) }
    var errorGps by remember { mutableStateOf<String?>(null) }
    var archivoPendiente by remember { mutableStateOf<File?>(null) }

    fun gps() { scope.launch { buscandoGps = true; errorGps = vm.tomarGps(); buscandoGps = false } }
    val permisoGps = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        if (r.values.any { it }) gps() else errorGps = "Sin permiso de ubicación. Se usará Alcoche (aproximado)."
    }
    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val f = archivoPendiente
        if (ok && f != null && f.length() > 0) { b.fotoRuta?.let { File(it).delete() }; b.fotoRuta = f.absolutePath } else f?.delete()
        archivoPendiente = null
    }

    Titulo("¿Qué muestra es?")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(Triple("sedimento", "Sedimento", Icons.Outlined.Landscape), Triple("cola", "Cola minera", Icons.Outlined.Factory),
            Triple("pescado", "Pescado", Icons.Outlined.SetMeal)).forEach { (valor, nombre, icono) ->
            OpcionGrande(nombre, icono, b.matriz == valor, Modifier.weight(1f)) { b.matriz = valor; vm.actualizarCodigo() }
        }
    }

    Titulo("Código de la muestra")
    OutlinedTextField(
        value = b.codigo, onValueChange = { b.codigo = it.uppercase().take(40); b.codigoEditado = true },
        singleLine = true, modifier = Modifier.fillMaxWidth(),
        supportingText = { Text("Escríbelo igual en la etiqueta del frasco o la bolsa.") },
    )

    Titulo("Lugar")
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val cerca = Localidades.masCercanaA(b.lat, b.lon)
            Text(
                if (b.conGps) "GPS: %.5f, %.5f".format(b.lat, b.lon) + (b.precisionM?.let { " (±%.0f m)".format(it) } ?: "")
                else "Alcoche (punto por defecto, aproximado)",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                if (b.conGps) "Cerca de ${cerca.first.nombre} (%.1f km)".format(cerca.second)
                else "Toma el GPS en el punto exacto donde sacas la muestra.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (buscandoGps) Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)); Text("Buscando GPS…")
            } else OutlinedButton(
                onClick = { permisoGps.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) { Icon(Icons.Outlined.MyLocation, null); Spacer(Modifier.width(8.dp)); Text(if (b.conGps) "Tomar GPS de nuevo" else "Tomar GPS aquí") }
            errorGps?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
    }
    OutlinedTextField(value = b.cuenca, onValueChange = { b.cuenca = it.take(60) }, label = { Text("Río o cuenca") },
        singleLine = true, modifier = Modifier.fillMaxWidth())

    Titulo("Foto del sitio")
    b.fotoRuta?.let { ruta ->
        val bmp = remember(ruta) { decodificar(ruta, 900) }
        if (bmp != null) Image(bmp.asImageBitmap(), "Foto del sitio", contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(180.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)))
    }
    OutlinedButton(
        onClick = {
            val f = vm.archivoFotoNuevo()
            archivoPendiente = f
            val uri: Uri = FileProvider.getUriForFile(context, context.packageName + ".archivos", f)
            runCatching { camara.launch(uri) }.onFailure { vm.avisar("No se encontró la cámara.") }
        },
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
    ) { Icon(Icons.Outlined.CameraAlt, null); Spacer(Modifier.width(8.dp)); Text(if (b.fotoRuta == null) "Tomar foto" else "Cambiar foto") }
    Text("La foto queda solo en este teléfono (no se envía al servidor).", style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

// ---------------------------------------------------------------------------------- paso 2

@Composable
private fun PasoMuestreo(b: Borrador) {
    if (b.esPescado) {
        Titulo("Especie")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ContextoAlcoche.peces.forEach { p ->
                FilterChip(selected = b.especie == p.nombre, onClick = { b.especie = p.nombre }, label = { Text(p.nombre) },
                    modifier = Modifier.heightIn(min = 48.dp))
            }
        }
        OutlinedTextField(value = b.especie, onValueChange = { b.especie = it.take(40) }, label = { Text("Otra especie / nombre local") },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CampoNumero("Talla (cm)", b.tallaCm, Modifier.weight(1f)) { b.tallaCm = it }
            CampoNumero("Peso (g)", b.pesoG, Modifier.weight(1f)) { b.pesoG = it }
        }
    } else {
        Nota("Muestra compuesta", "5 a 10 porciones de la capa de arriba (0 a 5 cm), en un radio de unos 5 m, mezcladas en una bolsa.")
        Titulo("Porciones mezcladas")
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { if (b.porciones > 1) b.porciones-- }, modifier = Modifier.size(56.dp)) { Text("−") }
            Text("${b.porciones}", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = 24.dp))
            OutlinedButton(onClick = { if (b.porciones < 30) b.porciones++ }, modifier = Modifier.size(56.dp)) { Text("+") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CampoNumero("Profundidad (cm)", b.profundidadCm, Modifier.weight(1f)) { b.profundidadCm = it }
            CampoNumero("Material fino (%)", b.finoPct, Modifier.weight(1f)) { b.finoPct = it }
        }
        Text("Material fino = limo y arcilla. El mercurio se junta en las partículas finas.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    OutlinedTextField(value = b.observaciones, onValueChange = { b.observaciones = it.take(300) }, label = { Text("Observaciones") },
        minLines = 3, modifier = Modifier.fillMaxWidth())
}

// ---------------------------------------------------------------------------------- paso 3

@Composable
private fun PasoProcedimiento(b: Borrador) {
    val pasos = if (b.esPescado) PASOS_PEZ else PASOS_SED
    Text("Marca cada paso al terminarlo. ${b.pasosHechos.count { it < pasos.size }} de ${pasos.size} hechos.",
        style = MaterialTheme.typography.bodyLarge)
    Nota("Seguridad", "La demostración usa materiales inocuos. Los reactivos y residuos los define y aprueba el laboratorio aliado.")
    pasos.forEachIndexed { i, p ->
        val hecho = i in b.pasosHechos
        Card(
            onClick = { if (hecho) b.pasosHechos.remove(i) else b.pasosHechos.add(i) },
            colors = CardDefaults.cardColors(containerColor = if (hecho) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (hecho) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked, null,
                    tint = if (hecho) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("${i + 1}. ${p.texto}", style = MaterialTheme.typography.bodyLarge)
                    p.nota?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                p.segundos?.let { Temporizador(it, onTermina = { if (i !in b.pasosHechos) b.pasosHechos.add(i) }) }
            }
        }
    }
}

@Composable
private fun Temporizador(segundos: Int, onTermina: () -> Unit) {
    var restante by rememberSaveable { mutableIntStateOf(segundos) }
    var corriendo by rememberSaveable { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(corriendo) {
        while (corriendo && restante > 0) { delay(1000); restante-- }
        if (corriendo && restante == 0) { corriendo = false; haptic.performHapticFeedback(HapticFeedbackType.LongPress); onTermina() }
    }
    val texto = "%d:%02d".format(restante / 60, restante % 60)
    TextButton(
        onClick = { if (restante == 0) restante = segundos; corriendo = !corriendo },
        modifier = Modifier.heightIn(min = 48.dp),
    ) {
        Icon(if (corriendo) Icons.Outlined.Timer else Icons.Outlined.PlayArrow, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(if (restante == 0) "Listo" else texto, fontWeight = FontWeight.Bold)
    }
}

// ---------------------------------------------------------------------------------- paso 4

@Composable
private fun PasoLectura(vm: TecnicoViewModel) {
    val b = vm.borrador
    if (b.esPescado) {
        Nota("Cabezal PEZ", "Está en etapa de concepto (voltametría con electrodo de oro). La muestra de pescado queda " +
            "registrada con su cadena de custodia para que el laboratorio la analice.")
        return
    }
    Titulo("Controles de la tanda")
    Control("Blanco de procedimiento", b.blancoProcedimiento) { b.blancoProcedimiento = it }
    Control("Cloro libre = 0 (antes de leer)", b.cloroLibreCero) { b.cloroLibreCero = it }
    Control("Referencia blanca", b.referenciaBlanca) { b.referenciaBlanca = it }
    Control("pH", b.ph) { b.ph = it }
    Text("Si un control falla, el resultado es «Inválido» y hay que repetir.", style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)

    Titulo("Lectura con el lector Ahgora")
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        val ops = listOf("bluetooth" to "Bluetooth", "manual" to "Escribir", "demostracion" to "Demo")
        ops.forEachIndexed { i, (v, t) ->
            SegmentedButton(selected = b.origenLectura == v, onClick = { b.origenLectura = v },
                shape = SegmentedButtonDefaults.itemShape(i, ops.size), modifier = Modifier.heightIn(min = 48.dp)) { Text(t) }
        }
    }
    when (b.origenLectura) {
        "bluetooth" -> LecturaBluetooth(vm)
        "manual" -> {
            Text("Escribe los valores R, G, B y C que muestra el lector (promedio de 10).", style = MaterialTheme.typography.bodyMedium)
            LecturaManual("1. Tarjeta blanca", b.refBlanca) { b.refBlanca = it }
            LecturaManual("2. Casilla de Hg sin usar (tira nueva)", b.sinUsar) { b.sinUsar = it }
            LecturaManual("3. Casilla de Hg reaccionada", b.reaccionada) { b.reaccionada = it }
        }
        else -> {
            Text("Llena las tres lecturas con valores de ejemplo. El resultado queda rotulado como demostración.",
                style = MaterialTheme.typography.bodyMedium)
            Button(onClick = { vm.llenarLecturaDemostracion() }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text("Llenar con lectura de demostración")
            }
            ResumenLecturas(b.refBlanca, b.sinUsar, b.reaccionada)
        }
    }
}

@Composable
private fun LecturaBluetooth(vm: TecnicoViewModel) {
    val b = vm.borrador
    val lector = vm.lector
    val scope = rememberCoroutineScope()
    var permiso by remember { mutableStateOf(lector.tienePermiso()) }
    var equipos by remember { mutableStateOf(if (permiso) lector.emparejados() else emptyList()) }
    var elegido by rememberSaveable { mutableStateOf<String?>(null) }
    var leyendo by remember { mutableStateOf<String?>(null) }
    var progreso by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    val pedir = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        permiso = ok; if (ok) equipos = lector.emparejados()
    }
    if (elegido == null) elegido = equipos.firstOrNull()?.direccion

    when {
        !lector.disponible() -> { Text("Este teléfono no tiene Bluetooth. Usa «Escribir».", color = MaterialTheme.colorScheme.error); return }
        !permiso -> {
            Text("Para hablar con el lector hace falta el permiso de Bluetooth.", style = MaterialTheme.typography.bodyMedium)
            Button(onClick = { lector.permisoNecesario?.let { pedir.launch(it) } }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Dar permiso de Bluetooth")
            }
            return
        }
        !lector.encendido() -> { Text("Enciende el Bluetooth del teléfono.", color = MaterialTheme.colorScheme.error); }
    }
    if (equipos.isEmpty()) {
        Text("No hay lectores emparejados. Empareja el lector «AHG-SED-01» en los ajustes de Bluetooth del teléfono.",
            style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = { equipos = lector.emparejados() }) { Text("Buscar de nuevo") }
        return
    }
    Text("Lector:", style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        equipos.forEach { e ->
            FilterChip(selected = elegido == e.direccion, onClick = { elegido = e.direccion },
                leadingIcon = { Icon(Icons.Outlined.Bluetooth, null, Modifier.size(18.dp)) }, label = { Text(e.nombre) },
                modifier = Modifier.heightIn(min = 48.dp))
        }
    }
    fun leer(cual: String, guardar: (RgbcDto) -> Unit) {
        val dir = elegido ?: return
        scope.launch {
            leyendo = cual; progreso = 0; error = null
            runCatching { lector.leer(dir, n = 10) { progreso = it } }
                .onSuccess { (prom, ultima) ->
                    guardar(prom)
                    b.lector = LectorDto(ultima.nodeId, ultima.testMode, ultima.sensor)
                    if (cual == "blanca" && b.referenciaBlanca == "no_medido") b.referenciaBlanca = "ok"
                }
                .onFailure { error = (it as? LectorBluetooth.ErrorLector)?.message ?: "Error del lector: ${it.message}" }
            leyendo = null
        }
    }
    Text("Lee en este orden, sin mover la caja:", style = MaterialTheme.typography.bodyMedium)
    FilaLectura("1. Tarjeta blanca", b.refBlanca, leyendo == "blanca", progreso, leyendo == null) { leer("blanca") { b.refBlanca = it } }
    FilaLectura("2. Casilla de Hg sin usar", b.sinUsar, leyendo == "sin", progreso, leyendo == null) { leer("sin") { b.sinUsar = it } }
    FilaLectura("3. Casilla de Hg reaccionada", b.reaccionada, leyendo == "rea", progreso, leyendo == null) { leer("rea") { b.reaccionada = it } }
    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
}

@Composable
private fun FilaLectura(titulo: String, valor: RgbcDto?, leyendo: Boolean, progreso: Int, habilitado: Boolean, onLeer: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.titleSmall)
                Text(
                    when {
                        leyendo -> "Leyendo $progreso de 10…"
                        valor != null -> "R ${valor.r} · G ${valor.g} · B ${valor.b} · C ${valor.c}"
                        else -> "Sin leer"
                    },
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (leyendo) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            else Button(onClick = onLeer, enabled = habilitado, modifier = Modifier.heightIn(min = 44.dp)) { Text(if (valor == null) "Leer" else "Repetir") }
        }
    }
}

@Composable
private fun LecturaManual(titulo: String, valor: RgbcDto?, onCambio: (RgbcDto?) -> Unit) {
    var r by remember { mutableStateOf(valor?.r?.toString().orEmpty()) }
    var g by remember { mutableStateOf(valor?.g?.toString().orEmpty()) }
    var bl by remember { mutableStateOf(valor?.b?.toString().orEmpty()) }
    var c by remember { mutableStateOf(valor?.c?.toString().orEmpty()) }
    fun publicar() {
        val v = listOf(r, g, bl, c).map { it.toIntOrNull() }
        onCambio(if (v.all { it != null && it >= 0 }) RgbcDto(v[0]!!, v[1]!!, v[2]!!, v[3]!!) else null)
    }
    Text(titulo, style = MaterialTheme.typography.titleSmall)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        CampoNumero("R", r, Modifier.weight(1f), entero = true) { r = it; publicar() }
        CampoNumero("G", g, Modifier.weight(1f), entero = true) { g = it; publicar() }
        CampoNumero("B", bl, Modifier.weight(1f), entero = true) { bl = it; publicar() }
        CampoNumero("C", c, Modifier.weight(1f), entero = true) { c = it; publicar() }
    }
}

@Composable
private fun ResumenLecturas(ref: RgbcDto?, sin: RgbcDto?, rea: RgbcDto?) {
    listOf("Tarjeta blanca" to ref, "Hg sin usar" to sin, "Hg reaccionada" to rea).forEach { (t, v) ->
        Text("$t: " + (v?.let { "R ${it.r} · G ${it.g} · B ${it.b} · C ${it.c}" } ?: "—"), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun Control(nombre: String, valor: String, onCambio: (String) -> Unit) {
    Column {
        Text(nombre, style = MaterialTheme.typography.bodyLarge)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            val ops = listOf("ok" to "Bien", "falla" to "Falló", "no_medido" to "No medido")
            ops.forEachIndexed { i, (v, t) ->
                SegmentedButton(selected = valor == v, onClick = { onCambio(v) }, shape = SegmentedButtonDefaults.itemShape(i, ops.size),
                    modifier = Modifier.heightIn(min = 44.dp)) { Text(t) }
            }
        }
    }
}

// ---------------------------------------------------------------------------------- paso 5

@Composable
private fun PasoResultado(vm: TecnicoViewModel) {
    val b = vm.borrador
    val res = b.resultado()
    if (b.esPescado) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Registrada para laboratorio", style = MaterialTheme.typography.headlineSmall)
                Text("Sin lectura de campo (cabezal PEZ en concepto). Queda «sin clasificar» hasta el resultado del laboratorio.",
                    style = MaterialTheme.typography.bodyLarge)
            }
        }
    } else TarjetaSemaforo(res.semaforo, res.deltaE, res.motivoInvalido, grande = true)

    Text(Cribado.ETIQUETA, style = MaterialTheme.typography.labelLarge, color = Paleta.Ambar, fontWeight = FontWeight.Bold)
    if (!Cribado.CALIBRACION_VALIDADA) {
        Text("Prueba de demostración: el criterio es provisional (${Cribado.CALIBRACION}) hasta validarlo con el laboratorio aliado. " +
            "No se convierte a mg/kg.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Nota("Mensaje para la comunidad", Cribado.MENSAJE_COMUNIDAD)

    Titulo("Resumen")
    Dato("Código", b.codigo)
    Dato("Muestra", when (b.matriz) { "cola" -> "Cola minera"; "pescado" -> "Pescado" + (b.especie.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""); else -> "Sedimento" })
    Dato("Lugar", if (b.conGps) "%.5f, %.5f".format(b.lat, b.lon) else "Alcoche (aproximado)")
    Dato("Río", b.cuenca)
    Dato("Procedimiento", "${b.pasosHechos.size} de ${(if (b.esPescado) PASOS_PEZ else PASOS_SED).size} pasos")
    Dato("Foto", if (b.fotoRuta != null) "Sí" else "No")
    Text("Al guardar queda en el teléfono; se envía al Servidor Ahgora cuando haya señal. Publicar un aviso lo decide un responsable.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

// ---------------------------------------------------------------------------------- piezas comunes

@Composable
internal fun Titulo(texto: String) {
    Text(texto, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(top = 4.dp).semantics { heading() })
}

@Composable
internal fun Dato(etiqueta: String, valor: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.4f))
        Text(valor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.6f))
    }
}

@Composable
private fun OpcionGrande(texto: String, icono: ImageVector, elegida: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = if (elegida) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
        border = BorderStroke(if (elegida) 2.dp else 1.dp, if (elegida) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.heightIn(min = 88.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icono, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
            Text(texto, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun CampoNumero(etiqueta: String, valor: String, modifier: Modifier, entero: Boolean = false, onCambio: (String) -> Unit) {
    OutlinedTextField(
        value = valor,
        onValueChange = { t -> onCambio(t.filter { it.isDigit() || (!entero && (it == ',' || it == '.')) }.take(8)) },
        label = { Text(etiqueta, maxLines = 1) }, singleLine = true, modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = if (entero) KeyboardType.Number else KeyboardType.Decimal),
    )
}

/** Semáforo con texto + color + acción (nunca el color solo). */
@Composable
internal fun TarjetaSemaforo(s: bo.ahgora.comunidad.data.tecnico.Semaforo, deltaE: Double?, motivo: String?, grande: Boolean = false) {
    val (color, fondo) = coloresSemaforo(s)
    Card(colors = CardDefaults.cardColors(containerColor = fondo), border = BorderStroke(2.dp, color), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(if (grande) 28.dp else 20.dp).background(color, CircleShape))
                Spacer(Modifier.width(10.dp))
                Text(s.etiqueta, style = if (grande) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge, color = color)
            }
            deltaE?.let { Text("Cambio de color (ΔE): %.1f".format(it), style = MaterialTheme.typography.bodyLarge) }
            motivo?.let { Text(it, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold) }
            Text(s.accion, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

internal fun coloresSemaforo(s: bo.ahgora.comunidad.data.tecnico.Semaforo) = when (s) {
    bo.ahgora.comunidad.data.tecnico.Semaforo.ROJO -> androidx.compose.ui.graphics.Color(0xFFB3261E) to androidx.compose.ui.graphics.Color(0xFFFBE3E1)
    bo.ahgora.comunidad.data.tecnico.Semaforo.AMARILLO -> androidx.compose.ui.graphics.Color(0xFF8A5A00) to androidx.compose.ui.graphics.Color(0xFFFFF1CC)
    bo.ahgora.comunidad.data.tecnico.Semaforo.VERDE -> androidx.compose.ui.graphics.Color(0xFF1E6B3A) to androidx.compose.ui.graphics.Color(0xFFE1F2E6)
    bo.ahgora.comunidad.data.tecnico.Semaforo.INVALIDO -> androidx.compose.ui.graphics.Color(0xFF4F5B62) to androidx.compose.ui.graphics.Color(0xFFECEFF1)
}

/** Lee una foto reducida para no gastar memoria en teléfonos modestos. */
internal fun decodificar(ruta: String, maxLado: Int): android.graphics.Bitmap? = runCatching {
    val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(ruta, o)
    var muestreo = 1
    while (maxOf(o.outWidth, o.outHeight) / (muestreo * 2) >= maxLado) muestreo *= 2
    BitmapFactory.decodeFile(ruta, BitmapFactory.Options().apply { inSampleSize = muestreo })
}.getOrNull()
