package com.example.fieldshift

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RotacionesActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_rotaciones)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        progressBar = findViewById(R.id.pbCargandoRotaciones)

        // 1. Mostrar datos actuales (último análisis guardado o respaldo local)
        cargarDatosUI()
        configurarInteracciones()
        configurarNavegacion()

        // 2. Sincronizar con el backend en segundo plano
        consultarBackend()
    }

    /** Opciones activas: último análisis de la NASA o lista de respaldo local. */
    private fun opcionesActivas(): List<OpcionRotacion> {
        return MockRepository.ultimoAnalisisNasa?.opciones ?: MockRepository.rotacionesComparativa
    }

    private fun alternativa(indice: Int): OpcionRotacion? {
        return opcionesActivas().filter { !it.actual }.getOrNull(indice)
    }

    private fun consultarBackend() {
        val prefs = getSharedPreferences("TerraPrefs", Context.MODE_PRIVATE)
        val usuarioId = prefs.getInt("user_id", -1)
        if (usuarioId == -1) return

        val codigosRotacion = MockRepository.cultivosSeleccionados.map { nombre ->
            MockRepository.catalogoEcoCrop.find { it.name.equals(nombre, ignoreCase = true) }?.id ?: nombre.lowercase()
        }

        val peticion = PeticionAnalisis(
            usuario_id = usuarioId,
            lat = MockRepository.latFinca,
            lon = MockRepository.lonFinca,
            rotacion = codigosRotacion,
            prioridades = MockRepository.prioridadesSeleccionadas.toList()
        )

        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val respuesta = RetrofitClient.apiService.analizarRotacion(peticion)
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    val body = respuesta.body()
                    if (respuesta.isSuccessful && body != null) {
                        MockRepository.ultimoAnalisisNasa = body
                        cargarDatosUI()
                    } else {
                        Toast.makeText(
                            this@RotacionesActivity,
                            "Datos locales (Servidor: HTTP ${respuesta.code()})",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@RotacionesActivity,
                        "Datos locales sincronizados (Servidor: ${e.message})",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun cargarDatosUI() {
        // --- Tarjetas ambientales (solo si hay análisis de la NASA) ---
        val clima = MockRepository.ultimoAnalisisNasa?.clima
        if (clima != null) {
            val nivelHumedad = (clima.humedad_nivel ?: "").replaceFirstChar { it.uppercase() }
            findViewById<TextView>(R.id.tvSmapHumedad)?.text = "Humedad: $nivelHumedad"

            val tipoClima = (clima.clima_tipo ?: "").replaceFirstChar { it.uppercase() }
            findViewById<TextView>(R.id.tvNasaClima)?.text = "Clima: $tipoClima"
        }

        // --- Rotación Actual ---
        val opciones = opcionesActivas()
        val actual = opciones.find { it.actual } ?: opciones.getOrNull(0)
        if (actual != null) {
            findViewById<TextView>(R.id.tvTituloActual)?.text = actual.nombre
            findViewById<TextView>(R.id.tvPorqueActual)?.text = actual.porque
            findViewById<TextView>(R.id.tvBeneficioActual)?.text = actual.beneficios.firstOrNull() ?: "Mantiene manejo conocido"
            findViewById<TextView>(R.id.tvRiesgoActual)?.text = actual.riesgos.firstOrNull() ?: "Sin riesgos críticos"
            findViewById<TextView>(R.id.tvEvidenciaActual)?.text = actual.evidencia.firstOrNull()?.let { "${it.dato}: ${it.valor} ${it.unidad}" } ?: "NASA POWER: Estable"
            findViewById<TextView>(R.id.tvDatosFaltantesActual)?.text = actual.vacios.firstOrNull() ?: "Falta análisis de suelo"
        }

        // --- Alternativa 1 ---
        val alt1 = alternativa(0)
        if (alt1 != null) {
            findViewById<TextView>(R.id.tvTituloAlt1)?.text = alt1.nombre
            findViewById<TextView>(R.id.tvPorqueAlt1)?.text = alt1.porque
            findViewById<TextView>(R.id.tvBeneficioAlt1)?.text = alt1.beneficios.firstOrNull() ?: "Aporta beneficios al suelo"
            findViewById<TextView>(R.id.tvRiesgoAlt1)?.text = alt1.riesgos.firstOrNull() ?: "Revisar clima"
            findViewById<TextView>(R.id.tvEvidenciaAlt1)?.text = alt1.evidencia.firstOrNull()?.let { "${it.dato}: ${it.valor} ${it.unidad}" } ?: "NASA POWER: Favorable"
            findViewById<TextView>(R.id.tvDatosFaltantesAlt1)?.text = alt1.vacios.firstOrNull() ?: "Falta precio de mercado"
        }

        // --- Alternativa 2 ---
        val alt2 = alternativa(1)
        if (alt2 != null) {
            findViewById<TextView>(R.id.tvTituloAlt2)?.text = alt2.nombre
            findViewById<TextView>(R.id.tvPorqueAlt2)?.text = alt2.porque
            findViewById<TextView>(R.id.tvBeneficioAlt2)?.text = alt2.beneficios.firstOrNull() ?: "Buena diversificación"
            findViewById<TextView>(R.id.tvRiesgoAlt2)?.text = alt2.riesgos.firstOrNull() ?: "Requiere rotación"
            findViewById<TextView>(R.id.tvEvidenciaAlt2)?.text = alt2.evidencia.firstOrNull()?.let { "${it.dato}: ${it.valor} ${it.unidad}" } ?: "NASA POWER: Adecuado"
            findViewById<TextView>(R.id.tvDatosFaltantesAlt2)?.text = alt2.vacios.firstOrNull() ?: "Falta historial local"
        }
    }

    private fun configurarInteracciones() {
        // Los IDs se resuelven en el clic para usar siempre el último análisis recibido
        val abrirActual = {
            val rot = opcionesActivas().find { it.actual } ?: opcionesActivas().first()
            abrirDetalle(rot.id)
        }
        val abrirAlt1 = { abrirDetalle(alternativa(0)?.id ?: "opcion_1") }
        val abrirAlt2 = { abrirDetalle(alternativa(1)?.id ?: "opcion_2") }

        findViewById<MaterialCardView>(R.id.cardRotacionActual)?.setOnClickListener { abrirActual() }

        findViewById<TextView>(R.id.btnVerDetalle1)?.setOnClickListener { abrirAlt1() }
        findViewById<MaterialCardView>(R.id.cardAlternativa1)?.setOnClickListener { abrirAlt1() }

        findViewById<TextView>(R.id.btnVerDetalle2)?.setOnClickListener { abrirAlt2() }
        findViewById<MaterialCardView>(R.id.cardAlternativa2)?.setOnClickListener { abrirAlt2() }
    }

    private fun abrirDetalle(rotacionId: String) {
        val intent = Intent(this, DetalleRotacionActivity::class.java).apply {
            putExtra(DetalleRotacionActivity.EXTRA_ROTACION_ID, rotacionId)
        }
        startActivity(intent)
    }

    private fun configurarNavegacion() {
        findViewById<LinearLayout>(R.id.navInicio)?.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        findViewById<LinearLayout>(R.id.navMiFinca)?.setOnClickListener {
            startActivity(Intent(this, MiFincaActivity::class.java))
            finish()
        }
        findViewById<LinearLayout>(R.id.navRotaciones)?.setOnClickListener {
            // Ya estamos aquí
        }
    }
}
