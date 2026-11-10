package com.mdpgroup08.scpd.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mdpgroup08.scpd.data.model.UserRole
import com.mdpgroup08.scpd.ui.auth.LoginScreen
import com.mdpgroup08.scpd.viewmodel.AppViewModelProvider
import com.mdpgroup08.scpd.viewmodel.SessionViewModel

/** Top-level screens: login, then one home per role. */
object Routes {
    const val LOGIN = "login"
    const val STUDENT_HOME = "student_home"
    const val CLUB_HOME = "club_home"
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
                    }
                    navController.navigate(home) {
                        popUpTo(Routes.LOGIN) { inclusive = true } // Back from the tabs exits the app
                    }
                }
            )
        }
        composable(Routes.STUDENT_HOME) { StudentHome(onLogOut = logOut) }
        composable(Routes.CLUB_HOME) { ClubHome(onLogOut = logOut) }
    }
}