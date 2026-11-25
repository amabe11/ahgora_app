package bo.ahgora.comunidad.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import bo.ahgora.comunidad.ui.pantallas.PantallaAvisos
import bo.ahgora.comunidad.ui.pantallas.PantallaAyuda
import bo.ahgora.comunidad.ui.pantallas.PantallaBienvenida
import bo.ahgora.comunidad.ui.pantallas.PantallaDetalleAviso
import bo.ahgora.comunidad.ui.pantallas.PantallaInicio
import bo.ahgora.comunidad.ui.pantallas.PantallaMapa
import bo.ahgora.comunidad.ui.pantallas.PantallaPreferencias
import bo.ahgora.comunidad.ui.pantallas.PantallaReporte
import bo.ahgora.comunidad.ui.pantallas.PantallaZona
import bo.ahgora.comunidad.ui.theme.Paleta
import kotlinx.coroutines.flow.StateFlow

private object Rutas {
    const val INICIO = "inicio"
    const val AVISOS = "avisos"
    const val MAPA = "mapa"
    const val AYUDA = "ayuda"
    const val DETALLE = "aviso/{id}"
    const val ZONA = "zona"
    const val PREFERENCIAS = "preferencias"
    const val REPORTE = "reporte"
    fun detalle(id: String) = "aviso/$id"
}

private data class Pestana(val ruta: String, val titulo: String, val icono: androidx.compose.ui.graphics.vector.ImageVector)

private val PESTANAS = listOf(
    Pestana(Rutas.INICIO, "Inicio", Icons.Outlined.Home),
    Pestana(Rutas.AVISOS, "Avisos", Icons.Outlined.Notifications),
    Pestana(Rutas.MAPA, "Mapa", Icons.Outlined.Map),
    Pestana(Rutas.AYUDA, "Ayuda", Icons.AutoMirrored.Outlined.HelpOutline),
)

private val TITULOS = mapOf(
    Rutas.INICIO to "Ahgora", Rutas.AVISOS to "Avisos de la zona", Rutas.MAPA to "Mapa de zonas",
    Rutas.AYUDA to "Ayuda", Rutas.DETALLE to "Detalle del aviso", Rutas.ZONA to "Cambiar zona",
    Rutas.PREFERENCIAS to "Preferencias", Rutas.REPORTE to "Reportar algo",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaizApp(vm: AhgoraViewModel, avisoDesdeNotificacion: StateFlow<String?>, onAvisoAbierto: () -> Unit) {
    val ajustes by vm.ajustes.collectAsStateWithLifecycle()
    if (!ajustes.bienvenidaHecha || ajustes.celda == null) {
        PantallaBienvenida(vm)
        return
    }

    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val entrada by nav.currentBackStackEntryAsState()
    val ruta = entrada?.destination?.route ?: Rutas.INICIO
    val esPestana = PESTANAS.any { it.ruta == ruta }
    val avisos by vm.avisos.collectAsStateWithLifecycle()
    val sinLeer = avisos.orEmpty().count { !it.leido && !it.retirado && it.celda.distancia(ajustes.celda!!) <= 1 }

    LaunchedEffect(Unit) { vm.mensajes.collect { snackbar.showSnackbar(it) } }
    val pendiente by avisoDesdeNotificacion.collectAsStateWithLifecycle()
    LaunchedEffect(pendiente) {
        pendiente?.let { nav.navigate(Rutas.detalle(it)); onAvisoAbierto() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        TITULOS[ruta] ?: "Ahgora",
                        style = if (ruta == Rutas.INICIO) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    if (!esPestana) IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (ruta != Rutas.PREFERENCIAS) IconButton(onClick = { nav.navigate(Rutas.PREFERENCIAS) }) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Preferencias")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Paleta.Petroleo, titleContentColor = Color.White,
                    navigationIconContentColor = Color.White, actionIconContentColor = Color.White,
                ),
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                PESTANAS.forEach { p ->
                    val seleccionada = ruta == p.ruta || (p.ruta == Rutas.AVISOS && ruta == Rutas.DETALLE)
                    NavigationBarItem(
                        selected = seleccionada,
                        onClick = { irAPestana(nav, p.ruta) },
                        icon = {
                            if (p.ruta == Rutas.AVISOS && sinLeer > 0) {
                                BadgedBox(badge = { Badge { Text("$sinLeer") } }) { Icon(p.icono, null) }
                            } else Icon(p.icono, null)
                        },
                        label = { Text(p.titulo) },
                        modifier = Modifier.semantics {
                            contentDescription = if (p.ruta == Rutas.AVISOS && sinLeer > 0) "Avisos, $sinLeer sin leer" else p.titulo
                        },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer),
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            NavHost(nav, startDestination = Rutas.INICIO) {
                composable(Rutas.INICIO) {
                    PantallaInicio(
                        vm,
                        onAbrirAviso = { nav.navigate(Rutas.detalle(it)) },
                        onVerAvisos = { irAPestana(nav, Rutas.AVISOS) },
                        onCambiarZona = { nav.navigate(Rutas.ZONA) },
                        onAyuda = { irAPestana(nav, Rutas.AYUDA) },
                        onReportar = { nav.navigate(Rutas.REPORTE) },
                    )
                }
                composable(Rutas.AVISOS) { PantallaAvisos(vm, onAbrirAviso = { nav.navigate(Rutas.detalle(it)) }) }
                composable(Rutas.MAPA) { PantallaMapa(vm, onAbrirAviso = { nav.navigate(Rutas.detalle(it)) }) }
                composable(Rutas.AYUDA) {
                    PantallaAyuda(vm, onPreferencias = { nav.navigate(Rutas.PREFERENCIAS) }, onReportar = { nav.navigate(Rutas.REPORTE) })
                }
                composable(Rutas.DETALLE) { e ->
                    val id = e.arguments?.getString("id").orEmpty()
                    PantallaDetalleAviso(
                        vm, id,
                        onAbrirAviso = { nav.navigate(Rutas.detalle(it)) },
                        onContacto = { irAPestana(nav, Rutas.AYUDA) },
                    )
                }
                composable(Rutas.ZONA) { PantallaZona(vm, onListo = { nav.popBackStack() }) }
                composable(Rutas.PREFERENCIAS) { PantallaPreferencias(vm, onCambiarZona = { nav.navigate(Rutas.ZONA) }) }
                composable(Rutas.REPORTE) { PantallaReporte(vm, onListo = { nav.popBackStack() }) }
            }
        }
    }
}

private fun irAPestana(nav: NavHostController, ruta: String) {
    nav.navigate(ruta) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
