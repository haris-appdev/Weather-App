package com.example.weatherapp.models

data class WeatherModel(
    val cityName: String,
    val currentTemp: Int,
    val conditionText: String,
    val windSpeed: Double,
    val humidity: Int,
    val uvIndex: Double,
    val sunsetTime: String,
    val hourlyTemps: List<Int>,
    val weeklyForecast: List<DailyForecast>
)

data class DailyForecast(
    val dayName: String,
    val maxTemp: Int,
    val minTemp: Int,
    val imageResId: Int
)