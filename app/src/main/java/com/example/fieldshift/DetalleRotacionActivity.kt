package com.example.fieldshift

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
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
        rotacionSeleccionada = if (analisisNasa != null) {
            analisisNasa.opciones.find { it.id == rotacionId } ?: analisisNasa.opciones.first()
        } else {
            MockRepository.obtenerRotacionPorId(rotacionId)
        }

        cargarDatos()
        configurarCicloCultivos()
        configurarAcciones()
        configurarNavegacion()
    }

    private fun cargarDatos() {
        val rot = rotacionSeleccionada

        // --- Encabezados ---
        val etiquetaTexto = if (rot.actual) "TU ROTACIÓN ACTUAL" else "ALTERNATIVA"
        findViewById<TextView>(R.id.tvEtiquetaDetalle).text = etiquetaTexto
        findViewById<TextView>(R.id.tvTituloDetalle).text = rot.nombre
        findViewById<TextView>(R.id.tvSubtituloDetalle).text = "${MockRepository.ubicacionFinca} · ${MockRepository.hectareasFinca} ha"

        // --- Filas Informativas ---
        findViewById<TextView>(R.id.tvDetallePorque).text = rot.porque

        // Beneficios
        val primerBeneficio = rot.beneficios.firstOrNull() ?: "Mantiene prácticas conocidas."
        findViewById<TextView>(R.id.tvDetalleBeneficio).text = primerBeneficio

        // Riesgos
        val primerRiesgo = rot.riesgos.firstOrNull() ?: "Ninguno crítico reportado."
        findViewById<TextView>(R.id.tvDetalleRiesgo).text = primerRiesgo

        // Evidencia NASA
        val evidenciaTexto = rot.evidencia.firstOrNull()?.let { "${it.dato}: ${it.valor} ${it.unidad} (${it.fuente})" } ?: "NASA POWER: Condiciones estables."
        findViewById<TextView>(R.id.tvDetalleEvidencia).text = evidenciaTexto

        // Datos faltantes / Vacíos
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
        findViewById<TextView>(R.id.tvAlertaDetalle).text = "Herramienta de apoyo: valida estas opciones con un agrónomo local."
    }

    private fun configurarCicloCultivos() {
        val cultivos = rotacionSeleccionada.cultivos

        if (cultivos.isNotEmpty()) {
            findViewById<TextView>(R.id.tvNumCultivo1)?.text = "01"
            findViewById<TextView>(R.id.tvNombreCultivo1)?.text = cultivos[0]
        }

        if (cultivos.size > 1) {
            findViewById<TextView>(R.id.tvNumCultivo2)?.text = "02"
            findViewById<TextView>(R.id.tvNombreCultivo2)?.text = cultivos[1]
        }

        if (cultivos.size > 2) {
            findViewById<View>(R.id.tvFlecha2)?.visibility = View.VISIBLE
            findViewById<View>(R.id.cardCultivo3)?.visibility = View.VISIBLE
            findViewById<TextView>(R.id.tvNumCultivo3)?.text = "03"
            findViewById<TextView>(R.id.tvNombreCultivo3)?.text = cultivos[2]
        }
    }

    private fun configurarAcciones() {
        // Acciones adicionales si las hay
    }

    private fun configurarNavegacion() {
        // Flecha superior izquierda para regresar
        findViewById<View>(R.id.btnVolver)?.setOnClickListener {
            finish()
        }

        // Barra inferior: Inicio (con el ID correcto del XML de detalle)
        findViewById<LinearLayout>(R.id.navInicioDetalle)?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }

        // Barra inferior: Rotaciones (con el ID correcto del XML de detalle)
        findViewById<LinearLayout>(R.id.navRotacionesDetalle)?.setOnClickListener {
            finish() // Cierra el detalle y regresa a la lista de rotaciones
        }

        // Barra inferior: Mi Finca (con el ID correcto del XML de detalle)
        findViewById<LinearLayout>(R.id.navMiFincaDetalle)?.setOnClickListener {
            val intent = Intent(this, MiFincaActivity::class.java)
            startActivity(intent)
        }

        // Botón principal de guardar al final de la tarjeta
        findViewById<View>(R.id.btnGuardarRotacion)?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }
    }
}