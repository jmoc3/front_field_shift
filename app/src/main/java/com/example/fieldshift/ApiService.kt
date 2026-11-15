package com.example.fieldshift

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

object ApiService {

    const val BASE_URL = "https://backfieldshift-production.up.railway.app"

    /**
     * Envía la petición a POST api/v1/analisis con lat, lon, rotacion y prioridades.
     * Retorna el objeto RespuestaAnalisis parseado según respuesta.jsonc.
     */
    fun consultarAnalisis(
        peticion: PeticionAnalisis,
        onSuccess: (RespuestaAnalisis) -> Unit,
        onError: (String) -> Unit
    ) {
        thread {
            try {
                val url = URL("$BASE_URL/api/v1/analisis")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                conn.setRequestProperty("Accept", "application/json")
                conn.connectTimeout = 12000
                conn.readTimeout = 15000
                conn.doInput = true
                conn.doOutput = true

                // Construir cuerpo JSON
                val bodyJson = JSONObject().apply {
                    put("lat", peticion.lat)
                    put("lon", peticion.lon)
                    put("rotacion", JSONArray(peticion.rotacion))
                    put("prioridades", JSONArray(peticion.prioridades))
                }

                OutputStreamWriter(conn.outputStream, "UTF-8").use { os ->
                    os.write(bodyJson.toString())
                    os.flush()
                }

                val responseCode = conn.responseCode
                if (responseCode in 200..299) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
                    val sb = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        sb.append(line)
                    }
                    reader.close()

                    val respuesta = parsearRespuestaJson(sb.toString())
                    MockRepository.respuestaActiva = respuesta
                    onSuccess(respuesta)
                } else {
                    val errorStream = conn.errorStream
                    val errorMsg = if (errorStream != null) {
                        BufferedReader(InputStreamReader(errorStream, "UTF-8")).readText()
                    } else {
                        "Error en el servidor: HTTP $responseCode"
                    }
                    onError(errorMsg)
                }
            } catch (e: Exception) {
                onError(e.message ?: "Error de conexión con el servidor")
            }
        }
    }

    fun parsearRespuestaJson(rawJson: String): RespuestaAnalisis {
        val root = JSONObject(rawJson)
        val version = root.optString("version", "1.0")
        val aviso = root.optString("aviso", "")

        // Clima
        val climaObj = root.optJSONObject("clima") ?: JSONObject()
        val clima = ClimaInfo(
            lluvia_anual_mm = climaObj.optInt("lluvia_anual_mm", 1849),
            temp_media_c = climaObj.optDouble("temp_media_c", 18.9),
            dias_calor = climaObj.optInt("dias_calor", 0),
            riesgo_helada = climaObj.optString("riesgo_helada", "bajo"),
            humedad_suelo = climaObj.optDouble("humedad_suelo", 0.67),
            fuente = climaObj.optString("fuente", "NASA POWER"),
            humedad_nivel = climaObj.optString("humedad_nivel", "baja"),
            clima_tipo = climaObj.optString("clima_tipo", "Seco/Cálido")
        )

        // Opciones
        val opcionesArr = root.optJSONArray("opciones") ?: JSONArray()
        val listaOpciones = mutableListOf<OpcionRotacion>()

        for (i in 0 until opcionesArr.length()) {
            val opcObj = opcionesArr.getJSONObject(i)
            val id = opcObj.optString("id", if (i == 0) "actual" else "opcion_$i")
            val actual = opcObj.optBoolean("actual", i == 0)
            val nombre = opcObj.optString("nombre", "")
            val puntaje = opcObj.optDouble("puntaje", 0.5)
            val porque = opcObj.optString("porque", "")

            // Cultivos
            val cultivosArr = opcObj.optJSONArray("cultivos") ?: JSONArray()
            val cultivos = mutableListOf<String>()
            for (j in 0 until cultivosArr.length()) {
                cultivos.add(cultivosArr.optString(j))
            }

            // Evidencia
            val evidenciaArr = opcObj.optJSONArray("evidencia") ?: JSONArray()
            val evidencias = mutableListOf<EvidenciaItem>()
            for (j in 0 until evidenciaArr.length()) {
                val evObj = evidenciaArr.getJSONObject(j)
                evidencias.add(
                    EvidenciaItem(
                        dato = evObj.optString("dato"),
                        valor = evObj.optDouble("valor", 0.0),
                        unidad = evObj.optString("unidad"),
                        fuente = evObj.optString("fuente", "NASA POWER"),
                        nota = evObj.optString("nota")
                    )
                )
            }

            // Beneficios
            val beneficiosArr = opcObj.optJSONArray("beneficios") ?: JSONArray()
            val beneficios = mutableListOf<String>()
            for (j in 0 until beneficiosArr.length()) {
                beneficios.add(beneficiosArr.optString(j))
            }

            // Riesgos
            val riesgosArr = opcObj.optJSONArray("riesgos") ?: JSONArray()
            val riesgos = mutableListOf<String>()
            for (j in 0 until riesgosArr.length()) {
                riesgos.add(riesgosArr.optString(j))
            }

            // Vacíos / Datos faltantes
            val vaciosArr = opcObj.optJSONArray("vacios") ?: JSONArray()
            val vacios = mutableListOf<String>()
            for (j in 0 until vaciosArr.length()) {
                vacios.add(vaciosArr.optString(j))
            }

            // Aporte
            val aporteObj = opcObj.optJSONObject("aporte") ?: JSONObject()
            val aguaObj = aporteObj.optJSONObject("agua") ?: JSONObject()
            val aporteAgua = AporteAgua(
                referencia = aguaObj.optInt("referencia", 100),
                rotacion = aguaObj.optInt("rotacion", 75),
                cambio_pct = aguaObj.optInt("cambio_pct", -25)
            )
            val sueloObj = aporteObj.optJSONObject("suelo") ?: JSONObject()
            val aporteSuelo = AporteSuelo(
                referencia = sueloObj.optDouble("referencia", 0.0),
                rotacion = sueloObj.optDouble("rotacion", 0.33),
                nivel = sueloObj.optString("nivel", "mayor")
            )
            val aporteInfo = AporteInfo(agua = aporteAgua, suelo = aporteSuelo)

            val etiqueta = if (actual) {
                "TU ROTACIÓN ACTUAL"
            } else if (id == "opcion_1" || i == 1) {
                "ALTERNATIVA 1"
            } else if (id == "opcion_2" || i == 2) {
                "ALTERNATIVA 2"
            } else {
                "ALTERNATIVA"
            }

            val evidenciaNasa = evidencias.firstOrNull()?.let {
                "${it.fuente}: ${it.dato} ${it.valor} ${it.unidad}"
            } ?: "SMAP: humedad baja. POWER: seco."

            val alerta = when {
                riesgos.isNotEmpty() -> riesgos.first()
                else -> "Validar época de siembra con asesor local"
            }

            listaOpciones.add(
                OpcionRotacion(
                    id = id,
                    actual = actual,
                    nombre = nombre,
                    cultivos = cultivos,
                    puntaje = puntaje,
                    porque = porque,
                    evidencia = evidencias,
                    beneficios = beneficios,
                    riesgos = riesgos,
                    vacios = vacios,
                    aporte = aporteInfo,
                    etiqueta = etiqueta,
                    beneficioTexto = beneficios.firstOrNull() ?: "",
                    riesgoTexto = riesgos.firstOrNull() ?: "",
                    evidenciaNasaTexto = evidenciaNasa,
                    datosFaltantesTexto = vacios.firstOrNull() ?: "Falta pH del suelo local y registro de lluvias en vereda.",
                    ahorroAguaPorcentaje = if (aporteAgua.cambio_pct > 0) "+${aporteAgua.cambio_pct}%" else "${aporteAgua.cambio_pct}%",
                    ahorroAguaValor = aporteAgua.rotacion,
                    saludSueloTexto = aporteSuelo.nivel.replaceFirstChar { it.uppercase() },
                    alertaTexto = alerta
                )
            )
        }

        return RespuestaAnalisis(
            version = version,
            clima = clima,
            opciones = listaOpciones,
            aviso = aviso
        )
    }
}
