package com.example.fieldshift

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

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
        val etiquetaTexto = if (rot.actual) "TU ROTACIÓN ACTUAL" else "ALTERNATIVA"
        findViewById<TextView>(R.id.tvEtiquetaDetalle).text = etiquetaTexto
        findViewById<TextView>(R.id.tvTituloDetalle).text = rot.nombre
        findViewById<TextView>(R.id.tvSubtituloDetalle).text = "${MockRepository.ubicacionFinca} · ${MockRepository.hectareasFinca} ha"

        // --- Filas informativas ---
        findViewById<TextView>(R.id.tvDetallePorque).text = rot.porque

        val primerBeneficio = rot.beneficios.firstOrNull() ?: "Mantiene prácticas conocidas."
        findViewById<TextView>(R.id.tvDetalleBeneficio).text = primerBeneficio

        val primerRiesgo = rot.riesgos.firstOrNull() ?: "Ninguno crítico reportado."
        findViewById<TextView>(R.id.tvDetalleRiesgo).text = primerRiesgo

        val evidenciaTexto = rot.evidencia.firstOrNull()?.let { "${it.dato}: ${it.valor} ${it.unidad} (${it.fuente})" } ?: "NASA POWER: Condiciones estables."
        findViewById<TextView>(R.id.tvDetalleEvidencia).text = evidenciaTexto

        val primerVacio = rot.vacios.firstOrNull() ?: "Falta análisis de suelo local detallado."
        findViewById<TextView>(R.id.tvDetalleDatosFaltantes).text = primerVacio

        // --- Métricas "Lo que puede aportar" ---
        val cambioAgua = rot.aporte?.agua?.cambio_pct ?: 0
        val porcentajeStr = if (cambioAgua > 0) "+$cambioAgua%" else "$cambioAgua%"
        val valorAgua = rot.aporte?.agua?.rotacion ?: 100

        findViewById<TextView>(R.id.tvAhorroAguaPorcentaje).text = porcentajeStr
        findViewById<TextView>(R.id.tvAhorroAguaValor).text = valorAgua.toString()

        val saludSueloNivel = rot.aporte?.suelo?.nivel?.replaceFirstChar { it.uppercase() } ?: "Igual"
        findViewById<TextView>(R.id.tvSaludSueloTexto).text = saludSueloNivel

        val barraEstaRotacion = findViewById<View>(R.id.barraEstaRotacion)
        val params = barraEstaRotacion.layoutParams as LinearLayout.LayoutParams
        params.weight = valorAgua.toFloat().coerceIn(10f, 100f)
        barraEstaRotacion.layoutParams = params

        // --- Aviso / Alerta ---
        val aviso = MockRepository.ultimoAnalisisNasa?.aviso
        findViewById<TextView>(R.id.tvAlertaDetalle).text =
            if (!aviso.isNullOrBlank()) aviso else "Herramienta de apoyo: valida estas opciones con un agrónomo local."
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
            findViewById<TextView>(R.id.tvNombreCultivo3)?.text = formatearNombreCultivo(cultivos[2])
            findViewById<ImageView>(R.id.ivIconCultivo3)?.setImageResource(obtenerIconoCultivo(cultivos[2]))
        } else {
            findViewById<View>(R.id.tvFlecha2)?.visibility = View.GONE
            findViewById<View>(R.id.cardCultivo3)?.visibility = View.GONE
        }
    }

    private fun formatearNombreCultivo(idONombre: String): String {
        val item = MockRepository.catalogoEcoCrop.find {
            it.id.equals(idONombre, ignoreCase = true) || it.name.equals(idONombre, ignoreCase = true)
        }
        return item?.name ?: idONombre.replaceFirstChar { it.uppercase() }
    }

    private fun obtenerIconoCultivo(nombre: String): Int {
        val lower = nombre.lowercase().trim()
        return when {
            lower.contains("papa") -> R.drawable.ic_crop_potato
            lower.contains("maiz") || lower.contains("maíz") -> R.drawable.ic_crop_corn
            lower.contains("frijol") || lower.contains("fríjol") || lower.contains("mucuna") -> R.drawable.ic_crop_bean
            lower.contains("arroz") -> R.drawable.ic_crop_rice
            lower.contains("batata") -> R.drawable.ic_crop_sweetpotato
            lower.contains("yuca") -> R.drawable.ic_crop_cassava
            lower.contains("avena") || lower.contains("trigo") || lower.contains("cebada") -> R.drawable.ic_crop_oat
            lower.contains("arveja") -> R.drawable.ic_crop_pea
            else -> R.drawable.ic_crop_potato
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
}
