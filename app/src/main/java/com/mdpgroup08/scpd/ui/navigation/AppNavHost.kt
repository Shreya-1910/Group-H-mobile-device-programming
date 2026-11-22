package com.mdpgroup08.scpd.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mdpgroup08.scpd.data.model.UserRole
import com.mdpgroup08.scpd.ui.auth.LoginScreen
import com.mdpgroup08.scpd.ui.auth.RegisterScreen
import com.mdpgroup08.scpd.viewmodel.AppViewModelProvider
import com.mdpgroup08.scpd.viewmodel.SessionViewModel

/** Top-level screens: login, register, then one home per role (R6-S2). */
object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val STUDENT_HOME = "student_home"
    const val CLUB_HOME = "club_home"
    const val ADMIN_HOME = "admin_home"
}

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    sessionViewModel: SessionViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val logOut: () -> Unit = {
        sessionViewModel.signOut()
        navController.navigate(Routes.LOGIN) {
            popUpTo(0) { inclusive = true } // clear history, so Back can't return to the tabs
        }
    }

    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onSignedIn = { role ->
                    val home = when (role) {
                        UserRole.STUDENT -> Routes.STUDENT_HOME
                        UserRole.CLUB -> Routes.CLUB_HOME
                        UserRole.ADMIN -> Routes.ADMIN_HOME
                    }
                    navController.navigate(home) {
                        popUpTo(Routes.LOGIN) { inclusive = true } // Back from tabs exits app
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Routes.REGISTER)
                }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegistered = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.REGISTER) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.REGISTER) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.STUDENT_HOME) { StudentHome(onLogOut = logOut) }
        composable(Routes.CLUB_HOME) { ClubHome(onLogOut = logOut) }
        composable(Routes.ADMIN_HOME) { AdminHome(onLogOut = logOut) }
    }
}
