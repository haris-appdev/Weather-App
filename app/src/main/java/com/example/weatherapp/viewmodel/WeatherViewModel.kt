package com.example.weatherapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherapp.models.WeatherModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.example.weatherapp.repository.WeatherRepository
import kotlinx.coroutines.launch

sealed class WeatherUiState {
    object Loading : WeatherUiState()
    data class Success(val data: WeatherModel) : WeatherUiState()
    data class Error(val message: String) : WeatherUiState()
}

class WeatherViewModel: ViewModel() {

    private val repository = WeatherRepository()
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
                val response = repository.fetchWeatherByCity(city)
                _uiState.value = WeatherUiState.Success(repository.mapResponseToModel(response))
            } catch (e: Exception) {
                handleFetchError(e)
            }
        }
    }

    fun fetchWeatherByCoords(lat: Double, lon: Double) {
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            try {
                val response = repository.fetchWeatherByCoords(lat, lon)
                _uiState.value = WeatherUiState.Success(repository.mapResponseToModel(response))
            } catch (e: Exception) {
                handleFetchError(e)
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

    private suspend fun handleFetchError(e: Exception) {
        if (_uiState.value is WeatherUiState.Success) {
            _errorEvents.emit("No internet connection. Showing offline data.")
        } else {
            _uiState.value = WeatherUiState.Error("Check your internet connection.")
        }
    }
}