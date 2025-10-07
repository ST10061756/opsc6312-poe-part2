package com.example.exploreo.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.shouldShowRationale
import android.Manifest
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Logout
import com.google.firebase.auth.FirebaseAuth
import coil.compose.AsyncImage
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.Color

// Home screen with the map tab and a settings shortcut in the top app bar
@OptIn(ExperimentalMaterial3Api::class)
@Composable 
fun HomeScreen(onOpenSettings: () -> Unit, mapType: String, trafficEnabled: Boolean) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Exploreo") }, actions = {
                IconButton(onClick = onOpenSettings) { Icon(Icons.Filled.Settings, contentDescription = "Settings") }
            })
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            MapWithPermission(mapType = mapType, trafficEnabled = trafficEnabled)
        }
    }
}

// Settings where I control theme, language, map type, traffic and image quality
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    darkMode: Boolean,
    onToggleDark: (Boolean) -> Unit,
    language: String,
    onLanguageChange: (String) -> Unit,
    mapType: String,
    onMapTypeChange: (String) -> Unit,
    trafficEnabled: Boolean,
    onToggleTraffic: (Boolean) -> Unit,
    imageQuality: String,
    onImageQualityChange: (String) -> Unit,
    onLogout: () -> Unit,
) {
    // Local UI state mirrors current persisted settings
    var selectedLanguage by remember { mutableStateOf(language) }
    var showAccount by remember { mutableStateOf(false) }
    var selectedMapType by remember { mutableStateOf(mapType) }
    var selectedImageQuality by remember { mutableStateOf(imageQuality) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Dark Mode", modifier = Modifier.weight(1f))
            Switch(checked = darkMode, onCheckedChange = onToggleDark)
        }
        Spacer(Modifier.height(12.dp))
        Text("Language", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        PressAwareAssistChip(onClick = { selectedLanguage = "English"; onLanguageChange("English") }) { Text("English") }
        Spacer(Modifier.height(24.dp))
        Text("Map", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        // Map type selection stays highlighted for the selected option
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedMapType == "Normal",
                onClick = { selectedMapType = "Normal"; onMapTypeChange("Normal") },
                label = { Text("Normal") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
            FilterChip(
                selected = selectedMapType == "Satellite",
                onClick = { selectedMapType = "Satellite"; onMapTypeChange("Satellite") },
                label = { Text("Satellite") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
            FilterChip(
                selected = selectedMapType == "Terrain",
                onClick = { selectedMapType = "Terrain"; onMapTypeChange("Terrain") },
                label = { Text("Terrain") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Show traffic", modifier = Modifier.weight(1f))
            Switch(checked = trafficEnabled, onCheckedChange = onToggleTraffic)
        }
        Spacer(Modifier.height(24.dp))
        Text("Images", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PressAwareAssistChip(onClick = { selectedImageQuality = "High"; onImageQualityChange("High") }) { Text("High quality") }
            PressAwareAssistChip(onClick = { selectedImageQuality = "Standard"; onImageQualityChange("Standard") }) { Text("Standard") }
        }
        Spacer(Modifier.height(24.dp))
        Text("Account", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        PressAwareButton(onClick = { showAccount = true }) { Text("View account details") }
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onBack) { Text("Back") }
            PressAwareButton(onClick = onLogout) { Icon(Icons.Filled.Logout, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Logout") }
        }
        if (showAccount) {
            val user = FirebaseAuth.getInstance().currentUser
            val display = user?.displayName?.takeIf { !it.isNullOrBlank() } ?: (user?.email?.substringBefore('@')?.replace('.', ' ')?.replace('_', ' ')?.replace('-', ' ')?.split(' ')?.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } } ?: "Unknown")
            val email = user?.email ?: "Unknown"
            AlertDialog(
                onDismissRequest = { showAccount = false },
                confirmButton = { TextButton(onClick = { showAccount = false }) { Text("Close") } },
                title = { Text("Account details") },
                text = {
                    Column(Modifier.fillMaxWidth()) {
                        Text("Name: ${display}")
                        Text("Email: ${email}")
                    }
                }
            )
        }
    }
}

private fun mapImageResOrNull(title: String): Int? {
    return when {
        title.contains("Table Mountain", ignoreCase = true) -> com.example.exploreo.R.drawable.table_mountain_cape_town
        title.contains("Waterfront", ignoreCase = true) -> com.example.exploreo.R.drawable.v_and_a_waterfront
        title.contains("Apartheid Museum", ignoreCase = true) -> com.example.exploreo.R.drawable.apartheid_meseum
        title.contains("Union Buildings", ignoreCase = true) -> com.example.exploreo.R.drawable.union_buildings_pretoria
        title.contains("Vilakazi Street", ignoreCase = true) -> com.example.exploreo.R.drawable.v_and_a_waterfront // fallback if specific not provided
        title.contains("Cape of Good Hope", ignoreCase = true) -> com.example.exploreo.R.drawable.cape_of_good_hope
        title.contains("Kruger", ignoreCase = true) -> com.example.exploreo.R.drawable.kruger_national_park
        title.contains("Blyde River Canyon", ignoreCase = true) -> com.example.exploreo.R.drawable.blyde_river_canyon
        title.contains("God's Window", ignoreCase = true) -> com.example.exploreo.R.drawable.gods_window
        title.contains("Durban Golden Mile", ignoreCase = true) -> com.example.exploreo.R.drawable.durban_golden_mile
        title.contains("uShaka", ignoreCase = true) -> com.example.exploreo.R.drawable.ushaka_marine_world
        title.contains("Drakensberg", ignoreCase = true) -> com.example.exploreo.R.drawable.drakensberg_amphitheatre
        title.contains("Addo Elephant", ignoreCase = true) -> com.example.exploreo.R.drawable.addo_elephant_park
        title.contains("Knysna Heads", ignoreCase = true) -> com.example.exploreo.R.drawable.knysna_heads
        title.contains("Hermanus", ignoreCase = true) -> com.example.exploreo.R.drawable.hermanus_whale_watching
        title.contains("Stellenbosch", ignoreCase = true) -> com.example.exploreo.R.drawable.stellenbosch_wine_route
        else -> null
    }
}

@Composable
private fun PressAwareButton(onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val colors = ButtonDefaults.outlinedButtonColors(
        containerColor = if (pressed) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    )
    OutlinedButton(onClick = onClick, interactionSource = interaction, colors = colors, border = ButtonDefaults.outlinedButtonBorder) {
        content()
    }
}

@Composable
private fun PressAwareAssistChip(onClick: () -> Unit, label: @Composable () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val colors = AssistChipDefaults.assistChipColors(
        containerColor = if (pressed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        labelColor = if (pressed) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    )
    AssistChip(onClick = onClick, label = label, interactionSource = interaction, colors = colors)
}

// Small helper that handles location permission, then shows the Google Map
@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun MapWithPermission(mapType: String, trafficEnabled: Boolean) {
    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    LaunchedEffect(Unit) { if (!locationPermission.status.isGranted) locationPermission.launchPermissionRequest() }
    if (locationPermission.status.isGranted) {
        val sa = LatLng(-28.4793, 24.6727) // Roughly central South Africa
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(sa, 5.5f)
        }
        val context = LocalContext.current

        // Curated list of notable places across South Africa
        val places = remember {
            listOf(
                "Table Mountain, Cape Town" to LatLng(-33.9628, 18.4098),
                "V&A Waterfront, Cape Town" to LatLng(-33.9036, 18.4207),
                "Cape of Good Hope" to LatLng(-34.3568, 18.4740),
                "Kirstenbosch Gardens" to LatLng(-33.9881, 18.4329),
                "Robben Island" to LatLng(-33.8066, 18.3662),
                "Kruger National Park (Skukuza)" to LatLng(-24.9923, 31.5969),
                "God's Window, Mpumalanga" to LatLng(-24.8782, 30.8947),
                "Blyde River Canyon" to LatLng(-24.5850, 30.8074),
                "Durban Golden Mile" to LatLng(-29.8527, 31.0456),
                "uShaka Marine World" to LatLng(-29.8677, 31.0456),
                "Drakensberg Amphitheatre" to LatLng(-28.6856, 28.9360),
                "Clarens, Free State" to LatLng(-28.5130, 28.4234),
                "Addo Elephant Park" to LatLng(-33.4459, 25.7499),
                "Garden Route (Knysna Heads)" to LatLng(-34.0809, 23.0606),
                "Hermanus Whale Watching" to LatLng(-34.4187, 19.2410),
                "Stellenbosch Wine Route" to LatLng(-33.9366, 18.8610),
                "Franschhoek Wine Tram" to LatLng(-33.9115, 19.1214),
                "Johannesburg Apartheid Museum" to LatLng(-26.2367, 28.0083),
                "Soweto (Vilakazi Street)" to LatLng(-26.2360, 27.9304),
                "Union Buildings, Pretoria" to LatLng(-25.7402, 28.2120)
            )
        }

        val properties = MapProperties(
            isMyLocationEnabled = true,
            mapType = when (mapType) {
                "Satellite" -> com.google.maps.android.compose.MapType.SATELLITE
                "Terrain" -> com.google.maps.android.compose.MapType.TERRAIN
                else -> com.google.maps.android.compose.MapType.NORMAL
            },
            isTrafficEnabled = trafficEnabled,
        )

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = properties,
            uiSettings = MapUiSettings(myLocationButtonEnabled = true, zoomControlsEnabled = true)
        ) {
            var selected by remember { mutableStateOf<Pair<String, LatLng>?>(null) }
            places.forEach { (title, latLng) ->
                Marker(
                    state = MarkerState(position = latLng),
                    title = title,
                    onClick = {
                        selected = title to latLng
                        true
                    }
                )
            }
            if (selected != null) {
                val (title, pos) = selected!!
                val photoUrl = "https://maps.googleapis.com/maps/api/streetview?size=600x300&location=${pos.latitude},${pos.longitude}&fov=80&pitch=0&key=" + (try { context.packageManager.getApplicationInfo(context.packageName, android.content.pm.PackageManager.GET_META_DATA).metaData?.getString("com.google.android.geo.API_KEY") } catch (e: Exception) { "" })
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { selected = null },
                    confirmButton = {
                        TextButton(onClick = {
                            val uri = Uri.parse("google.navigation:q=${pos.latitude},${pos.longitude}(${Uri.encode(title)})")
                            val intent = Intent(Intent.ACTION_VIEW, uri).apply { setPackage("com.google.android.apps.maps") }
                            context.startActivity(intent)
                        }) { Text("Navigate") }
                    },
                    dismissButton = { TextButton(onClick = { selected = null }) { Text("Close") } },
                    title = { Text(title) },
                    text = {
                        Column {
                            val localRes = mapImageResOrNull(title)
                            if (localRes != null) {
                                AsyncImage(model = localRes, contentDescription = null, modifier = Modifier.fillMaxWidth().height(160.dp))
                            } else {
                                AsyncImage(model = photoUrl, contentDescription = null, modifier = Modifier.fillMaxWidth().height(160.dp))
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("A must-see destination. Tap Navigate to get directions.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                )
            }
        }
    } else {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Location permission needed to show nearby spots")
            if (locationPermission.status.shouldShowRationale) {
                Text("Please grant permission in Settings")
            }
        }
    }
}



