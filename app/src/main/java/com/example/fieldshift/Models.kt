package com.example.fieldshift

import android.content.Context
import android.location.Geocoder
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.gson.annotations.SerializedName
import java.text.Normalizer

// Elemento de GET api/v1/cultivos (el backend envía más campos; solo se usan estos)
data class CultivoItem(
    val id: String,
    @SerializedName("nombre") val name: String
)

object MockRepository {

    var ultimoAnalisisNasa: RespuestaAnalisis? = null

    var nombreUsuario: String = "Elena"
    var ubicacionFinca: String = "Tunja, Boyacá"
    var latFinca: Double = 4.60
    var lonFinca: Double = -74.08
    var hectareasFinca: Double = 12.0

    // Ubicación a la que corresponden latFinca/lonFinca (null = aún no geocodificada)
    private var ubicacionGeocodificada: String? = null

    var cultivosSeleccionados: MutableList<String> = mutableListOf("Papa", "Maíz", "Fríjol")
    var prioridadesSeleccionadas: MutableSet<String> = mutableSetOf("agua")

    // Catálogo EcoCrop: viene solo de GET api/v1/cultivos (ver cargarCatalogo)
    var catalogoEcoCrop: List<CultivoItem> = emptyList()
        private set

    /** Descarga el catálogo una vez por sesión de la app. Devuelve false si no se pudo. */
    suspend fun cargarCatalogo(): Boolean {
        if (catalogoEcoCrop.isNotEmpty()) return true
        return try {
            val respuesta = RetrofitClient.apiService.obtenerCultivos()
            val cultivos = respuesta.body().orEmpty()
            if (respuesta.isSuccessful && cultivos.isNotEmpty()) catalogoEcoCrop = cultivos
            catalogoEcoCrop.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /** Al cerrar sesión se descartan los datos del usuario (el catálogo se conserva: es común a todos). */
    fun reiniciarSesion() {
        ultimoAnalisisNasa = null
        nombreUsuario = ""
        ubicacionFinca = ""
        hectareasFinca = 0.0
        ubicacionGeocodificada = null
        cultivosSeleccionados = mutableListOf()
        prioridadesSeleccionadas = mutableSetOf("agua")
    }

    private val ordenPrioridades = listOf("agua", "suelo", "economia", "equipos")

    /** Busca un cultivo por id o por nombre, sin importar mayúsculas ni tildes. */
    fun buscarCultivo(idONombre: String): CultivoItem? {
        val clave = normalizar(idONombre)
        return catalogoEcoCrop.find { it.id == clave || normalizar(it.name) == clave }
    }

    /** Ids que espera el backend para los cultivos seleccionados. */
    fun idsCultivosSeleccionados(): List<String> =
        cultivosSeleccionados.map { buscarCultivo(it)?.id ?: normalizar(it) }

    /**
     * El backend pondera por posición (0.4, 0.3, 0.2, 0.1). Se envían primero las elegidas
     * y luego el resto en orden por defecto, así "suelo" también cuenta aunque no tenga chip.
     */
    fun prioridadesParaAnalisis(): List<String> =
        (prioridadesSeleccionadas.toList() + ordenPrioridades).distinct()

    /** Convierte la ubicación escrita en coordenadas; si falla conserva las anteriores. */
    @Suppress("DEPRECATION")
    fun actualizarCoordenadas(context: Context) {
        if (ubicacionGeocodificada == ubicacionFinca || !Geocoder.isPresent()) return
        try {
            val resultado = Geocoder(context).getFromLocationName("$ubicacionFinca, Colombia", 1)?.firstOrNull()
            if (resultado != null) {
                latFinca = resultado.latitude
                lonFinca = resultado.longitude
                ubicacionGeocodificada = ubicacionFinca
            }
        } catch (e: Exception) {
            // Sin red o sin servicio de geocodificación: se usan las últimas coordenadas
        }
    }

    // Lista de respaldo para que DetalleRotacionActivity no falle si lee datos locales
    val rotacionesComparativa = listOf(
        OpcionRotacion(
            id = "actual",
            actual = true,
            nombre = "Papa → Maíz",
            cultivos = listOf("papa", "maiz"),
            puntaje = 0.50,
            porque = "Punto de partida para comparar.",
            evidencia = emptyList(),
            beneficios = listOf("Mantiene cultivos y equipos conocidos."),
            riesgos = listOf("Sin leguminosa; vigilar agua y suelo."),
            vacios = listOf("Falta pH del suelo local y registro de lluvias."),
            aporte = null
        ),
        OpcionRotacion(
            id = "opcion_1",
            actual = false,
            nombre = "Papa → Maíz → Fríjol",
            cultivos = listOf("papa", "maiz", "frijol"),
            puntaje = 0.85,
            porque = "Añade una leguminosa al ciclo.",
            evidencia = emptyList(),
            beneficios = listOf("Aporta nitrógeno y ahorra agua."),
            riesgos = listOf("Sensible a heladas. Validar fechas."),
            vacios = listOf("Falta pH del suelo local y registro de lluvias."),
            aporte = null
        ),
        OpcionRotacion(
            id = "opcion_2",
            actual = false,
            nombre = "Papa → Avena → Arveja",
            cultivos = listOf("papa", "avena", "arveja"),
            puntaje = 0.80,
            porque = "Combina un cereal y una leguminosa.",
            evidencia = emptyList(),
            beneficios = listOf("Protege el suelo y retiene humedad."),
            riesgos = listOf("Revisar mercado y ajuste de equipos."),
            vacios = listOf("Falta pH del suelo local y registro de lluvias."),
            aporte = null
        )
    )

    fun obtenerRotacionPorId(id: String): OpcionRotacion {
        return rotacionesComparativa.find { id.contains(it.id) } ?: rotacionesComparativa[1]
    }
}

/** "maíz " -> "maiz", "Trébol rojo" -> "trebol_rojo" */
fun normalizar(texto: String): String =
    Normalizer.normalize(texto.trim().lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace(" ", "_")

/**
 * Estado de carga de un botón (MaterialCardView con su texto adentro): lo deshabilita, lo atenúa
 * y cambia el texto mientras dura la petición; al terminar restaura el texto original.
 */
fun View.mostrarCargando(cargando: Boolean, textoCarga: String) {
    fun primerTexto(v: View): TextView? = when (v) {
        is TextView -> v
        is ViewGroup -> (0 until v.childCount).firstNotNullOfOrNull { primerTexto(v.getChildAt(it)) }
        else -> null
    }
    val texto = primerTexto(this)
    if (cargando) {
        if (tag == null) tag = texto?.text
        texto?.text = textoCarga
    } else {
        (tag as? CharSequence)?.let { texto?.text = it }
        tag = null
    }
    isEnabled = !cargando
    alpha = if (cargando) 0.6f else 1f
}

/** 1849.0 -> "1849", 18.9 -> "18.9" */
fun formatearNumero(valor: Double): String =
    if (valor % 1.0 == 0.0) valor.toLong().toString() else valor.toString()

fun EvidenciaData.comoTexto(): String = "$dato: ${formatearNumero(valor)} $unidad"

/**
 * Datos faltantes con los propios de esta rotación primero: los que se repiten en todas
 * las opciones del análisis (suelo, celdas de 50 km, precios…) van al final.
 */
fun OpcionRotacion.vaciosOrdenados(todas: List<OpcionRotacion>): List<String> {
    val comunes = todas.map { it.vacios.toSet() }.reduceOrNull { a, b -> a intersect b }.orEmpty()
    return vacios.sortedBy { it in comunes } // sortedBy es estable: conserva el orden del backend
}
