package bo.ahgora.comunidad.notif

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import bo.ahgora.comunidad.MainActivity
import bo.ahgora.comunidad.R
import bo.ahgora.comunidad.data.local.AvisoEntity

/**
 * Notificaciones genéricas (guía, sección 05): el texto de la pantalla bloqueada no revela el
 * contenido. Al tocarla se abre el aviso vigente por su id. La bandeja de la app funciona igual
 * si la persona no da el permiso.
 */
object Notificador {
    const val CANAL = "avisos_zona"
    const val EXTRA_AVISO_ID = "aviso_id"
    private const val NOTIF_ID = 4100

    fun crearCanal(context: Context) {
        val canal = NotificationChannel(CANAL, context.getString(R.string.canal_avisos_nombre), NotificationManager.IMPORTANCE_DEFAULT)
            .apply { description = context.getString(R.string.canal_avisos_desc) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    fun permisoConcedido(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun notificarCambios(context: Context, cambios: List<AvisoEntity>, preferenciaActiva: Boolean) {
        if (cambios.isEmpty() || !preferenciaActiva || !permisoConcedido(context)) return
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        // Si hay uno solo, se abre directo; si hay varios, la bandeja.
        val destino = cambios.singleOrNull()?.let { it.reemplazadoPor ?: it.id }
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (destino != null) putExtra(EXTRA_AVISO_ID, destino)
        }
        val pi = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(context.getString(R.string.notif_titulo))
            .setContentText(context.getString(R.string.notif_texto))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        @Suppress("MissingPermission")
        NotificationManagerCompat.from(context).notify(NOTIF_ID, n)
    }
}
