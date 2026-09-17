package io.github.theodorelx.tunweave

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.theodorelx.tunweave.ui.screen.AppSelectScreen
import io.github.theodorelx.tunweave.ui.screen.HomeScreen
import io.github.theodorelx.tunweave.ui.screen.SettingsScreen
import io.github.theodorelx.tunweave.ui.theme.TunWeaveTheme
import io.github.theodorelx.tunweave.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TunWeaveTheme {
                val mainViewModel: MainViewModel = viewModel()
                val navController = rememberNavController()

                // VPN permission launcher
                val vpnPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == RESULT_OK) {
                        mainViewModel.connectVpn(this@MainActivity)
                    }
                }

                // Request notification permission on Android 13+
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { /* granted or not — VPN works either way */ }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                // Handle tile-triggered VPN connect
                LaunchedEffect(intent) {
                    if (intent?.getBooleanExtra("request_vpn_connect", false) == true) {
                        val prepareIntent = android.net.VpnService.prepare(this@MainActivity)
                        if (prepareIntent != null) {
                            vpnPermissionLauncher.launch(prepareIntent)
                        } else {
                            mainViewModel.connectVpn(this@MainActivity)
                        }
                    }
                }

                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            viewModel = mainViewModel,
                            onNavigateToSettings = { navController.navigate("settings") },
                            onRequestVpnPermission = { vpnPermissionLauncher.launch(it) },
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            viewModel = mainViewModel,
                            onBack = { navController.popBackStack() },
                            onNavigateToAppSelect = { navController.navigate("app_select") },
                        )
                    }
                    composable("app_select") {
                        AppSelectScreen(
                            viewModel = mainViewModel,
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}
