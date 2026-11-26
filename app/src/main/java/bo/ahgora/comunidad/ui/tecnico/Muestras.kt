package bo.ahgora.comunidad.ui.tecnico

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bo.ahgora.comunidad.BuildConfig
import bo.ahgora.comunidad.data.remote.RgbcDto
import bo.ahgora.comunidad.data.tecnico.Cribado
import bo.ahgora.comunidad.data.tecnico.MuestraEntity
import bo.ahgora.comunidad.data.tecnico.Semaforo
import bo.ahgora.comunidad.data.zona.Localidades
import bo.ahgora.comunidad.ui.Formato
import bo.ahgora.comunidad.ui.componentes.etiquetaMapa
import bo.ahgora.comunidad.ui.theme.Paleta
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.CustomZoomButtonsDisplay
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File

private fun nombreMatriz(m: String) = when (m) { "cola" -> "Cola minera"; "pescado" -> "Pescado"; else -> "Sedimento" }

// ---------------------------------------------------------------------------------- lista

@Composable
fun PantallaMuestras(vm: TecnicoViewModel, onAbrir: (String) -> Unit) {
    val muestras by vm.muestras.collectAsStateWithLifecycle()
    val sesion by vm.sesion.collectAsStateWithLifecycle()
    val enviando by vm.enviando.collectAsStateWithLifecycle()
    val enLinea by vm.enLinea.collectAsStateWithLifecycle()
    val pendientes = muestras.count { it.pendienteSync && !it.soloLocal }

    LazyColumn(contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()) {
        item {
            if (sesion?.demostracion == true) {
                Surface(color = Paleta.AmbarFondo, contentColor = Paleta.Ambar, shape = RoundedCornerShape(12.dp)) {
                    Text("Modo demostración: las muestras quedan solo en este teléfono.", fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp).fillMaxWidth())
                }
            } else EstadoEnvio(pendientes, enviando, enLinea, onEnviar = { vm.enviar() })
        }
        if (muestras.isEmpty()) item {
            Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Todavía no hay muestras.", style = MaterialTheme.typography.titleMedium)
                Text("Toca «Nueva muestra» para registrar la primera.", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(muestras, key = { it.codigo }) { m -> TarjetaMuestra(m, onClick = { onAbrir(m.codigo) }) }
    }
}

@Composable
private fun EstadoEnvio(pendientes: Int, enviando: Boolean, enLinea: Boolean, onEnviar: () -> Unit) {
    val hayPendientes = pendientes > 0
    Surface(
        color = if (hayPendientes) Paleta.AmbarFondo else MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 12.dp, end = 6.dp).heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
            if (enviando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Icon(if (hayPendientes) Icons.Outlined.CloudUpload else Icons.Outlined.CloudDone, null)
            Spacer(Modifier.width(10.dp))
            Text(
                when {
                    enviando -> "Enviando al servidor…"
                    !hayPendientes -> "Todo enviado al servidor."
                    !enLinea -> "$pendientes sin enviar · se enviarán con señal"
                    else -> "$pendientes sin enviar"
                },
                style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f),
            )
            if (hayPendientes && enLinea && !enviando) TextButton(onClick = onEnviar, modifier = Modifier.heightIn(min = 48.dp)) { Text("Enviar") }
        }
    }
}

@Composable
private fun TarjetaMuestra(m: MuestraEntity, onClick: () -> Unit) {
    val s = Semaforo.deServidor(m.semaforo)
    val (color, fondo) = coloresSemaforo(s)
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(14.dp).background(color, CircleShape))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(m.codigo, style = MaterialTheme.typography.titleMedium)
                Text("${nombreMatriz(m.matriz)} · ${Formato.fechaCorta(m.fechaMs)}", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = fondo, contentColor = color, shape = RoundedCornerShape(50)) {
                        Text(if (m.matriz == "pescado" && s == Semaforo.INVALIDO) "Sin clasificar" else s.etiqueta,
                            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                    if (m.certeza == "en_verificacion") {
                        Spacer(Modifier.width(6.dp))
                        Text("En laboratorio", style = MaterialTheme.typography.labelMedium, color = Color_EnLab)
                    }
                }
            }
            EstadoSync(m)
        }
    }
}

private val Color_EnLab = androidx.compose.ui.graphics.Color(0xFF1D5B99)

@Composable
private fun EstadoSync(m: MuestraEntity) {
    val (icono, texto) = when {
        m.soloLocal -> Icons.Outlined.PhoneAndroid to "Solo teléfono"
        m.pendienteSync -> Icons.Outlined.CloudUpload to "Sin enviar"
        else -> Icons.Outlined.CloudDone to "Enviada"
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icono, null, tint = if (m.pendienteSync && !m.soloLocal) Paleta.Ambar else MaterialTheme.colorScheme.outline)
        Text(texto, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ---------------------------------------------------------------------------------- detalle

@Composable
fun PantallaDetalleMuestra(vm: TecnicoViewModel, codigo: String, onBorrada: () -> Unit) {
    val flujo = remember(codigo) { vm.muestra(codigo) }
    val e by flujo.collectAsStateWithLifecycle(initialValue = null)
    val ent = e ?: run {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Muestra no encontrada.") }
        return
    }
    val m = remember(ent.registroJson) { vm.registro(ent) }
    val s = Semaforo.deServidor(m.semaforo)
    var dialogoLab by remember { mutableStateOf(false) }
    var dialogoBorrar by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(m.codigo, style = MaterialTheme.typography.headlineSmall)
        if (m.cabezal == "PEZ" && s == Semaforo.INVALIDO) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
                Text("Sin clasificar: muestra de pescado para el laboratorio.", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
            }
        } else TarjetaSemaforo(s, m.lecturas?.deltaE, m.motivoInvalido)
        Text(Cribado.ETIQUETA + if (m.demostracion) " · DEMOSTRACIÓN" else "", style = MaterialTheme.typography.labelLarge,
            color = Paleta.Ambar, fontWeight = FontWeight.Bold)
        Text(
            when {
                ent.soloLocal -> "Guardada solo en este teléfono (sesión de demostración)."
                ent.pendienteSync -> "Sin enviar al servidor todavía."
                else -> "Enviada al servidor el ${Formato.fechaHora(ent.sincronizadoEn ?: 0)}."
            },
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Bloque("Identificación") {
            Dato("Muestra", nombreMatriz(m.matriz) + (m.muestreo?.especie?.let { " · $it" } ?: ""))
            Dato("Fecha", Formato.fechaHora(ent.fechaMs))
            Dato("Responsable", m.responsable)
            Dato("Lugar", if (m.lat != null && m.lon != null) "%.5f, %.5f".format(m.lat, m.lon) + (m.precisionM?.let { " ±%.0f m".format(it) } ?: "") else "—")
            m.localidad?.let { Dato("Localidad", it) }
            m.cuenca?.let { Dato("Río", it) }
        }
        m.muestreo?.let { mu ->
            Bloque("Muestreo") {
                mu.porciones?.let { Dato("Porciones", "$it") }
                mu.profundidadCm?.let { Dato("Profundidad", "$it cm") }
                mu.finoPct?.let { Dato("Material fino", "$it %") }
                mu.tallaCm?.let { Dato("Talla", "$it cm") }
                mu.pesoG?.let { Dato("Peso", "$it g") }
                if (mu.pasosTotal != null) Dato("Procedimiento", "${mu.pasosHechos ?: 0} de ${mu.pasosTotal} pasos")
                mu.observaciones?.let { Dato("Observaciones", it) }
            }
        }
        m.lecturas?.let { l ->
            Bloque("Lectura del lector") {
                FilaRgbc("Tarjeta blanca", l.referenciaBlanca)
                FilaRgbc("Hg sin usar", l.casillaSinUsar)
                FilaRgbc("Hg reaccionada", l.casillaReaccionada)
                Dato("Origen", when (l.origen) { "bluetooth" -> "Lector por Bluetooth"; "manual" -> "Escrita a mano"; else -> "Demostración" })
                m.lector?.nodeId?.let { Dato("Lector", it) }
                m.calibracion?.let { Dato("Calibración", it) }
            }
        }
        m.controles?.let { c ->
            Bloque("Controles") {
                fun t(v: String) = when (v) { "ok" -> "Bien"; "falla" -> "Falló"; else -> "No medido" }
                Dato("Blanco de procedimiento", t(c.blancoProcedimiento))
                Dato("Cloro libre = 0", t(c.cloroLibreCero))
                Dato("Referencia blanca", t(c.referenciaBlanca))
                Dato("pH", t(c.ph))
            }
        }
        ent.fotoRuta?.let { ruta ->
            val bmp = remember(ruta) { if (File(ruta).exists()) decodificar(ruta, 900) else null }
            if (bmp != null) Bloque("Foto del sitio") {
                Image(bmp.asImageBitmap(), "Foto del sitio", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(200.dp))
            }
        }
        Bloque("Cadena de custodia") {
            m.custodia.forEach { ev ->
                Row(Modifier.padding(vertical = 3.dp)) {
                    Box(Modifier.padding(top = 6.dp).size(10.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(when (ev.evento) { "registrada" -> "Registrada en campo"; "enviada_laboratorio" -> "Enviada al laboratorio"; else -> ev.evento },
                            style = MaterialTheme.typography.titleSmall)
                        Text("${ev.fecha.replace('T', ' ').take(16)} · ${ev.responsable}", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        ev.detalle?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }

        if (m.certeza == "cribado") {
            Button(onClick = { dialogoLab = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Icon(Icons.Outlined.LocalShipping, null); Spacer(Modifier.width(8.dp)); Text("Registrar envío al laboratorio")
            }
        } else m.laboratorio?.let { Text("Enviada a ${it.laboratorio} el ${it.enviadaEn.replace('T', ' ').take(16)}.", style = MaterialTheme.typography.bodyLarge) }
        if (ent.sincronizadoEn == null) {
            OutlinedButton(onClick = { dialogoBorrar = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Icon(Icons.Outlined.Delete, null); Spacer(Modifier.width(8.dp)); Text("Borrar (todavía no se envió)")
            }
        }
    }

    if (dialogoLab) {
        var lab by remember { mutableStateOf("Laboratorio aliado") }
        AlertDialog(
            onDismissRequest = { dialogoLab = false },
            title = { Text("Envío al laboratorio") },
            text = {
                Column {
                    Text("Queda en la cadena de custodia con fecha y responsable. La muestra pasa a «en verificación».")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(lab, { lab = it.take(60) }, label = { Text("Laboratorio") }, singleLine = true)
                }
            },
            confirmButton = { TextButton(onClick = { vm.marcarEnviadaLaboratorio(codigo, lab); dialogoLab = false }) { Text("Registrar") } },
            dismissButton = { TextButton(onClick = { dialogoLab = false }) { Text("Cancelar") } },
        )
    }
    if (dialogoBorrar) {
        AlertDialog(
            onDismissRequest = { dialogoBorrar = false },
            title = { Text("¿Borrar ${m.codigo}?") },
            text = { Text("Se borra de este teléfono. No se puede deshacer.") },
            confirmButton = { TextButton(onClick = { vm.borrar(codigo); dialogoBorrar = false; onBorrada() }) { Text("Borrar") } },
            dismissButton = { TextButton(onClick = { dialogoBorrar = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun Bloque(titulo: String, contenido: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            contenido()
        }
    }
}

@Composable
private fun FilaRgbc(t: String, v: RgbcDto?) = Dato(t, v?.let { "R ${it.r} · G ${it.g} · B ${it.b} · C ${it.c}" } ?: "—")

// ---------------------------------------------------------------------------------- mapa de muestras

@Composable
fun PantallaMapaMuestras(vm: TecnicoViewModel, onAbrir: (String) -> Unit) {
    val muestras by vm.muestras.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapa = remember {
        Configuration.getInstance().apply {
            val base = File(context.cacheDir, "osmdroid")
            osmdroidBasePath = base
            osmdroidTileCache = File(base, "tiles")
            userAgentValue = "AhgoraComunidad/${BuildConfig.VERSION_NAME}"
        }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            isTilesScaledToDpi = true
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.ALWAYS)
            zoomController.display.setPositions(false, CustomZoomButtonsDisplay.HorizontalPosition.RIGHT, CustomZoomButtonsDisplay.VerticalPosition.BOTTOM)
            controller.setZoom(12.5)
            controller.setCenter(GeoPoint(Localidades.ALCOCHE.lat, Localidades.ALCOCHE.lon))
        }
    }
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, ev ->
            if (ev == Lifecycle.Event.ON_RESUME) mapa.onResume() else if (ev == Lifecycle.Event.ON_PAUSE) mapa.onPause()
        }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs); mapa.onDetach() }
    }
    Column(Modifier.fillMaxSize()) {
        Text("Muestras registradas (${muestras.count { it.lat != null }}). Toca una para ver su ficha.",
            style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
        Box(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
            AndroidView(factory = { mapa }, modifier = Modifier.fillMaxSize(), update = { mv ->
                mv.overlays.clear()
                mv.overlays.add(Marker(mv).apply {
                    position = GeoPoint(Localidades.ALCOCHE.lat, Localidades.ALCOCHE.lon)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_TOP)
                    icon = etiquetaMapa(mv.context, "Alcoche", Paleta.Petroleo.toArgb(), android.graphics.Color.WHITE, conPunta = false)
                    infoWindow = null
                })
                muestras.filter { it.lat != null && it.lon != null }.forEach { m ->
                    val color = coloresSemaforo(Semaforo.deServidor(m.semaforo)).first
                    mv.overlays.add(Marker(mv).apply {
                        position = GeoPoint(m.lat!!, m.lon!!)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        icon = etiquetaMapa(mv.context, m.codigo.substringAfter('-'), color.toArgb(), android.graphics.Color.WHITE)
                        infoWindow = null
                        setOnMarkerClickListener { _, _ -> onAbrir(m.codigo); true }
                    })
                }
                mv.invalidate()
            })
            Text("© OpenStreetMap", style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.BottomStart).padding(4.dp)
                    .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp))
        }
    }
}
