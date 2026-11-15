package com.example.fieldshift

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
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

        findViewById<TextView>(R.id.tvSaludo).text = getString(R.string.saludo, nombre)

        // El backend no maneja parcelas: se muestra la superficie registrada
        findViewById<TextView>(R.id.tvParcelasValor).text = formatearNumero(MockRepository.hectareasFinca)
        findViewById<TextView>(R.id.tvParcelasSub).text = MockRepository.ubicacionFinca

        // Humedad del suelo en raíces (GWETROOT 0–1) del último análisis, como índice /100
        val humedad = MockRepository.ultimoAnalisisNasa?.clima?.humedad_suelo
        findViewById<TextView>(R.id.tvHumedadValor).text = humedad?.let { (it * 100).toInt().toString() } ?: "—"
        findViewById<TextView>(R.id.tvHumedadFuente).text = getString(R.string.inicio_humedad_fuente)

        findViewById<TextView>(R.id.tvLluviaSub).text = "NASA POWER"

        // Texto de respaldo mientras llega la actividad del servidor
        val secuenciaActual = MockRepository.cultivosSeleccionados.joinToString(" → ") { MockRepository.nombreCultivo(it) }
        findViewById<TextView>(R.id.tvActividadTitulo1)?.text = getString(R.string.rotacion_actual)
        findViewById<TextView>(R.id.tvActividadRotacion)?.text = secuenciaActual
        findViewById<TextView>(R.id.tvActividadFecha1)?.text = getString(R.string.act_tu_finca)
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
                    Toast.makeText(this@MainActivity, R.string.act_sin_conexion, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun mostrarDialogoPerfil() {
        val prefs = getSharedPreferences("TerraPrefs", Context.MODE_PRIVATE)
        val nombre = prefs.getString("user_nombre", null) ?: MockRepository.nombreUsuario
        val correo = prefs.getString("user_email", null).orEmpty()

        val dialogo = AlertDialog.Builder(this)
            .setTitle(nombre)
            .setMessage(listOf(correo, MockRepository.ubicacionFinca).filter { it.isNotBlank() }.joinToString("\n"))
            .setPositiveButton(R.string.cerrar_sesion) { _, _ -> cerrarSesion() }
            .setNegativeButton(R.string.cancelar, null)
            .setNeutralButton(R.string.idioma) { _, _ -> mostrarDialogoIdioma() }
            .create()
        dialogo.show()
        // Mismos colores que el diálogo de Mi Finca
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.parseColor("#B83B2A"))
        dialogo.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.parseColor("#7A766E"))
    }

    /** Lista de idiomas con el actual marcado; cada idioma se muestra en su propio nombre. */
    private fun mostrarDialogoIdioma() {
        val codigos = arrayOf("es", "en")
        AlertDialog.Builder(this)
            .setTitle(R.string.idioma)
            .setSingleChoiceItems(arrayOf("Español", "English"), codigos.indexOf(Idioma.actual())) { dialogo, i ->
                dialogo.dismiss()
                Idioma.cambiar(codigos[i])
            }
            .setNegativeButton(R.string.cancelar, null)
            .show()
            .getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.parseColor("#7A766E"))
    }

    /** Borra los datos locales del usuario y vuelve a Acceso sin dejar pantallas atrás. */
    private fun cerrarSesion() {
        getSharedPreferences("TerraPrefs", Context.MODE_PRIVATE).edit().clear().apply()
        MockRepository.reiniciarSesion()
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
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

        // "Lluvia 7 días": el backend lo envía como texto ("12 mm · 40 % menos que lo normal..." / "40 % less than normal")
        val lluvia = items.find { it.tipo == "lluvia" }?.detalle
        val cambio = lluvia?.let { Regex("(\\d+) % (más|menos|more|less)").find(it) }
        findViewById<TextView>(R.id.tvLluviaValor).text = when {
            cambio != null -> (if (cambio.groupValues[2] in setOf("más", "more")) "+" else "−") + cambio.groupValues[1] + "%"
            lluvia?.contains("igual a lo normal") == true || lluvia?.contains("same as normal") == true -> "0%"
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

    /** "2026-11-14" -> "14 nov", "2026-12" -> "dic 2026" (meses en el idioma de la app) */
    private fun formatearFecha(fecha: String?): String? {
        val partes = fecha?.split("-") ?: return null
        val meses = resources.getStringArray(R.array.meses_cortos)
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

        // Ícono de perfil: datos de la cuenta y cerrar sesión
        findViewById<MaterialCardView>(R.id.btnPerfilTop)?.setOnClickListener { mostrarDialogoPerfil() }

        // Navegación de las tarjetas principales
        findViewById<MaterialCardView>(R.id.cardConfigurarFinca)?.setOnClickListener {
            startActivity(Intent(this, MiFincaActivity::class.java))
        }
        findViewById<MaterialCardView>(R.id.cardCalcularRotaciones)?.setOnClickListener {
            startActivity(Intent(this, RotacionesActivity::class.java))
        }
    }
}