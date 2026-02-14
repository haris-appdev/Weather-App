package com.example.weatherapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherapp.R
import com.example.weatherapp.models.DailyForecast
import com.example.weatherapp.models.ForecastItem
import com.example.weatherapp.models.ForecastResponse
import com.example.weatherapp.models.WeatherModel
import com.example.weatherapp.models.WeatherResponse
import com.example.weatherapp.network.RetrofitClient
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

sealed class WeatherUiState {
    object Loading : WeatherUiState()
    data class Success(val data: WeatherModel) : WeatherUiState()
    data class Error(val message: String) : WeatherUiState()
}

class WeatherViewModel: ViewModel() {

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val uiState = _uiState

    init {
        fetchWeather("Lahore")
    }

    fun formatUnixToTime(unixTimestamp: Long): String {
        val date = Date(unixTimestamp * 1000L)
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        return sdf.format(date)
    }

    fun fetchWeather(city: String) {
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            try {
                val apiKey = "178475033c4b53f1a110e87cccc661f9"

                val currentDeferred = async { RetrofitClient.apiService.getCurrentWeather(city, apiKey) }
                val forecastDeferred = async { RetrofitClient.apiService.getFiveDayForecast(city, "metric", apiKey) }

                val currentResponse = currentDeferred.await()
                val forecastResponse = forecastDeferred.await()

                mapResponseToUi(currentResponse, forecastResponse)
            } catch (e: Exception) {
                _uiState.value = WeatherUiState.Error("Failed to load weather: ${e.message}")
            }
        }
    }

    fun fetchWeatherByCoords(lat: Double, lon: Double) {
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            try {
                val apiKey = "178475033c4b53f1a110e87cccc661f9"

                val currentDeferred = async { RetrofitClient.apiService.getWeatherByCoords(lat, lon, apiKey) }
                val forecastDeferred = async { RetrofitClient.apiService.getFiveDayForecastByCoords(lat, lon, "metric", apiKey) }

                val currentResponse = currentDeferred.await()
                val forecastResponse = forecastDeferred.await()

                mapResponseToUi(currentResponse, forecastResponse)
            } catch (e: Exception) {
                _uiState.value = WeatherUiState.Error("Location failed: ${e.message}")
            }
        }
    }

    private fun mapResponseToUi(response: WeatherResponse, forecast: ForecastResponse) {
        val celsiusTemp = (response.main.temp - 273.15).toInt()
        val mappedData = WeatherModel(
            cityName = response.name,
            currentTemp = celsiusTemp,
            conditionText = response.weather.firstOrNull()?.main ?: "Clear",
            windSpeed = response.wind.speed,
            humidity = response.main.humidity,
            uvIndex = estimateGlobalUv(response.coord.lat, response.clouds.all).let {
                "%.1f".format(it).toDouble()
            },
            sunsetTime = formatUnixToTime(response.sys.sunset),
            hourlyTemps = forecast.list.take(8).map { (it.main.temp).toInt() },
            weeklyForecast = parseWeeklyForecast(forecast.list)
        )
        _uiState.value = WeatherUiState.Success(mappedData)
    }

    private fun estimateGlobalUv(lat: Double ,cloudCoverage: Int): Double {
        val calender = Calendar.getInstance()
        val hour = calender.get(Calendar.HOUR_OF_DAY)
        val dayOfYear = calender.get(Calendar.DAY_OF_YEAR)

        val declination = 23.45 * Math.sin(Math.toRadians(360.0 * (284 + dayOfYear) / 365.0))

        val hourAngle = (hour - 12) * 15.0

        val latRad = Math.toRadians(lat)
        val decRad = Math.toRadians(declination)
        val hourRad = Math.toRadians(hourAngle)

        val sinElevation = Math.sin(latRad) * Math.sin(decRad) +
                Math.cos(latRad) * Math.cos(decRad) * Math.cos(hourRad)

        val elevationAngle = Math.toDegrees(Math.asin(sinElevation))

        if (elevationAngle <= 0) return 0.0

        val clearSkyUv = 13.0 * (elevationAngle / 90.0)

        val cloudFactor = 1.0 - (cloudCoverage / 100.0 * 0.7)

        return clearSkyUv * cloudFactor
    }

    private fun parseWeeklyForecast(forecastList: List<ForecastItem>): List<DailyForecast> {
        val sdf = SimpleDateFormat("EEEE", Locale.getDefault())

        return forecastList.groupBy {
            sdf.format(Date(it.dt * 1000L))
        }.map { (dayName, items) ->
            val maxTemp = items.maxOf { it.main.temp }.toInt()
            val minTemp = items.minOf { it.main.temp }.toInt()
            val mainCondition = items[items.size / 2].weather[0].main

            DailyForecast(
                dayName = if (dayName == sdf.format(Date())) "Today" else dayName,
                maxTemp = maxTemp,
                minTemp = minTemp,
                imageResId = when (mainCondition) {
                    "Rain" -> R.drawable.boot
                    "Clouds" -> R.drawable.coat
                    else -> R.drawable.shirt
                }
            )
        }.take(7)
    }
}