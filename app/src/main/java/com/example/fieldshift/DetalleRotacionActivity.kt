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
import com.google.android.material.card.MaterialCardView

class DetalleRotacionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ROTACION_ID = "extra_rotacion_id"
    }

    private lateinit var rotacionSeleccionada: OpcionRotacion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_detalle_rotacion)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainDetalleRotacion)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val rotacionId = intent.getStringExtra(EXTRA_ROTACION_ID) ?: "alternativa_1"
        rotacionSeleccionada = MockRepository.obtenerRotacionPorId(rotacionId)

        cargarDatos()
        configurarCicloCultivos()
        configurarAcciones()
        configurarNavegacion()
    }

    private fun cargarDatos() {
        val rot = rotacionSeleccionada

        // Encabezados
        findViewById<TextView>(R.id.tvEtiquetaDetalle).text = rot.etiqueta
        findViewById<TextView>(R.id.tvTituloDetalle).text = rot.nombre
        findViewById<TextView>(R.id.tvSubtituloDetalle).text =
            "${MockRepository.ubicacionFinca} · ${MockRepository.hectareasFinca} ha"

        // Filas informativas
        findViewById<TextView>(R.id.tvDetallePorque).text = rot.porque
        findViewById<TextView>(R.id.tvDetalleBeneficio).text = rot.beneficioTexto
        findViewById<TextView>(R.id.tvDetalleRiesgo).text = rot.riesgoTexto
        findViewById<TextView>(R.id.tvDetalleEvidencia).text = rot.evidenciaNasaTexto
        findViewById<TextView>(R.id.tvDetalleDatosFaltantes).text = rot.datosFaltantesTexto

        // Métricas "Lo que puede aportar"
        findViewById<TextView>(R.id.tvAhorroAguaPorcentaje).text = rot.ahorroAguaPorcentaje
        findViewById<TextView>(R.id.tvAhorroAguaValor).text = rot.ahorroAguaValor.toString()
        findViewById<TextView>(R.id.tvSaludSueloTexto).text = rot.saludSueloTexto

        val barraEstaRotacion = findViewById<View>(R.id.barraEstaRotacion)
        val params = barraEstaRotacion.layoutParams as LinearLayout.LayoutParams
        params.weight = rot.ahorroAguaValor.toFloat().coerceIn(10f, 100f)
        barraEstaRotacion.layoutParams = params

        // Aviso / Alerta
        findViewById<TextView>(R.id.tvAlertaDetalle).text = rot.alertaTexto
    }

    private fun configurarCicloCultivos() {
        val cultivos = rotacionSeleccionada.cultivos

        // Cultivo 1
        if (cultivos.isNotEmpty()) {
            findViewById<TextView>(R.id.tvNumCultivo1).text = "01"
            findViewById<TextView>(R.id.tvNombreCultivo1).text = cultivos[0]
            findViewById<ImageView>(R.id.ivIconCultivo1).setImageResource(obtenerIconoCultivo(cultivos[0]))
        }

        // Cultivo 2
        if (cultivos.size > 1) {
            findViewById<TextView>(R.id.tvNumCultivo2).text = "02"
            findViewById<TextView>(R.id.tvNombreCultivo2).text = cultivos[1]
            findViewById<ImageView>(R.id.ivIconCultivo2).setImageResource(obtenerIconoCultivo(cultivos[1]))
        }

        // Cultivo 3 (si existe)
        if (cultivos.size > 2) {
            findViewById<View>(R.id.tvFlecha2).visibility = View.VISIBLE
            findViewById<View>(R.id.cardCultivo3).visibility = View.VISIBLE
            findViewById<TextView>(R.id.tvNumCultivo3).text = "03"
            findViewById<TextView>(R.id.tvNombreCultivo3).text = cultivos[2]
            findViewById<ImageView>(R.id.ivIconCultivo3).setImageResource(obtenerIconoCultivo(cultivos[2]))
        } else {
            findViewById<View>(R.id.tvFlecha2).visibility = View.GONE
            findViewById<View>(R.id.cardCultivo3).visibility = View.GONE
        }
    }

    private fun obtenerIconoCultivo(nombre: String): Int {
        val lower = nombre.lowercase().trim()
        return when {
            lower.contains("papa") -> R.drawable.ic_crop_potato
            lower.contains("maiz") || lower.contains("maíz") -> R.drawable.ic_crop_corn
            lower.contains("frijol") || lower.contains("fríjol") -> R.drawable.ic_crop_bean
            lower.contains("avena") -> R.drawable.ic_crop_oat
            lower.contains("arveja") -> R.drawable.ic_crop_pea
            else -> R.drawable.ic_crop_potato
        }
    }

    private fun configurarAcciones() {
        // Botón volver
        findViewById<LinearLayout>(R.id.btnVolver).setOnClickListener {
            finish()
        }

        // Botón "Guardar esta rotación en mi finca"
        findViewById<MaterialCardView>(R.id.btnGuardarRotacion).setOnClickListener {
            MockRepository.cultivosSeleccionados = rotacionSeleccionada.cultivos.toMutableList()
            Toast.makeText(
                this,
                "Rotación guardada en tu finca: ${rotacionSeleccionada.nombre}",
                Toast.LENGTH_LONG
            ).show()

            // Vuelve a Inicio con los datos actualizados
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }
    }

    private fun configurarNavegacion() {
        findViewById<LinearLayout>(R.id.navInicioDetalle).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }

        findViewById<LinearLayout>(R.id.navRotacionesDetalle).setOnClickListener {
            finish()
        }

        findViewById<LinearLayout>(R.id.navMiFincaDetalle).setOnClickListener {
            startActivity(Intent(this, MiFincaActivity::class.java))
        }
    }
}
