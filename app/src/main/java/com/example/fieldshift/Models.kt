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
    val evidencia: List<EvidenciaItem> = emptyList(),
    val beneficios: List<String> = emptyList(),
    val riesgos: List<String> = emptyList(),
    val vacios: List<String> = emptyList(),
    // Propiedades adicionales exactas de la vista de diseño
    val etiqueta: String = if (actual) "TU ROTACIÓN ACTUAL" else "ALTERNATIVA",
    val beneficioTexto: String = beneficios.firstOrNull() ?: "",
    val riesgoTexto: String = riesgos.firstOrNull() ?: "",
    val evidenciaNasaTexto: String = "SMAP: humedad baja. POWER: seco.",
    val datosFaltantesTexto: String = vacios.firstOrNull() ?: "Falta pH del suelo local y registro de lluvias en vereda.",
    val ahorroAguaPorcentaje: String = "-25%",
    val ahorroAguaValor: Int = 75,
    val saludSueloTexto: String = "Mayor",
    val alertaTexto: String = "Validar época de siembra con asesor local"
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

// 4. Datos de prueba (Mock) sincronizados entre "Inicio", "Mi finca", "Rotaciones" y "respuesta.json"
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

    // Lista fija y fidedigna según el diseño de Figma/mockup
    val rotacionesComparativa = listOf(
        OpcionRotacion(
            id = "actual",
            actual = true,
            etiqueta = "TU ROTACIÓN ACTUAL",
            nombre = "Papa → Maíz",
            cultivos = listOf("Papa", "Maíz"),
            puntaje = 0.50,
            porque = "Punto de partida para comparar.",
            beneficioTexto = "Mantiene cultivos y equipos conocidos.",
            riesgoTexto = "Sin leguminosa; vigilar agua y suelo.",
            evidenciaNasaTexto = "SMAP: humedad baja. POWER: seco.",
            datosFaltantesTexto = "Falta pH del suelo local y registro de lluvias en vereda.",
            ahorroAguaPorcentaje = "0%",
            ahorroAguaValor = 100,
            saludSueloTexto = "Línea base",
            alertaTexto = "Revisar rotación con cultivo de cobertura"
        ),
        OpcionRotacion(
            id = "alternativa_1",
            actual = false,
            etiqueta = "ALTERNATIVA 1",
            nombre = "Papa → Maíz → Frijol",
            cultivos = listOf("Papa", "Maíz", "Frijol"),
            puntaje = 0.85,
            porque = "Añade una leguminosa al ciclo.",
            beneficioTexto = "Aporta nitrógeno y ahorra agua.",
            riesgoTexto = "Sensible a heladas. Validar fechas.",
            evidenciaNasaTexto = "SMAP: humedad baja. POWER: seco.",
            datosFaltantesTexto = "Falta pH del suelo local y registro de lluvias en vereda.",
            ahorroAguaPorcentaje = "-25%",
            ahorroAguaValor = 75,
            saludSueloTexto = "Mayor",
            alertaTexto = "Validar época de siembra con asesor local"
        ),
        OpcionRotacion(
            id = "alternativa_2",
            actual = false,
            etiqueta = "ALTERNATIVA 2",
            nombre = "Papa → Avena → Arveja",
            cultivos = listOf("Papa", "Avena", "Arveja"),
            puntaje = 0.80,
            porque = "Combina un cereal y una leguminosa.",
            beneficioTexto = "Protege el suelo y retiene humedad.",
            riesgoTexto = "Revisar mercado y ajuste de equipos.",
            evidenciaNasaTexto = "SMAP: humedad baja. POWER: seco.",
            datosFaltantesTexto = "Falta pH del suelo local y registro de lluvias en vereda.",
            ahorroAguaPorcentaje = "-20%",
            ahorroAguaValor = 80,
            saludSueloTexto = "Mayor",
            alertaTexto = "Ajustar fechas con calendario de siembra local"
        )
    )

    fun obtenerRotacionPorId(id: String): OpcionRotacion {
        return rotacionesComparativa.find { it.id == id } ?: rotacionesComparativa[1]
    }

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
