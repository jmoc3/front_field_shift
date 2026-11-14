package com.example.fieldshift

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Patterns
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.card.MaterialCardView

class LoginActivity : AppCompatActivity() {

    private lateinit var etLoginEmail: EditText
    private lateinit var btnEntrar: MaterialCardView

    private lateinit var etRegNombre: EditText
    private lateinit var etRegEmail: EditText
    private lateinit var etRegUbicacion: EditText
    private lateinit var etRegHectareas: EditText
    private lateinit var spinnerCultivo1: Spinner
    private lateinit var spinnerCultivo2: Spinner
    private lateinit var layoutCultivosExtra: LinearLayout
    private lateinit var btnAnadirCultivo: MaterialCardView
    private lateinit var btnGuardarRegistro: MaterialCardView

    private data class CultivoExtraItem(
        val contenedorBloque: LinearLayout,
        val tvLabel: TextView,
        val spinner: Spinner
    )
    private val cultivosAdicionales = mutableListOf<CultivoExtraItem>()

    private val opcionesEcoCrop: List<String> by lazy {
        listOf("Seleccionar cultivo") + MockRepository.catalogoEcoCrop.map { it.name }
    }

    // Expresión regular: solo letras (incluyendo tildes y ñ) y espacios
    private val regexSoloLetras = Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]{2,}$")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainLogin)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        cargarSesionPreviaSiExiste()
        inicializarVistas()
        configurarSpinnersBase()
        configurarAcciones()
    }

    private fun cargarSesionPreviaSiExiste() {
        try {
            val prefs = getSharedPreferences("TerraPrefs", Context.MODE_PRIVATE)
            prefs.getString("user_nombre", null)?.let {
                if (it.isNotBlank()) MockRepository.nombreUsuario = it
            }
            prefs.getString("user_ubicacion", null)?.let {
                if (it.isNotBlank()) MockRepository.ubicacionFinca = it
            }

            val haGuardadas = prefs.getString("user_hectareas", null)
            haGuardadas?.toDoubleOrNull()?.toInt()?.let {
                MockRepository.hectareasFinca = it
            }

            prefs.getString("user_cultivos", null)?.let { guardados ->
                val lista = guardados.split("||").filter { it.isNotBlank() }
                if (lista.size >= 2) {
                    MockRepository.cultivosSeleccionados.clear()
                    MockRepository.cultivosSeleccionados.addAll(lista)
                }
            }
        } catch (e: Exception) {
            getSharedPreferences("TerraPrefs", Context.MODE_PRIVATE).edit().clear().apply()
        }
    }

    private fun inicializarVistas() {
        etLoginEmail = findViewById(R.id.etLoginEmail)
        btnEntrar = findViewById(R.id.btnEntrar)

        etRegNombre = findViewById(R.id.etRegNombre)
        etRegEmail = findViewById(R.id.etRegEmail)
        etRegUbicacion = findViewById(R.id.etRegUbicacion)
        etRegHectareas = findViewById(R.id.etRegHectareas)
        spinnerCultivo1 = findViewById(R.id.spinnerCultivo1)
        spinnerCultivo2 = findViewById(R.id.spinnerCultivo2)
        layoutCultivosExtra = findViewById(R.id.layoutCultivosExtra)
        btnAnadirCultivo = findViewById(R.id.btnAnadirCultivo)
        btnGuardarRegistro = findViewById(R.id.btnGuardarRegistro)
    }

    private fun crearAdaptadorEcoCrop(): ArrayAdapter<String> {
        return ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            opcionesEcoCrop
        )
    }

    private fun configurarSpinnersBase() {
        spinnerCultivo1.adapter = crearAdaptadorEcoCrop()
        spinnerCultivo2.adapter = crearAdaptadorEcoCrop()
    }

    private fun esCorreoValido(email: String): Boolean {
        return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun configurarAcciones() {
        // Acción 1: Ingresar con correo existente
        btnEntrar.setOnClickListener {
            val email = etLoginEmail.text.toString().trim()

            if (!esCorreoValido(email)) {
                etLoginEmail.error = "Ingresa un correo válido (ej: tu@correo.com)"
                etLoginEmail.requestFocus()
                return@setOnClickListener
            }

            val prefs = getSharedPreferences("TerraPrefs", Context.MODE_PRIVATE)
            val nombreGuardado = prefs.getString("user_nombre", null)

            if (nombreGuardado.isNullOrBlank()) {
                val nombrePorCorreo = email.substringBefore("@").replaceFirstChar { it.uppercase() }
                MockRepository.nombreUsuario = nombrePorCorreo
            }

            Toast.makeText(this, "Bienvenido de nuevo, ${MockRepository.nombreUsuario}", Toast.LENGTH_SHORT).show()
            irAMainActivity()
        }

        // Acción 2: Añadir cultivo extra
        btnAnadirCultivo.setOnClickListener {
            agregarSelectorCultivoExtra()
        }

        // Acción 3: Guardar registro con validaciones completas
        btnGuardarRegistro.setOnClickListener {
            val nombre = etRegNombre.text.toString().trim()
            val email = etRegEmail.text.toString().trim()
            val ubicacion = etRegUbicacion.text.toString().trim()
            val hectareasStr = etRegHectareas.text.toString().trim()

            // 1. Validar Nombre (solo letras, mínimo 2 caracteres, sin números)
            if (!regexSoloLetras.matches(nombre)) {
                etRegNombre.error = "Ingresa un nombre válido (solo letras)"
                etRegNombre.requestFocus()
                return@setOnClickListener
            }

            // 2. Validar Correo electrónico con formato real (@ y dominio)
            if (!esCorreoValido(email)) {
                etRegEmail.error = "Ingresa un correo válido (ej: tu@correo.com)"
                etRegEmail.requestFocus()
                return@setOnClickListener
            }

            // 3. Validar Ubicación (que tenga al menos 3 letras y no sea números como "123")
            val cantidadLetrasUbicacion = ubicacion.count { it.isLetter() }
            if (ubicacion.length < 3 || cantidadLetrasUbicacion < 3) {
                etRegUbicacion.error = "Ingresa un municipio y departamento válidos"
                etRegUbicacion.requestFocus()
                return@setOnClickListener
            }

            // 4. Validar Hectáreas (número mayor a 0)
            val hectareasNum = hectareasStr.toDoubleOrNull()
            if (hectareasNum == null || hectareasNum <= 0) {
                etRegHectareas.error = "Ingresa una superficie mayor a 0 ha"
                etRegHectareas.requestFocus()
                return@setOnClickListener
            }

            // 5. Validar Cultivos base
            if (spinnerCultivo1.selectedItemPosition == 0 || spinnerCultivo2.selectedItemPosition == 0) {
                Toast.makeText(this, "Selecciona al menos el Cultivo 1 y Cultivo 2 de tu rotación", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 6. Validar Cultivos adicionales
            for ((index, item) in cultivosAdicionales.withIndex()) {
                if (item.spinner.selectedItemPosition == 0) {
                    val num = index + 3
                    Toast.makeText(this, "Selecciona el Cultivo $num o elimínalo si no lo necesitas", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            val hectareasInt = hectareasNum.toInt().coerceAtLeast(1)

            val todosLosCultivos = mutableListOf<String>()
            todosLosCultivos.add(spinnerCultivo1.selectedItem.toString())
            todosLosCultivos.add(spinnerCultivo2.selectedItem.toString())
            cultivosAdicionales.forEach {
                todosLosCultivos.add(it.spinner.selectedItem.toString())
            }

            MockRepository.nombreUsuario = nombre
            MockRepository.ubicacionFinca = ubicacion
            MockRepository.hectareasFinca = hectareasInt
            MockRepository.cultivosSeleccionados.clear()
            MockRepository.cultivosSeleccionados.addAll(todosLosCultivos)

            val prefs = getSharedPreferences("TerraPrefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("user_nombre", nombre)
                .putString("user_email", email)
                .putString("user_ubicacion", ubicacion)
                .putString("user_hectareas", hectareasInt.toString())
                .putString("user_cultivos", todosLosCultivos.joinToString("||"))
                .apply()

            Toast.makeText(this, "Finca registrada con éxito para $nombre", Toast.LENGTH_SHORT).show()
            irAMainActivity()
        }
    }

    private fun agregarSelectorCultivoExtra() {
        val densidad = resources.displayMetrics.density
        val numeroInicial = 3 + cultivosAdicionales.size

        val bloqueCompleto = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (16 * densidad).toInt()
            }
        }

        val filaCabecera = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (8 * densidad).toInt()
            }
        }

        val tvLabel = TextView(this).apply {
            text = "Cultivo $numeroInicial"
            setTextColor(Color.parseColor("#5C5953"))
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val tvEliminar = TextView(this).apply {
            text = "✕ Quitar"
            setTextColor(Color.parseColor("#B34038"))
            textSize = 12f
            setTypeface(typeface, Typeface.BOLD)
            setPadding((8 * densidad).toInt(), (2 * densidad).toInt(), (4 * densidad).toInt(), (2 * densidad).toInt())
        }

        filaCabecera.addView(tvLabel)
        filaCabecera.addView(tvEliminar)

        val contenedorSpinner = FrameLayout(this).apply {
            setBackgroundResource(R.drawable.bg_input_rounded)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (50 * densidad).toInt()
            )
        }

        val nuevoSpinner = Spinner(this).apply {
            background = null
            setPadding((12 * densidad).toInt(), 0, (36 * densidad).toInt(), 0)
            adapter = crearAdaptadorEcoCrop()
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        val iconoFlecha = TextView(this).apply {
            text = "▾"
            setTextColor(Color.parseColor("#171715"))
            textSize = 14f
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.END or Gravity.CENTER_VERTICAL
            ).apply {
                marginEnd = (16 * densidad).toInt()
            }
        }

        contenedorSpinner.addView(nuevoSpinner)
        contenedorSpinner.addView(iconoFlecha)

        bloqueCompleto.addView(filaCabecera)
        bloqueCompleto.addView(contenedorSpinner)

        val itemNuevo = CultivoExtraItem(bloqueCompleto, tvLabel, nuevoSpinner)
        cultivosAdicionales.add(itemNuevo)
        layoutCultivosExtra.addView(bloqueCompleto)

        tvEliminar.setOnClickListener {
            layoutCultivosExtra.removeView(bloqueCompleto)
            cultivosAdicionales.remove(itemNuevo)
            reenumerarCultivosAdicionales()
        }
    }

    private fun reenumerarCultivosAdicionales() {
        cultivosAdicionales.forEachIndexed { index, item ->
            val numeroReal = 3 + index
            item.tvLabel.text = "Cultivo $numeroReal"
        }
    }

    private fun irAMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}