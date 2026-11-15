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
        // Si el catálogo falta o está en otro idioma, se recarga y se repintan los nombres
        lifecycleScope.launch {
            if (withContext(Dispatchers.IO) { MockRepository.cargarCatalogo() }) renderizarSelectoresCultivos()
        }
        configurarBotones()
        configurarChipsPrioridades()
        configurarNavegacion()
    }

    private fun renderizarSelectoresCultivos() {
        containerCultivos.removeAllViews()
        val densidad = resources.displayMetrics.density

        MockRepository.cultivosSeleccionados.forEachIndexed { index, idCultivo ->
            val nombreCultivo = MockRepository.nombreCultivo(idCultivo)
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
                text = getString(R.string.cultivo_n_nombre, index + 1, nombreCultivo)
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
                    Toast.makeText(this, getString(R.string.se_quito, MockRepository.nombreCultivo(eliminado)), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, R.string.minimo_cultivos, Toast.LENGTH_SHORT).show()
                }
                true
            }

            containerCultivos.addView(card)
        }
    }

    private fun mostrarSelectorEcoCrop(indiceEditar: Int?) {
        // El catálogo viene de GET api/v1/cultivos (en el idioma actual); se pide si aún no está
        lifecycleScope.launch {
            if (!withContext(Dispatchers.IO) { MockRepository.cargarCatalogo() }) {
                Toast.makeText(this@MiFincaActivity, R.string.error_catalogo, Toast.LENGTH_SHORT).show()
                return@launch
            }
            abrirSelectorEcoCrop(indiceEditar)
        }
    }

    private fun abrirSelectorEcoCrop(indiceEditar: Int?) {
        val cultivoActual = indiceEditar?.let { MockRepository.idCultivo(MockRepository.cultivosSeleccionados[it]) }
        val opcionesMostradas = MockRepository.catalogoEcoCrop.map { if (it.id == cultivoActual) "✓  ${it.name}" else it.name }.toTypedArray()

        val builder = AlertDialog.Builder(this)
            .setTitle(if (indiceEditar != null) getString(R.string.seleccionar_cultivo_n, indiceEditar + 1) else getString(R.string.anadir_cultivo_titulo))
            .setItems(opcionesMostradas) { _, which ->
                val elegido = MockRepository.catalogoEcoCrop[which].id
                if (indiceEditar != null) MockRepository.cultivosSeleccionados[indiceEditar] = elegido
                else MockRepository.cultivosSeleccionados.add(elegido)
                descartarAnalisis()
                renderizarSelectoresCultivos()
            }
            .setNegativeButton(R.string.cancelar, null)

        if (indiceEditar != null && MockRepository.cultivosSeleccionados.size > 2) {
            builder.setNeutralButton(R.string.quitar_cultivo) { _, _ ->
                val eliminado = MockRepository.cultivosSeleccionados.removeAt(indiceEditar)
                descartarAnalisis()
                renderizarSelectoresCultivos()
                Toast.makeText(this, getString(R.string.se_quito, MockRepository.nombreCultivo(eliminado)), Toast.LENGTH_SHORT).show()
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
            aplicarEstiloChip(chipAgua, tvChipAgua, getString(R.string.chip_agua), MockRepository.prioridadesSeleccionadas.contains("agua"))
            aplicarEstiloChip(chipEconomia, tvChipEconomia, getString(R.string.chip_economia), MockRepository.prioridadesSeleccionadas.contains("economia"))
            aplicarEstiloChip(chipEquipos, tvChipEquipos, getString(R.string.chip_equipos), MockRepository.prioridadesSeleccionadas.contains("equipos"))
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
            etHectareas.error = getString(R.string.error_superficie)
            return
        }

        val prefs = getSharedPreferences("TerraPrefs", Context.MODE_PRIVATE)
        val usuarioId = prefs.getInt("user_id", -1)

        if (usuarioId == -1) {
            Toast.makeText(this, R.string.error_sesion, Toast.LENGTH_LONG).show()
            return
        }

        val todosLosCultivos = MockRepository.idsCultivosSeleccionados()
        btnActualizarNasa.mostrarCargando(true, getString(R.string.cargando_analisis))

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

                            Toast.makeText(this@MiFincaActivity, R.string.analisis_ok, Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@MiFincaActivity, RotacionesActivity::class.java))
                            finish()
                        } else {
                            val errorServidor = respuestaAnalisis.errorBody()?.string() ?: getString(R.string.desconocido)
                            Toast.makeText(this@MiFincaActivity, getString(R.string.error_analisis, respuestaAnalisis.code(), errorServidor), Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        btnActualizarNasa.mostrarCargando(false, "")
                        Toast.makeText(this@MiFincaActivity, getString(R.string.error_perfil, respuestaUser.code()), Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    btnActualizarNasa.mostrarCargando(false, "")
                    Toast.makeText(this@MiFincaActivity, getString(R.string.error_red, e.message), Toast.LENGTH_LONG).show()
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