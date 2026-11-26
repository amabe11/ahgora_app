package bo.ahgora.comunidad

import android.app.Application
import android.content.Context
import bo.ahgora.comunidad.data.AhgoraRepository
import bo.ahgora.comunidad.data.Conexion
import bo.ahgora.comunidad.data.Ubicador
import bo.ahgora.comunidad.data.local.AhgoraDatabase
import bo.ahgora.comunidad.data.prefs.AjustesStore
import bo.ahgora.comunidad.data.remote.AhgoraApi
import bo.ahgora.comunidad.data.tecnico.LectorBluetooth
import bo.ahgora.comunidad.data.tecnico.SesionStore
import bo.ahgora.comunidad.data.tecnico.TecnicoRepository
import bo.ahgora.comunidad.notif.Notificador
import bo.ahgora.comunidad.sync.SyncWorker

/** Dependencias de la app, creadas una sola vez. */
class Contenedor(context: Context) {
    val ajustes = AjustesStore(context)
    val db = AhgoraDatabase.crear(context)
    val api = AhgoraApi()
    val repositorio = AhgoraRepository(db, api, ajustes)
    val conexion = Conexion(context)
    val ubicador = Ubicador(context)
    // Modo técnico
    val sesion = SesionStore(context)
    val tecnico = TecnicoRepository(db.muestras(), api, ajustes, sesion)
    val lector = LectorBluetooth(context)
}

class AhgoraApp : Application() {
    lateinit var contenedor: Contenedor
        private set

    override fun onCreate() {
        super.onCreate()
        contenedor = Contenedor(this)
        Notificador.crearCanal(this)
        if (contenedor.ajustes.actual.bienvenidaHecha || contenedor.sesion.actual != null) {
            SyncWorker.programarPeriodica(this)
        }
    }
}
