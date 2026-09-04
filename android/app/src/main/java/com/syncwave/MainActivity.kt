package com.syncwave

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.syncwave.core.network.BuildConfigCompat
import com.syncwave.feature.audio.AudioGuestScreen
import com.syncwave.feature.audio.AudioShareScreen
import com.syncwave.feature.home.HomeScreen
import com.syncwave.feature.host.HostScreen
import com.syncwave.feature.receiver.ReceiverScreen
import com.syncwave.feature.room.RoomCodeScreen
import com.syncwave.feature.scan.QrScannerScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        BuildConfigCompat.setBaseUrl(BuildConfig.SYNCWAVE_BASE_URL)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val nav = rememberNavController()
                    NavHost(navController = nav, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                onHost = { nav.navigate("host") },
                                onJoin = { nav.navigate("join") },
                                onAudio = { nav.navigate("audio") }
                            )
                        }
                        composable("host") { HostScreen(onBack = { nav.popBackStack() }) }
                        composable("audio") { AudioShareScreen(onBack = { nav.popBackStack() }) }
                        composable("join") {
                            RoomCodeScreen(
                                onCancel = { nav.popBackStack() },
                                onJoined = { code -> nav.navigate("receiver/$code") },
                                onJoinAudio = { code -> nav.navigate("audio_guest/$code") },
                                onScan = { nav.navigate("scan") }
                            )
                        }
                        composable("scan") {
                            QrScannerScreen(
                                onCode = { code ->
                                    nav.navigate("receiver/$code") { popUpTo("join") }
                                },
                                onCancel = { nav.popBackStack() }
                            )
                        }
                        composable("audio_guest/{code}") { entry ->
                            val code = entry.arguments?.getString("code").orEmpty()
                            AudioGuestScreen(roomCode = code, onBack = { nav.popBackStack() })
                        }
                        composable("receiver/{code}") { entry ->
                            val code = entry.arguments?.getString("code").orEmpty()
                            ReceiverScreen(roomCode = code, onBack = { nav.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}
