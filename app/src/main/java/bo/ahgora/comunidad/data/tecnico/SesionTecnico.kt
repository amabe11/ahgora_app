package bo.ahgora.comunidad.data.tecnico

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Sesión del técnico (guía, sección 6.2: «acceso protegido por usuario y contraseña»).
 * La contraseña es la clave del equipo del Servidor Ahgora (`x-ahg-key`): se comprueba con el servidor
 * la primera vez y se guarda CIFRADA con el Android Keystore. Sin señal se puede volver a entrar con el
 * mismo usuario y clave que ya se comprobaron en este teléfono.
 */
data class Sesion(
    val usuario: String,
    /** Sin clave: registra muestras de demostración y no envía nada al servidor. */
    val demostracion: Boolean,
    val desde: Long,
)

class SesionStore(context: Context) {
    private val sp = context.getSharedPreferences("ahgora_tecnico", Context.MODE_PRIVATE)
    private val _sesion = MutableStateFlow(leer())
    val sesion: StateFlow<Sesion?> = _sesion.asStateFlow()
    val actual: Sesion? get() = _sesion.value

    private fun leer(): Sesion? {
        val u = sp.getString(K_USUARIO, null) ?: return null
        return Sesion(u, sp.getBoolean(K_DEMO, false), sp.getLong(K_DESDE, 0L))
    }

    /** Clave del equipo descifrada (null en demostración o si no hay sesión). */
    fun clave(): String? = sp.getString(K_CLAVE, null)?.let { Cofre.descifrar(it) }

    fun iniciar(usuario: String, clave: String?) {
        val demo = clave == null
        sp.edit {
            putString(K_USUARIO, usuario)
            putBoolean(K_DEMO, demo)
            putLong(K_DESDE, System.currentTimeMillis())
            if (clave != null) {
                putString(K_CLAVE, Cofre.cifrar(clave))
                putString(K_HUELLA, huella(usuario, clave))
            } else remove(K_CLAVE)
        }
        _sesion.value = leer()
    }

    /** ¿Este usuario y clave ya se comprobaron con el servidor en este teléfono? (para entrar sin señal) */
    fun yaComprobada(usuario: String, clave: String): Boolean = sp.getString(K_HUELLA, null) == huella(usuario, clave)

    fun cerrar() {
        // Se conserva la huella para poder volver a entrar sin señal; la clave cifrada se borra.
        sp.edit { remove(K_USUARIO); remove(K_DEMO); remove(K_DESDE); remove(K_CLAVE) }
        _sesion.value = null
    }

    private fun huella(usuario: String, clave: String): String =
        MessageDigest.getInstance("SHA-256").digest("ahgora:${usuario.trim().lowercase()}:$clave".toByteArray())
            .joinToString("") { "%02x".format(it) }

    private companion object {
        const val K_USUARIO = "usuario"
        const val K_DEMO = "demostracion"
        const val K_DESDE = "desde"
        const val K_CLAVE = "clave_cifrada"
        const val K_HUELLA = "huella"
    }
}

/** Cifrado AES-GCM con una llave que vive en el Android Keystore (no se puede extraer del teléfono). */
private object Cofre {
    private const val ALIAS = "ahgora-tecnico"

    private fun llave(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        kg.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return kg.generateKey()
    }

    fun cifrar(texto: String): String {
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, llave()) }
        val datos = c.doFinal(texto.toByteArray())
        return Base64.encodeToString(c.iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(datos, Base64.NO_WRAP)
    }

    fun descifrar(guardado: String): String? = runCatching {
        val (iv, datos) = guardado.split(":").map { Base64.decode(it, Base64.NO_WRAP) }
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, llave(), GCMParameterSpec(128, iv)) }
        String(c.doFinal(datos))
    }.getOrNull()
}
