package com.example.weatherapp.activities

import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weatherapp.models.WeatherModel
import com.example.weatherapp.viewmodel.WeatherUiState
import com.example.weatherapp.viewmodel.WeatherViewModel
import android.Manifest
import androidx.compose.material3.Button
import androidx.core.app.ActivityCompat
import com.example.weatherapp.R
import com.example.weatherapp.models.DailyForecast
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import java.util.Calendar

@Composable
fun WeatherBackground(condition: String) {
    val imageRes = when (condition) {
        "Sunny" -> R.drawable.default_bg
        "Rainy" -> R.drawable.default_bg
        "Cloudy", "Mostly Cloudy" -> R.drawable.default_bg
        else -> R.drawable.default_bg
    }

    Image(
        painter = painterResource(id = imageRes),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
    )
}

@Composable
fun WeatherSearchBar(
    onSearch: (String) -> Unit,
    onLocationClick: () -> Unit
    ) {
    var text by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_search),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f)
            )
            TextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Search city", color = Color.White.copy(alpha = 0.5f)) },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    cursorColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        onSearch(text)
                        text = ""
                    }
                )
            )
            IconButton(onClick = onLocationClick) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_mylocation),
                    contentDescription = "Current Location",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun HeroSection(cityName: String, temperature: Int, condition: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 64.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = cityName,
            color = Color.White,
            fontSize = 32.sp,
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "$temperature°",
            color = Color.White,
            fontSize = 100.sp,
            style = MaterialTheme.typography.displayLarge
        )

        Text(
            text = condition,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 20.sp,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun HourlyForecastItem(time: String, iconRes: Int, temp: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = time, color = Color.LightGray, fontSize = 12.sp)

        Spacer(modifier = Modifier.height(8.dp))

        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = Color.Unspecified
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "$temp°", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun HourlyScrollingCard(hourlyTemps: List<Int>) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                text = "NEXT 24 HOURS",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                items(hourlyTemps.size) { index ->
                    val temp = hourlyTemps[index]
                    val hour = (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) + (index * 3)) % 24
                    val timeLabel = when {
                        hour == 0 -> "12 AM"
                        hour < 12 -> "$hour AM"
                        hour == 12 -> "12 PM"
                        else -> "${hour - 12} PM"
                    }
                    HourlyForecastItem(
                        time = timeLabel,
                        iconRes = R.drawable.contrast,
                        temp = temp
                    )
                }
            }
        }
    }
}

@Composable
fun DailyForecastRow(day: String, iconRes: Int, highTemp: Int, lowTemp: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = day,
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )

        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = Color.Unspecified
        )

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End
        ) {
            Text(text = "$highTemp°", color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "$lowTemp°", color = Color.White.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun WeeklyForecastCard(forecast: List<DailyForecast>) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.contrast),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "7-DAY FORECAST",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            forecast.forEach { daily ->
                DailyForecastRow(
                    day = daily.dayName,
                    iconRes = daily.imageResId,
                    highTemp = daily.maxTemp,
                    lowTemp = daily.minTemp
                )
            }
        }
    }
}

@Composable
fun DetailTile(label: String, value: String, iconRes: Int) {
    GlassCard(modifier = Modifier.width(160.dp)) {
        Column(horizontalAlignment = Alignment.Start) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Oops!", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = message, color = Color.LightGray, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Text("Try Again")
        }
    }
}

@Composable
fun WeatherSuccessView(weather: WeatherModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HeroSection(
            cityName = weather.cityName,
            temperature = weather.currentTemp,
            condition = weather.conditionText
        )

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            AiSummarySection(currentTemp = weather.currentTemp, condition = weather.conditionText)
            Spacer(modifier = Modifier.height(24.dp))
            HourlyScrollingCard(hourlyTemps = weather.hourlyTemps)
            Spacer(modifier = Modifier.height(24.dp))
            WeeklyForecastCard(forecast = weather.weeklyForecast)
            Spacer(modifier = Modifier.height(16.dp))

            WeatherDetailGrid(weather)

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun WeatherDetailGrid(weather: WeatherModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                DetailTile("WIND", "${weather.windSpeed} m/s", R.drawable.contrast)
            }
            Box(modifier = Modifier.weight(1f)) {
                DetailTile("HUMIDITY", "${weather.humidity}%", R.drawable.contrast)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                DetailTile("UV INDEX", "${weather.uvIndex} uv", R.drawable.contrast)
            }
            Box(modifier = Modifier.weight(1f)) {
                DetailTile("SUNSET", weather.sunsetTime, R.drawable.contrast)
            }
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.3f),
                shape = RoundedCornerShape(28.dp)
            )
    ) {
        Box(modifier = Modifier.padding(20.dp)) {
            content()
        }
    }
}

@Composable
fun AiSummarySection(currentTemp: Int, condition: String) {
    val recommendation = when {
        condition.contains("Rain", ignoreCase = true) ->
            "It's raining. Grab an umbrella and boots!" to R.drawable.boot
        currentTemp < 15 ->
            "It's a bit chilly. A light jacket is recommended." to R.drawable.coat
        else ->
            "The weather is pleasant. Enjoy your day!" to R.drawable.shirt
    }

    GlassCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                painter = painterResource(id = recommendation.second),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )

            Text(
                text = recommendation.first,
                color = Color.White,
                fontSize = 16.sp,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun LoadingView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Color.White)
    }
}

class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val viewModel = WeatherViewModel()
        viewModel.fetchWeather("Lahore")

        setContent {
            val context = LocalContext.current
            val uiState by viewModel.uiState.collectAsState()
            val isRefreshing by viewModel.isRefreshing.collectAsState()

            Box(modifier = Modifier.fillMaxSize()) {
                val condition = if (uiState is WeatherUiState.Success) {
                    (uiState as WeatherUiState.Success).data.conditionText
                } else "Cloudy"

                WeatherBackground(condition = condition)

                LaunchedEffect(Unit) {
                    viewModel.errorEvents.collect { message ->
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
                }

                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        val currentCity = if (uiState is WeatherUiState.Success) {
                            (uiState as WeatherUiState.Success).data.cityName
                        } else "Lahore"
                        viewModel.refreshWeather(currentCity)
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(40.dp))
                        WeatherSearchBar(
                            onSearch = { city -> viewModel.fetchWeather(city) },
                            onLocationClick = { fetchCurrentLocationWeather(viewModel) }
                        )

                        when (val state = uiState) {
                            is WeatherUiState.Loading -> LoadingView()
                            is WeatherUiState.Success -> WeatherSuccessView(state.data)
                            is WeatherUiState.Error -> ErrorView(state.message) {
                                viewModel.fetchWeather("Lahore")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun fetchCurrentLocationWeather(viewModel: WeatherViewModel) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    viewModel.fetchWeatherByCoords(it.latitude, it.longitude)
                }
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        }
    }
}