package com.syncwave

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.core.content.ContextCompat
import com.syncwave.core.network.BuildConfigCompat
import com.syncwave.feature.audio.AudioGuestScreen
import com.syncwave.feature.audio.AudioShareScreen
import com.syncwave.feature.home.HomeScreen
import com.syncwave.feature.room.RoomCodeScreen
import com.syncwave.feature.scan.QrScannerScreen

class MainActivity : ComponentActivity() {
    private val permissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.POST_NOTIFICATIONS
    )

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        val allGranted = granted.all { it.value }
        if (!allGranted) {
            Toast.makeText(this, "Permissions required for audio sharing", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        BuildConfigCompat.setBaseUrl(BuildConfig.SYNCWAVE_BASE_URL)

        requestPermissions.launch(permissions)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val nav = rememberNavController()
                    NavHost(navController = nav, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                onHost = { nav.navigate("audio") },
                                onJoin = { nav.navigate("join") },
                                onAudio = { nav.navigate("audio") }
                            )
                        }
                        composable("audio") { AudioShareScreen(onBack = { nav.popBackStack() }) }
                        composable("join") {
                            RoomCodeScreen(
                                onCancel = { nav.popBackStack() },
                                onJoined = { code -> nav.navigate("audio_guest/$code") },
                                onScan = { nav.navigate("scan") }
                            )
                        }
                        composable("scan") {
                            QrScannerScreen(
                                onCode = { code ->
                                    nav.navigate("audio_guest/$code") { popUpTo("join") }
                                },
                                onCancel = { nav.popBackStack() }
                            )
                        }
                        composable("audio_guest/{code}") { entry ->
                            val code = entry.arguments?.getString("code").orEmpty()
                            AudioGuestScreen(roomCode = code, onBack = { nav.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}
