package bo.ahgora.comunidad.ui.pantallas

import android.graphics.Color as AColor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bo.ahgora.comunidad.BuildConfig
import bo.ahgora.comunidad.data.local.AvisoEntity
import bo.ahgora.comunidad.data.zona.Celda
import bo.ahgora.comunidad.data.zona.Localidades
import bo.ahgora.comunidad.ui.AhgoraViewModel
import bo.ahgora.comunidad.ui.Formato
import bo.ahgora.comunidad.ui.Presentacion
import bo.ahgora.comunidad.ui.componentes.AvisoTarjeta
import bo.ahgora.comunidad.ui.componentes.EstadoChip
import bo.ahgora.comunidad.ui.componentes.TituloSeccion
import bo.ahgora.comunidad.ui.theme.Paleta
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polygon
import java.io.File

private enum class ModoMapa { ESQUEMA, FONDO, LISTA }

/** Mapa (guía, secciones 04 y 06): vista generalizada por zona y opción equivalente en lista. */
@Composable
fun PantallaMapa(vm: AhgoraViewModel, onAbrirAviso: (String) -> Unit) {
    val ajustes by vm.ajustes.collectAsStateWithLifecycle()
    val avisos by vm.avisos.collectAsStateWithLifecycle()
    val enLinea by vm.enLinea.collectAsStateWithLifecycle()
    val mia = ajustes.celda ?: return
    var modo by rememberSaveable { mutableStateOf(ModoMapa.ESQUEMA) }
    var radio by rememberSaveable { mutableStateOf(3) }
    var seleccion by remember { mutableStateOf<Celda?>(mia) }

    val vigentes = avisos.orEmpty().filter { !it.retirado }
    val porCelda = vigentes.groupBy { it.celda }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            val opciones = listOf(ModoMapa.ESQUEMA to "Esquema", ModoMapa.FONDO to "Con fondo", ModoMapa.LISTA to "Lista")
            opciones.forEachIndexed { i, (m, txt) ->
                SegmentedButton(
                    selected = modo == m, onClick = { modo = m },
                    shape = SegmentedButtonDefaults.itemShape(i, opciones.size),
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text(txt) }
            }
        }

        when (modo) {
            ModoMapa.ESQUEMA -> {
                Text(
                    "Cada cuadro es una zona de unos 11 km. Funciona sin internet. Toca un cuadro para ver sus avisos.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf(3 to "Cerca", 7 to "Más amplio").forEachIndexed { i, (r, txt) ->
                        SegmentedButton(
                            selected = radio == r, onClick = { radio = r },
                            shape = SegmentedButtonDefaults.itemShape(i, 2), modifier = Modifier.heightIn(min = 48.dp),
                        ) { Text(txt) }
                    }
                }
                EsquemaZonas(mia, radio, porCelda, seleccion, onTocar = { seleccion = it })
                Leyenda()
            }
            ModoMapa.FONDO -> {
                if (!enLinea) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CloudOff, null, tint = Paleta.Ambar)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Sin conexión: el fondo solo muestra lo que ya se cargó antes. El esquema y la lista funcionan igual.",
                            style = MaterialTheme.typography.bodyMedium, color = Paleta.Ambar,
                        )
                    }
                }
                MapaConFondo(mia, porCelda, onTocar = { seleccion = it })
                Text(
                    "Mapa base © colaboradores de OpenStreetMap.",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Leyenda()
            }
            ModoMapa.LISTA -> ListaZonas(mia, ajustes.nombreZona, porCelda, onElegir = { seleccion = it })
        }

        seleccion?.let { c ->
            val enCelda = porCelda[c].orEmpty()
            TituloSeccion(
                (if (c == mia) "Tu zona · ${ajustes.nombreZona}" else Formato.relacion(c, mia)) + " (${enCelda.size})"
            )
            if (enCelda.isEmpty()) Text("No hay avisos vigentes publicados para esta zona.", style = MaterialTheme.typography.bodyMedium)
            enCelda.forEach { a -> AvisoTarjeta(a, mia, onClick = { onAbrirAviso(a.id) }) }
        }
    }
}

private fun peor(avisos: List<AvisoEntity>?): Presentacion? =
    avisos?.map { Presentacion.de(it) }?.maxByOrNull { it.prioridad }

@Composable
private fun EsquemaZonas(
    mia: Celda,
    radio: Int,
    porCelda: Map<Celda, List<AvisoEntity>>,
    seleccion: Celda?,
    onTocar: (Celda) -> Unit,
) {
    val medidor = rememberTextMeasurer()
    val n = 2 * radio + 1
    val latMax = (mia.latIdx + radio + 1) * Celda.PASO
    val lonMin = (mia.lonIdx - radio) * Celda.PASO
    val span = n * Celda.PASO
    val locales = remember(mia, radio) {
        Localidades.todas.filter { it.celda.distancia(mia) <= radio }
    }
    val conAvisos = porCelda.keys.count { it.distancia(mia) <= radio }
    val textoAcc = "Esquema de zonas alrededor de tu zona. $conAvisos zona(s) con avisos vigentes en este recuadro. " +
        "La lista equivalente está en la pestaña Lista."
    val colorTexto = MaterialTheme.colorScheme.onSurface
    val colorGrilla = MaterialTheme.colorScheme.outlineVariant
    val colorFondo = MaterialTheme.colorScheme.surface

    Card(
        colors = CardDefaults.cardColors(containerColor = colorFondo),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Canvas(
            Modifier.fillMaxWidth().aspectRatio(1f).padding(8.dp)
                .semantics { contentDescription = textoAcc }
                .pointerInput(mia, radio) {
                    detectTapGestures { o ->
                        val lado = size.width.toFloat() / n
                        val col = (o.x / lado).toInt().coerceIn(0, n - 1)
                        val fila = (o.y / lado).toInt().coerceIn(0, n - 1)
                        onTocar(Celda(mia.latIdx + radio - fila, mia.lonIdx - radio + col))
                    }
                },
        ) {
            val lado = size.width / n
            for (fila in 0 until n) for (col in 0 until n) {
                val celda = Celda(mia.latIdx + radio - fila, mia.lonIdx - radio + col)
                val tl = Offset(col * lado, fila * lado)
                val p = peor(porCelda[celda])
                if (p != null) {
                    drawRect(p.fondo, tl, Size(lado, lado))
                    drawRect(p.color, tl, Size(lado, lado), style = Stroke(2f))
                    val cantidad = porCelda[celda]!!.size
                    val t = medidor.measure(
                        cantidad.toString(),
                        TextStyle(color = p.color, fontSize = if (radio > 4) 11.sp else 16.sp, fontWeight = FontWeight.Bold),
                    )
                    drawText(t, topLeft = Offset(tl.x + (lado - t.size.width) / 2, tl.y + (lado - t.size.height) / 2))
                } else {
                    drawRect(colorGrilla, tl, Size(lado, lado), style = Stroke(1f))
                }
                if (celda == seleccion) drawRect(Paleta.Verde, tl + Offset(3f, 3f), Size(lado - 6f, lado - 6f), style = Stroke(4f))
                if (celda == mia) {
                    drawRect(Paleta.Petroleo, tl, Size(lado, lado), style = Stroke(6f))
                    val t = medidor.measure("Tú", TextStyle(color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold))
                    drawRect(Paleta.Petroleo, tl, Size(t.size.width + 8f, t.size.height + 4f))
                    drawText(t, topLeft = tl + Offset(4f, 2f))
                }
            }
            // Localidades de referencia
            for (loc in locales) {
                val x = ((loc.lon - lonMin) / span * size.width).toFloat()
                val y = ((latMax - loc.lat) / span * size.height).toFloat()
                drawCircle(Paleta.Petroleo, radius = 5f, center = Offset(x, y))
                val t = medidor.measure(loc.nombre, TextStyle(color = colorTexto, fontSize = 10.sp))
                drawText(t, topLeft = Offset((x + 6f).coerceAtMost(size.width - t.size.width), y - t.size.height - 2f))
            }
            // Norte y escala
            val norte = medidor.measure("N ↑", TextStyle(color = Paleta.Petroleo, fontSize = 12.sp, fontWeight = FontWeight.Bold))
            drawText(norte, topLeft = Offset(size.width - norte.size.width - 4f, 4f))
            val escala = medidor.measure("1 cuadro ≈ 11 km", TextStyle(color = colorTexto, fontSize = 10.sp))
            drawRect(colorFondo.copy(alpha = 0.85f), Offset(2f, size.height - escala.size.height - 6f), Size(escala.size.width + 8f, escala.size.height + 4f))
            drawText(escala, topLeft = Offset(6f, size.height - escala.size.height - 4f))
        }
    }
}

@Composable
private fun Leyenda() {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(Presentacion.PENDIENTE, Presentacion.EN_ANALISIS, Presentacion.CONFIRMADO, Presentacion.CONFIRMADO_INDICACIONES)
            .forEach { EstadoChip(it) }
    }
    Text(
        "El número indica cuántos avisos vigentes tiene la zona. El borde oscuro marca tu zona.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ListaZonas(mia: Celda, nombreMio: String?, porCelda: Map<Celda, List<AvisoEntity>>, onElegir: (Celda) -> Unit) {
    val zonas = porCelda.entries.sortedBy { it.key.distancia(mia) }
    Text("Zonas con avisos vigentes, de la más cercana a la más lejana.", style = MaterialTheme.typography.bodyMedium)
    if (zonas.isEmpty()) Text("No hay avisos vigentes guardados.", style = MaterialTheme.typography.bodyLarge)
    zonas.forEach { (celda, lista) ->
        val p = peor(lista)!!
        Card(
            onClick = { onElegir(celda) },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    if (celda == mia) "Tu zona · ${nombreMio.orEmpty()}" else Formato.relacion(celda, mia),
                    style = MaterialTheme.typography.titleSmall,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EstadoChip(p)
                    Spacer(Modifier.width(8.dp))
                    Text("${lista.size} aviso(s)", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

/** Mapa OpenStreetMap con las zonas dibujadas. Las teselas vistas quedan en caché para usarse sin señal. */
@Composable
private fun MapaConFondo(mia: Celda, porCelda: Map<Celda, List<AvisoEntity>>, onTocar: (Celda) -> Unit) {
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
            setMultiTouchControls(true)
            controller.setZoom(10.0)
            controller.setCenter(GeoPoint(mia.latCentro, mia.lonCentro))
        }
    }
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_RESUME -> mapa.onResume()
                Lifecycle.Event.ON_PAUSE -> mapa.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs); mapa.onDetach() }
    }
    Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
        AndroidView(
            factory = { mapa },
            update = { mv ->
                mv.overlays.clear()
                val celdas = porCelda.keys + mia
                for (c in celdas) {
                    val p = peor(porCelda[c])
                    val poly = Polygon(mv).apply {
                        points = listOf(
                            GeoPoint(c.latMin, c.lonMin), GeoPoint(c.latMin, c.lonMax),
                            GeoPoint(c.latMax, c.lonMax), GeoPoint(c.latMax, c.lonMin), GeoPoint(c.latMin, c.lonMin),
                        )
                        fillPaint.color = (p?.fondo ?: Color.Transparent).copy(alpha = 0.55f).toArgb()
                        outlinePaint.color = if (c == mia) Paleta.Petroleo.toArgb() else (p?.color ?: Color.Gray).toArgb()
                        outlinePaint.strokeWidth = if (c == mia) 8f else 4f
                        title = if (c == mia) "Tu zona" else Formato.relacion(c, mia)
                        setOnClickListener { _, _, _ -> onTocar(c); true }
                    }
                    mv.overlays.add(poly)
                }
                mv.setBackgroundColor(AColor.WHITE)
                mv.invalidate()
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
