package com.example.fieldshift

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
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

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Ajuste para bordes de pantalla
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        cargarDatosInicio()
        cargarActividadesRecientes() // Llamada al API al abrir la app
        configurarNavegacion()
    }

    override fun onResume() {
        super.onResume()
        // Se ejecuta al regresar de otras pantallas para refrescar los datos
        cargarDatosInicio()
        cargarActividadesRecientes() // Refresca las actividades si vuelves del formulario
    }

    private fun cargarDatosInicio() {
        val prefs = getSharedPreferences("TerraPrefs", Context.MODE_PRIVATE)
        val nombre = prefs.getString("user_nombre", MockRepository.nombreUsuario)
        val hectareas = prefs.getString("user_hectareas", MockRepository.hectareasFinca.toString())

        findViewById<TextView>(R.id.tvSaludo).text = "Hola, $nombre"
        findViewById<TextView>(R.id.tvParcelasValor).text = "4"
        findViewById<TextView>(R.id.tvParcelasSub).text = "$hectareas ha en total"

        findViewById<TextView>(R.id.tvHumedadValor).text = "62"
        findViewById<TextView>(R.id.tvHumedadFuente).text = "SMAP · índice /100"

        findViewById<TextView>(R.id.tvLluviaValor).text = "−12%"
        findViewById<TextView>(R.id.tvLluviaSub).text = "NASA POWER"

        // Texto de respaldo inicial
        val secuenciaActual = MockRepository.cultivosSeleccionados.joinToString(separator = " → ")
        findViewById<TextView>(R.id.tvActividadRotacion)?.text = "Parcela Norte · $secuenciaActual"
    }

    // ========================================================
    // Peticion GET Actividades
    // ========================================================
    private fun cargarActividadesRecientes() {
        val prefs = getSharedPreferences("TerraPrefs", Context.MODE_PRIVATE)
        // Tomamos el usuario_id (si no hay, usamos 1 por defecto para la prueba)
        val usuarioId = prefs.getInt("user_id", 1)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Hacemos el GET pasando los datos
                val respuesta = RetrofitClient.apiService.obtenerActividad(
                    usuarioId = usuarioId,
                    lat = 4.60,
                    lon = -74.08
                )

                withContext(Dispatchers.Main) {
                    if (respuesta.isSuccessful && respuesta.body() != null) {
                        val listaActividades = respuesta.body()!!.actividad

                        if (listaActividades.isNotEmpty()) {
                            // Mensajito negro flotante confirmando que conectó
//                            Toast.makeText(this@MainActivity, "Actividades descargadas: ${listaActividades.size}", Toast.LENGTH_SHORT).show()

                            // Extraemos la primera actividad (Ej: "Análisis de rotaciones realizado")
                            val primeraActividad = listaActividades[0]

                            // Pintamos el dato real del servidor en tu tarjeta de abajo
                            findViewById<TextView>(R.id.tvActividadRotacion)?.text = "${primeraActividad.titulo}: ${primeraActividad.detalle}"
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Si falla el internet o el servidor, no explota, solo avisa
                    Toast.makeText(this@MainActivity, "Sin conexión para actividades", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun configurarNavegacion() {
        // Navegación de la barra inferior
        findViewById<LinearLayout>(R.id.navInicio)?.setOnClickListener {
            // Ya estamos aquí
        }
        findViewById<LinearLayout>(R.id.navRotaciones)?.setOnClickListener {
            startActivity(Intent(this, RotacionesActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navMiFinca)?.setOnClickListener {
            startActivity(Intent(this, MiFincaActivity::class.java))
        }

        // Navegación de las tarjetas principales
        findViewById<MaterialCardView>(R.id.cardConfigurarFinca)?.setOnClickListener {
            startActivity(Intent(this, MiFincaActivity::class.java))
        }
        findViewById<MaterialCardView>(R.id.cardCalcularRotaciones)?.setOnClickListener {
            startActivity(Intent(this, RotacionesActivity::class.java))
        }
    }
}