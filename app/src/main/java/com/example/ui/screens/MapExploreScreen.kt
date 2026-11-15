package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.core.CityAnchor
import com.example.core.IndiaData
import com.example.core.LatLngPoint
import com.example.core.LocationHelper
import com.example.core.LocalizedStrings
import com.example.data.model.Donation
import com.example.ui.components.DonationCard
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File

@Composable
fun MapExploreScreen(
    donations: List<Donation>,
    userLocation: LatLngPoint,
    selectedCity: CityAnchor,
    strings: LocalizedStrings,
    onSelectCity: (String) -> Unit,
    onUpdateGps: (LatLngPoint) -> Unit,
    onRequestFood: (Donation) -> Unit,
    onTrackDonation: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var dietFilter by remember { mutableStateOf("all") }
    var focusPoint by remember(userLocation) { mutableStateOf(userLocation) }
    var gpsStatus by remember { mutableStateOf("Privacy mode: ~1 km area radius") }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            scope.launch {
                val pt = LocationHelper.fetchCurrentLocation(context)
                if (pt != null) {
                    onUpdateGps(pt)
                    focusPoint = pt
                    gpsStatus = "Live GPS active"
                } else {
                    gpsStatus = "Using city anchor (${selectedCity.name})"
                }
            }
        } else {
            gpsStatus = "Location permission denied; using ${selectedCity.name}"
        }
    }

    val filtered = remember(donations, searchQuery, dietFilter, userLocation) {
        donations.filter { d ->
            val matchesQuery = searchQuery.isBlank() ||
                d.foodName.contains(searchQuery, ignoreCase = true) ||
                d.areaLabel.contains(searchQuery, ignoreCase = true) ||
                d.district.contains(searchQuery, ignoreCase = true) ||
                d.pinCode.contains(searchQuery, ignoreCase = true)
            val matchesDiet = dietFilter == "all" || d.dietType == dietFilter
            matchesQuery && matchesDiet
        }.sortedBy {
            IndiaData.haversineKm(userLocation.lat, userLocation.lng, it.approxLat, it.approxLng)
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Live Food Map (OpenStreetMap)",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = gpsStatus,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = {
                            val hasCoarse = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            val hasFine = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasCoarse || hasFine) {
                                scope.launch {
                                    val pt = LocationHelper.fetchCurrentLocation(context)
                                    if (pt != null) {
                                        onUpdateGps(pt)
                                        focusPoint = pt
                                        gpsStatus = "Live GPS active"
                                    }
                                }
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    )
                                )
                            }
                        },
                        modifier = Modifier.testTag("use_gps_button")
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("My GPS")
                    }
                }
            }

            // OpenStreetMap View
            item {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    OsmDonationMap(
                        center = focusPoint,
                        donations = filtered,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(22.dp))
                    )
                }
            }

            // Privacy Notice
            item {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.privacyProtectedNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // City Jump Chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(IndiaData.majorCities) { city ->
                        FilterChip(
                            selected = selectedCity.name == city.name,
                            onClick = {
                                onSelectCity(city.name)
                                focusPoint = LatLngPoint(city.lat, city.lng)
                            },
                            label = { Text(city.name) }
                        )
                    }
                }
            }

            // Search + Diet Filters
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        placeholder = { Text("Search food, area, district, or PIN code") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("map_search_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val chips = listOf(
                            "all" to "All Food",
                            "veg" to "Veg Only",
                            "vegan" to "Vegan",
                            "non_veg" to "Non-Veg"
                        )
                        items(chips) { (key, label) ->
                            FilterChip(
                                selected = dietFilter == key,
                                onClick = { dietFilter = key },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.LocationSearching,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(34.dp)
                            )
                            Text("No matching food listings found", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Try switching Indian cities above or clearing your search filter.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filtered, key = { it.id }) { donation ->
                    DonationCard(
                        donation = donation,
                        userLocation = userLocation,
                        primaryActionLabel = strings.requestFood,
                        onPrimaryAction = { onRequestFood(donation) },
                        onTrackClick = { onTrackDonation(donation.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun OsmDonationMap(
    center: LatLngPoint,
    donations: List<Donation>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mapView = remember {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidBasePath = File(context.cacheDir, "osmdroid")
            osmdroidTileCache = File(context.cacheDir, "osmdroid/tiles")
        }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(12.0)
        }
    }

    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { map ->
            map.controller.setCenter(GeoPoint(center.lat, center.lng))
            map.overlays.clear()

            val userMarker = Marker(map).apply {
                position = GeoPoint(center.lat, center.lng)
                title = "Your Reference Area"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            map.overlays.add(userMarker)

            donations.forEach { d ->
                val marker = Marker(map).apply {
                    position = GeoPoint(d.approxLat, d.approxLng)
                    title = "${d.foodName} (${d.servings} meals)"
                    snippet = "${d.areaLabel}, ${d.district} • ${d.status}"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                map.overlays.add(marker)

                if (d.locationSharingEnabled) {
                    val volMarker = Marker(map).apply {
                        position = GeoPoint(d.volunteerLat, d.volunteerLng)
                        title = "Volunteer: ${d.assignedVolunteerName}"
                        snippet = "Live delivery sharing active"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    }
                    map.overlays.add(volMarker)
                }
            }
            map.invalidate()
        }
    )
}
