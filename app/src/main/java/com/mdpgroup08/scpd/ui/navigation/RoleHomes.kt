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
import com.mdpgroup08.scpd.ui.admin.AdminApprovalScreen
import com.mdpgroup08.scpd.ui.admin.AdminScreen
import com.mdpgroup08.scpd.ui.club.CreateEventScreen
import com.mdpgroup08.scpd.ui.club.MyEventsScreen
import com.mdpgroup08.scpd.ui.student.ManualCheckInScreen
import com.mdpgroup08.scpd.ui.student.PointsScreen

/** One bottom-bar tab. */
data class Tab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

// Student tabs (R6-S4)
private val studentTabs = listOf(
    Tab("student_marketplace", "Marketplace", Icons.Filled.Search),
    Tab("student_my_events", "My events", Icons.Filled.DateRange),
    Tab("student_check_in", "Check-in", Icons.Filled.CheckCircle),
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

// Admin tabs (R6)
private val adminTabs = listOf(
    Tab("admin_queue", "Queue", Icons.Filled.CheckCircle),
    Tab("admin_accounts", "Accounts", Icons.Filled.Person),
    Tab("admin_audit", "Audit log", Icons.AutoMirrored.Filled.List)
)

/** Student home. */
@Composable
fun StudentHome(onLogOut: () -> Unit) {

    val tabNav = rememberNavController()

    Scaffold(
        bottomBar = {
            BottomBar(tabNav, studentTabs)
        }
    ) { padding ->

        NavHost(
            tabNav,
            startDestination = studentTabs.first().route,
            modifier = Modifier.padding(padding)
        ) {

            composable("student_marketplace") {
                ComingSoonScreen("Marketplace")
            }

            composable("student_my_events") {
                ComingSoonScreen("My events")
            }

            // R3-S3: Manual six-digit check-in code entry
            composable("student_check_in") {