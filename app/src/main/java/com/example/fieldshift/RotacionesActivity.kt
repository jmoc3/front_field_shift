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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainRotaciones)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        cargarDatos()
        configurarInteracciones()
        configurarNavegacion()
    }

    private fun cargarDatos() {
        val rotaciones = MockRepository.rotacionesComparativa

        // Rotación Actual
        val actual = rotaciones.getOrNull(0)
        if (actual != null) {
            findViewById<TextView>(R.id.tvTituloActual).text = actual.nombre
            findViewById<TextView>(R.id.tvPorqueActual).text = actual.porque
            findViewById<TextView>(R.id.tvBeneficioActual).text = actual.beneficioTexto
            findViewById<TextView>(R.id.tvRiesgoActual).text = actual.riesgoTexto
            findViewById<TextView>(R.id.tvEvidenciaActual).text = actual.evidenciaNasaTexto
            findViewById<TextView>(R.id.tvDatosFaltantesActual).text = actual.datosFaltantesTexto
        }

        // Alternativa 1
        val alt1 = rotaciones.getOrNull(1)
        if (alt1 != null) {
            findViewById<TextView>(R.id.tvTituloAlt1).text = alt1.nombre
            findViewById<TextView>(R.id.tvPorqueAlt1).text = alt1.porque
            findViewById<TextView>(R.id.tvBeneficioAlt1).text = alt1.beneficioTexto
            findViewById<TextView>(R.id.tvRiesgoAlt1).text = alt1.riesgoTexto
            findViewById<TextView>(R.id.tvEvidenciaAlt1).text = alt1.evidenciaNasaTexto
            findViewById<TextView>(R.id.tvDatosFaltantesAlt1).text = alt1.datosFaltantesTexto
        }

        // Alternativa 2
        val alt2 = rotaciones.getOrNull(2)
        if (alt2 != null) {
            findViewById<TextView>(R.id.tvTituloAlt2).text = alt2.nombre
            findViewById<TextView>(R.id.tvPorqueAlt2).text = alt2.porque
            findViewById<TextView>(R.id.tvBeneficioAlt2).text = alt2.beneficioTexto
            findViewById<TextView>(R.id.tvRiesgoAlt2).text = alt2.riesgoTexto
            findViewById<TextView>(R.id.tvEvidenciaAlt2).text = alt2.evidenciaNasaTexto
            findViewById<TextView>(R.id.tvDatosFaltantesAlt2).text = alt2.datosFaltantesTexto
        }
    }

    private fun abrirDetalle(rotacionId: String) {
        val intent = Intent(this, DetalleRotacionActivity::class.java).apply {
            putExtra(DetalleRotacionActivity.EXTRA_ROTACION_ID, rotacionId)
        }
        startActivity(intent)
    }

    private fun configurarInteracciones() {
        // Clics a Detalle de Alternativa 1
        findViewById<TextView>(R.id.btnVerDetalle1).setOnClickListener {
            abrirDetalle("alternativa_1")
        }
        findViewById<MaterialCardView>(R.id.cardAlternativa1).setOnClickListener {
            abrirDetalle("alternativa_1")
        }

        // Clics a Detalle de Alternativa 2
        findViewById<TextView>(R.id.btnVerDetalle2).setOnClickListener {
            abrirDetalle("alternativa_2")
        }
        findViewById<MaterialCardView>(R.id.cardAlternativa2).setOnClickListener {
            abrirDetalle("alternativa_2")
        }

        // Clic a Detalle de Rotación Actual
        findViewById<MaterialCardView>(R.id.cardRotacionActual).setOnClickListener {
            abrirDetalle("actual")
        }
    }

    private fun configurarNavegacion() {
        findViewById<LinearLayout>(R.id.navInicio).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }

        findViewById<LinearLayout>(R.id.navMiFinca).setOnClickListener {
            startActivity(Intent(this, MiFincaActivity::class.java))
        }
    }
}
