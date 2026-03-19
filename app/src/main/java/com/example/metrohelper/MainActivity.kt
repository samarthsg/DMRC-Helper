package com.example.metrohelper

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Subway
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.metrohelper.ui.theme.MetroHelperTheme
import com.google.android.gms.location.LocationServices
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


data class DashboardItem(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

data class MetroStation(
    val name: String,
    val lat: Double,
    val lon: Double
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                100
            )
        }

        enableEdgeToEdge()

        setContent {
            MetroHelperTheme {
                MetroApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetroApp() {
    val navController = rememberNavController()
    val items = listOf(
        BottomNavItem("metromap", "Metro Map", Icons.Default.Map),
        BottomNavItem("neareststation", "Nearby", Icons.Default.LocationOn), // ✅ NEW
        BottomNavItem("booktickets", "Tickets", Icons.Default.ConfirmationNumber),
        BottomNavItem("availableparkings", "Parkings", Icons.Default.LocalParking),
        BottomNavItem("aboutapp", "About App", Icons.Default.Info)
    )


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DMRC Helper") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )

        },
        bottomBar = {
            BottomNavigationBar(navController, items)
        }
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            NavigationGraph(navController)
        }
    }
}

data class BottomNavItem(
    val route: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun BottomNavigationBar(
    navController: NavHostController,
    items: List<BottomNavItem>
) {

    val currentRoute = currentRoute(navController)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {

            items.forEach { item ->

                val selected = currentRoute == item.route

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()          // ✅ Use full height
                        .clickable {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center   // ✅ Center vertically
                ) {


                Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = if (selected)
                            Color(0xFFD32F2F)
                        else
                            Color.Gray,
                        modifier = Modifier.size(26.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = item.title,
                        fontSize = 11.sp,
                        fontWeight = if (selected)
                            FontWeight.Bold
                        else
                            FontWeight.Normal,
                        color = if (selected)
                            Color(0xFFD32F2F)
                        else
                            Color.Gray
                    )

                    // Indicator
                    if (selected) {
                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .height(3.dp)
                                .background(
                                    Color(0xFFD32F2F),
                                    RoundedCornerShape(50)
                                )
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun NavigationGraph(navController: NavHostController) {
    NavHost(navController, startDestination = "metromap") {

        composable("metromap") { MetroMapScreen() }

        composable("neareststation") { NearestStationScreen() } // ✅ NEW

        composable("booktickets") { BookTicketsScreen() }
        composable("availableparkings") { AvailableParkingsScreen() }
        composable("aboutapp") { AboutAppScreen() }
    }

}

@Composable
fun MetroMapScreen() {

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {

                detectTransformGestures { _, pan, zoom, _ ->

                    // Limit zoom
                    val newScale =
                        (scale * zoom).coerceIn(1f, 4f)

                    scale = newScale

                    // Apply pan with limits
                    val maxOffset = 500f * (newScale - 1)

                    offsetX =
                        (offsetX + pan.x)
                            .coerceIn(-maxOffset, maxOffset)

                    offsetY =
                        (offsetY + pan.y)
                            .coerceIn(-maxOffset, maxOffset)
                }
            },
        contentAlignment = Alignment.Center
    ) {

        Image(
            painter = painterResource(id = R.drawable.metromap),
            contentDescription = "Delhi Metro Map",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                ),
            contentScale = ContentScale.Fit
        )
    }
}

/*----------------- Nearby Station Finder ---------*/

fun loadMetroStations(context: Context): List<MetroStation> {

    val stations = mutableListOf<MetroStation>()

    val jsonString = context.assets
        .open("dmrc_stations.json")
        .bufferedReader()
        .use { it.readText() }

    val jsonArray = JSONArray(jsonString)

    for (i in 0 until jsonArray.length()) {

        val obj = jsonArray.getJSONObject(i)

        stations.add(
            MetroStation(
                obj.getString("Station"),
                obj.getDouble("Latitude"),
                obj.getDouble("Longitude")
            )
        )
    }

    return stations
}

fun findNearestStation(
    userLat: Double,
    userLon: Double,
    stations: List<MetroStation>
): MetroStation? {

    return stations.minByOrNull {

        val result = FloatArray(1)

        android.location.Location.distanceBetween(
            userLat,
            userLon,
            it.lat,
            it.lon,
            result
        )

        result[0]
    }
}

fun calculateDistance(
    userLat: Double,
    userLon: Double,
    stationLat: Double,
    stationLon: Double
): Float {

    val result = FloatArray(1)

    android.location.Location.distanceBetween(
        userLat,
        userLon,
        stationLat,
        stationLon,
        result
    )

    return result[0] / 1000   // km
}

suspend fun getRoute(
    startLat: Double,
    startLon: Double,
    endLat: Double,
    endLon: Double
): List<GeoPoint> {

    return withContext(Dispatchers.IO) {

        val url =
            "https://router.project-osrm.org/route/v1/driving/$startLon,$startLat;$endLon,$endLat?overview=full&geometries=geojson"

        val client = OkHttpClient()

        val request = Request.Builder()
            .url(url)
            .build()

        val response = client.newCall(request).execute()

        val json = JSONObject(response.body!!.string())

        val coords = json
            .getJSONArray("routes")
            .getJSONObject(0)
            .getJSONObject("geometry")
            .getJSONArray("coordinates")

        val points = mutableListOf<GeoPoint>()

        for (i in 0 until coords.length()) {

            val coord = coords.getJSONArray(i)

            points.add(
                GeoPoint(
                    coord.getDouble(1),
                    coord.getDouble(0)
                )
            )
        }

        points
    }
}

@SuppressLint("MissingPermission")
@Composable
fun NearestStationScreen() {

    val context = LocalContext.current

    var userLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var nearestStation by remember { mutableStateOf<MetroStation?>(null) }
    var routePoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }

    val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(context)

    val stations = remember { loadMetroStations(context) }

    //---- GET USER LOCATION
    LaunchedEffect(Unit) {

        fusedLocationClient.getCurrentLocation(
            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
            null
        ).addOnSuccessListener { location ->

                location?.let {

                    userLocation = GeoPoint(
                        it.latitude,
                        it.longitude
                    )

                    nearestStation =
                        findNearestStation(
                            it.latitude,
                            it.longitude,
                            stations
                        )
                }
            }
    }

    //----- GET ROUTE AFTER LOCATION + STATION ARE KNOWN
    LaunchedEffect(userLocation, nearestStation) {

        if (userLocation != null && nearestStation != null) {

            routePoints = getRoute(
                userLocation!!.latitude,
                userLocation!!.longitude,
                nearestStation!!.lat,
                nearestStation!!.lon
            )
        }
    }

    //--- UI
    Column(Modifier.fillMaxSize()) {

        nearestStation?.let {

            Text(
                text = "Nearest Station: ${it.name}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(10.dp)
            )
        }

        AndroidView(
            modifier = Modifier.fillMaxSize(),

            factory = { ctx ->

                val mapView = MapView(ctx)

                mapView.setMultiTouchControls(true)

                mapView.controller.setZoom(15.0)

                mapView
            },

            update = { mapView ->

                mapView.overlays.clear()

                userLocation?.let { userPoint ->

                    mapView.controller.setCenter(userPoint)

                    val userMarker = Marker(mapView)

                    userMarker.position = userPoint
                    userMarker.title = "You are here"

                    mapView.overlays.add(userMarker)
                }

                nearestStation?.let { station ->

                    val stationPoint =
                        GeoPoint(station.lat, station.lon)

                    val stationMarker = Marker(mapView)

                    stationMarker.position = stationPoint
                    stationMarker.title = station.name

                    mapView.overlays.add(stationMarker)
                }

                if (routePoints.isNotEmpty()) {

                    val polyline = Polyline()

                    polyline.setPoints(routePoints)

                    polyline.outlinePaint.color =
                        android.graphics.Color.BLUE

                    polyline.outlinePaint.strokeWidth = 8f

                    mapView.overlays.add(polyline)
                }

                mapView.invalidate()
            }
        )
    }
}


@Composable
fun BookTicketsScreen() {

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "Book Metro Tickets",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // DMRC Website
        TicketOptionCard(
            icon = Icons.Default.ConfirmationNumber,
            title = "DMRC Official Booking",
            subtitle = "Book via DMRC website",
            backgroundColor = Color(0xFFD32F2F)
        ) {
            openLinkSafely(
                context,
                "https://qrticket.dmrc.org/qrapp/"
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // WhatsApp Booking
        TicketOptionCard(
            icon = Icons.Default.Info,
            title = "Book via WhatsApp",
            subtitle = "DMRC WhatsApp service",
            backgroundColor = Color(0xFF25D366)
        ) {

            val message = "Hi DMRC, I want to book a metro ticket."

            val encodedMessage = Uri.encode(message)

            val whatsappUrl =
                "https://wa.me/919650855800?text=$encodedMessage"

            openLinkSafely(context, whatsappUrl)
        }

    }
}
fun openLinkSafely(context: Context, url: String) {

    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)

    } catch (e: Exception) {

        Toast.makeText(
            context,
            "Please install a browser or WhatsApp to continue",
            Toast.LENGTH_LONG
        ).show()
    }
}

// -------- Google Maps Nearest Loc fun() --------
/*fun openNearestMetroInMaps(context: Context) {

    try {
        // Google Maps search intent
        val uri = Uri.parse("geo:0,0?q=nearest DMRC metro station")

        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }

        context.startActivity(intent)

    } catch (e: Exception) {

        // Fallback to browser if Maps not installed
        openLinkSafely(
            context,
            "https://www.google.com/maps/search/nearest+DMRC+metro+station"
        )
    }
} */


@Composable
fun TicketOptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}



@Composable
fun AvailableParkingsScreen() {
    CenteredText("Available Parkings Screen 🅿️")
}

@Composable
fun AboutAppScreen() {
    CenteredText("About App Screen ℹ️")
}

@Composable
fun CenteredText(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

/* Optional: Dashboard-like grid reused later */
@Composable
fun DashboardGrid() {
    val items = listOf(
        DashboardItem("Plan Journey", Icons.Filled.Subway),
        DashboardItem("Nearest Station", Icons.Filled.LocationOn),
        DashboardItem("Live Status", Icons.Filled.Train),
        DashboardItem("Fare Calculator", Icons.Filled.AccountBalanceWallet)
    )

    LazyVerticalGrid(columns = GridCells.Fixed(2)) {
        items(items) { item -> DashboardCard(item) }
    }
}

@Composable
fun DashboardCard(item: DashboardItem) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .height(130.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = Color(0xFFD32F2F),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun currentRoute(navController: NavHostController): String? {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    return navBackStackEntry?.destination?.route
}

@Preview(showBackground = true)
@Composable
fun MetroAppPreview() {
    MetroHelperTheme {
        MetroApp()
    }
}
