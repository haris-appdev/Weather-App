package com.example.weatherapp.models

import com.google.gson.annotations.SerializedName

data class VisualCrossingResponse(
    val resolvedAddress: String,
    val timezone: String,
    val days: List<DayForecast>,
    val currentConditions: CurrentConditions
)

data class DayForecast(
    val datetime: String,
    @SerializedName("tempmax") val tempMax: Double,
    @SerializedName("tempmin") val tempMin: Double,
    val temp: Double,
    val humidity: Double?,
    @SerializedName("windspeed") val windSpeed: Double?,
    @SerializedName("unindex") val uvIndex: Double?,
    val sunrise: String?,
    val sunset: String?,
    val conditions: String?,
    val icon: String?,
    val hours: List<HourForecast>
)

data class HourForecast(
    val datetime: String,
    val temp: Double,
    val conditions: String,
    val icon: String
)

data class CurrentConditions(
    val datetime: String,
    val temp: Double,
    val humidity: Double?,
    @SerializedName("windspeed") val windSpeed: Double?,
    @SerializedName("unindex") val uvIndex: Double?,
    val visibility: Double?,
    val sunrise: String?,
    val sunset: String?,
    val conditions: String,
    val icon: String
)
