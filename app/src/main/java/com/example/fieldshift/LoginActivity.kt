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
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    private val opcionesEcoCrop: List<String>
        get() = listOf(getString(R.string.seleccionar_cultivo)) + MockRepository.catalogoEcoCrop.map { it.name }

    /** Id del cultivo elegido en un selector (la posición 0 es "Seleccionar cultivo"). */
    private fun idElegido(spinner: Spinner): String =
        MockRepository.catalogoEcoCrop[spinner.selectedItemPosition - 1].id

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
        configurarSelectorIdioma()
        configurarSpinnersBase()
        configurarAcciones()
        cargarCatalogoCultivos()
    }

    /** "ES | EN": el idioma activo relleno en negro; tocar el otro cambia la app. */
    private fun configurarSelectorIdioma() {
        val activo = Idioma.actual()
        mapOf("es" to R.id.tvIdiomaEs, "en" to R.id.tvIdiomaEn).forEach { (codigo, id) ->
            val tv = findViewById<TextView>(id)
            val seleccionado = codigo == activo
            tv.setBackgroundResource(if (seleccionado) R.drawable.bg_idioma_activo else 0)
            tv.setTextColor(Color.parseColor(if (seleccionado) "#FFFFFF" else "#7A766E"))
            tv.setOnClickListener { if (!seleccionado) Idioma.cambiar(codigo) }
        }
    }

    /** Pide GET api/v1/cultivos y rellena los selectores ya creados. */
    private fun cargarCatalogoCultivos() {
        lifecycleScope.launch {
            val cargado = withContext(Dispatchers.IO) { MockRepository.cargarCatalogo() }
            if (!cargado) {
                Toast.makeText(this@LoginActivity, R.string.error_catalogo, Toast.LENGTH_LONG).show()
                return@launch
            }
            (listOf(spinnerCultivo1, spinnerCultivo2) + cultivosAdicionales.map { it.spinner }).forEach { spinner ->
                val posicion = spinner.selectedItemPosition
                spinner.adapter = crearAdaptadorEcoCrop()
                spinner.setSelection(posicion.coerceIn(0, opcionesEcoCrop.size - 1))
            }
        }
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

            // Lectura protegida con getString para evitar conflictos de tipos
            val haGuardadas = prefs.getString("user_hectareas", null)
            haGuardadas?.toDoubleOrNull()?.let {
                MockRepository.hectareasFinca = it
            }

            prefs.getString("user_cultivos", null)?.let { guardados ->
                val lista = guardados.split("||").filter { it.isNotBlank() }
                if (lista.size >= 2) {
                    // Versiones anteriores guardaban nombres en español: se pasan a ids
                    MockRepository.cultivosSeleccionados = lista.map { MockRepository.idCultivo(it) }.toMutableList()
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
        btnEntrar.setOnClickListener {
            val email = etLoginEmail.text.toString().trim()

            if (!esCorreoValido(email)) {
                etLoginEmail.error = getString(R.string.error_correo)
                etLoginEmail.requestFocus()
                return@setOnClickListener
            }

            btnEntrar.mostrarCargando(true, getString(R.string.cargando_entrar))

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val peticion = LoginRequest(correo = email)
                    val respuesta = RetrofitClient.apiService.login(peticion)

                    // El login no trae la rotación: se lee de GET api/v1/user para no
                    // pisarla luego con los cultivos por defecto al guardar Mi Finca
                    val idUsuario = respuesta.body()?.usuario?.id
                    val usuarioDetalle = if (respuesta.isSuccessful && idUsuario != null) {
                        try {
                            RetrofitClient.apiService.obtenerUsuario(idUsuario).body()
                        } catch (e: Exception) {
                            null
                        }
                    } else null

                    withContext(Dispatchers.Main) {
                        btnEntrar.mostrarCargando(false, "")
                        if (respuesta.isSuccessful && respuesta.body() != null) {
                            val datosBackend = respuesta.body()!!
                            val prefs = getSharedPreferences("TerraPrefs", Context.MODE_PRIVATE)

                            // Otra cuenta en el mismo dispositivo: se descartan sus datos locales
                            if (prefs.getString("user_email", null) != email) {
                                prefs.edit().clear().apply()
                                MockRepository.ultimoAnalisisNasa = null
                            }

                            restaurarRotacion(usuarioDetalle)?.let { cultivos ->
                                MockRepository.cultivosSeleccionados = cultivos.toMutableList()
                                prefs.edit().putString("user_cultivos", cultivos.joinToString("||")).apply()
                            }

                            val idReal = datosBackend.usuario?.id
                            if (idReal != null) {
                                prefs.edit().putInt("user_id", idReal).apply()
                            } else {
                                Toast.makeText(this@LoginActivity, R.string.error_sin_usuario, Toast.LENGTH_LONG).show()
                            }

                            prefs.edit().putString("user_email", email).apply()

                            val nombreReal = datosBackend.usuario?.nombre
                            if (!nombreReal.isNullOrBlank()) {
                                MockRepository.nombreUsuario = nombreReal
                                prefs.edit().putString("user_nombre", nombreReal).apply()
                            }

                            datosBackend.usuario?.ubicacion?.let {
                                MockRepository.ubicacionFinca = it
                                prefs.edit().putString("user_ubicacion", it).apply()
                            }

                            datosBackend.usuario?.hectareas?.let {
                                MockRepository.hectareasFinca = it
                                prefs.edit().putString("user_hectareas", it.toString()).apply()
                            }

                            Toast.makeText(this@LoginActivity, R.string.conectado, Toast.LENGTH_SHORT).show()
                            irAMainActivity()
                        } else if (respuesta.code() == 404) {
                            Toast.makeText(this@LoginActivity, R.string.error_no_registrado, Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this@LoginActivity, getString(R.string.error_servidor, respuesta.code()), Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        btnEntrar.mostrarCargando(false, "")
                        Toast.makeText(this@LoginActivity, getString(R.string.error_red, e.message), Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        btnAnadirCultivo.setOnClickListener {
            agregarSelectorCultivoExtra()
        }

        btnGuardarRegistro.setOnClickListener {
            val nombre = etRegNombre.text.toString().trim()
            val email = etRegEmail.text.toString().trim()
            val ubicacion = etRegUbicacion.text.toString().trim()
            val hectareasStr = etRegHectareas.text.toString().trim()

            if (!regexSoloLetras.matches(nombre)) {
                etRegNombre.error = getString(R.string.error_nombre)
                etRegNombre.requestFocus()
                return@setOnClickListener
            }

            if (!esCorreoValido(email)) {
                etRegEmail.error = getString(R.string.error_correo)
                etRegEmail.requestFocus()
                return@setOnClickListener
            }

            val cantidadLetrasUbicacion = ubicacion.count { it.isLetter() }
            if (ubicacion.length < 3 || cantidadLetrasUbicacion < 3) {
                etRegUbicacion.error = getString(R.string.error_ubicacion)
                etRegUbicacion.requestFocus()
                return@setOnClickListener
            }

            val hectareasNum = hectareasStr.toDoubleOrNull()
            if (hectareasNum == null || hectareasNum <= 0) {
                etRegHectareas.error = getString(R.string.error_superficie)
                etRegHectareas.requestFocus()
                return@setOnClickListener
            }

            if (spinnerCultivo1.selectedItemPosition == 0 || spinnerCultivo2.selectedItemPosition == 0) {
                Toast.makeText(this@LoginActivity, R.string.error_min_cultivos, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            for ((index, item) in cultivosAdicionales.withIndex()) {
                if (item.spinner.selectedItemPosition == 0) {
                    val num = index + 3
                    Toast.makeText(this@LoginActivity, getString(R.string.error_selecciona_cultivo_n, num), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // Se guardan los ids del catálogo, no los nombres (cambian con el idioma)
            val todosLosCultivos = (listOf(spinnerCultivo1, spinnerCultivo2) + cultivosAdicionales.map { it.spinner })
                .map { idElegido(it) }

            btnGuardarRegistro.mostrarCargando(true, getString(R.string.cargando_registro))

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val peticion = RegistroRequest(
                        nombre = nombre,
                        correo = email,
                        ubicacion = ubicacion,
                        hectareas = hectareasNum,
                        rotacion = todosLosCultivos
                    )
                    val respuesta = RetrofitClient.apiService.registro(peticion)

                    withContext(Dispatchers.Main) {
                        btnGuardarRegistro.mostrarCargando(false, "")

                        if (respuesta.isSuccessful && respuesta.body() != null) {
                            val datosRegistro = respuesta.body()!!

                            MockRepository.nombreUsuario = nombre
                            MockRepository.ubicacionFinca = ubicacion
                            MockRepository.hectareasFinca = hectareasNum
                            MockRepository.cultivosSeleccionados = todosLosCultivos.toMutableList()
                            MockRepository.ultimoAnalisisNasa = null

                            val prefs = getSharedPreferences("TerraPrefs", Context.MODE_PRIVATE)
                            prefs.edit().clear().apply()

                            val idReal = datosRegistro.usuario?.id
                            if (idReal != null) {
                                prefs.edit().putInt("user_id", idReal).apply()
                            } else {
                                Toast.makeText(this@LoginActivity, R.string.error_sin_usuario, Toast.LENGTH_LONG).show()
                            }

                            prefs.edit()
                                .putString("user_nombre", nombre)
                                .putString("user_email", email)
                                .putString("user_ubicacion", ubicacion)
                                .putString("user_hectareas", hectareasNum.toString())
                                .putString("user_cultivos", todosLosCultivos.joinToString("||"))
                                .apply()

                            Toast.makeText(this@LoginActivity, R.string.registro_ok, Toast.LENGTH_SHORT).show()
                            irAMainActivity()
                        } else {
                            val errorExacto = respuesta.errorBody()?.string()
                            Toast.makeText(this@LoginActivity, getString(R.string.error_codigo, respuesta.code(), errorExacto), Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        btnGuardarRegistro.mostrarCargando(false, "")
                        Toast.makeText(this@LoginActivity, getString(R.string.error_red, e.message), Toast.LENGTH_LONG).show()
                    }
                }
            }
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
            text = getString(R.string.cultivo_n, numeroInicial)
            setTextColor(Color.parseColor("#5C5953"))
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val tvEliminar = TextView(this).apply {
            text = getString(R.string.quitar)
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
            item.tvLabel.text = getString(R.string.cultivo_n, numeroReal)
        }
    }

    /** "Papa → Maíz → Fríjol" de la rotación actual guardada en el backend (siempre en español) -> ids. */
    private fun restaurarRotacion(usuario: UsuarioDetalle?): List<String>? {
        val rotaciones = usuario?.rotaciones ?: return null
        val actual = rotaciones.find { it.data?.actual == true } ?: rotaciones.firstOrNull() ?: return null
        val cultivos = actual.rotacion.split("→").map { it.trim() }.filter { it.isNotEmpty() }
            .map { MockRepository.idCultivo(it) }
        return cultivos.takeIf { it.size >= 2 }
    }

    private fun irAMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}