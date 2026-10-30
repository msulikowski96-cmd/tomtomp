package com.example.tomtomp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tomtomp.MapViewModel

@Composable
fun NavigationPanel(viewModel: MapViewModel, modifier: Modifier = Modifier) {
    val g = viewModel.guidance
    ElevatedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (viewModel.arrived) {
                Text("Dotarłeś do celu", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            } else {
                Text(
                    g.distanceToNextMeters?.let(::formatDistance) ?: "—",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(prettifyManeuver(g.maneuver))
                if (g.road.isNotBlank()) Text(g.road, style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Pozostało: ${g.remainingMeters?.let(::formatDistance) ?: "—"}")
                    Text("Czas: ${g.remainingSeconds?.let { formatMinutes(it / 60) } ?: "—"}")
                }
            }
            Button(onClick = viewModel::stopNavigation, modifier = Modifier.width(180.dp)) {
                Text(if (viewModel.arrived) "Zakończ" else "Zatrzymaj")
            }
        }
    }
}

/** Virtual horizon: road elements the SDK predicts ahead of the vehicle. */
@Composable
fun HorizonCard(viewModel: MapViewModel, modifier: Modifier = Modifier) {
    val info = viewModel.horizon ?: return
    ElevatedCard(modifier.width(150.dp)) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Horyzont", fontWeight = FontWeight.Bold)
            Text(if (info.onRoad) "Na drodze" else "Poza drogą", style = MaterialTheme.typography.bodySmall)
            Text("Przejechano: ${formatDistance(info.offsetMeters)}", style = MaterialTheme.typography.bodySmall)
            info.elementsAhead.groupingBy { it }.eachCount().entries.take(6).forEach { (name, count) ->
                Text("$name × $count", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

fun formatDistance(meters: Double): String =
    if (meters >= 1000) "%.1f km".format(meters / 1000.0) else "${(meters / 10).toInt() * 10} m"

fun formatMinutes(minutes: Long): String =
    if (minutes >= 60) "${minutes / 60} h ${minutes % 60} min" else "$minutes min"

private fun prettifyManeuver(raw: String): String =
    raw.replace(Regex("([a-z])([A-Z])"), "$1 $2").ifBlank { "Jedź prosto" }
