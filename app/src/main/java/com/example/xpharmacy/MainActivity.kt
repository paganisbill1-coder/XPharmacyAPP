package com.example.xpharmacy

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.delay
import kotlin.random.Random

// Μοντέλο δεδομένων προσθέτοντας συντεταγμένες (LatLng)
data class Pharmacy(
    val id: Int,
    val name: String,
    val distanceMeters: Int,
    val location: LatLng,
    var isOpen: Boolean
)

class MainActivity : ComponentActivity() {

    private val CHANNEL_ID = "pharmacy_channel"

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            PharmacyScreen()
        }
    }

    @Composable
    fun PharmacyScreen() {
        var pharmacies by remember {
            mutableStateOf(
                listOf(
                    Pharmacy(1, "Φαρμακείο 1", 120, LatLng(37.9838, 23.7275), false), // Κέντρο Αθήνας
                    Pharmacy(2, "Φαρμακείο 2", 300, LatLng(37.9850, 23.7300), false),
                    Pharmacy(3, "Φαρμακείο 3", 700, LatLng(37.9800, 23.7200), false),
                    Pharmacy(4, "Φαρμακείο 4", 1200, LatLng(37.9845, 23.7250), false) // Το πιο κοντινό
                ).sortedBy { it.distanceMeters }
            )
        }

        var closestWasOpen by remember { mutableStateOf(false) }

        // Αρχική θέση κάμερας στον χάρτη
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(LatLng(37.9838, 23.7275), 14f)
        }

        LaunchedEffect(Unit) {
            while (true) {
                delay(10000) // Αλλαγή κάθε 10 δευτερόλεπτα για να το τεστάρεις πιο γρήγορα

                val updatedList = pharmacies.map {
                    it.copy(isOpen = Random.nextBoolean())
                }
                pharmacies = updatedList

                val closestPharmacy = updatedList.firstOrNull()

                if (closestPharmacy != null) {
                    if (closestPharmacy.isOpen && !closestWasOpen) {
                        showNotification()
                    }
                    closestWasOpen = closestPharmacy.isOpen
                }
            }
        }

        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize()) {

                // Χάρτης Google Maps
                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp), // Το ύψος του χάρτη στην οθόνη
                    cameraPositionState = cameraPositionState
                ) {
                    pharmacies.forEach { pharmacy ->
                        Marker(
                            state = MarkerState(position = pharmacy.location),
                            title = pharmacy.name,
                            snippet = if (pharmacy.isOpen) "ΑΝΟΙΧΤΟ" else "ΚΛΕΙΣΤΟ",
                            icon = BitmapDescriptorFactory.defaultMarker(
                                if (pharmacy.isOpen) BitmapDescriptorFactory.HUE_GREEN else BitmapDescriptorFactory.HUE_RED
                            )
                        )
                    }
                }

                Text(
                    text = "Εφημερεύοντα Φαρμακεία",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(16.dp)
                )

                // Λίστα Φαρμακείων
                LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                    items(pharmacies) { pharmacy ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = pharmacy.name, style = MaterialTheme.typography.titleMedium)
                                Text(text = "Απόσταση: ${pharmacy.distanceMeters} μέτρα")
                                Text(
                                    text = if (pharmacy.isOpen) "ΕΦΗΜΕΡΕΥΕΙ (ΑΝΟΙΧΤΟ)" else "ΚΛΕΙΣΤΟ",
                                    color = if (pharmacy.isOpen) Color(0xFF4CAF50) else Color.Red,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Pharmacy Alerts"
            val descriptionText = "Ειδοποιήσεις για κοντινά φαρμακεία"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification() {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Ενημέρωση Φαρμακείου")
            .setContentText("Το κοντινότερο φαρμακείο σε εσάς μόλις άνοιξε!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                notify(101, builder.build())
            }
        }
    }
}