package com.example.paisesapp

import com.google.gson.JsonElement
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface RestCountriesApi {
    @Headers("Authorization: Bearer rc_live_338afea2d20545da984c424b3a290e48")
    @GET("countries/v5")
    fun obtenerPaises(
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0
    ): Call<JsonElement>
}