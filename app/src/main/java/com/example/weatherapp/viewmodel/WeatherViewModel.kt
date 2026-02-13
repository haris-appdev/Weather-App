package com.example.weatherapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherapp.R
import com.example.weatherapp.models.DailyForecast
import com.example.weatherapp.models.WeatherModel
import com.example.weatherapp.retorfit.RetrofitClient
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
                val response = RetrofitClient.apiService.getCurrentWeather(city, "178475033c4b53f1a110e87cccc661f9")
                mapResponseToUi(response)
            } catch (e: Exception) {
                _uiState.value = WeatherUiState.Error("Failed to load weather: ${e.message}")
            }
        }
    }

    fun fetchWeatherByCoords(lat: Double, lon: Double) {
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            try {
                val response = RetrofitClient.apiService.getWeatherByCoords(lat, lon, "178475033c4b53f1a110e87cccc661f9")
                mapResponseToUi(response)
            } catch (e: Exception) {
                _uiState.value = WeatherUiState.Error("Location failed: ${e.message}")
            }
        }
    }

    private fun mapResponseToUi(response: com.example.weatherapp.models.WeatherResponse) {
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
            hourlyTemps = listOf(celsiusTemp, celsiusTemp - 1, celsiusTemp - 2, celsiusTemp - 3),
            weeklyForecast = listOf(
                DailyForecast("Today", celsiusTemp + 2, celsiusTemp - 4, R.drawable.shirt),
                DailyForecast("Tuesday", 22, 16, R.drawable.shirt),
                DailyForecast("Wednesday", 25, 19, R.drawable.shirt),
                DailyForecast("Thursday", 24, 18, R.drawable.shirt),
                DailyForecast("Friday", 22, 16, R.drawable.shirt),
                DailyForecast("Saturday", 25, 19, R.drawable.shirt),
                DailyForecast("Sunday", 22, 16, R.drawable.shirt)
            )
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
}