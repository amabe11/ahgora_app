package bo.ahgora.comunidad.ui.pantallas

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bo.ahgora.comunidad.ui.AhgoraViewModel
import bo.ahgora.comunidad.ui.Presentacion
import bo.ahgora.comunidad.ui.Textos
import bo.ahgora.comunidad.ui.componentes.EstadoChip
import bo.ahgora.comunidad.ui.componentes.FilaAccion
import bo.ahgora.comunidad.ui.componentes.Nota
import bo.ahgora.comunidad.ui.componentes.TituloSeccion

/** Preguntas breves (guía, sección 07 «Contenidos breves de educación»). Incluidas en la instalación. */
private val PREGUNTAS = listOf(
    "¿Qué es Ahgora?" to "Te avisa qué muestras de tu comunidad se revisan por mercurio y qué dijo el laboratorio.",
    "¿Qué es una prueba rápida (cribado)?" to "Es una prueba que se hace en el río para decidir qué muestras llevar primero al " +
        "laboratorio. Es una primera señal, no el resultado final.",
    "¿Un aviso quiere decir que estoy enfermo?" to "No. El aviso habla de una muestra del río o de la tierra, no de las personas. " +
        "Las indicaciones de salud solo las da la autoridad de salud.",
    "¿Puedo comer pescado?" to "Comer pescado es bueno. Los peces que comen otros peces (surubí, bagre, blanquillo) suelen tener " +
        "más mercurio; el sábalo, la yatorana y el pacú, menos. Mira la pestaña Peces del Mapa y pregunta en tu centro de salud.",
    "¿Por qué no se analiza el agua?" to "El mercurio se junta en el fondo del río y en los peces, más que en el agua. " +
        "Por eso se revisan esas muestras.",
    "¿Funciona sin internet?" to "Sí. Los avisos, la ayuda y los contactos quedan guardados en el teléfono.",
    "¿Qué datos guarda?" to "Solo tu comunidad y tus preferencias. No pide cuenta ni guarda dónde estás exactamente.",
)

@Composable
fun PantallaAyuda(vm: AhgoraViewModel, onPreferencias: () -> Unit, onReportar: () -> Unit) {
    val ajustes by vm.ajustes.collectAsStateWithLifecycle()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TituloSeccion("¿Dónde pregunto?")
        Contacto(
            Icons.Outlined.SupportAgent, "Equipo Ahgora de la zona",
            "El contacto de ${ajustes.nombreZona ?: "tu comunidad"} se acuerda con la comunidad al empezar el piloto. Aquí estará su teléfono.",
        )
        Contacto(
            Icons.Outlined.HealthAndSafety, "Centro de salud de referencia",
            "Para dudas de salud, acude a tu centro de salud. Las indicaciones sanitarias solo las da la autoridad de salud.",
        )
        Contacto(
            Icons.Outlined.Groups, "Promotor o promotora de salud comunitaria",
            "Si tu comunidad tiene promotor de salud, también puede orientarte sobre los avisos.",
        )

        Nota("Lo más importante", "Un aviso es una ${Textos.PRIMERA_SENAL.replaceFirstChar { it.lowercase() }}")

        TituloSeccion("Qué quiere decir cada aviso")
        Presentacion.entries.forEach { p ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(14.dp).semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    EstadoChip(p)
                    Text(p.queSignifica, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        TituloSeccion("Preguntas frecuentes")
        PREGUNTAS.forEach { (q, r) -> Pregunta(q, r) }

        TituloSeccion("Participa y ajusta")
        FilaAccion(Icons.Outlined.Campaign, "Reportar algo que viste", "Ayuda a decidir dónde tomar muestras", onReportar)
        FilaAccion(Icons.Outlined.Settings, "Preferencias", "Mensajes, letra grande, comunidad", onPreferencias)
    }
}

@Composable
private fun Contacto(icono: ImageVector, titulo: String, texto: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(14.dp).semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.Top) {
            Icon(icono, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(titulo, style = MaterialTheme.typography.titleSmall)
                Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Pregunta(pregunta: String, respuesta: String) {
    var abierta by rememberSaveable(pregunta) { mutableStateOf(false) }
    Card(
        onClick = { abierta = !abierta },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).animateContentSize()
            .semantics { stateDescription = if (abierta) "Abierta" else "Cerrada" },
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(pregunta, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Icon(if (abierta) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null)
            }
            if (abierta) {
                Spacer(Modifier.size(6.dp))
                Text(respuesta, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
