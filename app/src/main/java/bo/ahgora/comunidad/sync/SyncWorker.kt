package bo.ahgora.comunidad.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import bo.ahgora.comunidad.AhgoraApp
import bo.ahgora.comunidad.notif.Notificador
import java.util.concurrent.TimeUnit

/**
 * Descarga en segundo plano las nuevas versiones y retiros (guía, sección 06 «Regresa la conexión»).
 * WorkManager solo lo ejecuta cuando hay red, y reintenta con espera creciente si falla.
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as AhgoraApp
        val repo = app.contenedor.repositorio
        if (repo.ajustes.actual.celda == null) return Result.success()
        val r = repo.sincronizar()
        if (!r.ok) return if (runAttemptCount < 5) Result.retry() else Result.failure()
        Notificador.notificarCambios(applicationContext, r.cambiosCerca, repo.ajustes.actual.notificaciones)
        return Result.success()
    }

    companion object {
        private const val PERIODICA = "ahgora-sync-periodica"
        private const val AHORA = "ahgora-sync-al-volver-la-red"

        private val conRed = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        /** Cada 6 h, solo con red. */
        fun programarPeriodica(context: Context) {
            val req = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(conRed)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODICA, ExistingPeriodicWorkPolicy.KEEP, req)
        }

        /** Se ejecuta en cuanto haya red (al instante si ya la hay). */
        fun sincronizarCuandoHayaRed(context: Context) {
            val req = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(conRed)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(AHORA, ExistingWorkPolicy.REPLACE, req)
        }
    }
}
