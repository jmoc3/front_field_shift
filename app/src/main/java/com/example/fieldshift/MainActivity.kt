package com.example.fieldshift

import android.content.Context
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

        // Las filas 2 y 3 y la lluvia del diseño son de ejemplo: se ocultan hasta tener datos reales
        findViewById<View>(R.id.filaActividad2).visibility = View.GONE
        findViewById<View>(R.id.filaActividad3).visibility = View.GONE
        findViewById<TextView>(R.id.tvLluviaValor).text = "—"

        cargarDatosInicio()
        configurarNavegacion()
        // onResume también se ejecuta al abrir: ahí se piden las actividades
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

        findViewById<TextView>(R.id.tvSaludo).text = "Hola, $nombre"

        // El backend no maneja parcelas: se muestra la superficie registrada
        findViewById<TextView>(R.id.tvParcelasValor).text = formatearNumero(MockRepository.hectareasFinca)
        findViewById<TextView>(R.id.tvParcelasSub).text = MockRepository.ubicacionFinca

        // Humedad del suelo en raíces (GWETROOT 0–1) del último análisis, como índice /100
        val humedad = MockRepository.ultimoAnalisisNasa?.clima?.humedad_suelo
        findViewById<TextView>(R.id.tvHumedadValor).text = humedad?.let { (it * 100).toInt().toString() } ?: "—"
        findViewById<TextView>(R.id.tvHumedadFuente).text = "NASA POWER · índice /100"

        findViewById<TextView>(R.id.tvLluviaSub).text = "NASA POWER"

        // Texto de respaldo mientras llega la actividad del servidor
        val secuenciaActual = MockRepository.cultivosSeleccionados.joinToString(separator = " → ")
        findViewById<TextView>(R.id.tvActividadTitulo1)?.text = "Rotación actual"
        findViewById<TextView>(R.id.tvActividadRotacion)?.text = secuenciaActual
        findViewById<TextView>(R.id.tvActividadFecha1)?.text = "Tu finca"
    }

    // ========================================================
    // Peticion GET Actividades
    // ========================================================
    private fun cargarActividadesRecientes() {
        val prefs = getSharedPreferences("TerraPrefs", Context.MODE_PRIVATE)
        val usuarioId = prefs.getInt("user_id", -1)
        if (usuarioId == -1) {
            mostrarActividades(emptyList())
            return
        }

        // Mientras carga se ocultan las filas para no mostrar la actividad anterior
        val pbActividad = findViewById<View>(R.id.pbActividad)
        pbActividad.visibility = View.VISIBLE
        filasActividad.forEach { findViewById<View>(it[0]).visibility = View.GONE }
        findViewById<TextView>(R.id.tvLluviaValor).text = "…"

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                MockRepository.actualizarCoordenadas(this@MainActivity)
                val respuesta = RetrofitClient.apiService.obtenerActividad(
                    usuarioId = usuarioId,
                    lat = MockRepository.latFinca,
                    lon = MockRepository.lonFinca
                )

                withContext(Dispatchers.Main) {
                    pbActividad.visibility = View.GONE
                    mostrarActividades(respuesta.body()?.actividad.orEmpty())
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    pbActividad.visibility = View.GONE
                    mostrarActividades(emptyList())
                    // Si falla el internet o el servidor, no explota, solo avisa
                    Toast.makeText(this@MainActivity, "Sin conexión para actividades", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Filas de "Actividad reciente": contenedor, ícono, título, detalle y fecha
    private val filasActividad = listOf(
        intArrayOf(R.id.filaActividad1, R.id.ivActividad1, R.id.tvActividadTitulo1, R.id.tvActividadRotacion, R.id.tvActividadFecha1),
        intArrayOf(R.id.filaActividad2, R.id.ivActividad2, R.id.tvActividadTitulo2, R.id.tvActividadLluvia, R.id.tvActividadFecha2),
        intArrayOf(R.id.filaActividad3, R.id.ivActividad3, R.id.tvActividadTitulo3, R.id.tvActividadSuelo, R.id.tvActividadFecha3)
    )

    /** Pinta hasta 3 actividades; sin datos, la primera fila conserva la rotación actual. */
    private fun mostrarActividades(items: List<ActividadItem>) {
        filasActividad.forEachIndexed { i, ids ->
            val item = items.getOrNull(i)
            findViewById<View>(ids[0]).visibility = if (item != null || i == 0) View.VISIBLE else View.GONE
            if (item == null) return@forEachIndexed

            findViewById<ImageView>(ids[1]).setImageResource(iconoActividad(item.tipo))
            findViewById<TextView>(ids[2]).text = item.titulo
            findViewById<TextView>(ids[3]).text = item.detalle
            findViewById<TextView>(ids[4]).text = formatearFecha(item.fecha) ?: item.fuente
        }

        // "Lluvia 7 días": el backend lo envía como texto ("12 mm · 40 % menos que lo normal...")
        val lluvia = items.find { it.tipo == "lluvia" }?.detalle
        val cambio = lluvia?.let { Regex("(\\d+) % (más|menos)").find(it) }
        findViewById<TextView>(R.id.tvLluviaValor).text = when {
            cambio != null -> (if (cambio.groupValues[2] == "más") "+" else "−") + cambio.groupValues[1] + "%"
            lluvia?.contains("igual a lo normal") == true -> "0%"
            else -> "—"
        }
    }

    private fun iconoActividad(tipo: String): Int = when (tipo) {
        "analisis", "rotacion" -> R.drawable.ic_chart
        "lluvia", "temporada" -> R.drawable.ic_cloud_rain
        "humedad" -> R.drawable.ic_water_drop
        "helada" -> R.drawable.ic_alert_circle
        "calor" -> R.drawable.ic_sun
        else -> R.drawable.ic_check
    }

    /** "2026-11-14" -> "14 nov", "2026-12" -> "dic 2026" */
    private fun formatearFecha(fecha: String?): String? {
        val partes = fecha?.split("-") ?: return null
        val meses = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
        val mes = partes.getOrNull(1)?.toIntOrNull()?.let { meses.getOrNull(it - 1) } ?: return fecha
        return if (partes.size >= 3) "${partes[2].toIntOrNull() ?: partes[2]} $mes" else "$mes ${partes[0]}"
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