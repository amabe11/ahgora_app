package bo.ahgora.comunidad.ui.pantallas

import android.content.Context
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.SetMeal
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bo.ahgora.comunidad.BuildConfig
import bo.ahgora.comunidad.data.local.AvisoEntity
import bo.ahgora.comunidad.data.mapa.ContextoAlcoche
import bo.ahgora.comunidad.data.mapa.LadoEtiqueta
import bo.ahgora.comunidad.data.mapa.LugarMinero
import bo.ahgora.comunidad.data.mapa.NivelPez
import bo.ahgora.comunidad.data.mapa.NivelPosible
import bo.ahgora.comunidad.data.mapa.Pez
import bo.ahgora.comunidad.data.mapa.TramoRio
import bo.ahgora.comunidad.data.prefs.ZONA_POR_DEFECTO
import bo.ahgora.comunidad.data.zona.Celda
import bo.ahgora.comunidad.data.zona.Localidades
import bo.ahgora.comunidad.ui.AhgoraViewModel
import bo.ahgora.comunidad.ui.Presentacion
import bo.ahgora.comunidad.ui.componentes.AvisoTarjeta
import bo.ahgora.comunidad.ui.theme.Paleta
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.CustomZoomButtonsDisplay
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import java.io.File

/** Las dos vistas del mapa: dónde puede haber mercurio y qué peces viven en los ríos. */
private enum class Capa(val titulo: String) { MERCURIO("Mercurio"), PECES("Peces") }

/** Lo que la persona tocó en el mapa. */
private sealed interface Seleccion {
    data class Lugar(val lugar: LugarMinero) : Seleccion
    data class Tramo(val tramo: TramoRio) : Seleccion
    data class Avisos(val celda: Celda) : Seleccion
    data object MiComunidad : Seleccion
}

// Símbolos para las etiquetas del mapa (se arman por código para no depender de la codificación del archivo).
private val PEZ = String(Character.toChars(0x1F41F))
private val CASA = String(Character.toChars(0x1F3E0))
private val ALERTA = String(Character.toChars(0x26A0))
private val CAMPANA = String(Character.toChars(0x1F514))

// Colores del mapa: siempre acompañados de texto (accesibilidad).
private object ColoresMapa {
    val Alto = Color(0xFFB3261E)
    val AltoFondo = Color(0xFFFBE3E1)
    val Medio = Color(0xFFC25E00)
    val MedioFondo = Color(0xFFFFEBD6)
    val Menos = Color(0xFF1E6B3A)
    val MenosFondo = Color(0xFFE1F2E6)
    val Mas = Color(0xFF9A6700)
    val MasFondo = Color(0xFFFFF1CC)
    val Rio = Color(0xFF1D5B99)

    fun de(n: NivelPosible) = if (n == NivelPosible.ALTO) Alto else Medio
    fun fondo(n: NivelPosible) = if (n == NivelPosible.ALTO) AltoFondo else MedioFondo
    fun de(n: NivelPez) = when (n) { NivelPez.MENOS -> Menos; NivelPez.MAS -> Mas; NivelPez.MUCHO -> Alto }
    fun fondo(n: NivelPez) = when (n) { NivelPez.MENOS -> MenosFondo; NivelPez.MAS -> MasFondo; NivelPez.MUCHO -> AltoFondo }
}

/**
 * Mapa de la localidad (guía, secciones 04 y 06) dividido en dos:
 *  1. «Mercurio»: lugares con minería de oro reportada, donde es más probable el mercurio, y las zonas
 *     con avisos aprobados de Ahgora.
 *  2. «Peces»: qué peces se pescan en cada tramo de río y cuánto mercurio suelen acumular.
 * Cada vista tiene su lista equivalente, que funciona sin internet.
 */
@Composable
fun PantallaMapa(vm: AhgoraViewModel, onAbrirAviso: (String) -> Unit) {
    val ajustes by vm.ajustes.collectAsStateWithLifecycle()
    val avisos by vm.avisos.collectAsStateWithLifecycle()
    val enLinea by vm.enLinea.collectAsStateWithLifecycle()
    val mia = ajustes.celda ?: return
    var capa by rememberSaveable { mutableStateOf(Capa.MERCURIO) }
    var verLista by rememberSaveable { mutableStateOf(false) }
    var seleccion by remember { mutableStateOf<Seleccion?>(null) }
    val porCelda = avisos.orEmpty().filter { !it.retirado }.groupBy { it.celda }
    val nombreMio = ajustes.nombreZona ?: "Mi comunidad"

    Column(Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = capa.ordinal) {
            Capa.entries.forEach { c ->
                Tab(
                    selected = capa == c,
                    onClick = { capa = c; seleccion = null },
                    modifier = Modifier.heightIn(min = 56.dp),
                    text = { Text(c.titulo, style = MaterialTheme.typography.titleSmall) },
                    icon = { Icon(if (c == Capa.MERCURIO) Icons.Outlined.Warning else Icons.Outlined.SetMeal, null) },
                )
            }
        }
        Text(
            if (capa == Capa.MERCURIO) "Dónde puede haber mercurio: lugares con minería de oro."
            else "Peces de los ríos y cuánto mercurio suelen tener.",
            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        if (!enLinea && !verLista) {
            Row(Modifier.padding(horizontal = 16.dp).padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CloudOff, null, tint = Paleta.Ambar, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "Sin señal: el fondo del mapa puede no verse. Toca «Lista».",
                    style = MaterialTheme.typography.bodySmall, color = Paleta.Ambar,
                )
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (verLista) {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).padding(top = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (capa == Capa.MERCURIO) ListaMercurio(mia, nombreMio, porCelda, onAbrirAviso)
                    else ListaPeces()
                }
            } else {
                MapaOsm(
                    capa = capa, mia = mia, nombreMio = nombreMio, porCelda = porCelda,
                    onTocar = { seleccion = it },
                    modifier = Modifier.fillMaxSize().semantics {
                        contentDescription = if (capa == Capa.MERCURIO)
                            "Mapa de lugares con minería de oro cerca de $nombreMio. La misma información está en Lista."
                        else "Mapa de peces de los ríos cerca de $nombreMio. La misma información está en Lista."
                    },
                )
            }
            SmallFloatingActionButton(
                onClick = { verLista = !verLista; seleccion = null },
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
            ) {
                Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (verLista) Icons.Outlined.Map else Icons.AutoMirrored.Outlined.List, null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (verLista) "Mapa" else "Lista", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (!verLista) {
            PanelInferior(capa, seleccion, mia, nombreMio, porCelda, onAbrirAviso, onCerrar = { seleccion = null })
        }
    }
}

// ---------------------------------------------------------------------------------- panel inferior

@Composable
private fun PanelInferior(
    capa: Capa,
    seleccion: Seleccion?,
    mia: Celda,
    nombreMio: String,
    porCelda: Map<Celda, List<AvisoEntity>>,
    onAbrirAviso: (String) -> Unit,
    onCerrar: () -> Unit,
) {
    Surface(tonalElevation = 3.dp, shadowElevation = 6.dp, modifier = Modifier.fillMaxWidth()) {
      Box {
        Column(
            Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState()).padding(16.dp)
                .padding(end = if (seleccion != null) 40.dp else 0.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (seleccion) {
                null -> if (capa == Capa.MERCURIO) LeyendaMercurio() else LeyendaPeces()
                is Seleccion.Lugar -> FichaLugar(seleccion.lugar)
                is Seleccion.Tramo -> FichaTramo(seleccion.tramo)
                is Seleccion.Avisos -> {
                    val lista = porCelda[seleccion.celda].orEmpty()
                    Text(
                        if (seleccion.celda == mia) "Avisos en $nombreMio" else "Avisos de esta zona",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (lista.isEmpty()) Text("No hay avisos vigentes aquí.", style = MaterialTheme.typography.bodyLarge)
                    lista.forEach { a -> AvisoTarjeta(a, mia, onClick = { onAbrirAviso(a.id) }) }
                }
                Seleccion.MiComunidad -> {
                    Text(nombreMio, style = MaterialTheme.typography.titleLarge)
                    Text(
                        if (capa == Capa.MERCURIO) "Tu comunidad. Los círculos de colores muestran dónde hay minería de oro."
                        else "Tu comunidad. Toca un río para ver qué peces viven allí.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    porCelda[mia].orEmpty().forEach { a -> AvisoTarjeta(a, mia, onClick = { onAbrirAviso(a.id) }) }
                }
            }
        }
        if (seleccion != null) {
            IconButton(onClick = onCerrar, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                Icon(Icons.Outlined.Close, contentDescription = "Cerrar")
            }
        }
      }
    }
}

@Composable
private fun LeyendaMercurio() {
    Text("Toca un círculo del mapa para saber más.", style = MaterialTheme.typography.bodyLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Pastilla(NivelPosible.ALTO.etiqueta, ColoresMapa.Alto, ColoresMapa.AltoFondo)
        Pastilla(NivelPosible.MEDIO.etiqueta, ColoresMapa.Medio, ColoresMapa.MedioFondo)
        Pastilla("Aviso de Ahgora", Presentacion.PENDIENTE.color, Presentacion.PENDIENTE.fondo)
    }
    Text(
        "Es más probable encontrar mercurio donde hay minería de oro y río abajo. No es una medición: solo el laboratorio lo confirma.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun LeyendaPeces() {
    Text("Toca un río del mapa para ver sus peces.", style = MaterialTheme.typography.bodyLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        NivelPez.entries.forEach { Pastilla(it.etiqueta, ColoresMapa.de(it), ColoresMapa.fondo(it)) }
    }
    Text("Comer pescado es bueno. Elige más los peces verdes.", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun FichaLugar(l: LugarMinero) {
    Pastilla(l.nivel.etiqueta, ColoresMapa.de(l.nivel), ColoresMapa.fondo(l.nivel))
    Text(l.nombre, style = MaterialTheme.typography.titleLarge)
    Text(l.queSeSabe, style = MaterialTheme.typography.bodyLarge)
    Text(
        "No es una medición de mercurio. Fuente: ${l.fuente}. Ubicación aproximada.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun FichaTramo(t: TramoRio) {
    Text(t.nombre, style = MaterialTheme.typography.titleLarge)
    Text(t.detalle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    t.peces.mapNotNull { ContextoAlcoche.pez(it) }.sortedBy { it.nivel.ordinal }.forEach { FilaPez(it) }
    AvisoSaludPeces()
}

@Composable
private fun AvisoSaludPeces() {
    Text(
        "Comer pescado es bueno. Elige más los peces verdes. Si estás embarazada o para los niños, pregunta en tu centro de salud.",
        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold,
    )
    Text(
        ContextoAlcoche.FUENTE_PECES + " Es información de referencia, no una indicación de salud.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun FilaPez(p: Pez) {
    val color = ColoresMapa.de(p.nivel)
    Card(
        colors = CardDefaults.cardColors(containerColor = ColoresMapa.fondo(p.nivel)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
            contentDescription = "${p.nombre}: ${p.nivel.etiqueta}. Come ${p.come.lowercase()}."
        },
    ) {
        Row(Modifier.padding(12.dp).heightIn(min = 32.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(16.dp).background(color, CircleShape))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(p.nombre, style = MaterialTheme.typography.titleMedium)
                Text("Come: ${p.come.lowercase()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(p.nivel.etiqueta, style = MaterialTheme.typography.labelLarge, color = color)
        }
    }
}

@Composable
private fun Pastilla(texto: String, color: Color, fondo: Color) {
    Surface(color = fondo, contentColor = color, shape = RoundedCornerShape(50), border = BorderStroke(1.dp, color.copy(alpha = 0.4f))) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).background(color, CircleShape))
            Spacer(Modifier.width(6.dp))
            Text(texto, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

// ---------------------------------------------------------------------------------- listas (sin internet)

@Composable
private fun ListaMercurio(mia: Celda, nombreMio: String, porCelda: Map<Celda, List<AvisoEntity>>, onAbrirAviso: (String) -> Unit) {
    val cerca = porCelda.filterKeys { it.distancia(mia) <= 1 }.values.flatten()
    Text("Avisos de Ahgora cerca de $nombreMio (${cerca.size})", style = MaterialTheme.typography.titleMedium)
    if (cerca.isEmpty()) Text("No hay avisos vigentes cerca.", style = MaterialTheme.typography.bodyLarge)
    cerca.forEach { a -> AvisoTarjeta(a, mia, onClick = { onAbrirAviso(a.id) }) }
    Spacer(Modifier.size(4.dp))
    Text("Lugares con minería de oro", style = MaterialTheme.typography.titleMedium)
    ContextoAlcoche.lugares.forEach { l ->
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, ColoresMapa.de(l.nivel).copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth(),
        ) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { FichaLugar(l) } }
    }
}

@Composable
private fun ListaPeces() {
    NivelPez.entries.forEach { n ->
        Text(n.etiqueta, style = MaterialTheme.typography.titleMedium, color = ColoresMapa.de(n))
        Text(n.explicacion, style = MaterialTheme.typography.bodyMedium)
        ContextoAlcoche.peces.filter { it.nivel == n }.forEach { FilaPez(it) }
        Spacer(Modifier.size(4.dp))
    }
    AvisoSaludPeces()
}

// ---------------------------------------------------------------------------------- mapa OpenStreetMap

/**
 * Mapa real de la zona (OpenStreetMap). Un solo MapView para las dos capas, para no cargar el
 * teléfono. Las teselas ya vistas quedan en caché y se ven sin señal.
 */
@Composable
private fun MapaOsm(
    capa: Capa,
    mia: Celda,
    nombreMio: String,
    porCelda: Map<Celda, List<AvisoEntity>>,
    onTocar: (Seleccion) -> Unit,
    modifier: Modifier,
) {
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
            // Letras del mapa del tamaño normal del teléfono (sin esto se ven diminutas).
            isTilesScaledToDpi = true
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.ALWAYS)
            // Botones + y − a la derecha, en columna, para no tapar las etiquetas del centro.
            zoomController.display.setPositions(
                false, CustomZoomButtonsDisplay.HorizontalPosition.RIGHT, CustomZoomButtonsDisplay.VerticalPosition.BOTTOM,
            )
            minZoomLevel = 6.0
            maxZoomLevel = 17.0
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
    // Cada capa abre con su propio encuadre (después de que el mapa tenga tamaño).
    LaunchedEffect(capa, mia) {
        if (mapa.width > 0) encuadrar(mapa, capa, mia)
        else mapa.addOnFirstLayoutListener { _, _, _, _, _ -> encuadrar(mapa, capa, mia) }
    }

    // clipToBounds: el MapView dibuja fuera de su espacio y taparía las pestañas.
    Box(modifier.clipToBounds()) {
        AndroidView(
            factory = { mapa },
            update = { mv ->
                mv.overlays.clear()
                when (capa) {
                    Capa.MERCURIO -> dibujarMercurio(mv, mia, porCelda, onTocar)
                    Capa.PECES -> dibujarPeces(mv, onTocar)
                }
                val avisosMios = if (capa == Capa.MERCURIO) porCelda[mia].orEmpty() else emptyList()
                mv.overlays.add(marcadorMiComunidad(mv, mia, nombreMio, avisosMios, onTocar))
                mv.invalidate()
            },
            modifier = Modifier.fillMaxSize(),
        )
        SmallFloatingActionButton(
            onClick = { encuadrar(mapa, capa, mia) },
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
        ) { Icon(Icons.Outlined.MyLocation, contentDescription = "Volver a mi comunidad") }
        Text(
            "© OpenStreetMap",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.align(Alignment.BottomStart).padding(4.dp)
                .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp),
        )
    }
}

private fun encuadrar(mapa: MapView, capa: Capa, mia: Celda) {
    val esPiloto = mia == ZONA_POR_DEFECTO
    when {
        capa == Capa.MERCURIO && esPiloto -> {
            mapa.controller.setZoom(ContextoAlcoche.ZOOM_INICIAL)
            mapa.controller.setCenter(GeoPoint(ContextoAlcoche.CENTRO_LAT, ContextoAlcoche.CENTRO_LON))
        }
        capa == Capa.PECES && esPiloto -> {
            // Se ve desde Alcoche hasta Rurrenabaque, donde se midió el mercurio de los peces.
            mapa.controller.setZoom(8.0)
            mapa.controller.setCenter(GeoPoint(-15.05, -67.62))
        }
        else -> {
            mapa.controller.setZoom(if (capa == Capa.MERCURIO) 10.5 else 9.0)
            mapa.controller.setCenter(GeoPoint(mia.latCentro, mia.lonCentro))
        }
    }
}

private fun dibujarMercurio(mv: MapView, mia: Celda, porCelda: Map<Celda, List<AvisoEntity>>, onTocar: (Seleccion) -> Unit) {
    val d = mv.resources.displayMetrics.density
    // Lugares con minería de oro reportada: círculo + etiqueta con nombre.
    for (l in ContextoAlcoche.lugares) {
        val color = ColoresMapa.de(l.nivel)
        mv.overlays.add(Polygon(mv).apply {
            points = Polygon.pointsAsCircle(GeoPoint(l.lat, l.lon), l.radioM)
            fillPaint.color = color.copy(alpha = 0.28f).toArgb()
            outlinePaint.color = color.toArgb()
            outlinePaint.strokeWidth = 2.5f * d
            setOnClickListener { _, _, _ -> onTocar(Seleccion.Lugar(l)); true }
        })
        mv.overlays.add(Marker(mv).apply {
            position = GeoPoint(l.lat, l.lon)
            when (l.lado) {
                LadoEtiqueta.ARRIBA -> setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                LadoEtiqueta.IZQUIERDA -> setAnchor(1.05f, Marker.ANCHOR_CENTER)
                LadoEtiqueta.DERECHA -> setAnchor(-0.05f, Marker.ANCHOR_CENTER)
            }
            icon = etiqueta(
                mv.context, "$ALERTA ${l.corto}", color.toArgb(), android.graphics.Color.WHITE,
                conPunta = l.lado == LadoEtiqueta.ARRIBA,
            )
            infoWindow = null
            setOnMarkerClickListener { _, _ -> onTocar(Seleccion.Lugar(l)); true }
        })
    }
    // Avisos aprobados de Ahgora en otras zonas: una pastilla en el centro de la zona (sin cuadrados).
    // Los de mi comunidad se muestran en la etiqueta de la comunidad.
    for ((celda, lista) in porCelda) {
        if (celda == mia) continue
        val p = lista.map { Presentacion.de(it) }.maxByOrNull { it.prioridad } ?: continue
        mv.overlays.add(Marker(mv).apply {
            position = GeoPoint(celda.latCentro, celda.lonCentro)
            // Encima del centro de la zona: así no tapa la etiqueta de la comunidad (que va debajo).
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = etiqueta(
                mv.context, "$CAMPANA ${lista.size}", p.color.toArgb(),
                android.graphics.Color.WHITE, borde = android.graphics.Color.WHITE,
            )
            infoWindow = null
            setOnMarkerClickListener { _, _ -> onTocar(Seleccion.Avisos(celda)); true }
        })
    }
}

private fun dibujarPeces(mv: MapView, onTocar: (Seleccion) -> Unit) {
    for (t in ContextoAlcoche.tramos) {
        val peces = t.peces.mapNotNull { ContextoAlcoche.pez(it) }
        // El borde del marcador toma el color del pez con más mercurio del tramo.
        val peor = peces.maxByOrNull { it.nivel.ordinal }?.nivel ?: NivelPez.MENOS
        mv.overlays.add(Marker(mv).apply {
            position = GeoPoint(t.lat, t.lon)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = etiqueta(
                mv.context, "$PEZ ${t.nombre.substringBefore(" (")} · ${peces.size}",
                ColoresMapa.Rio.toArgb(), android.graphics.Color.WHITE, borde = ColoresMapa.de(peor).toArgb(),
            )
            infoWindow = null
            setOnMarkerClickListener { _, _ -> onTocar(Seleccion.Tramo(t)); true }
        })
    }
}

private fun marcadorMiComunidad(
    mv: MapView,
    mia: Celda,
    nombre: String,
    avisos: List<AvisoEntity>,
    onTocar: (Seleccion) -> Unit,
): Marker {
    val piloto = Localidades.ALCOCHE
    val punto = if (mia == piloto.celda) GeoPoint(piloto.lat, piloto.lon) else GeoPoint(mia.latCentro, mia.lonCentro)
    val peor = avisos.map { Presentacion.de(it) }.maxByOrNull { it.prioridad }
    val texto = "$CASA $nombre" + when (avisos.size) { 0 -> ""; 1 -> " · 1 aviso"; else -> " · ${avisos.size} avisos" }
    return Marker(mv).apply {
        position = punto
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_TOP)
        icon = etiqueta(
            mv.context, texto, Paleta.Petroleo.toArgb(), android.graphics.Color.WHITE, conPunta = false,
            borde = peor?.color?.toArgb() ?: android.graphics.Color.WHITE,
        )
        infoWindow = null
        setOnMarkerClickListener { _, _ -> onTocar(if (avisos.isEmpty()) Seleccion.MiComunidad else Seleccion.Avisos(mia)); true }
    }
}

/** Pastilla de texto para el mapa (se ve siempre, sin tener que tocar el marcador). */
private fun etiqueta(
    context: Context,
    texto: String,
    fondo: Int,
    colorTexto: Int,
    conPunta: Boolean = true,
    borde: Int? = null,
): BitmapDrawable {
    val d = context.resources.displayMetrics.density
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 13f * d
        typeface = Typeface.DEFAULT_BOLD
        color = colorTexto
    }
    val padH = 8f * d
    val padV = 5f * d
    val punta = if (conPunta) 7f * d else 0f
    val ancho = paint.measureText(texto) + 2 * padH
    val alto = paint.fontMetrics.let { it.descent - it.ascent } + 2 * padV
    val trazo = if (borde != null) 3f * d else 0f
    val bmp = android.graphics.Bitmap.createBitmap(
        (ancho + 2 * trazo).toInt() + 1, (alto + punta + 2 * trazo).toInt() + 1, android.graphics.Bitmap.Config.ARGB_8888,
    )
    val c = android.graphics.Canvas(bmp)
    val rect = RectF(trazo, trazo, trazo + ancho, trazo + alto)
    if (borde != null) {
        val pb = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = borde }
        c.drawRoundRect(RectF(0f, 0f, ancho + 2 * trazo, alto + 2 * trazo), alto / 2 + trazo, alto / 2 + trazo, pb)
    }
    val pf = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fondo }
    c.drawRoundRect(rect, alto / 2, alto / 2, pf)
    if (conPunta) {
        val cx = trazo + ancho / 2
        val path = android.graphics.Path().apply {
            moveTo(cx - punta, rect.bottom - 1); lineTo(cx + punta, rect.bottom - 1); lineTo(cx, rect.bottom + punta); close()
        }
        c.drawPath(path, pf)
    }
    c.drawText(texto, trazo + padH, trazo + padV - paint.fontMetrics.ascent, paint)
    return BitmapDrawable(context.resources, bmp)
}
