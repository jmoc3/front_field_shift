package com.example.fieldshift

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.card.MaterialCardView

class RotacionesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_rotaciones)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        cargarDatos()
        configurarClicsTarjetas()
        configurarNavegacion()
    }

    private fun cargarDatos() {
        val analisisNasa = MockRepository.ultimoAnalisisNasa
        val opciones = analisisNasa?.opciones ?: MockRepository.rotacionesComparativa

        // --- Rotación Actual ---
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
        val alt1 = opciones.filter { !it.actual }.getOrNull(0)
        if (alt1 != null) {
            findViewById<TextView>(R.id.tvTituloAlt1)?.text = alt1.nombre
            findViewById<TextView>(R.id.tvPorqueAlt1)?.text = alt1.porque
            findViewById<TextView>(R.id.tvBeneficioAlt1)?.text = alt1.beneficios.firstOrNull() ?: "Aporta beneficios al suelo"
            findViewById<TextView>(R.id.tvRiesgoAlt1)?.text = alt1.riesgos.firstOrNull() ?: "Revisar clima"
            findViewById<TextView>(R.id.tvEvidenciaAlt1)?.text = alt1.evidencia.firstOrNull()?.let { "${it.dato}: ${it.valor} ${it.unidad}" } ?: "NASA POWER: Favorable"
            findViewById<TextView>(R.id.tvDatosFaltantesAlt1)?.text = alt1.vacios.firstOrNull() ?: "Falta precio de mercado"
        }

        // --- Alternativa 2 ---
        val alt2 = opciones.filter { !it.actual }.getOrNull(1)
        if (alt2 != null) {
            findViewById<TextView>(R.id.tvTituloAlt2)?.text = alt2.nombre
            findViewById<TextView>(R.id.tvPorqueAlt2)?.text = alt2.porque
            findViewById<TextView>(R.id.tvBeneficioAlt2)?.text = alt2.beneficios.firstOrNull() ?: "Buena diversificación"
            findViewById<TextView>(R.id.tvRiesgoAlt2)?.text = alt2.riesgos.firstOrNull() ?: "Requiere rotación"
            findViewById<TextView>(R.id.tvEvidenciaAlt2)?.text = alt2.evidencia.firstOrNull()?.let { "${it.dato}: ${it.valor} ${it.unidad}" } ?: "NASA POWER: Adecuado"
            findViewById<TextView>(R.id.tvDatosFaltantesAlt2)?.text = alt2.vacios.firstOrNull() ?: "Falta historial local"
        }
    }

    private fun configurarClicsTarjetas() {
        val analisisNasa = MockRepository.ultimoAnalisisNasa
        val opciones = analisisNasa?.opciones ?: MockRepository.rotacionesComparativa

        val idActual = opciones.find { it.actual }?.id ?: "actual"
        val idAlt1 = opciones.filter { !it.actual }.getOrNull(0)?.id ?: "opcion_1"
        val idAlt2 = opciones.filter { !it.actual }.getOrNull(1)?.id ?: "opcion_2"

        // Clic para abrir el detalle de cada tarjeta
        findViewById<MaterialCardView>(R.id.cardRotacionActual)?.setOnClickListener {
            val intent = Intent(this, DetalleRotacionActivity::class.java).apply {
                putExtra(DetalleRotacionActivity.EXTRA_ROTACION_ID, idActual)
            }
            startActivity(intent)
        }

        findViewById<MaterialCardView>(R.id.cardAlternativa1)?.setOnClickListener {
            val intent = Intent(this, DetalleRotacionActivity::class.java).apply {
                putExtra(DetalleRotacionActivity.EXTRA_ROTACION_ID, idAlt1)
            }
            startActivity(intent)
        }

        findViewById<MaterialCardView>(R.id.cardAlternativa2)?.setOnClickListener {
            val intent = Intent(this, DetalleRotacionActivity::class.java).apply {
                putExtra(DetalleRotacionActivity.EXTRA_ROTACION_ID, idAlt2)
            }
            startActivity(intent)
        }
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