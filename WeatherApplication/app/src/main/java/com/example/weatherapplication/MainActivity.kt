package com.example.weatherapplication

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.net.ssl.HttpsURLConnection


class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WeatherApp()
        }
    }
}

@Composable
fun WeatherApp() {
    var cityName by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var maxTemperature by remember { mutableStateOf("") }
    var minTemperature by remember { mutableStateOf("") }
    var isDateValid by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Enter City Name:")
        TextField(
            value = cityName,
            onValueChange = { cityName = it },
            modifier = Modifier.padding(16.dp)
        )

        Text(text = "Enter Date (YYYY-MM-DD):")
        TextField(
            value = date,
            onValueChange = {
                date = it
                isDateValid = validateDate(it)
            },
            modifier = Modifier.padding(16.dp),
            isError = !isDateValid,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        if (!isDateValid) {
            Text(
                text = "Please enter a valid date in YYYY-MM-DD format",
                color = Color.Red,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp)
            )
        }

        Button(
            onClick = {
                if (isDateValid) {
                    fetchWeatherData(cityName, date) { result ->
                        when (result) {
                            is Result.Success -> {
                                maxTemperature = result.data.maxTempC
                                minTemperature = result.data.minTempC
                            }
                            is Result.Error -> {
                                Log.e("WeatherApp", "Error fetching weather data: ${result.exception.message}")
                            }
                        }
                    }
                }
            },
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Get Weather")
        }

        Text(text = "Max Temperature: $maxTemperature")
        Text(text = "Min Temperature: $minTemperature")
    }
}

data class WeatherForecast(
    val maxTempC: String,
    val minTempC: String
)

sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
}

private fun fetchWeatherData(
    city: String,
    date: String,
    callback: (Result<WeatherForecast>) -> Unit
) {
    val apiKey = "f302e3f2ffa44556980180712240304"
    val baseUrl = "https://api.weatherapi.com/v1/history.json"
    val urlString = "$baseUrl?key=$apiKey&q=$city&dt=$date"

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val url = URL(urlString)
            val uc: HttpsURLConnection = url.openConnection() as HttpsURLConnection
            val br = BufferedReader(InputStreamReader(uc.inputStream))
            var line: String?
            val lin2 = StringBuilder()
            while (br.readLine().also { line = it } != null) {
                lin2.append(line)
            }

            val jsonObject = JSONObject(lin2.toString())
            val forecastObject = jsonObject.getJSONObject("forecast")
            val forecastDayArray = forecastObject.getJSONArray("forecastday")
            val firstForecastDay = forecastDayArray.getJSONObject(0)
            val dayObject = firstForecastDay.getJSONObject("day")
            val maxTempC = dayObject.getDouble("maxtemp_c").toString()
            val minTempC = dayObject.getDouble("mintemp_c").toString()

            val weatherForecast = WeatherForecast(maxTempC, minTempC)
            withContext(Dispatchers.Main) {
                callback(Result.Success(weatherForecast))
            }
        } catch (exception: Exception) {
            withContext(Dispatchers.Main) {
                callback(Result.Error(exception))
            }
        }
    }
}

private fun validateDate(date: String): Boolean {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        sdf.isLenient = false
        val parsedDate = sdf.parse(date)
        val cal = Calendar.getInstance()
        cal.time = parsedDate
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val day = cal.get(Calendar.DAY_OF_MONTH)
        day <= maxDay
    } catch (e: Exception) {
        false
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MaterialTheme {
        Surface {
            WeatherApp()
        }
    }
}