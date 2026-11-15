package com.example.fieldshift

data class CultivoItem(
    val id: String,
    val name: String
)

object MockRepository {

    var ultimoAnalisisNasa: RespuestaAnalisis? = null

    var nombreUsuario: String = "Elena"
    var ubicacionFinca: String = "Tunja, Boyacá"
    var latFinca: Double = 4.60
    var lonFinca: Double = -74.08
    var hectareasFinca: Int = 12
    var parcelasFinca: Int = 4

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
        CultivoItem("zanahoria", "Zanahoria"),
        CultivoItem("batata", "Batata"),
        CultivoItem("mucuna", "Mucuna"),
        CultivoItem("yuca", "Yuca")
    )

    // Lista de respaldo para que DetalleRotacionActivity no falle si lee datos locales
    val rotacionesComparativa = listOf(
        OpcionRotacion(
            id = "actual",
            actual = true,
            nombre = "Papa → Maíz",
            cultivos = listOf("Papa", "Maíz"),
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
            nombre = "Papa → Maíz → Frijol",
            cultivos = listOf("Papa", "Maíz", "Frijol"),
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
            cultivos = listOf("Papa", "Avena", "Arveja"),
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