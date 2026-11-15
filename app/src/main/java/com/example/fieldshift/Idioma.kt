package com.example.fieldshift

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Idioma de la app: el que el usuario eligió o, si no eligió, el del teléfono.
 * Español solo si el teléfono está en español; cualquier otro idioma usa inglés (values/ por defecto).
 */
object Idioma {

    fun actual(): String {
        val elegido = AppCompatDelegate.getApplicationLocales()
        val locale = if (!elegido.isEmpty) elegido[0] else LocaleListCompat.getAdjustedDefault()[0]
        return if (locale?.language == "es") "es" else "en"
    }

    /** Cambia el idioma ("es" o "en"), lo recuerda entre sesiones y recrea las pantallas abiertas. */
    fun cambiar(codigo: String) {
        if (codigo == actual()) return
        // Lo que vino del servidor está en el idioma anterior: se vuelve a pedir
        MockRepository.ultimoAnalisisNasa = null
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(codigo))
    }
}
