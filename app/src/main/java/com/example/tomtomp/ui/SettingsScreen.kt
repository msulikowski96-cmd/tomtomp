package com.example.tomtomp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tomtomp.MapViewModel
import com.example.tomtomp.UnitSystem
import com.tomtom.sdk.init.TomTomSdk
import com.tomtom.sdk.init.TomTomSdkFailure
import com.tomtom.sdk.routing.options.calculation.RouteType
import java.util.Locale

@Composable
fun SettingsScreen(viewModel: MapViewModel, modifier: Modifier = Modifier) {
    val s = viewModel.settings
    var failures by remember { mutableStateOf(listOf<String>()) }
    LaunchedEffect(Unit) {
        TomTomSdk.failures.collect { failure: TomTomSdkFailure ->
            failures = failures + failure.toString()
        }
    }

    Column(
        modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Section("Mapa")
        Toggle("Ruch drogowy (natężenie i zdarzenia)", s.showTraffic) { s.showTraffic = it }
        Toggle("Styl nocny", s.nightMap) { s.nightMap = it }
        Toggle("Cieniowanie rzeźby terenu", s.hillShading) { s.hillShading = it }

        Section("Personalizacja")
        Text("Język mapy i komunikatów")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(Locale("pl", "PL") to "Polski", Locale.UK to "English", Locale.GERMANY to "Deutsch").forEach { (l, name) ->
                FilterChip(selected = s.language == l, onClick = { s.language = l }, label = { Text(name) })
            }
        }
        Text("Jednostki")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UnitSystem.entries.forEach { u ->
                FilterChip(selected = s.unitSystem == u, onClick = { s.unitSystem = u }, label = { Text(u.label) })
            }
        }

        Section("Trasy")
        Text("Typ trasy")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(RouteType.Fast to "Najszybsza", RouteType.Short to "Najkrótsza", RouteType.Efficient to "Ekonomiczna")
                .forEach { (t, name) ->
                    FilterChip(selected = s.routeType == t, onClick = { s.routeType = t }, label = { Text(name) })
                }
        }
        Toggle("Unikaj dróg płatnych", s.avoidTolls) { s.avoidTolls = it }
        Toggle("Unikaj autostrad", s.avoidMotorways) { s.avoidMotorways = it }
        Text("Trasy alternatywne: ${s.maxAlternatives}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (0..2).forEach { n ->
                FilterChip(selected = s.maxAlternatives == n, onClick = { s.maxAlternatives = n }, label = { Text("$n") })
            }
        }

        Section("Zaawansowane")
        Toggle("Symuluj jazdę po trasie (bez ruchu fizycznego)", s.simulateLocation) { s.simulateLocation = it }
        Text("SDK zainicjalizowane: ${TomTomSdk.isInitialized}")
        Text("Dane mapy: online z lokalnym cache (tryb minimalny z dokumentacji)")
        if (failures.isEmpty()) {
            Text("Błędy SDK: brak", style = MaterialTheme.typography.bodySmall)
        } else {
            failures.forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun Section(title: String) {
    HorizontalDivider()
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun Toggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
