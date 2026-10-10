package com.curiouskids.cashapp

import androidx.compose.runtime.Composable
import android.hardware.usb.UsbDevice
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.curiouskids.cashapp.feature.home.ui.HomeScreen
import com.curiouskids.cashapp.feature.home.ui.HomeViewModel
import com.curiouskids.cashapp.feature.home.PlaceholderScreen
import com.curiouskids.cashapp.feature.profile.ui.ProfileScreen
import com.curiouskids.cashapp.feature.profile.ui.ProfileViewModel
import com.curiouskids.cashapp.ui.UsbSerialScreen
import com.curiouskids.cashapp.ui.UsbViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.curiouskids.cashapp.feature.auth.ui.AuthScreen
import com.curiouskids.cashapp.feature.auth.ui.AuthViewModel

@Composable
fun SkyjumpersNavHost(
    usbViewModel: UsbViewModel,
    onRequestUsbPermission: (UsbDevice) -> Unit,
    startDestination: String,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable("auth") {
            val authViewModel: AuthViewModel = hiltViewModel()
            AuthScreen(
                viewModel = authViewModel,
                onAuthSuccess = {
                    navController.navigate("home") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            )
        }
        composable("home") {
            val homeViewModel: HomeViewModel = hiltViewModel()
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToRecharge = { navController.navigate("wallet_recharge") },
                onNavigateToHistory = { navController.navigate("wallet_history") },
                onNavigateToBookings = { navController.navigate("bookings") },
                onNavigateToOffers = { navController.navigate("offers") },
                onNavigateToProfile = { navController.navigate("profile") }
            )
        }
        composable("wallet_recharge") {
            PlaceholderScreen(onNavigateToKiosk = { navController.navigate("usb_kiosk") })
        }
        composable("wallet_history") {
            PlaceholderScreen(onNavigateToKiosk = { navController.navigate("usb_kiosk") })
        }
        composable("bookings") {
            PlaceholderScreen(onNavigateToKiosk = { navController.navigate("usb_kiosk") })
        }
        composable("offers") {
            PlaceholderScreen(onNavigateToKiosk = { navController.navigate("usb_kiosk") })
        }
        composable("profile") {
            val profileViewModel: ProfileViewModel = hiltViewModel()
            ProfileScreen(
                viewModel = profileViewModel,
                onNavigateBack = { navController.popBackStack() },
                onLogoutCompleted = {
                    navController.navigate("auth") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable("usb_kiosk") {
            UsbSerialScreen(
                viewModel = usbViewModel,
                onRequestPermission = onRequestUsbPermission
            )
        }
    }
}


