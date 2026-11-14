package com.example.fieldshift

// 1. Contrato para enviar en "Mi finca" (peticion.json)
data class PeticionAnalisis(
    val lat: Double,
    val lon: Double,
    val rotacion: List<String>,
    val prioridades: List<String>
)

// 2. Contrato para leer "respuesta.json"
data class RespuestaAnalisis(
    val version: String,
    val clima: ClimaInfo,
    val opciones: List<OpcionRotacion>,
    val aviso: String
)

data class ClimaInfo(
    val lluvia_anual_mm: Int,
    val temp_media_c: Double,
    val dias_calor: Int,
    val riesgo_helada: String,
    val humedad_suelo: Double,
    val fuente: String
)

data class OpcionRotacion(
    val id: String,
    val actual: Boolean,
    val nombre: String,
    val cultivos: List<String>,
    val puntaje: Double,
    val porque: String,
    val evidencia: List<EvidenciaItem>,
    val beneficios: List<String>,
    val riesgos: List<String>,
    val vacios: List<String>
)

data class EvidenciaItem(
    val dato: String,
    val valor: Double,
    val unidad: String,
    val fuente: String,
    val nota: String
)

// 3. Contrato para el selector EcoCrop (cultivos.json para Gabriel Ortega)
data class CultivoItem(
    val id: String,
    val name: String
)

// 4. Datos de prueba (Mock) sincronizados entre "Inicio", "Mi finca" y "respuesta.json"
object MockRepository {
    var nombreUsuario: String = "Elena"
    var ubicacionFinca: String = "Tunja, Boyacá"
    var latFinca: Double = 4.60
    var lonFinca: Double = -74.08
    var hectareasFinca: Int = 12
    var parcelasFinca: Int = 4

    // Secuencia inicial mostrada en el diseño de Mi finca (Papa -> Maíz -> Frijol)
    var cultivosSeleccionados: MutableList<String> = mutableListOf("Papa", "Maíz", "Frijol")
    var prioridadesSeleccionadas: MutableSet<String> = mutableSetOf("agua")

    val catalogoEcoCrop = listOf(
        CultivoItem("papa", "Papa"),
        CultivoItem("maiz", "Maíz"),
        CultivoItem("frijol", "Frijol"),
        CultivoItem("avena", "Avena"),
        CultivoItem("arveja", "Arveja"),
        CultivoItem("trigo", "Trigo"),
        CultivoItem("cebada", "Cebada"),
        CultivoItem("zanahoria", "Zanahoria")
    )

    var respuestaMock = RespuestaAnalisis(
        version = "1.0",
        clima = ClimaInfo(
            lluvia_anual_mm = 980,
            temp_media_c = 14.2,
            dias_calor = 3,
            riesgo_helada = "medio",
            humedad_suelo = 0.62,
            fuente = "NASA POWER"
        ),
        opciones = listOf(
            OpcionRotacion(
                id = "actual",
                actual = true,
                nombre = "Papa → Maíz → Frijol",
                cultivos = listOf("papa", "maiz", "frijol"),
                puntaje = 0.48,
                porque = "Es la rotación que usas hoy; sirve de punto de comparación.",
                evidencia = emptyList(),
                beneficios = listOf("Ya conoces el manejo"),
                riesgos = listOf("Dos ciclos de maíz seguidos favorecen plagas"),
                vacios = listOf("No conocemos el análisis de suelo de tu finca")
            ),
            OpcionRotacion(
                id = "opcion_1",
                actual = false,
                nombre = "Maíz → Frijol → Papa",
                cultivos = listOf("maiz", "frijol", "papa"),
                puntaje = 0.82,
                porque = "Rompe el maíz repetido e incluye una leguminosa que aporta nitrógeno.",
                evidencia = listOf(
                    EvidenciaItem(
                        dato = "Lluvia media",
                        valor = 2.7,
                        unidad = "mm/día",
                        fuente = "NASA POWER",
                        nota = "Cubre la demanda del frijol sin riego extra."
                    )
                ),
                beneficios = listOf("Menos fertilizante", "Menos plagas del maíz"),
                riesgos = listOf("El frijol es sensible a heladas"),
                vacios = listOf("Precio local del frijol no incluido")
            )
        ),
        aviso = "Herramienta de apoyo: revisa estas opciones con un agrónomo local."
    )
}