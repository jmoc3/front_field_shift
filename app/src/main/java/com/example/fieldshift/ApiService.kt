package com.example.fieldshift

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {

    // Endpoint para actualizar los datos de Mi Finca
    @PATCH("api/v1/user")
    suspend fun updateUser(
        @Body request: UpdateUserRequest
    ): Response<UpdateUserResponse>

    // Datos del usuario y sus rotaciones activas (para restaurar la rotación al iniciar sesión)
    @GET("api/v1/user")
    suspend fun obtenerUsuario(
        @Query("id_usuario") idUsuario: Int
    ): Response<UsuarioDetalle>

    // Catálogo de cultivos EcoCrop (única fuente para los selectores)
    @GET("api/v1/cultivos")
    suspend fun obtenerCultivos(): Response<List<CultivoItem>>

    // Endpoint para Iniciar Sesión
    @POST("api/v1/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    // Endpoint para Registrarse
    @POST("api/v1/registro")
    suspend fun registro(
        @Body request: RegistroRequest
    ): Response<RegistroResponse>

    // POST para pedir el cálculo a la NASA (Se usa en MiFincaActivity)
    @POST("api/v1/analisis")
    suspend fun analizarRotacion(@Body peticion: PeticionAnalisis): retrofit2.Response<RespuestaAnalisis>

    // GET para pedir la lista de tarjetas del Inicio
    @GET("api/v1/actividad")
    suspend fun obtenerActividad(
        @Query("usuario_id") usuarioId: Int,
        @Query("lat") lat: Double,
        @Query("lon") lon: Double
    ): retrofit2.Response<RespuestaActividad>
}