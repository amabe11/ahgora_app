package bo.ahgora.comunidad.ui.tecnico

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import bo.ahgora.comunidad.BuildConfig
import bo.ahgora.comunidad.data.tecnico.MuestraEntity
import bo.ahgora.comunidad.data.tecnico.TecnicoRepository
import bo.ahgora.comunidad.ui.Formato
import bo.ahgora.comunidad.ui.componentes.Nota
import bo.ahgora.comunidad.ui.theme.Paleta
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private object RutasT {
    const val MUESTRAS = "t/muestras"
    const val NUEVA = "t/nueva"
    const val DETALLE = "t/muestra/{codigo}"
    const val MAPA = "t/mapa"
    const val CUENTA = "t/cuenta"
    fun detalle(c: String) = "t/muestra/$c"
}

/** Perfil técnico (guía, sección 6.2): «un perfil dentro de la misma app, con acceso protegido por usuario y contraseña». */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TecnicoRaiz(vm: TecnicoViewModel = viewModel()) {
    val sesion by vm.sesion.collectAsStateWithLifecycle()
    if (sesion == null) { PantallaIngreso(vm); return }

    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val entrada by nav.currentBackStackEntryAsState()
    val ruta = entrada?.destination?.route ?: RutasT.MUESTRAS
    val esPestana = ruta in listOf(RutasT.MUESTRAS, RutasT.MAPA, RutasT.CUENTA)
    LaunchedEffect(Unit) { vm.mensajes.collect { snackbar.showSnackbar(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(when (ruta) {
                        RutasT.MUESTRAS -> "Muestras · Técnico"; RutasT.NUEVA -> "Nueva muestra"; RutasT.MAPA -> "Mapa de muestras"
                        RutasT.CUENTA -> "Mi cuenta"; else -> "Ficha de la muestra"
                    })
                },
                navigationIcon = {
                    if (!esPestana) IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Volver") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Paleta.VerdeOscuro, titleContentColor = Color.White,
                    navigationIconContentColor = Color.White, actionIconContentColor = Color.White,
                ),
            )
        },
        bottomBar = {
            if (esPestana) NavigationBar {
                listOf(Triple(RutasT.MUESTRAS, "Muestras", Icons.AutoMirrored.Outlined.List), Triple(RutasT.MAPA, "Mapa", Icons.Outlined.Map),
                    Triple(RutasT.CUENTA, "Cuenta", Icons.Outlined.Person)).forEach { (r, t, i) ->
                    NavigationBarItem(selected = ruta == r, onClick = { irA(nav, r) }, icon = { Icon(i, null) }, label = { Text(t) })
                }
            }
        },
        floatingActionButton = {
            if (ruta == RutasT.MUESTRAS) ExtendedFloatingActionButton(
                onClick = { vm.nuevoBorrador(); nav.navigate(RutasT.NUEVA) },
                icon = { Icon(Icons.Outlined.Add, null) }, text = { Text("Nueva muestra") },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { p ->
        Box(Modifier.fillMaxSize().padding(p)) {
            NavHost(nav, startDestination = RutasT.MUESTRAS) {
                composable(RutasT.MUESTRAS) { PantallaMuestras(vm, onAbrir = { nav.navigate(RutasT.detalle(it)) }) }
                composable(RutasT.NUEVA) { PantallaNuevaMuestra(vm, onGuardada = { nav.popBackStack() }) }
                composable(RutasT.DETALLE) { e ->
                    PantallaDetalleMuestra(vm, e.arguments?.getString("codigo").orEmpty(), onBorrada = { nav.popBackStack() })
                }
                composable(RutasT.MAPA) { PantallaMapaMuestras(vm, onAbrir = { nav.navigate(RutasT.detalle(it)) }) }
                composable(RutasT.CUENTA) { PantallaCuenta(vm) }
            }
        }
    }
}

private fun irA(nav: NavHostController, ruta: String) = nav.navigate(ruta) {
    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

// ---------------------------------------------------------------------------------- ingreso

@Composable
private fun PantallaIngreso(vm: TecnicoViewModel) {
    val scope = rememberCoroutineScope()
    var usuario by rememberSaveable { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    var verClave by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var cargando by remember { mutableStateOf(false) }
    val enLinea by vm.enLinea.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).navigationBarsPadding().imePadding()) {
        Column(Modifier.fillMaxWidth().background(Paleta.VerdeOscuro).statusBarsPadding().padding(24.dp)) {
            Text("Ahgora", style = MaterialTheme.typography.displaySmall, color = Color.White, modifier = Modifier.semantics { heading() })
            Text("Modo técnico de campo", style = MaterialTheme.typography.titleMedium, color = Paleta.Menta)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Registra y documenta las muestras de Alcoche: código, lugar, foto, procedimiento, lectura del lector y semáforo.",
                style = MaterialTheme.typography.bodyLarge)
            OutlinedTextField(usuario, { usuario = it.take(40); error = null }, label = { Text("Usuario (tu nombre)") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                clave, { clave = it; error = null }, label = { Text("Clave del equipo") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (verClave) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { verClave = !verClave }) {
                        Icon(if (verClave) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, if (verClave) "Ocultar clave" else "Ver clave")
                    }
                },
                supportingText = { Text("Es la clave del Servidor Ahgora. La primera vez se necesita señal.") },
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            Button(
                onClick = {
                    scope.launch {
                        cargando = true
                        val r = vm.ingresar(usuario, clave)
                        cargando = false
                        if (r is TecnicoRepository.Ingreso.Error) error = r.mensaje
                    }
                },
                enabled = !cargando, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) { Text(if (cargando) "Comprobando…" else "Ingresar") }
            if (!enLinea) Text("Sin señal: solo puedes entrar con un usuario y clave que ya usaste en este teléfono.",
                style = MaterialTheme.typography.bodySmall, color = Paleta.Ambar)
            OutlinedButton(
                onClick = { val r = vm.ingresarDemostracion(usuario); if (r is TecnicoRepository.Ingreso.Error) error = r.mensaje },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) { Text("Probar sin clave (demostración)") }
            Text("En demostración las muestras quedan solo en el teléfono y no se envían al servidor.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { vm.irAComunidad() }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Volver a la app de la comunidad") }
        }
    }
}

// ---------------------------------------------------------------------------------- cuenta

@Composable
private fun PantallaCuenta(vm: TecnicoViewModel) {
    val sesion by vm.sesion.collectAsStateWithLifecycle()
    val muestras by vm.muestras.collectAsStateWithLifecycle()
    val ajustes by vm.ajustes.collectAsStateWithLifecycle()
    val ultimo by vm.ultimoEnvio.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val s = sesion ?: return
    val pendientes = muestras.count { it.pendienteSync && !it.soloLocal }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Titulo("Técnico")
        Dato("Usuario", s.usuario)
        Dato("Modo", if (s.demostracion) "Demostración (sin clave)" else "Conectado al Servidor Ahgora")
        Dato("Desde", Formato.fechaHora(s.desde))
        Dato("Zona de trabajo", "Alcoche, Caranavi")
        Titulo("Muestras")
        Dato("Registradas", "${muestras.size}")
        Dato("Sin enviar", "$pendientes")
        ultimo?.let { Dato("Último envío", Formato.fechaHora(it)) }
        Dato("Servidor", ajustes.apiBaseUrl.removePrefix("https://").trimEnd('/'))
        if (!s.demostracion) Button(onClick = { vm.enviar() }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Enviar ahora") }
        OutlinedButton(
            onClick = { if (muestras.isEmpty()) vm.avisar("No hay muestras para exportar.") else compartirCsv(context, vm.csv(muestras), muestras) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) { Text("Exportar CSV para el laboratorio") }
        Nota("Datos protegidos", "Las coordenadas exactas, las lecturas y las fotos son de acceso restringido. " +
            "La comunidad solo ve avisos aprobados por un responsable, con la zona aproximada.")
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { vm.irAComunidad() }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Ir a la app de la comunidad") }
        TextButton(onClick = { vm.cerrarSesion() }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("Cerrar sesión", color = MaterialTheme.colorScheme.error)
        }
        Text("Ahgora ${BuildConfig.VERSION_NAME} · modo técnico", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun compartirCsv(context: Context, contenido: String, muestras: List<MuestraEntity>) {
    val dir = File(context.cacheDir, "exportes").apply { mkdirs() }
    val nombre = "ahgora_muestras_" + SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date()) + ".csv"
    val archivo = File(dir, nombre).apply { writeText(contenido, Charsets.UTF_8) }
    val uri = FileProvider.getUriForFile(context, context.packageName + ".archivos", archivo)
    val envio = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "Ahgora · ${muestras.size} muestra(s)")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(envio, "Enviar CSV"))
}
