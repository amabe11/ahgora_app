package bo.ahgora.comunidad.data.mapa

/**
 * Contexto territorial de la zona piloto (Alcoche, municipio de Caranavi) incluido en la instalación,
 * para que los dos mapas funcionen sin servidor.
 *
 * IMPORTANTE (guía, sección 02): esto NO es una medición de mercurio. Marca lugares con minería de oro
 * reportada públicamente, donde es más probable encontrar mercurio. Solo un análisis de laboratorio
 * de una muestra confirma su presencia; eso llega a la app como aviso aprobado.
 *
 * Fuentes revisadas (nov. 2026):
 *  - AJAM, inspección 2–3 oct. 2025: maquinaria pesada y chutes de lavado cerca de Alcoche, ruta
 *    Caranavi – Guanay, riberas del río Coroico (Visión 360 / ABI, 8 oct. 2025).
 *  - Comunidad Flor de Mayo, cantón Alcoche: denuncia de minería en el lote 15 (ANF, 19 sep. 2025).
 *  - Ley Municipal 04/2026 de Caranavi «Municipio Ecológico», prohíbe la minería (La Patria, 15 abr. 2026).
 *  - Río Kaka entre Caranavi y Puerto Pando: cooperativas y dragas (Mongabay, sep. 2026).
 *  - Tipuani, Mapiri y Kaka: principales ríos de oro aluvial del Alto Beni (Maurice-Bourgoin et al., AMBIO 1999).
 *  - Peces: UMSA – WCS Bolivia, 10 especies del mercado de Rurrenabaque, 2024 (Mongabay, mar. 2026; OPS, oct. 2026).
 *
 * Las coordenadas de las áreas son aproximadas (centro del poblado o del tramo de río). El equipo
 * debe ajustarlas con la comunidad antes del piloto.
 */

/** Qué tan probable es encontrar mercurio según la actividad minera reportada. */
enum class NivelPosible(val etiqueta: String, val explicacion: String) {
    ALTO("Mucha minería", "Zona minera de oro desde hace años, con dragas o cooperativas en el río."),
    MEDIO("Minería reportada", "Se denunció o se encontró minería de oro hace poco."),
}

/** Hacia dónde se dibuja la etiqueta del lugar, para que no se tapen entre sí. */
enum class LadoEtiqueta { ARRIBA, IZQUIERDA, DERECHA }

data class LugarMinero(
    val id: String,
    val nombre: String,
    /** Nombre corto para la etiqueta del mapa. */
    val corto: String,
    val rio: String,
    val nivel: NivelPosible,
    /** Frase corta para la tarjeta. */
    val queSeSabe: String,
    val fuente: String,
    val lat: Double,
    val lon: Double,
    /** Radio aproximado del área en metros. */
    val radioM: Double,
    val lado: LadoEtiqueta = LadoEtiqueta.ARRIBA,
)

/** Cuánto mercurio suele acumular el pez según lo que come (nivel trófico). */
enum class NivelPez(val etiqueta: String, val explicacion: String) {
    MENOS("Menos mercurio", "En el estudio tuvieron menos mercurio. Casi todos comen plantas, frutos o lo que hay en el fondo."),
    MAS("Más mercurio", "Come otros peces. Acumula más mercurio en su carne."),
    MUCHO("Mucho mercurio", "Fue el pez con más mercurio del estudio."),
}

data class Pez(
    val id: String,
    val nombre: String,
    val cientifico: String,
    val nivel: NivelPez,
    val come: String,
)

/** Tramo de río con los peces que se pescan allí. */
data class TramoRio(
    val id: String,
    val nombre: String,
    val detalle: String,
    val lat: Double,
    val lon: Double,
    val peces: List<String>,
    /** El tramo donde se midió el mercurio de los peces. */
    val medido: Boolean = false,
)

object ContextoAlcoche {

    /** Vista inicial del mapa de mercurio: Alcoche y el río abajo hasta Guanay. */
    const val CENTRO_LAT = -15.62
    const val CENTRO_LON = -67.82
    const val ZOOM_INICIAL = 10.0

    val lugares: List<LugarMinero> = listOf(
        LugarMinero(
            "coroico-alcoche", "Río Coroico, cerca de Alcoche", "Río Coroico", "Río Coroico", NivelPosible.MEDIO,
            "En octubre de 2025 se encontró minería de oro con maquinaria pesada en las orillas del río, " +
                "en la ruta Caranavi – Guanay. En la comunidad Flor de Mayo (cantón Alcoche) también se denunció minería.",
            "AJAM, oct. 2025 · ANF, sep. 2025", -15.7511, -67.6748, 3_500.0,
        ),
        LugarMinero(
            "kaka-guanay-teoponte", "Guanay y Teoponte, río Kaka", "Guanay y Teoponte", "Ríos Kaka, Mapiri y Tipuani",
            NivelPosible.ALTO,
            "Río abajo de Alcoche. Zona minera de oro desde hace décadas: cooperativas y dragas en el río Kaka, " +
                "donde se juntan los ríos que bajan de las minas.",
            "Maurice-Bourgoin et al., 1999 · Mongabay, sep. 2026", -15.498, -67.851, 6_500.0,
        ),
        LugarMinero(
            "tipuani", "Tipuani", "Tipuani", "Río Tipuani", NivelPosible.ALTO,
            "Uno de los ríos con más minería de oro aluvial del norte de La Paz.",
            "Maurice-Bourgoin et al., 1999", -15.545, -67.985, 5_000.0, LadoEtiqueta.IZQUIERDA,
        ),
        LugarMinero(
            "mapiri", "Mapiri", "Mapiri", "Río Mapiri", NivelPosible.ALTO,
            "Minería de oro aluvial con dragas en el río Mapiri.",
            "Maurice-Bourgoin et al., 1999 · Asamblea Dptal. La Paz, 2023", -15.252, -68.165, 5_000.0,
        ),
    )

    val peces: List<Pez> = listOf(
        Pez("sabalo", "Sábalo", "Prochilodus nigricans", NivelPez.MENOS, "Barro y restos del fondo del río"),
        Pez("yatorana", "Yatorana", "Brycon amazonicus", NivelPez.MENOS, "Frutos, semillas e insectos"),
        Pez("pacu", "Pacú", "Piaractus brachypomus", NivelPez.MENOS, "Frutos y semillas"),
        Pez("tambaqui", "Tambaquí", "Colossoma macropomum", NivelPez.MENOS, "Frutos y semillas"),
        Pez("paiche", "Paiche", "Arapaima gigas", NivelPez.MENOS, "Peces pequeños (crece muy rápido)"),
        Pez("surubi", "Surubí", "Pseudoplatystoma fasciatum", NivelPez.MAS, "Otros peces"),
        Pez("bagre", "Bagre (muturo)", "Zungaro zungaro", NivelPez.MAS, "Otros peces"),
        Pez("pantalon", "Pantalón", "Sorubimichthys planiceps", NivelPez.MAS, "Otros peces"),
        Pez("corvina", "Corvina", "Plagioscion squamosissimus", NivelPez.MAS, "Otros peces y camarones"),
        Pez("blanquillo", "Blanquillo", "Calophysus macropterus", NivelPez.MUCHO, "Peces y restos de animales"),
    )

    val tramos: List<TramoRio> = listOf(
        TramoRio(
            "t-coroico", "Río Coroico (Alcoche)",
            "Peces que suben por el río Coroico. Confirma la lista con los pescadores de Alcoche.",
            -15.738, -67.690, listOf("sabalo", "yatorana", "surubi", "bagre"),
        ),
        TramoRio(
            "t-kaka", "Río Kaka (Teoponte – Guanay)",
            "Río abajo de Alcoche, donde hay más minería.",
            -15.50, -67.85, listOf("sabalo", "yatorana", "pacu", "surubi", "bagre"),
        ),
        TramoRio(
            "t-altobeni", "Río Alto Beni (Palos Blancos)",
            "Río vecino que también baja al río Beni.",
            -15.58, -67.25, listOf("sabalo", "yatorana", "pacu", "surubi", "bagre"),
        ),
        TramoRio(
            "t-beni", "Río Beni (Rurrenabaque)",
            "Aquí se midió el mercurio de 10 peces que la gente compra en el mercado (UMSA – WCS, 2024).",
            -14.441, -67.528, peces.map { it.id }, medido = true,
        ),
    )

    fun pez(id: String): Pez? = peces.firstOrNull { it.id == id }

    const val FUENTE_PECES = "Estudio UMSA – WCS Bolivia: mercurio en 10 peces del mercado de Rurrenabaque (2024)."
}
