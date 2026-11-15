package com.example.fieldshift

// ==========================================
// 1. MODELOS PARA ACTIVIDAD RECIENTE (NUEVO)
// ==========================================
data class RespuestaActividad(
    val version: String,
    val actividad: List<ActividadItem>
)

data class ActividadItem(
    val tipo: String,
    val titulo: String,
    val detalle: String,
    val fecha: String?,
    val fuente: String
)

// ==========================================
// 2. MODELOS PARA ANÁLISIS NASA
// ==========================================
data class PeticionAnalisis(
    val usuario_id: Int,
    val lat: Double,
    val lon: Double,
    val rotacion: List<String>,
    val prioridades: List<String>
)

data class RespuestaAnalisis(
    val version: String,
    val clima: ClimaData,
    val opciones: List<OpcionRotacion>,
    val aviso: String?
)

data class ClimaData(
    val lluvia_anual_mm: Int,
    val temp_media_c: Double,
    val dias_calor: Int,
    val riesgo_helada: String,
    val humedad_suelo: Double,
    val fuente: String,
    val humedad_nivel: String?,
    val clima_tipo: String?
)

data class OpcionRotacion(
    val id: String,
    val actual: Boolean,
    val nombre: String,
    val cultivos: List<String>,
    val puntaje: Double,
    val porque: String,
    val evidencia: List<EvidenciaData>,
    val beneficios: List<String>,
    val riesgos: List<String>,
    val vacios: List<String>,
    val aporte: AporteData?
)

data class EvidenciaData(
    val dato: String,
    val valor: Double,
    val unidad: String,
    val fuente: String,
    val nota: String
)

data class AporteData(
    val agua: AguaData,
    val suelo: SueloData
)

data class AguaData(
    val referencia: Int,
    val rotacion: Int,
    val cambio_pct: Int
)

data class SueloData(
    val referencia: Double,
    val rotacion: Double,
    val nivel: String
)

// ==========================================
// 3. MODELOS PARA MI FINCA (PATCH)
// ==========================================
data class UpdateUserRequest(
    val usuario_id: Int,
    val ubicacion: String,
    val hectareas: Double,
    val rotacion: List<String>
)

data class UpdateUserResponse(
    val mensaje: String,
    val usuario: UsuarioData
)

data class UsuarioData(
    val id: Int,
    val nombre: String,
    val correo: String,
    val ubicacion: String,
    val hectareas: Double,
    val rotacion_actual: String?
)

// ==========================================
// 4. MODELOS PARA LOGIN Y REGISTRO
// ==========================================
data class UsuarioLogin(
    val id: Int,
    val nombre: String,
    val correo: String,
    val ubicacion: String,
    val hectareas: Double
)

data class LoginRequest(
    val correo: String
)

data class LoginResponse(
    val mensaje: String?,
    val usuario: UsuarioLogin?,
    val token: String?,
    val error: String?
)

data class RegistroRequest(
    val nombre: String,
    val correo: String,
    val ubicacion: String,
    val hectareas: Double,
    val rotacion: List<String>
)

data class RegistroResponse(
    val mensaje: String?,
    val usuario: UsuarioLogin?
)