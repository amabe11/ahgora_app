package bo.ahgora.comunidad.data.zona

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Localidades de referencia incluidas en la instalación (funcionan sin internet). Sirven para
 * nombrar la zona («cerca de Guanay») y para elegirla de una lista si no se usa el GPS.
 * Las coordenadas son aproximadas al centro del poblado: solo se usan para ubicar la celda de 0,1°.
 * La lista prioriza las cuencas auríferas del norte de La Paz y del Beni, y suma capitales.
 */
data class Localidad(
    val nombre: String,
    val region: String,
    val lat: Double,
    val lon: Double,
) {
    val celda: Celda get() = Celda.de(lat, lon)
}

object Localidades {

    /**
     * Localidad del piloto: Alcoche, cantón del municipio de Caranavi, a orillas del río Coroico
     * (ruta Caranavi – Guanay). Coordenadas del poblado según OpenStreetMap / GeoNames 3924291.
     * Es la comunidad que la app trae elegida por defecto.
     */
    val ALCOCHE = Localidad("Alcoche", "Caranavi, La Paz", -15.7511, -67.6748)

    val todas: List<Localidad> = listOf(
        ALCOCHE,
        // Norte de La Paz: cuencas de los ríos Coroico, Kaka, Tipuani, Mapiri y Beni
        Localidad("Guanay", "Larecaja, La Paz", -15.498, -67.883),
        Localidad("Teoponte", "Larecaja, La Paz", -15.497, -67.820),
        Localidad("Tipuani", "Larecaja, La Paz", -15.545, -67.985),
        Localidad("Mapiri", "Larecaja, La Paz", -15.252, -68.165),
        Localidad("Sorata", "Larecaja, La Paz", -15.773, -68.649),
        Localidad("Caranavi", "Caranavi, La Paz", -15.833, -67.567),
        Localidad("Palos Blancos", "Sud Yungas, La Paz", -15.583, -67.250),
        Localidad("Coroico", "Nor Yungas, La Paz", -16.188, -67.726),
        Localidad("Chulumani", "Sud Yungas, La Paz", -16.408, -67.528),
        Localidad("Apolo", "Franz Tamayo, La Paz", -14.720, -68.500),
        Localidad("San Buenaventura", "Abel Iturralde, La Paz", -14.433, -67.550),
        Localidad("Ixiamas", "Abel Iturralde, La Paz", -13.767, -68.133),
        Localidad("La Paz", "Murillo, La Paz", -16.496, -68.134),
        Localidad("El Alto", "Murillo, La Paz", -16.500, -68.193),
        // Beni y Pando
        Localidad("Rurrenabaque", "José Ballivián, Beni", -14.441, -67.528),
        Localidad("Reyes", "José Ballivián, Beni", -14.296, -67.336),
        Localidad("Riberalta", "Vaca Díez, Beni", -11.010, -66.070),
        Localidad("Guayaramerín", "Vaca Díez, Beni", -10.826, -65.357),
        Localidad("Trinidad", "Cercado, Beni", -14.833, -64.900),
        Localidad("Cobija", "Nicolás Suárez, Pando", -11.027, -68.769),
        // Otras capitales
        Localidad("Cochabamba", "Cercado, Cochabamba", -17.390, -66.157),
        Localidad("Oruro", "Cercado, Oruro", -17.983, -67.150),
        Localidad("Potosí", "Tomás Frías, Potosí", -19.584, -65.753),
        Localidad("Sucre", "Oropeza, Chuquisaca", -19.033, -65.263),
        Localidad("Santa Cruz de la Sierra", "Andrés Ibáñez, Santa Cruz", -17.783, -63.182),
        Localidad("Tarija", "Cercado, Tarija", -21.536, -64.730),
    ).sortedBy { it.nombre }

    /** Lista ordenada de la más cercana a la más lejana de Alcoche: las comunidades vecinas aparecen primero. */
    val cercaDeAlcoche: List<Localidad> by lazy {
        todas.sortedBy { distanciaKm(ALCOCHE.lat, ALCOCHE.lon, it.lat, it.lon) }
    }

    /** Localidad más cercana al centro de la celda y su distancia aproximada en km. */
    fun masCercana(celda: Celda): Pair<Localidad, Double> =
        todas.map { it to distanciaKm(celda.latCentro, celda.lonCentro, it.lat, it.lon) }
            .minBy { it.second }

    /** Nombre automático de la zona: «Cerca de Guanay» o, si no hay referencia cercana, el rango. */
    fun nombreSugerido(celda: Celda): String {
        // Varias localidades pueden caer en la misma celda (p. ej. Guanay y Teoponte).
        val dentro = todas.filter { it.celda == celda }
            .sortedBy { distanciaKm(celda.latCentro, celda.lonCentro, it.lat, it.lon) }
            .map { it.nombre }
        if (dentro.isNotEmpty()) return when (dentro.size) {
            1 -> dentro[0]
            2 -> "${dentro[0]} y ${dentro[1]}"
            else -> "${dentro[0]}, ${dentro[1]} y alrededores"
        }
        val (loc, km) = masCercana(celda)
        return if (km <= 40) "Cerca de ${loc.nombre}" else "Zona ${celda.rangoLegible()}"
    }

    fun distanciaKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * r * asin(sqrt(a))
    }
}
