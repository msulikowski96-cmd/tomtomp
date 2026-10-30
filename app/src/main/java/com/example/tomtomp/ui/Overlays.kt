package com.example.tomtomp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tomtomp.MapViewModel
import com.example.tomtomp.Mode
import com.example.tomtomp.PlannedRoute
import com.tomtom.sdk.search.model.result.SearchResult

@Composable
fun MapOverlays(
    viewModel: MapViewModel,
    onRecenter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize()) {
        when (viewModel.mode) {
            Mode.Navigating -> {
                NavigationPanel(viewModel, Modifier.align(Alignment.TopCenter).padding(top = 12.dp))
                HorizonCard(viewModel, Modifier.align(Alignment.CenterEnd))
            }
            else -> SearchPanel(viewModel, Modifier.align(Alignment.TopCenter).padding(top = 12.dp))
        }

        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            viewModel.message?.let { msg ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(msg, Modifier.weight(1f))
                        TextButton(onClick = viewModel::dismissMessage) { Text("OK") }
                    }
                }
            }
            when (viewModel.mode) {
                Mode.Browse -> viewModel.selected?.let { PlaceCard(viewModel, it) }
                Mode.RoutePreview -> RoutePreviewCard(viewModel)
                Mode.Navigating -> Unit
            }
        }

        FloatingActionButton(
            onClick = onRecenter,
            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 200.dp),
        ) { Text("◎", fontSize = 22.sp) }
    }
}

@Composable
private fun SearchPanel(viewModel: MapViewModel, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        ElevatedCard {
            OutlinedTextField(
                value = viewModel.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Szukaj miejsca lub adresu") },
                trailingIcon = {
                    if (viewModel.query.isNotEmpty()) {
                        TextButton(onClick = viewModel::clearSearch) { Text("✕") }
                    } else {
                        TextButton(onClick = { viewModel.submitSearch() }) { Text("Szukaj") }
                    }
                },
            )
            if (viewModel.query.isNotEmpty()) {
                TextButton(onClick = { viewModel.submitSearch() }) { Text("Szukaj „${viewModel.query}”") }
            }
        }
        if (viewModel.suggestions.isNotEmpty()) {
            ElevatedCard(Modifier.padding(top = 4.dp)) {
                LazyColumn {
                    items(viewModel.suggestions) { suggestion ->
                        Text(
                            suggestion,
                            Modifier.fillMaxWidth().clickable { viewModel.submitSearch(suggestion) }.padding(12.dp),
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
        if (viewModel.results.isNotEmpty()) {
            ElevatedCard(Modifier.padding(top = 4.dp)) {
                LazyColumn {
                    items(viewModel.results) { result -> ResultRow(result) { viewModel.selectResult(result) } }
                }
            }
        }
    }
}

@Composable
private fun ResultRow(result: SearchResult, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(12.dp)) {
        Text(result.place.name.ifBlank { "Miejsce" }, fontWeight = FontWeight.SemiBold)
        result.place.address?.freeformAddress?.let {
            Text(it, style = MaterialTheme.typography.bodySmall)
        }
    }
    HorizontalDivider()
}

@Composable
private fun PlaceCard(viewModel: MapViewModel, place: com.example.tomtomp.SelectedPlace) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(place.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            place.address?.let { Text(it) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = viewModel::planRoute) { Text("Wyznacz trasę") }
                OutlinedButton(onClick = viewModel::clearSelection) { Text("Zamknij") }
            }
        }
    }
}

@Composable
private fun RoutePreviewCard(viewModel: MapViewModel) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Trasy do: ${viewModel.selected?.name.orEmpty()}", fontWeight = FontWeight.Bold)
            LazyColumn(Modifier.fillMaxWidth()) {
                itemsIndexed(viewModel.routes) { index, planned ->
                    FilterChip(
                        selected = index == viewModel.activeRouteIndex,
                        onClick = { viewModel.selectRoute(index) },
                        label = { Text(routeSummary(planned, index)) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = viewModel::startNavigation) { Text("Rozpocznij nawigację") }
                OutlinedButton(onClick = viewModel::clearRoutes) { Text("Anuluj") }
            }
        }
    }
}

private fun routeSummary(planned: PlannedRoute, index: Int): String {
    val summary = planned.route.summary
    val km = summary.length.inMeters() / 1000.0
    val minutes = summary.travelTime.inWholeMinutes
    val delay = summary.trafficDelay.inWholeMinutes
    val label = if (index == 0) "Najlepsza" else "Alternatywa $index"
    return "$label · ${"%.1f".format(km)} km · ${formatMinutes(minutes)}" +
        if (delay > 0) " (+$delay min korek)" else ""
}
