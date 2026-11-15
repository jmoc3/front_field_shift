package com.example.fieldshift

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.EditText
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

class MiFincaActivity : AppCompatActivity() {

    private lateinit var containerCultivos: LinearLayout
    private lateinit var etUbicacion: EditText
    private lateinit var etHectareas: EditText
    private lateinit var cultivosOriginales: List<String>
    private lateinit var btnActualizarNasa: MaterialCardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_mi_finca)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainMiFinca)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        containerCultivos = findViewById(R.id.containerCultivos)
        etUbicacion = findViewById(R.id.etUbicacion)
        etHectareas = findViewById(R.id.etHectareas)
        btnActualizarNasa = findViewById(R.id.btnActualizarNasa)

        etUbicacion.setText(MockRepository.ubicacionFinca)
        etHectareas.setText(formatearNumero(MockRepository.hectareasFinca))

        cultivosOriginales = MockRepository.cultivosSeleccionados.toList()

        renderizarSelectoresCultivos()
        configurarBotones()
        configurarChipsPrioridades()
        configurarNavegacion()
    }

    private fun renderizarSelectoresCultivos() {
        containerCultivos.removeAllViews()
        val densidad = resources.displayMetrics.density

        MockRepository.cultivosSeleccionados.forEachIndexed { index, nombreCultivo ->
            val card = MaterialCardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (52 * densidad).toInt()
                ).apply {
                    bottomMargin = (10 * densidad).toInt()
                }
                radius = 14f * densidad
                cardElevation = 0f
                setCardBackgroundColor(Color.WHITE)
                strokeColor = Color.parseColor("#E2DFD7")
                strokeWidth = (1 * densidad).toInt()
                isClickable = true
                isFocusable = true
            }

            val row = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                val padH = (16 * densidad).toInt()
                setPadding(padH, 0, padH, 0)
            }

            val tvLabel = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = "Cultivo ${index + 1}: $nombreCultivo"
                setTextColor(Color.parseColor("#171715"))
                textSize = 14f
            }

            val tvArrow = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                text = "▾"
                setTextColor(Color.parseColor("#171715"))
                textSize = 14f
            }

            row.addView(tvLabel)
            row.addView(tvArrow)
            card.addView(row)

            card.setOnClickListener { mostrarSelectorEcoCrop(index) }

            card.setOnLongClickListener {
                if (MockRepository.cultivosSeleccionados.size > 2) {
                    val eliminado = MockRepository.cultivosSeleccionados.removeAt(index)
                    descartarAnalisis()
                    renderizarSelectoresCultivos()
                    Toast.makeText(this, "Se quitó $eliminado", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Mínimo 2 cultivos", Toast.LENGTH_SHORT).show()
                }
                true
            }

            containerCultivos.addView(card)
        }
    }

    private fun mostrarSelectorEcoCrop(indiceEditar: Int?) {
        // El catálogo viene de GET api/v1/cultivos; si aún no está, se pide antes de abrir el selector
        if (MockRepository.catalogoEcoCrop.isEmpty()) {
            lifecycleScope.launch {
                if (withContext(Dispatchers.IO) { MockRepository.cargarCatalogo() }) {
                    mostrarSelectorEcoCrop(indiceEditar)
                } else {
                    Toast.makeText(this@MiFincaActivity, "No se pudo cargar el catálogo de cultivos", Toast.LENGTH_SHORT).show()
                }
            }
            return
        }

        val cultivoActual = if (indiceEditar != null) MockRepository.cultivosSeleccionados[indiceEditar] else null
        val opcionesMostradas = MockRepository.catalogoEcoCrop.map { if (it.name == cultivoActual) "✓  ${it.name}" else it.name }.toTypedArray()

        val builder = AlertDialog.Builder(this)
            .setTitle(if (indiceEditar != null) "Seleccionar Cultivo ${indiceEditar + 1}" else "Añadir cultivo")
            .setItems(opcionesMostradas) { _, which ->
                val elegido = MockRepository.catalogoEcoCrop[which].name
                if (indiceEditar != null) MockRepository.cultivosSeleccionados[indiceEditar] = elegido
                else MockRepository.cultivosSeleccionados.add(elegido)
                descartarAnalisis()
                renderizarSelectoresCultivos()
            }
            .setNegativeButton("Cancelar", null)

        if (indiceEditar != null && MockRepository.cultivosSeleccionados.size > 2) {
            builder.setNeutralButton("Quitar cultivo") { _, _ ->
                val eliminado = MockRepository.cultivosSeleccionados.removeAt(indiceEditar)
                descartarAnalisis()
                renderizarSelectoresCultivos()
                Toast.makeText(this, "Se quitó $eliminado", Toast.LENGTH_SHORT).show()
            }
        }

        val dialog = builder.create()
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.parseColor("#7A766E"))
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.setTextColor(Color.parseColor("#B83B2A"))
    }

    private fun configurarBotones() {
        findViewById<MaterialCardView>(R.id.btnAnadirCultivo).setOnClickListener { mostrarSelectorEcoCrop(null) }
        btnActualizarNasa.setOnClickListener { guardarYEnviarPeticion() }
    }

    private fun configurarChipsPrioridades() {
        val chipAgua = findViewById<MaterialCardView>(R.id.chipAgua)
        val tvChipAgua = findViewById<TextView>(R.id.tvChipAgua)
        val chipEconomia = findViewById<MaterialCardView>(R.id.chipEconomia)
        val tvChipEconomia = findViewById<TextView>(R.id.tvChipEconomia)
        val chipEquipos = findViewById<MaterialCardView>(R.id.chipEquipos)
        val tvChipEquipos = findViewById<TextView>(R.id.tvChipEquipos)

        fun actualizarVistaChips() {
            aplicarEstiloChip(chipAgua, tvChipAgua, "Ahorrar agua", MockRepository.prioridadesSeleccionadas.contains("agua"))
            aplicarEstiloChip(chipEconomia, tvChipEconomia, "Economía", MockRepository.prioridadesSeleccionadas.contains("economia"))
            aplicarEstiloChip(chipEquipos, tvChipEquipos, "Equipos", MockRepository.prioridadesSeleccionadas.contains("equipos"))
        }

        chipAgua.setOnClickListener { alternarPrioridad("agua"); actualizarVistaChips() }
        chipEconomia.setOnClickListener { alternarPrioridad("economia"); actualizarVistaChips() }
        chipEquipos.setOnClickListener { alternarPrioridad("equipos"); actualizarVistaChips() }

        actualizarVistaChips()
    }

    private fun alternarPrioridad(clave: String) {
        if (MockRepository.prioridadesSeleccionadas.contains(clave)) {
            if (MockRepository.prioridadesSeleccionadas.size > 1) MockRepository.prioridadesSeleccionadas.remove(clave)
        } else {
            MockRepository.prioridadesSeleccionadas.add(clave)
        }
        descartarAnalisis()
    }

    /** Cambiaron prioridades o cultivos: Rotaciones debe pedir un análisis nuevo. */
    private fun descartarAnalisis() {
        MockRepository.ultimoAnalisisNasa = null
    }

    private fun aplicarEstiloChip(card: MaterialCardView, tv: TextView, textoBase: String, seleccionado: Boolean) {
        val densidad = resources.displayMetrics.density
        if (seleccionado) {
            card.setCardBackgroundColor(Color.parseColor("#141413"))
            card.strokeWidth = 0
            tv.setTextColor(Color.WHITE)
            tv.text = "✓  $textoBase"
        } else {
            card.setCardBackgroundColor(Color.WHITE)
            card.strokeWidth = (1 * densidad).toInt()
            card.strokeColor = Color.parseColor("#E2DFD7")
            tv.setTextColor(Color.parseColor("#171715"))
            tv.text = textoBase
        }
    }

    private fun guardarYEnviarPeticion() {
        val ubicacion = etUbicacion.text.toString().trim().ifEmpty { "Piedecuesta, Santander" }
        val hectareasNum = etHectareas.text.toString().toDoubleOrNull()

        if (hectareasNum == null || hectareasNum <= 0) {
            etHectareas.error = "Ingresa una superficie válida"
            return
        }

        val prefs = getSharedPreferences("TerraPrefs", Context.MODE_PRIVATE)
        val usuarioId = prefs.getInt("user_id", -1)

        if (usuarioId == -1) {
            Toast.makeText(this, "Error: Inicia sesión nuevamente.", Toast.LENGTH_LONG).show()
            return
        }

        val todosLosCultivos = MockRepository.cultivosSeleccionados.toList()
        btnActualizarNasa.mostrarCargando(true, "Analizando con NASA…")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. PATCH: Actualizar Info Usuario (los cultivos viajan como ids del catálogo)
                val idsCultivos = MockRepository.idsCultivosSeleccionados()
                val peticionUser = UpdateUserRequest(usuarioId, ubicacion, hectareasNum, idsCultivos)
                val respuestaUser = RetrofitClient.apiService.updateUser(peticionUser)

                if (respuestaUser.isSuccessful && respuestaUser.body() != null) {
                    val datosServidor = respuestaUser.body()!!
                    MockRepository.ubicacionFinca = datosServidor.usuario.ubicacion
                    MockRepository.hectareasFinca = datosServidor.usuario.hectareas

                    prefs.edit()
                        .putString("user_ubicacion", datosServidor.usuario.ubicacion)
                        .putString("user_hectareas", datosServidor.usuario.hectareas.toString())
                        .putString("user_cultivos", todosLosCultivos.joinToString("||"))
                        .apply()
                    // Los datos cambiaron: si el análisis falla, Rotaciones lo volverá a pedir
                    MockRepository.ultimoAnalisisNasa = null

                    // 2. POST: Pedir Análisis NASA con las coordenadas de la ubicación escrita
                    MockRepository.actualizarCoordenadas(this@MiFincaActivity)
                    val peticionAnalisis = PeticionAnalisis(
                        usuario_id = usuarioId,
                        lat = MockRepository.latFinca,
                        lon = MockRepository.lonFinca,
                        rotacion = idsCultivos,
                        prioridades = MockRepository.prioridadesParaAnalisis()
                    )
                    val respuestaAnalisis = RetrofitClient.apiService.analizarRotacion(peticionAnalisis)

                    withContext(Dispatchers.Main) {
                        btnActualizarNasa.mostrarCargando(false, "")
                        if (respuestaAnalisis.isSuccessful && respuestaAnalisis.body() != null) {

                            // GUARDAMOS EL RESULTADO PARA QUE LO LEA LA SIGUIENTE PANTALLA
                            MockRepository.ultimoAnalisisNasa = respuestaAnalisis.body()

                            Toast.makeText(this@MiFincaActivity, "Análisis de NASA completado", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@MiFincaActivity, RotacionesActivity::class.java))
                            finish()
                        } else {
                            val errorServidor = respuestaAnalisis.errorBody()?.string() ?: "Desconocido"
                            Toast.makeText(this@MiFincaActivity, "Error análisis (${respuestaAnalisis.code()}): $errorServidor", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        btnActualizarNasa.mostrarCargando(false, "")
                        Toast.makeText(this@MiFincaActivity, "Error al actualizar perfil: ${respuestaUser.code()}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    btnActualizarNasa.mostrarCargando(false, "")
                    Toast.makeText(this@MiFincaActivity, "Error de red: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun configurarNavegacion() {
        findViewById<LinearLayout>(R.id.navInicio).setOnClickListener { finish() }
        findViewById<LinearLayout>(R.id.navRotaciones).setOnClickListener {
            startActivity(Intent(this, RotacionesActivity::class.java))
            finish()
        }
    }
}