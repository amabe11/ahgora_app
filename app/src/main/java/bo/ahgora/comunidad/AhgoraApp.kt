package bo.ahgora.comunidad

import android.app.Application
import android.content.Context
import bo.ahgora.comunidad.data.AhgoraRepository
import bo.ahgora.comunidad.data.Conexion
import bo.ahgora.comunidad.data.Ubicador
import bo.ahgora.comunidad.data.local.AhgoraDatabase
import bo.ahgora.comunidad.data.prefs.AjustesStore
import bo.ahgora.comunidad.data.remote.AhgoraApi
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
}

class AhgoraApp : Application() {
    lateinit var contenedor: Contenedor
        private set

    override fun onCreate() {
        super.onCreate()
        contenedor = Contenedor(this)
        Notificador.crearCanal(this)
        if (contenedor.ajustes.actual.bienvenidaHecha) {
            SyncWorker.programarPeriodica(this)
        }
    }
}
