package com.mdpgroup08.scpd.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mdpgroup08.scpd.ui.club.CreateEventScreen
import com.mdpgroup08.scpd.ui.club.MyEventsScreen

/** One bottom-bar tab. */
data class Tab(val route: String, val label: String, val icon: ImageVector)

// Student tabs (R6-S4)
private val studentTabs = listOf(
    Tab("student_marketplace", "Marketplace", Icons.Filled.Search),
    Tab("student_my_events", "My events", Icons.Filled.DateRange),
    Tab("student_points", "Points", Icons.Filled.Star),
    Tab("student_profile", "Profile", Icons.Filled.Person)
)

// Club tabs (R6-S5)
private val clubTabs = listOf(
    Tab("club_my_events", "My events", Icons.AutoMirrored.Filled.List),
    Tab("club_create_event", "Create event", Icons.Filled.Add),
    Tab("club_check_in", "Check-in", Icons.Filled.CheckCircle),
    Tab("club_attendance", "Attendance", Icons.Filled.Face)
)

/** Student home. Member 3: replace ComingSoonScreen for your tabs with your screens. */
@Composable
fun StudentHome(onLogOut: () -> Unit) {
    val tabNav = rememberNavController()
    Scaffold(bottomBar = { BottomBar(tabNav, studentTabs) }) { padding ->
        NavHost(tabNav, startDestination = studentTabs.first().route, modifier = Modifier.padding(padding)) {
            composable("student_marketplace") { ComingSoonScreen("Marketplace") }
            composable("student_my_events") { ComingSoonScreen("My events") }
            composable("student_points") { ComingSoonScreen("Points", release = 3) }
            composable("student_profile") { StudentProfileScreen(onLogOut) }
        }
    }
}

/** Club home, with Log out in the top bar. Member 2: replace ComingSoonScreen for your tabs. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClubHome(onLogOut: () -> Unit) {
    val tabNav = rememberNavController()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("s-CPD Club") },
                actions = {
                    IconButton(onClick = onLogOut) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Log out")
                    }
                }
            )
        },
        bottomBar = { BottomBar(tabNav, clubTabs) }
    ) { padding ->
        NavHost(tabNav, startDestination = clubTabs.first().route, modifier = Modifier.padding(padding)) {
            composable("club_my_events") {
                MyEventsScreen(
                    onCreateEvent = { tabNav.navigate("club_create_event") { launchSingleTop = true } },
                    onEditDraft = { id -> tabNav.navigate("club_edit_event/$id") }
                )
            }
            composable("club_create_event") {
                CreateEventScreen(onSaved = {
                    tabNav.navigate("club_my_events") {
                        popUpTo(tabNav.graph.findStartDestination().id)
                        launchSingleTop = true
                    }
                })
            }
            composable("club_edit_event/{eventId}") { entry ->
                CreateEventScreen(
                    draftId = entry.arguments?.getString("eventId").orEmpty(),
                    onSaved = { tabNav.popBackStack() }
                )
            }
            composable("club_check_in") { ComingSoonScreen("Check-in", release = 2) }
            composable("club_attendance") { ComingSoonScreen("Attendance", release = 2) }
        }
    }
}

/** Bottom bar shared by both roles. Remembers each tab's state when switching. */
@Composable
private fun BottomBar(navController: NavHostController, tabs: List<Tab>) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    NavigationBar {
        tabs.forEach { tab ->
            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(tab.label) }
            )
        }
    }
}

/** Placeholder for tabs that aren't built yet. */
@Composable
fun ComingSoonScreen(title: String, release: Int = 1) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            if (release > 1) "Coming in Release $release" else "Coming soon",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Student Profile tab: a Log out button for Release 1. */
@Composable
private fun StudentProfileScreen(onLogOut: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Profile", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onLogOut) { Text("Log out") }
    }
}