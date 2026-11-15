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

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        cargarDatosInicio()
        configurarNavegacion()
    }

    override fun onResume() {
        super.onResume()
        // Se ejecuta automáticamente al regresar de "Mi finca" o "Rotaciones" para refrescar los cambios
        cargarDatosInicio()
    }

    private fun cargarDatosInicio() {
        // Datos sincronizados con la respuesta activa del backend
        val respuesta = MockRepository.respuestaActiva
        val clima = respuesta.clima

        findViewById<TextView>(R.id.tvSaludo).text = "Hola, ${MockRepository.nombreUsuario}"
        findViewById<TextView>(R.id.tvParcelasValor).text = MockRepository.parcelasFinca.toString()
        findViewById<TextView>(R.id.tvParcelasSub).text = "${MockRepository.hectareasFinca} ha en total"

        val indiceSuelo = (clima.humedad_suelo * 100).toInt()
        findViewById<TextView>(R.id.tvHumedadValor).text = indiceSuelo.toString()
        findViewById<TextView>(R.id.tvHumedadFuente).text = "SMAP · índice /100"

        findViewById<TextView>(R.id.tvLluviaValor).text = "−12%"
        findViewById<TextView>(R.id.tvLluviaSub).text = clima.fuente

        val secuenciaActual = MockRepository.cultivosSeleccionados.joinToString(" → ")
        findViewById<TextView>(R.id.tvActividadRotacion).text =
            "Parcela Norte · $secuenciaActual"
    }

    private fun configurarNavegacion() {
        val irAMiFinca = {
            startActivity(Intent(this, MiFincaActivity::class.java))
        }

        findViewById<MaterialCardView>(R.id.cardConfigurarFinca).setOnClickListener { irAMiFinca() }
        findViewById<LinearLayout>(R.id.navMiFinca).setOnClickListener { irAMiFinca() }
        findViewById<MaterialCardView>(R.id.btnPerfilTop).setOnClickListener { irAMiFinca() }

        val irARotaciones = {
            startActivity(Intent(this, RotacionesActivity::class.java))
        }

        findViewById<MaterialCardView>(R.id.cardCalcularRotaciones).setOnClickListener { irARotaciones() }
        findViewById<LinearLayout>(R.id.navRotaciones).setOnClickListener { irARotaciones() }
    }
}
