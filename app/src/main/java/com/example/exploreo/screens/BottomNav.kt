package com.example.exploreo.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import android.content.Intent
import android.net.Uri
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.google.android.gms.maps.model.LatLng
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ArrowBack
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavType
import androidx.compose.ui.Alignment
import androidx.navigation.navArgument
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState

private object Tabs {
    const val MAP = "tab_map"
    const val EXPLORE = "tab_explore"
    const val BOOKMARKS = "tab_bookmarks"
    const val PLANNER = "tab_planner"
    const val SETTINGS = "tab_settings"
}

// App shell with bottom navigation that hosts all the tabs
@Composable
fun HomeNavShell(
    onLogout: () -> Unit,
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
) {
    val navController = rememberNavController()
    val items = listOf(
        Triple(Tabs.MAP, Icons.Filled.Map, "Map"),
        Triple(Tabs.EXPLORE, Icons.Filled.Explore, "Explore"),
        Triple(Tabs.BOOKMARKS, Icons.Filled.Bookmark, "Bookmarks"),
        Triple(Tabs.PLANNER, Icons.Filled.PlaylistAdd, "Planner"),
        Triple(Tabs.SETTINGS, Icons.Filled.Settings, "Settings"),
    )
    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route
                items.forEach { (route, icon, label) ->
                    NavigationBarItem(
                        selected = currentRoute == route,
                        onClick = {
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Tabs.EXPLORE,
            modifier = Modifier.padding(padding)
        ) {
            composable(Tabs.MAP) { HomeScreen(onOpenSettings = { navController.navigate(Tabs.SETTINGS) }, mapType = mapType, trafficEnabled = trafficEnabled) }
            composable(Tabs.EXPLORE) { ExploreScreen(imageQuality = imageQuality) }
            composable(Tabs.BOOKMARKS) { BookmarksScreen(imageQuality = imageQuality, onOpen = { b -> navController.navigate("bookmark/${b.id}") }) }
            composable(Tabs.PLANNER) { PlannerScreen(onOpenSaved = { navController.navigate("planner/saved") }) }
            composable(Tabs.SETTINGS) {
                SettingsScreen(
                    onBack = { navController.navigate(Tabs.MAP) },
                    darkMode = darkMode,
                    onToggleDark = onToggleDark,
                    language = language,
                    onLanguageChange = onLanguageChange,
                    mapType = mapType,
                    onMapTypeChange = onMapTypeChange,
                    trafficEnabled = trafficEnabled,
                    onToggleTraffic = onToggleTraffic,
                    imageQuality = imageQuality,
                    onImageQualityChange = onImageQualityChange,
                    onLogout = onLogout,
                )
            }
            composable("planner/saved") { PlannerSavedScreen(onBack = { navController.popBackStack() }) }
            composable(
                route = "bookmark/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id") ?: ""
                BookmarkDetailScreen(id = id, onBack = { navController.popBackStack() }, imageQuality = imageQuality)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
// Explore screen shows curated places by province and quick bookmarking
@Composable
fun ExploreScreen(imageQuality: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val pm = context.packageManager
    val ai = pm.getApplicationInfo(context.packageName, android.content.pm.PackageManager.GET_META_DATA)
    val googleKey = ai.metaData?.getString("com.google.android.geo.AIzaSyDus4_Qj5by0wfU6z4h2NkevI2QlKRWP2Y") ?: ""
    val scope = rememberCoroutineScope()

    // Simple model for attractions I render in a grid
    data class Attraction(val name: String, val coords: LatLng)
    val provinces: Map<String, List<Attraction>> = remember {
        mapOf(
            "Western Cape" to listOf(
                Attraction("Table Mountain, Cape Town", LatLng(-33.9628, 18.4098)),
                Attraction("V&A Waterfront", LatLng(-33.9036, 18.4207)),
                Attraction("Cape of Good Hope", LatLng(-34.3568, 18.4740)),
                Attraction("Stellenbosch Wine Route", LatLng(-33.9366, 18.8610)),
                Attraction("Hermanus Whale Watching", LatLng(-34.4187, 19.2410)),
            ),
            "Gauteng" to listOf(
                Attraction("Apartheid Museum", LatLng(-26.2367, 28.0083)),
                Attraction("Vilakazi Street, Soweto", LatLng(-26.2360, 27.9304)),
                Attraction("Union Buildings, Pretoria", LatLng(-25.7402, 28.2120)),
            ),
            "Mpumalanga" to listOf(
                Attraction("Kruger National Park (Skukuza)", LatLng(-24.9923, 31.5969)),
                Attraction("Blyde River Canyon", LatLng(-24.5850, 30.8074)),
                Attraction("God's Window", LatLng(-24.8782, 30.8947)),
            ),
            "KwaZulu-Natal" to listOf(
                Attraction("Durban Golden Mile", LatLng(-29.8527, 31.0456)),
                Attraction("uShaka Marine World", LatLng(-29.8677, 31.0456)),
                Attraction("Drakensberg Amphitheatre", LatLng(-28.6856, 28.9360)),
            ),
            "Eastern Cape" to listOf(
                Attraction("Addo Elephant Park", LatLng(-33.4459, 25.7499)),
                Attraction("Tsitsikamma National Park", LatLng(-34.0329, 23.8879)),
            ),
            "Free State" to listOf(
                Attraction("Golden Gate Highlands Park", LatLng(-28.5162, 28.6155)),
                Attraction("Clarens", LatLng(-28.5130, 28.4234)),
            ),
            "North West" to listOf(
                Attraction("Pilanesberg National Park", LatLng(-25.2510, 27.0819)),
                Attraction("Sun City", LatLng(-25.3442, 27.0996)),
            ),
            "Limpopo" to listOf(
                Attraction("Mapungubwe National Park", LatLng(-22.1963, 29.3957)),
                Attraction("Bela-Bela Hot Springs", LatLng(-24.8833, 28.2833)),
            ),
            "Northern Cape" to listOf(
                Attraction("Augrabies Falls", LatLng(-28.5965, 20.3397)),
                Attraction("Kgalagadi Transfrontier Park", LatLng(-26.4470, 20.6111)),
            ),
            "Garden Route" to listOf(
                Attraction("Knysna Heads", LatLng(-34.0809, 23.0606)),
                Attraction("Mossel Bay Point", LatLng(-34.1808, 22.1460)),
            ),
        )
    }

    // Snackbars for bookmark actions
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        SnackbarHost(hostState = snackbarHostState)
        Text("Explore South Africa by Province", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

        provinces.forEach { (province, items) ->
            ElevatedCard {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Text(province, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(160.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 0.dp, max = 360.dp)
                    ) {
                        items(items) { a ->
                            val size = if (imageQuality == "High") "800x600" else "600x400"
                            val photoUrl = "https://maps.googleapis.com/maps/api/streetview?size=${size}&location=${a.coords.latitude},${a.coords.longitude}&fov=80&pitch=0&key=${googleKey}"
                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val uri = Uri.parse("google.navigation:q=${a.coords.latitude},${a.coords.longitude}(${Uri.encode(a.name)})")
                                        val intent = Intent(Intent.ACTION_VIEW, uri).apply { setPackage("com.google.android.apps.maps") }
                                        context.startActivity(intent)
                                    }
                            ) {
                                Column(Modifier.padding(8.dp)) {
                                    val localRes = attractionImageResOrNull(a.name)
                                    if (localRes != null) {
                                        Image(
                                            painter = painterResource(id = localRes),
                                            contentDescription = a.name,
                                            modifier = Modifier.fillMaxWidth().height(160.dp),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        AsyncImage(
                                            model = photoUrl,
                                            contentDescription = a.name,
                                            modifier = Modifier.fillMaxWidth().height(160.dp),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(a.name, style = MaterialTheme.typography.titleSmall, maxLines = 2)
                                    Spacer(Modifier.height(6.dp))
                                    val repo = com.example.exploreo.data.FirestoreRepository()
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        PressAwareButton(onClick = { scope.launch {
                                            val existing = repo.listBookmarks()
                                            val already = existing.any { it.placeName.equals(a.name, ignoreCase = true) || (Math.abs(it.lat - a.coords.latitude) < 1e-5 && Math.abs(it.lon - a.coords.longitude) < 1e-5) }
                                            if (already) {
                                                snackbarHostState.showSnackbar("You have already added to bookmarks")
                                            } else {
                                                repo.addBookmark(
                                                    com.example.exploreo.data.Place(
                                                        name = a.name,
                                                        lon = a.coords.longitude,
                                                        lat = a.coords.latitude,
                                                        country = "South Africa",
                                                        city = null,
                                                        categories = listOf("tourism.sights")
                                                    )
                                                )
                                                snackbarHostState.showSnackbar("Added to Bookmarks")
                                            }
                                        } }) { Text("Bookmark") }
                                        PressAwareButton(onClick = {
                                            val uri = Uri.parse("geo:${a.coords.latitude},${a.coords.longitude}?q=${Uri.encode(a.name)}")
                                            val intent = Intent(Intent.ACTION_VIEW, uri).apply { setPackage("com.google.android.apps.maps") }
                                            context.startActivity(intent)
                                        }) { Text("Navigate") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun attractionImageResOrNull(name: String): Int? {
    return when {
        name.contains("Table Mountain", ignoreCase = true) -> com.example.exploreo.R.drawable.table_mountain_cape_town
        name.contains("Apartheid Museum", ignoreCase = true) -> com.example.exploreo.R.drawable.apartheid_meseum
        name.contains("Union Buildings", ignoreCase = true) -> com.example.exploreo.R.drawable.union_buildings_pretoria
        name.contains("uShaka", ignoreCase = true) -> com.example.exploreo.R.drawable.ushaka_marine_world
        name.startsWith("Addo Elephant Park") -> com.example.exploreo.R.drawable.addo_elephant_park
        name.startsWith("Blyde River Canyon") -> com.example.exploreo.R.drawable.blyde_river_canyon
        name.contains("Drakensberg Amphitheatre") -> com.example.exploreo.R.drawable.drakensberg_amphitheatre
        name.startsWith("Durban Golden Mile") -> com.example.exploreo.R.drawable.durban_golden_mile
        name.startsWith("God's Window") -> com.example.exploreo.R.drawable.gods_window
        name.startsWith("Golden Gate Highlands") -> com.example.exploreo.R.drawable.golden_gate_highlands
        name.contains("Kgalagadi Transfrontier Park") -> com.example.exploreo.R.drawable.kgalagadi_transfrontier_park
        name.contains("Knysna Heads") -> com.example.exploreo.R.drawable.knysna_heads
        name.startsWith("Kruger National Park") -> com.example.exploreo.R.drawable.kruger_national_park
        name.startsWith("Mapungubwe National Park") -> com.example.exploreo.R.drawable.mapungubwe_national_park
        name.startsWith("Pilanesberg National Park") -> com.example.exploreo.R.drawable.pilanesberg_national_park
        name.startsWith("Tsitsikamma National Park") -> com.example.exploreo.R.drawable.tsitsikamma_national_park
        name.contains("Augrabies Falls") -> com.example.exploreo.R.drawable.augrabies_falls
        name.contains("V&A Waterfront") || name.contains("Waterfront") -> com.example.exploreo.R.drawable.v_and_a_waterfront
        name.contains("Cape of Good Hope") -> com.example.exploreo.R.drawable.cape_of_good_hope
        name.contains("Stellenbosch Wine Route") -> com.example.exploreo.R.drawable.stellenbosch_wine_route
        name.contains("Hermanus Whale Watching") || name.contains("Hermanus") -> com.example.exploreo.R.drawable.hermanus_whale_watching

        else -> null
    }
}

// Bookmarks page lists saved places with quick open/remove actions
@Composable
fun BookmarksScreen(imageQuality: String, onOpen: (com.example.exploreo.data.Bookmark) -> Unit = {}) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var items by remember { mutableStateOf<List<com.example.exploreo.data.Bookmark>>(emptyList()) }
    val repo = remember { com.example.exploreo.data.FirestoreRepository() }
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        loading = true
        items = repo.listBookmarks()
        loading = false
    }
    val apiKey = androidx.compose.ui.platform.LocalContext.current.getString(com.example.exploreo.R.string.geoapify_api_key)
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Your bookmarks", style = MaterialTheme.typography.titleLarge)
        if (loading) { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
        Spacer(Modifier.height(8.dp))
        androidx.compose.foundation.lazy.LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items.size) { idx ->
                val b = items[idx]
                ElevatedCard(modifier = Modifier.clickable { onOpen(b) }) { 
                    Column(Modifier.padding(12.dp)) {
                        val (w, h) = if (imageQuality == "High") 800 to 400 else 600 to 300
                        val staticUrl = "https://maps.geoapify.com/v1/staticmap?style=osm-bright&width=${w}&height=${h}&marker=lonlat:${b.lon},${b.lat};type:material;color:%23FFD700;size:large&zoom=14&apiKey=${apiKey}"
                        AsyncImage(model = staticUrl, contentDescription = null, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        Text(b.placeName, style = MaterialTheme.typography.titleMedium)
                        Text("${b.lat}, ${b.lon}", style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PressAwareButton(onClick = { scope.launch { repo.removeBookmark(b.id); items = repo.listBookmarks() } }) { Text("Remove") }
                            PressAwareButton(onClick = {
                                val uri = Uri.parse("geo:${b.lat},${b.lon}?q=${Uri.encode(b.placeName)}")
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                intent.setPackage("com.google.android.apps.maps")
                                context.startActivity(intent)
                            }) { Text("Navigate") }
                        }
                    }
                }
            }
        }
    }
}

// Outlined button that slightly fills when pressed (nice affordance)
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

@OptIn(ExperimentalMaterial3Api::class)
// Bookmark details with a bigger static map and actions
@Composable
fun BookmarkDetailScreen(id: String, onBack: () -> Unit, imageQuality: String) {
    val repo = remember { com.example.exploreo.data.FirestoreRepository() }
    var bookmark by remember { mutableStateOf<com.example.exploreo.data.Bookmark?>(null) }
    var loading by remember { mutableStateOf(true) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val apiKey = androidx.compose.ui.platform.LocalContext.current.getString(com.example.exploreo.R.string.geoapify_api_key)
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(id) {
        loading = true
        bookmark = repo.getBookmark(id)
        loading = false
    }
    Scaffold(topBar = { TopAppBar(title = { Text(bookmark?.placeName ?: "Bookmark") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (loading) { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            bookmark?.let { b ->
                val (w, h) = if (imageQuality == "High") 1200 to 600 else 800 to 400
                val staticUrl = "https://maps.geoapify.com/v1/staticmap?style=osm-bright&width=${w}&height=${h}&marker=lonlat:${b.lon},${b.lat};type:material;color:%23FFD700;size:large&zoom=14&apiKey=${apiKey}"
                AsyncImage(model = staticUrl, contentDescription = null, modifier = Modifier.fillMaxWidth())
                Text(b.placeName, style = MaterialTheme.typography.headlineSmall)
                Text("${b.lat}, ${b.lon}", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PressAwareButton(onClick = {
                        val geo = Uri.parse("geo:${b.lat},${b.lon}?q=${Uri.encode(b.placeName)}")
                        val intent = Intent(Intent.ACTION_VIEW, geo).apply { setPackage("com.google.android.apps.maps") }
                        context.startActivity(intent)
                    }) { Text("Navigate") }
                    PressAwareButton(onClick = {
                        scope.launch { repo.removeBookmark(b.id); onBack() }
                    }) { Text("Remove") }
                    PressAwareButton(onClick = {
                        val shareText = "${b.placeName} (${b.lat}, ${b.lon})"
                        val sendIntent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, shareText) }
                        context.startActivity(Intent.createChooser(sendIntent, "Share place"))
                    }) { Text("Share") }
                }
            }
        }
    }
}

// Planner lets me build a trip itinerary and save it
@Composable
fun PlannerScreen(onOpenSaved: () -> Unit) {
    val repo = remember { com.example.exploreo.data.FirestoreRepository() }
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf(listOf<com.example.exploreo.data.ItineraryItem>()) }
    var showAdd by remember { mutableStateOf(false) }
    var lastId by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val apiKey = androidx.compose.ui.platform.LocalContext.current.getString(com.example.exploreo.R.string.geoapify_api_key)
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
        
        SnackbarHost(hostState = snackbarHostState)
        Text("Trip Planner", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onOpenSaved) { Icon(Icons.Filled.Bookmark, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Saved itineraries") }
            Button(onClick = { showAdd = true }) { Text("Add item") }
            OutlinedButton(onClick = { scope.launch { repo.getLatestItinerary()?.let { items = it.items } } }) { Text("Load latest") }
            OutlinedButton(onClick = {
                val shareText = buildString {
                    append("My Exploreo itinerary:\n")
                    items.forEachIndexed { i, it ->
                        val whenStr = it.time?.let { t ->
                            val d = it.dateMillis?.let { dm -> java.text.SimpleDateFormat("EEE, d MMM", java.util.Locale.getDefault()).format(java.util.Date(dm)) }
                            if (d != null) "$d $t" else t
                        } ?: ""
                        append("${i + 1}. ${it.title}${if (whenStr.isNotBlank()) " ($whenStr)" else ""}${if (it.note.isNotBlank()) ": ${it.note}" else ""}\n")
                    }
                }
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
                val chooser = Intent.createChooser(sendIntent, "Share itinerary")
                context.startActivity(chooser)
            }) { Text("Share") }
        }

        Spacer(Modifier.height(12.dp))
        androidx.compose.foundation.lazy.LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items.size) { idx ->
                val it = items[idx]
                ElevatedCard {
                    Column(Modifier.padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(it.title, style = MaterialTheme.typography.titleMedium)
                                if (it.note.isNotBlank()) Text(it.note, style = MaterialTheme.typography.bodyMedium)
                                val whenStr = it.time?.let { t ->
                                    val d = it.dateMillis?.let { dm -> java.text.SimpleDateFormat("EEE, d MMM", java.util.Locale.getDefault()).format(java.util.Date(dm)) }
                                    if (d != null) "$d $t" else t
                                }
                                if (!whenStr.isNullOrBlank()) Text(whenStr, style = MaterialTheme.typography.bodySmall)
                            }
                            if (it.lat != null && it.lon != null) {
                                val staticUrl = "https://maps.geoapify.com/v1/staticmap?style=osm-bright&width=400&height=180&marker=lonlat:${it.lon},${it.lat};type:material;color:%23FFD700;size:large&zoom=14&apiKey=${apiKey}"
                                AsyncImage(model = staticUrl, contentDescription = null, modifier = Modifier.width(140.dp).height(90.dp))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                if (it.lat != null && it.lon != null) {
                                    val uri = Uri.parse("google.navigation:q=${it.lat},${it.lon}(${Uri.encode(it.title)})")
                                    val intent = Intent(Intent.ACTION_VIEW, uri).apply { setPackage("com.google.android.apps.maps") }
                                    context.startActivity(intent)
                                }
                            }) { Text("Navigate") }
                            OutlinedButton(onClick = {
                                items = items.filterIndexed { i, _ -> i != idx }
                            }) { Text("Remove") }
                        }
                    }
                }
            }
        }

        if (lastId != null) { Text("Saved: ${lastId}") }
        }

        // Floating save action anchored above bottom navigation
        FloatingActionButton(
            onClick = {
                scope.launch {
                    if (items.isEmpty()) {
                        snackbarHostState.showSnackbar("Add at least one item before saving")
                        return@launch
                    }
                    val id = repo.saveItinerary(items)
                    lastId = id
                    if (id.isNullOrBlank()) {
                        snackbarHostState.showSnackbar("Please sign in to save your itinerary")
                    } else {
                        snackbarHostState.showSnackbar("Itinerary saved")
                    }
                }
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).padding(bottom = 72.dp),
            containerColor = Color(0xFFFFD54F), // amber 300 for a clear yellow
            contentColor = Color(0xFF000000)
        ) {
            Text("Save")
        }
    }

    if (showAdd) {
        var title by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }
        var dateMillis by remember { mutableStateOf<Long?>(null) }
        var time by remember { mutableStateOf<String?>(null) }
        var lat by remember { mutableStateOf("") }
        var lon by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            confirmButton = {
                TextButton(onClick = {
                    if (title.isNotBlank()) {
                        val latD = lat.toDoubleOrNull()
                        val lonD = lon.toDoubleOrNull()
                        items = items + com.example.exploreo.data.ItineraryItem(
                            title = title,
                            note = note,
                            dateMillis = dateMillis,
                            time = time,
                            lat = latD,
                            lon = lonD,
                        )
                        showAdd = false
                    }
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } },
            title = { Text("Add itinerary item") },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note (optional)") })
                    Spacer(Modifier.height(8.dp))
                    // Prefill from bookmarks
                    var bookmarks by remember { mutableStateOf<List<com.example.exploreo.data.Bookmark>>(emptyList()) }
                    val repoBk = remember { com.example.exploreo.data.FirestoreRepository() }
                    var expanded by remember { mutableStateOf(false) }
                    var selectedText by remember { mutableStateOf("") }
                    LaunchedEffect(Unit) { bookmarks = repoBk.listBookmarks() }
                    OutlinedTextField(
                        value = selectedText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Pick from bookmarks (optional)") },
                        trailingIcon = { TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Hide" else "Choose") } },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        bookmarks.forEach { b ->
                            DropdownMenuItem(text = { Text(b.placeName) }, onClick = {
                                selectedText = b.placeName
                                lat = b.lat.toString()
                                lon = b.lon.toString()
                                expanded = false
                            })
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = time ?: "", onValueChange = { time = it.ifBlank { null } }, label = { Text("Time e.g. 10:00") })
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = lat, onValueChange = { lat = it }, label = { Text("Latitude (optional)") })
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = lon, onValueChange = { lon = it }, label = { Text("Longitude (optional)") })
                }
            }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
// Saved itineraries page that lists all previously saved plans
@Composable
fun PlannerSavedScreen(onBack: () -> Unit) {
    val repo = remember { com.example.exploreo.data.FirestoreRepository() }
    var itineraries by remember { mutableStateOf<List<com.example.exploreo.data.Itinerary>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        loading = true
        // Load all saved itineraries for this user
        itineraries = repo.listItineraries()
        loading = false
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Saved itineraries") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (loading) { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            if (!loading && itineraries.isEmpty()) {
                Text("No saved itineraries yet", style = MaterialTheme.typography.bodyMedium)
            }
            itineraries.forEach { itin ->
                ElevatedCard {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Show a friendly title without exposing internal IDs
                        Text("Itinerary", style = MaterialTheme.typography.titleMedium)
                        itin.items.forEachIndexed { index, item ->
                            Column {
                                Text("${index + 1}. ${item.title}", style = MaterialTheme.typography.bodyLarge)
                                if (item.note.isNotBlank()) Text(item.note, style = MaterialTheme.typography.bodySmall)
                                val whenStr = item.time?.let { t ->
                                    val d = item.dateMillis?.let { dm -> java.text.SimpleDateFormat("EEE, d MMM", java.util.Locale.getDefault()).format(java.util.Date(dm)) }
                                    if (d != null) "$d $t" else t
                                }
                                if (!whenStr.isNullOrBlank()) Text(whenStr, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

