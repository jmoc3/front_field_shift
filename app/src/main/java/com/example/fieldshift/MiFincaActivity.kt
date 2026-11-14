package com.example.fieldshift

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
import com.google.android.material.card.MaterialCardView

class MiFincaActivity : AppCompatActivity() {

    private lateinit var containerCultivos: LinearLayout
    private lateinit var etUbicacion: EditText
    private lateinit var etHectareas: EditText
    private lateinit var cultivosOriginales: List<String>

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

        etUbicacion.setText(MockRepository.ubicacionFinca)
        etHectareas.setText(MockRepository.hectareasFinca.toString())

        // Guardamos copia para comparar si la rotación fue alterada al guardar
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
                radius = 14 * densidad
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

            // Toque normal: abre el catálogo EcoCrop
            card.setOnClickListener {
                mostrarSelectorEcoCrop(index)
            }

            // Toque largo: elimina rápido si hay más de 2 cultivos
            card.setOnLongClickListener {
                if (MockRepository.cultivosSeleccionados.size > 2) {
                    val eliminado = MockRepository.cultivosSeleccionados.removeAt(index)
                    renderizarSelectoresCultivos()
                    Toast.makeText(this, "Se quitó $eliminado de la rotación", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Debes mantener mínimo 2 cultivos en la rotación", Toast.LENGTH_SHORT).show()
                }
                true
            }

            containerCultivos.addView(card)
        }
    }

    private fun mostrarSelectorEcoCrop(indiceEditar: Int?) {
        // TODO: Descomentar GET "api/v1/cultivos" cuando el backend esté desplegado
        val cultivoActual = if (indiceEditar != null) MockRepository.cultivosSeleccionados[indiceEditar] else null

        val opcionesMostradas = MockRepository.catalogoEcoCrop.map { item ->
            if (item.name == cultivoActual) "✓  ${item.name}" else item.name
        }.toTypedArray()

        val builder = AlertDialog.Builder(this)
            .setTitle(if (indiceEditar != null) "Seleccionar Cultivo ${indiceEditar + 1}" else "Añadir cultivo EcoCrop")
            .setItems(opcionesMostradas) { _, which ->
                val elegido = MockRepository.catalogoEcoCrop[which].name
                if (indiceEditar != null) {
                    MockRepository.cultivosSeleccionados[indiceEditar] = elegido
                } else {
                    MockRepository.cultivosSeleccionados.add(elegido)
                }
                renderizarSelectoresCultivos()
            }
            .setNegativeButton("Cancelar", null)

        // Botón de acción separado en el pie del diálogo
        if (indiceEditar != null && MockRepository.cultivosSeleccionados.size > 2) {
            builder.setNeutralButton("Quitar cultivo") { _, _ ->
                val eliminado = MockRepository.cultivosSeleccionados.removeAt(indiceEditar)
                renderizarSelectoresCultivos()
                Toast.makeText(this, "Se quitó $eliminado de la rotación", Toast.LENGTH_SHORT).show()
            }
        }

        val dialog = builder.create()
        dialog.show()

        // Colores sobrios de la paleta TERRA para los botones del diálogo
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.parseColor("#7A766E"))
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.setTextColor(Color.parseColor("#B83B2A"))
    }

    private fun configurarBotones() {
        findViewById<MaterialCardView>(R.id.btnAnadirCultivo).setOnClickListener {
            mostrarSelectorEcoCrop(null)
        }

        findViewById<MaterialCardView>(R.id.btnActualizarNasa).setOnClickListener {
            guardarYEnviarPeticion()
        }
    }

    private fun configurarChipsPrioridades() {
        val chipAgua = findViewById<MaterialCardView>(R.id.chipAgua)
        val tvChipAgua = findViewById<TextView>(R.id.tvChipAgua)

        val chipEconomia = findViewById<MaterialCardView>(R.id.chipEconomia)
        val tvChipEconomia = findViewById<TextView>(R.id.tvChipEconomia)

        val chipEquipos = findViewById<MaterialCardView>(R.id.chipEquipos)
        val tvChipEquipos = findViewById<TextView>(R.id.tvChipEquipos)

        fun actualizarVistaChips() {
            aplicarEstiloChip(
                chipAgua, tvChipAgua, "Ahorrar agua",
                MockRepository.prioridadesSeleccionadas.contains("agua")
            )
            aplicarEstiloChip(
                chipEconomia, tvChipEconomia, "Economía",
                MockRepository.prioridadesSeleccionadas.contains("economia")
            )
            aplicarEstiloChip(
                chipEquipos, tvChipEquipos, "Equipos",
                MockRepository.prioridadesSeleccionadas.contains("equipos")
            )
        }

        chipAgua.setOnClickListener {
            alternarPrioridad("agua")
            actualizarVistaChips()
        }
        chipEconomia.setOnClickListener {
            alternarPrioridad("economia")
            actualizarVistaChips()
        }
        chipEquipos.setOnClickListener {
            alternarPrioridad("equipos")
            actualizarVistaChips()
        }

        actualizarVistaChips()
    }

    private fun alternarPrioridad(clave: String) {
        if (MockRepository.prioridadesSeleccionadas.contains(clave)) {
            if (MockRepository.prioridadesSeleccionadas.size > 1) {
                MockRepository.prioridadesSeleccionadas.remove(clave)
            }
        } else {
            MockRepository.prioridadesSeleccionadas.add(clave)
        }
    }

    private fun aplicarEstiloChip(
        card: MaterialCardView,
        tv: TextView,
        textoBase: String,
        seleccionado: Boolean
    ) {
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
        val ubicacion = etUbicacion.text.toString().trim().ifEmpty { "Tunja, Boyacá" }
        val hectareas = etHectareas.text.toString().toIntOrNull() ?: 12

        MockRepository.ubicacionFinca = ubicacion
        MockRepository.hectareasFinca = hectareas

        // Construcción exacta del contrato peticion.json
        val codigosRotacion = MockRepository.cultivosSeleccionados.map { nombre ->
            MockRepository.catalogoEcoCrop.find { it.name == nombre }?.id ?: nombre.lowercase()
        }

        val peticion = PeticionAnalisis(
            lat = MockRepository.latFinca,
            lon = MockRepository.lonFinca,
            rotacion = codigosRotacion,
            prioridades = MockRepository.prioridadesSeleccionadas.toList()
        )

        // TODO: descomentar llamada POST "api/v1/analisis" enviando "peticion" cuando el backend este desplegado
        // val respuestaServidor = ApiClient.enviarAnalisis(peticion)

        val mensajeToast = if (MockRepository.cultivosSeleccionados != cultivosOriginales) {
            "Análisis actualizado: ${MockRepository.cultivosSeleccionados.joinToString(" → ")}"
        } else {
            "Configuración de la finca actualizada"
        }

        Toast.makeText(this, mensajeToast, Toast.LENGTH_SHORT).show()

        // Regresa a Inicio reflejando los datos actualizados
        finish()
    }

    private fun configurarNavegacion() {
        findViewById<LinearLayout>(R.id.navInicio).setOnClickListener {
            finish()
        }
        findViewById<LinearLayout>(R.id.navRotaciones).setOnClickListener {
            Toast.makeText(this, "Vista Rotaciones (rama de Kaito)", Toast.LENGTH_SHORT).show()
        }
    }
}