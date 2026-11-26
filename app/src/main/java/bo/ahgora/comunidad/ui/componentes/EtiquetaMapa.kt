package bo.ahgora.comunidad.ui.componentes

import android.content.Context
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable

/** Pastilla de texto para el mapa (se ve siempre, sin tener que tocar el marcador). */
fun etiquetaMapa(
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
