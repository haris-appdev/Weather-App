package com.example.weatherapp.repository

import com.example.weatherapp.BuildConfig
import com.example.weatherapp.R
import com.example.weatherapp.models.DailyForecast
import com.example.weatherapp.models.VisualCrossingResponse
import com.example.weatherapp.models.WeatherModel
import com.example.weatherapp.network.RetrofitClient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WeatherRepository{

    private val api = RetrofitClient.apiService
    private val apiKey = BuildConfig.WEATHER_API_KEY

    suspend fun fetchWeatherByCity(city: String): VisualCrossingResponse {
        return api.getFullWeatherData(location = city, apiKey = apiKey)
    }

    suspend fun fetchWeatherByCoords(lat: Double, lon: Double): VisualCrossingResponse {
        return api.getFullWeatherDataByCoords(lat = lat, lon = lon, apiKey = apiKey)
    }

    fun mapResponseToModel(response: VisualCrossingResponse): WeatherModel {
        val current = response.currentConditions
        val today = response.days.first()

        return WeatherModel(
            cityName = response.resolvedAddress.split(",").first(),
            currentTemp = current.temp.toInt(),
            conditionText = current.conditions,
            windSpeed = current.windSpeed ?: 0.0,
            humidity = current.humidity?.toInt() ?: 0,
            uvIndex = current.uvIndex ?: 0.0,
            sunsetTime = formatToTime(current.sunset ?: ""),
            hourlyTemps = today.hours.map { it.temp.toInt() },
            weeklyForecast = response.days.map { day ->
                DailyForecast(
                    dayName = formatToDayName(day.datetime),
                    maxTemp = day.tempMax.toInt(),
                    minTemp = day.tempMin.toInt(),
                    imageResId = mapIconToResource(day.icon ?: "")
                )
            }
        )
    }

    private fun formatToDayName(dateStr: String): String {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
        return SimpleDateFormat("EEEE", Locale.getDefault()).format(date ?: Date())
    }

    private fun formatToTime(isoString: String): String = try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        outputFormat.format(inputFormat.parse(isoString) ?: Date())
    } catch (e: Exception) { isoString.take(5) }

    private fun mapIconToResource(iconId: String): Int = when (iconId) {
        "snow" -> R.drawable.snow_icon
        "rain" -> R.drawable.rain_icon
        "fog" -> R.drawable.fog_icon
        "wind" -> R.drawable.wind_icon
        "cloudy" -> R.drawable.cloudy_icon
        "partly-cloudy-day" -> R.drawable.partly_cloudy_day
        "partly-cloudy-night" -> R.drawable.partly_cloudy_night
        "clear-day" -> R.drawable.clear_day
        "clear-night" -> R.drawable.clear_night
        else -> R.drawable.default_weather_icon
    }
}