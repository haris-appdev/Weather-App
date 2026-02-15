package com.example.weatherapp.network

import com.example.weatherapp.models.VisualCrossingResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface WeatherApiService {

    @GET("timeline/{city}")
    suspend fun getFullWeatherData(
        @Path("city") location: String,
        @Query("unitGroup") unitGroup: String = "metric",
        @Query("key") apiKey: String,
        @Query("contentType") contentType: String = "json"
    ): VisualCrossingResponse

    @GET("timeline/{lat},{lon}")
    suspend fun getFullWeatherDataByCoords(
        @Path("lat") lat: Double,
        @Path("lon") lon: Double,
        @Query("unitGroup") unitGroup: String = "metric",
        @Query("key") apiKey: String,
        @Query("contentType") contentType: String = "json"
    ): VisualCrossingResponse
}