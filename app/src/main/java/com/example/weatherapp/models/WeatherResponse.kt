package com.example.weatherapp.models

data class WeatherResponse(
    val main: MainData,
    val wind: WindData,
    val weather: List<WeatherDescription>,
    val sys: SysData,
    val clouds: CloudsData,
    val coord: CoordData,
    val name: String
)

data class MainData(
    val temp: Double,
    val humidity: Int
)

data class WindData(
    val speed: Double
)

data class WeatherDescription(
    val main: String,
    val description: String
)

data class SysData(
    val sunset: Long,
    val sunrise: Long
)

data class CloudsData(val all: Int)

data class CoordData(val lat: Double, val lon: Double)