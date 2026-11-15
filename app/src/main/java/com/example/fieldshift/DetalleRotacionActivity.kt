package com.example.fieldshift

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DetalleRotacionActivity : AppCompatActivity() {

    private lateinit var rotacionSeleccionada: OpcionRotacion

    companion object {
        const val EXTRA_ROTACION_ID = "extra_rotacion_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_detalle_rotacion)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val rotacionId = intent.getStringExtra(EXTRA_ROTACION_ID) ?: "opcion_1"

        // Buscamos en el último análisis de la NASA guardado, o en el respaldo si no existe
        val analisisNasa = MockRepository.ultimoAnalisisNasa
        rotacionSeleccionada = if (analisisNasa != null && analisisNasa.opciones.isNotEmpty()) {
            analisisNasa.opciones.find { it.id == rotacionId } ?: analisisNasa.opciones.first()
        } else {
            MockRepository.obtenerRotacionPorId(rotacionId)
        }

        cargarDatos()
        configurarCicloCultivos()
        configurarNavegacion()
    }

    private fun cargarDatos() {
        val rot = rotacionSeleccionada

        // --- Encabezados ---
        val etiquetaTexto = if (rot.actual) "TU ROTACIÓN ACTUAL" else "ALTERNATIVA ${rot.id.substringAfter("opcion_", "")}".trim()
        findViewById<TextView>(R.id.tvEtiquetaDetalle).text = etiquetaTexto
        findViewById<TextView>(R.id.tvTituloDetalle).text = rot.nombre
        findViewById<TextView>(R.id.tvSubtituloDetalle).text =
            "${MockRepository.ubicacionFinca} · ${formatearNumero(MockRepository.hectareasFinca)} ha"

        // --- Filas informativas: el primer elemento y "Ver N más" para el resto ---
        findViewById<TextView>(R.id.tvDetallePorque).text = rot.porque
        configurarFila(R.id.tvDetalleBeneficio, R.id.tvVerMasBeneficio, rot.beneficios, "Mantiene prácticas conocidas.")
        configurarFila(R.id.tvDetalleRiesgo, R.id.tvVerMasRiesgo, rot.riesgos, "Ninguno crítico reportado.")
        configurarFila(R.id.tvDetalleEvidencia, R.id.tvVerMasEvidencia, rot.evidencia.map { evidenciaEnDosNiveles(it) }, "Sin evidencia disponible.")

        val todasLasOpciones = MockRepository.ultimoAnalisisNasa?.opciones ?: listOf(rot)
        configurarFila(R.id.tvDetalleDatosFaltantes, R.id.tvVerMasDatosFaltantes, rot.vaciosOrdenados(todasLasOpciones), "Falta análisis de suelo local detallado.")

        // --- Consumo de agua: barras relativas a la mayor de las dos ---
        val agua = rot.aporte?.agua
        val referenciaAgua = agua?.referencia ?: 100
        val valorAgua = agua?.rotacion
        val cambioAgua = agua?.cambio_pct

        findViewById<TextView>(R.id.tvAhorroAguaPorcentaje).text = when {
            cambioAgua == null -> "—"
            cambioAgua > 0 -> "+$cambioAgua%"
            else -> "$cambioAgua%"
        }
        findViewById<TextView>(R.id.tvAguaReferencia).text = referenciaAgua.toString()
        findViewById<TextView>(R.id.tvAhorroAguaValor).text = valorAgua?.toString() ?: "Sin dato"

        val maxAgua = maxOf(referenciaAgua, valorAgua ?: 0).coerceAtLeast(1).toFloat()
        ajustarBarra(R.id.barraAguaReferencia, referenciaAgua / maxAgua)
        ajustarBarra(R.id.barraEstaRotacion, (valorAgua ?: 0) / maxAgua)

        // --- Salud del suelo (0–1) ---
        val suelo = rot.aporte?.suelo
        findViewById<TextView>(R.id.tvSaludSueloTexto).text =
            suelo?.nivel?.replaceFirstChar { it.uppercase() } ?: "Igual"
        findViewById<TextView>(R.id.tvSueloRotacionLabel).text = "Esta rotación"
        ajustarBarra(R.id.barraSueloReferencia, (suelo?.referencia ?: 0.0).toFloat())
        ajustarBarra(R.id.barraSueloRotacion, (suelo?.rotacion ?: 0.0).toFloat())

        // --- Aviso / Alerta ---
        val aviso = MockRepository.ultimoAnalisisNasa?.aviso
        findViewById<TextView>(R.id.tvAlertaDetalle).text =
            if (!aviso.isNullOrBlank()) aviso else "Herramienta de apoyo: valida estas opciones con un agrónomo local."
    }

    /** Muestra el primer elemento; si hay más, "Ver N más ↓" despliega la lista con viñetas. */
    private fun configurarFila(idTexto: Int, idVerMas: Int, items: List<CharSequence>, vacio: String) {
        val texto = findViewById<TextView>(idTexto)
        val verMas = findViewById<TextView>(idVerMas)

        if (items.size <= 1) {
            texto.text = items.firstOrNull() ?: vacio
            verMas.visibility = View.GONE
            return
        }

        var expandido = false
        fun pintar() {
            texto.text = if (expandido) {
                items.foldIndexed(SpannableStringBuilder()) { i, acc, item ->
                    if (i > 0) acc.append("\n")
                    acc.append("• ").append(item)
                }
            } else items[0]
            verMas.text = if (expandido) "Ver menos ↑" else "Ver ${items.size - 1} más ↓"
        }

        verMas.visibility = View.VISIBLE
        verMas.setOnClickListener { expandido = !expandido; pintar() }
        pintar()
    }

    /** Dato en negro y, debajo, la nota de EcoCrop en el gris secundario de la tabla. */
    private fun evidenciaEnDosNiveles(evidencia: EvidenciaData): CharSequence {
        val texto = SpannableStringBuilder(evidencia.comoTexto())
        if (evidencia.nota.isNotBlank()) {
            texto.append("\n")
            val inicio = texto.length
            texto.append(evidencia.nota)
            texto.setSpan(ForegroundColorSpan(Color.parseColor("#7A766E")), inicio, texto.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return texto
    }

    /** Fracción 0–1 del ancho de una barra (su contenedor tiene weightSum = 100). */
    private fun ajustarBarra(idBarra: Int, fraccion: Float) {
        val barra = findViewById<View>(idBarra)
        val params = barra.layoutParams as LinearLayout.LayoutParams
        params.weight = (fraccion * 100f).coerceIn(0f, 100f)
        barra.layoutParams = params
    }

    private fun configurarCicloCultivos() {
        val cultivos = rotacionSeleccionada.cultivos

        if (cultivos.isNotEmpty()) {
            findViewById<TextView>(R.id.tvNumCultivo1)?.text = "01"
            findViewById<TextView>(R.id.tvNombreCultivo1)?.text = formatearNombreCultivo(cultivos[0])
            findViewById<ImageView>(R.id.ivIconCultivo1)?.setImageResource(obtenerIconoCultivo(cultivos[0]))
        }

        if (cultivos.size > 1) {
            findViewById<TextView>(R.id.tvNumCultivo2)?.text = "02"
            findViewById<TextView>(R.id.tvNombreCultivo2)?.text = formatearNombreCultivo(cultivos[1])
            findViewById<ImageView>(R.id.ivIconCultivo2)?.setImageResource(obtenerIconoCultivo(cultivos[1]))
        }

        if (cultivos.size > 2) {
            findViewById<View>(R.id.tvFlecha2)?.visibility = View.VISIBLE
            findViewById<View>(R.id.cardCultivo3)?.visibility = View.VISIBLE
            findViewById<TextView>(R.id.tvNumCultivo3)?.text = "03"
            // El diseño tiene 3 casillas: si la rotación actual tiene más cultivos se indica cuántos faltan
            val extra = if (cultivos.size > 3) " +${cultivos.size - 3}" else ""
            findViewById<TextView>(R.id.tvNombreCultivo3)?.text = formatearNombreCultivo(cultivos[2]) + extra
            findViewById<ImageView>(R.id.ivIconCultivo3)?.setImageResource(obtenerIconoCultivo(cultivos[2]))
        } else {
            findViewById<View>(R.id.tvFlecha2)?.visibility = View.GONE
            findViewById<View>(R.id.cardCultivo3)?.visibility = View.GONE
        }
    }

    private fun formatearNombreCultivo(idONombre: String): String =
        MockRepository.buscarCultivo(idONombre)?.name
            ?: idONombre.replace("_", " ").replaceFirstChar { it.uppercase() }

    // Íconos por grupo de cultivo (ids de data/cultivos.json del backend)
    private val leguminosas = setOf(
        "frijol", "haba", "garbanzo", "lenteja", "soya", "mani", "caupi", "guandul", "alfalfa",
        "trebol_rojo", "tarwi", "frijol_mungo", "mucuna", "canavalia"
    )
    private val granos = setOf(
        "trigo", "cebada", "avena", "sorgo", "mijo", "centeno", "quinua", "amaranto", "trigo_sarraceno",
        "girasol", "colza", "algodon", "ajonjoli", "cartamo", "linaza"
    )
    private val raices = setOf("batata", "malanga", "zanahoria", "remolacha", "cebolla")

    private fun obtenerIconoCultivo(idONombre: String): Int {
        val id = MockRepository.buscarCultivo(idONombre)?.id ?: normalizar(idONombre)
        return when (id) {
            "papa" -> R.drawable.ic_crop_potato
            "maiz" -> R.drawable.ic_crop_corn
            "arroz" -> R.drawable.ic_crop_rice
            "yuca" -> R.drawable.ic_crop_cassava
            "arveja" -> R.drawable.ic_crop_pea
            in leguminosas -> R.drawable.ic_crop_bean
            in granos -> R.drawable.ic_crop_oat
            in raices -> R.drawable.ic_crop_sweetpotato
            else -> R.drawable.ic_crop_pea // hortalizas de fruto (tomate, ahuyama)
        }
    }

    private fun configurarNavegacion() {
        // Flecha superior izquierda para regresar
        findViewById<View>(R.id.btnVolver)?.setOnClickListener {
            finish()
        }

        // Barra inferior: Inicio
        findViewById<LinearLayout>(R.id.navInicioDetalle)?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }

        // Barra inferior: Rotaciones
        findViewById<LinearLayout>(R.id.navRotacionesDetalle)?.setOnClickListener {
            finish() // Cierra el detalle y regresa a la lista de rotaciones
        }

        // Barra inferior: Mi Finca
        findViewById<LinearLayout>(R.id.navMiFincaDetalle)?.setOnClickListener {
            startActivity(Intent(this, MiFincaActivity::class.java))
        }

        // Botón "Guardar esta rotación en mi finca"
        findViewById<View>(R.id.btnGuardarRotacion)?.setOnClickListener {
            val nombresFormateados = rotacionSeleccionada.cultivos.map { formatearNombreCultivo(it) }
            MockRepository.cultivosSeleccionados = nombresFormateados.toMutableList()
            // La rotación actual cambió: el próximo ingreso a Rotaciones pide un análisis nuevo
            MockRepository.ultimoAnalisisNasa = null

            val prefs = getSharedPreferences("TerraPrefs", Context.MODE_PRIVATE)
            prefs.edit().putString("user_cultivos", nombresFormateados.joinToString("||")).apply()
            guardarRotacionEnServidor(prefs.getInt("user_id", -1))

            Toast.makeText(
                this,
                "Rotación guardada en tu finca: ${rotacionSeleccionada.nombre}",
                Toast.LENGTH_LONG
            ).show()

            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }
    }

    /** PATCH api/v1/user para que el backend también tenga la nueva rotación actual. */
    private fun guardarRotacionEnServidor(usuarioId: Int) {
        if (usuarioId == -1) return
        val peticion = UpdateUserRequest(
            usuario_id = usuarioId,
            ubicacion = MockRepository.ubicacionFinca,
            hectareas = MockRepository.hectareasFinca,
            rotacion = MockRepository.idsCultivosSeleccionados()
        )
        // Scope independiente de la pantalla, que se cierra justo después
        CoroutineScope(Dispatchers.IO).launch {
            try {
                RetrofitClient.apiService.updateUser(peticion)
            } catch (e: Exception) {
                // Sin conexión: se conserva localmente y se sincroniza en el próximo análisis
            }
        }
    }
}
