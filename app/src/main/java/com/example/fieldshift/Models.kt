package com.example.fieldshift

// 1. Contrato para enviar en "Mi finca" (peticion.json)
data class PeticionAnalisis(
    val lat: Double,
    val lon: Double,
    val rotacion: List<String>,
    val prioridades: List<String>
)

// 2. Contrato para leer "respuesta.jsonc"
data class RespuestaAnalisis(
    val version: String,
    val clima: ClimaInfo,
    val opciones: List<OpcionRotacion>,
    val aviso: String = ""
)

data class ClimaInfo(
    val lluvia_anual_mm: Int = 1849,
    val temp_media_c: Double = 18.9,
    val dias_calor: Int = 0,
    val riesgo_helada: String = "bajo",
    val humedad_suelo: Double = 0.67,
    val fuente: String = "NASA POWER",
    val humedad_nivel: String = "baja",
    val clima_tipo: String = "Seco/Cálido"
)

data class AporteAgua(
    val referencia: Int = 100,
    val rotacion: Int = 75,
    val cambio_pct: Int = -25
)

data class AporteSuelo(
    val referencia: Double = 0.0,
    val rotacion: Double = 0.33,
    val nivel: String = "mayor"
)

data class AporteInfo(
    val agua: AporteAgua = AporteAgua(),
    val suelo: AporteSuelo = AporteSuelo()
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
    val aporte: AporteInfo = AporteInfo(),
    // Propiedades mapeadas según diseño y respuesta.jsonc
    val etiqueta: String = if (actual) "TU ROTACIÓN ACTUAL" else if (id == "opcion_1") "ALTERNATIVA 1" else if (id == "opcion_2") "ALTERNATIVA 2" else "ALTERNATIVA",
    val beneficioTexto: String = beneficios.firstOrNull() ?: "",
    val riesgoTexto: String = riesgos.firstOrNull() ?: "",
    val evidenciaNasaTexto: String = evidencia.firstOrNull()?.let { "${it.fuente}: ${it.dato} ${it.valor} ${it.unidad}" } ?: "SMAP: humedad baja. POWER: seco.",
    val datosFaltantesTexto: String = vacios.firstOrNull() ?: "Falta pH del suelo local y registro de lluvias en vereda.",
    val ahorroAguaPorcentaje: String = if (aporte.agua.cambio_pct > 0) "+${aporte.agua.cambio_pct}%" else "${aporte.agua.cambio_pct}%",
    val ahorroAguaValor: Int = aporte.agua.rotacion,
    val saludSueloTexto: String = aporte.suelo.nivel.replaceFirstChar { it.uppercase() },
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

// 4. Datos sincronizados entre "Inicio", "Mi finca", "Rotaciones" y el Backend
object MockRepository {
    var nombreUsuario: String = "Elena"
    var ubicacionFinca: String = "Tunja, Boyacá"
    var latFinca: Double = 4.60
    var lonFinca: Double = -74.08
    var hectareasFinca: Int = 12
    var parcelasFinca: Int = 4

    // Secuencia actual de la finca
    var cultivosSeleccionados: MutableList<String> = mutableListOf("Papa", "Maíz", "Frijol")
    var prioridadesSeleccionadas: MutableSet<String> = mutableSetOf("agua")

    val catalogoEcoCrop = listOf(
        CultivoItem("papa", "Papa"),
        CultivoItem("maiz", "Maíz"),
        CultivoItem("frijol", "Fríjol"),
        CultivoItem("arroz", "Arroz"),
        CultivoItem("batata", "Batata"),
        CultivoItem("yuca", "Yuca"),
        CultivoItem("mucuna", "Mucuna"),
        CultivoItem("avena", "Avena"),
        CultivoItem("arveja", "Arveja"),
        CultivoItem("trigo", "Trigo"),
        CultivoItem("cebada", "Cebada"),
        CultivoItem("zanahoria", "Zanahoria")
    )

    // Respuesta activa del backend (o fallback real sincronizado con respuesta.jsonc)
    var respuestaActiva: RespuestaAnalisis = RespuestaAnalisis(
        version = "1.0",
        clima = ClimaInfo(
            lluvia_anual_mm = 1849,
            temp_media_c = 18.9,
            dias_calor = 0,
            riesgo_helada = "bajo",
            humedad_suelo = 0.67,
            fuente = "NASA POWER",
            humedad_nivel = "baja",
            clima_tipo = "Seco/Cálido"
        ),
        opciones = listOf(
            OpcionRotacion(
                id = "actual",
                actual = true,
                nombre = "Papa → Maíz",
                cultivos = listOf("papa", "maiz"),
                puntaje = 0.50,
                porque = "Punto de partida para comparar.",
                beneficios = listOf("Mantiene cultivos y equipos conocidos."),
                riesgos = listOf("Sin leguminosa; vigilar agua y suelo."),
                evidencia = listOf(
                    EvidenciaItem("Humedad", 0.67, "fracción", "NASA POWER", "Humedad moderada en la zona de raíces.")
                ),
                vacios = listOf("Falta pH del suelo local y registro de lluvias en vereda."),
                aporte = AporteInfo(
                    agua = AporteAgua(100, 100, 0),
                    suelo = AporteSuelo(0.0, 0.0, "igual")
                ),
                etiqueta = "TU ROTACIÓN ACTUAL",
                beneficioTexto = "Mantiene cultivos y equipos conocidos.",
                riesgoTexto = "Sin leguminosa; vigilar agua y suelo.",
                evidenciaNasaTexto = "SMAP: humedad baja. POWER: seco.",
                datosFaltantesTexto = "Falta pH del suelo local y registro de lluvias en vereda.",
                ahorroAguaPorcentaje = "0%",
                ahorroAguaValor = 100,
                saludSueloTexto = "Igual",
                alertaTexto = "Revisar rotación con cultivo de cobertura"
            ),
            OpcionRotacion(
                id = "opcion_1",
                actual = false,
                nombre = "Papa → Maíz → Frijol",
                cultivos = listOf("papa", "maiz", "frijol"),
                puntaje = 0.85,
                porque = "Añade una leguminosa al ciclo.",
                beneficios = listOf("Aporta nitrógeno y ahorra agua."),
                riesgos = listOf("Sensible a heladas. Validar fechas."),
                evidencia = listOf(
                    EvidenciaItem("Humedad", 0.67, "fracción", "NASA POWER", "Humedad moderada en la zona de raíces.")
                ),
                vacios = listOf("Falta pH del suelo local y registro de lluvias en vereda."),
                aporte = AporteInfo(
                    agua = AporteAgua(100, 75, -25),
                    suelo = AporteSuelo(0.0, 0.33, "mayor")
                ),
                etiqueta = "ALTERNATIVA 1",
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
                id = "opcion_2",
                actual = false,
                nombre = "Papa → Avena → Arveja",
                cultivos = listOf("papa", "avena", "arveja"),
                puntaje = 0.80,
                porque = "Combina un cereal y una leguminosa.",
                beneficios = listOf("Protege el suelo y retiene humedad."),
                riesgos = listOf("Revisar mercado y ajuste de equipos."),
                evidencia = listOf(
                    EvidenciaItem("Humedad", 0.67, "fracción", "NASA POWER", "Humedad moderada en la zona de raíces.")
                ),
                vacios = listOf("Falta pH del suelo local y registro de lluvias en vereda."),
                aporte = AporteInfo(
                    agua = AporteAgua(100, 80, -20),
                    suelo = AporteSuelo(0.0, 0.33, "mayor")
                ),
                etiqueta = "ALTERNATIVA 2",
                beneficioTexto = "Protege el suelo y retiene humedad.",
                riesgoTexto = "Revisar mercado y ajuste de equipos.",
                evidenciaNasaTexto = "SMAP: humedad baja. POWER: seco.",
                datosFaltantesTexto = "Falta pH del suelo local y registro de lluvias en vereda.",
                ahorroAguaPorcentaje = "-20%",
                ahorroAguaValor = 80,
                saludSueloTexto = "Mayor",
                alertaTexto = "Ajustar fechas con calendario de siembra local"
            )
        ),
        aviso = "Herramienta de apoyo: revisa estas opciones con un agrónomo local."
    )

    fun obtenerOpciones(): List<OpcionRotacion> {
        return respuestaActiva.opciones
    }

    fun obtenerRotacionPorId(id: String): OpcionRotacion {
        return respuestaActiva.opciones.find { it.id == id } ?: respuestaActiva.opciones.getOrNull(1) ?: respuestaActiva.opciones.first()
    }
}
