package com.example.weatherapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherapp.R
import com.example.weatherapp.models.DailyForecast
import com.example.weatherapp.models.VisualCrossingResponse
import com.example.weatherapp.models.WeatherModel
import com.example.weatherapp.network.RetrofitClient
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing

    private val _errorEvents = MutableSharedFlow<String>()
    val errorEvents = _errorEvents.asSharedFlow()

    fun fetchWeather(city: String) {
        viewModelScope.launch {
            if (_uiState.value !is WeatherUiState.Success) {
                _uiState.value = WeatherUiState.Loading
            }

            try {
                val apiKey = "LEGUMZ7RTBTHJLAYREFDLJYDJ"
                val response = RetrofitClient.apiService.getFullWeatherData(
                    location = city,
                    unitGroup = "metric",
                    apiKey = apiKey,
                    contentType = "json"
                )
                mapVisualCrossingToUi(response)
            } catch (e: Exception) {
                if (_uiState.value is WeatherUiState.Success) {
                    _errorEvents.emit("No internet connection. Showing offline data.")
                } else {
                    _uiState.value = WeatherUiState.Error("Check your internet connection.")
                }
            }
        }
    }

    fun refreshWeather(cityName: String) {
        viewModelScope.launch {
            _isRefreshing.value = true
            fetchWeather(cityName)
            isRefreshing.value = false
        }
    }

    fun fetchWeatherByCoords(lat: Double, lon: Double) {
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            try {
                val apiKey = "LEGUMZ7RTBTHJLAYREFDLJYDJ"

                val response = RetrofitClient.apiService.getFullWeatherDataByCoords(
                    lat = lat,
                    lon = lon,
                    unitGroup = "metric",
                    apiKey = apiKey,
                    contentType = "json"
                    )
                mapVisualCrossingToUi(response)
            } catch (e: Exception) {
                Log.e("WEATHER_DEBUG", "Coords fetch failed: ${e.message}")
                _uiState.value = WeatherUiState.Error("Location failed: ${e.message}")
            }
        }
    }

    private fun mapVisualCrossingToUi(response: VisualCrossingResponse) {
        val current = response.currentConditions
        val today = response.days.first()

        val mappedData = WeatherModel(
            cityName = response.resolvedAddress.split(",").first(),
            currentTemp = current.temp.toInt(),
            conditionText = current.conditions,
            windSpeed = current.windspeed ?: 0.0,
            humidity = current.humidity?.toInt() ?: 0,
            uvIndex = current.uvindex ?: 0.0,
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
        _uiState.value = WeatherUiState.Success(mappedData)
    }

    private fun formatToDayName(dateStr: String): String {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
        val sdf = SimpleDateFormat("EEEE", Locale.getDefault())
        return sdf.format(date ?: Date())
    }

    private fun formatToTime(isoString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val date = inputFormat.parse(isoString)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            isoString.take(5)
        }
    }

    private fun mapIconToResource(iconId: String): Int {
        return when (iconId) {
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
}