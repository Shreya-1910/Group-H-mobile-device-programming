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
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mdpgroup08.scpd.ui.admin.AdminApprovalScreen
import com.mdpgroup08.scpd.ui.admin.AdminScreen
import com.mdpgroup08.scpd.ui.admin.ApprovalQueueScreen
import com.mdpgroup08.scpd.ui.club.AttendanceScreen
import com.mdpgroup08.scpd.ui.club.ClubCheckInScreen
import com.mdpgroup08.scpd.ui.club.CreateEventScreen
import com.mdpgroup08.scpd.ui.club.MyEventsScreen
import com.mdpgroup08.scpd.ui.club.ShowCodeScreen
import com.mdpgroup08.scpd.ui.student.StudentCheckInScreen
import com.mdpgroup08.scpd.ui.student.MarketplaceScreen
import com.mdpgroup08.scpd.ui.student.PointsScreen
import com.mdpgroup08.scpd.ui.student.EventDetailsScreen
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mdpgroup08.scpd.viewmodel.AppViewModelProvider
import com.mdpgroup08.scpd.viewmodel.StudentRemindersViewModel
import com.mdpgroup08.scpd.ui.student.StudentEventsScreen

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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHome(onLogOut: () -> Unit) {
    val tabNav = rememberNavController()

    // R4-S21: keep the 3-hour reminders in step with this student's bookings
    viewModel<StudentRemindersViewModel>(factory = AppViewModelProvider.Factory)

    // Android 13+ asks before showing notifications
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Student Portal") },
                actions = {
                    IconButton(onClick = onLogOut) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Log out"
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomBar(tabNav, studentTabs)
        }
    ) { padding ->
        NavHost(
            tabNav,
            startDestination = studentTabs.first().route,
            modifier = Modifier.padding(padding)
        ) {
            // R2-S1: browse approved events. Event details (R2-S3) come in task 5 part 3.
            composable("student_marketplace") {
                MarketplaceScreen(onOpenEvent = { /* details screen comes next */ })
            }

            // R4-S10: the student's bookings and waitlists, also offline
            composable("student_my_events") {
                StudentEventsScreen(
                    onOpenEvent = { id -> tabNav.navigate("student_event/$id") },
                    onBrowse = { tabNav.navigate("student_marketplace") { launchSingleTop = true } }
                )
            }

            // R3-S2, R3-S3, R3-S4: check in with the organiser's 6-digit code
            composable("student_check_in") {
                StudentCheckInScreen()
            }

            composable("student_points") {
                PointsScreen()
            }

            composable("student_profile") {
                ComingSoonScreen("Profile", onLogOut = onLogOut)
            }

            // R2-S1, R2-S2: browse and search approved events
            composable("student_marketplace") {
                MarketplaceScreen(onOpenEvent = { id -> tabNav.navigate("student_event/$id") })
            }

            // R2-S3: event details with the state-aware main button
            composable("student_event/{eventId}") {
                EventDetailsScreen(onBack = { tabNav.popBackStack() })
            }
        }
    }
}

/** Club home. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClubHome(onLogOut: () -> Unit) {
    val tabNav = rememberNavController()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Club Portal") },
                actions = {
                    IconButton(onClick = onLogOut) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Log out"
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomBar(tabNav, clubTabs)
        }
    ) { padding ->
        NavHost(
            tabNav,
            startDestination = clubTabs.first().route,
            modifier = Modifier.padding(padding)
        ) {
            composable("club_my_events") {
                MyEventsScreen(
                    onCreateEvent = { tabNav.navigate("club_create_event") },
                    onEditDraft = { draftId -> tabNav.navigate("club_create_event/$draftId") },
                    onGenerateQr = { eventId -> tabNav.navigate("club_check_in/$eventId") },
                    onCopyEvent = { eventId -> tabNav.navigate("club_copy_event/$eventId") }
                )
            }

            composable("club_create_event") {
                CreateEventScreen(onSaved = { tabNav.navigate("club_my_events") })
            }

            composable("club_create_event/{draftId}") { backStackEntry ->
                val draftId = backStackEntry.arguments?.getString("draftId") ?: ""
                CreateEventScreen(draftId = draftId, onSaved = { tabNav.navigate("club_my_events") })
            }

            // R1-S11: a new draft filled from a rejected, cancelled or expired event
            composable("club_copy_event/{eventId}") { backStackEntry ->
                val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
                CreateEventScreen(copyFromId = eventId, onSaved = { tabNav.navigate("club_my_events") })
            }

            // R3-S1, R4-S23: rotating signed code for one event (eventId read by ShowCodeViewModel)
            composable("club_check_in/{eventId}") { backStackEntry ->
                val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
                ShowCodeScreen(
                    onBack = { tabNav.popBackStack() },
                    onViewList = { tabNav.navigate("club_attendance/$eventId") }
                )
            }

            composable("club_check_in") {
                ClubCheckInScreen(onOpenEvent = { id -> tabNav.navigate("club_check_in/$id") })
            }

            // R3-S5, R3-S6: attendance list with Mark present
            composable("club_attendance") {
                ClubCheckInScreen(
                    onOpenEvent = { id -> tabNav.navigate("club_attendance/$id") },
                    title = "Attendance",
                    subtitle = "See who has checked in, and mark students present by hand.",
                    buttonLabel = "View attendance"
                )
            }

            composable("club_attendance/{eventId}") {
                AttendanceScreen(onBack = { tabNav.popBackStack() })
            }
        }
    }
}

/** Admin home. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHome(onLogOut: () -> Unit) {
    val tabNav = rememberNavController()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Portal") },
                actions = {
                    IconButton(onClick = onLogOut) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Log out"
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomBar(tabNav, adminTabs)
        }
    ) { padding ->
        NavHost(
            tabNav,
            startDestination = adminTabs.first().route,
            modifier = Modifier.padding(padding)
        ) {
            composable("admin_queue") {
                ApprovalQueueScreen()
            }

            composable("admin_accounts") {
                AdminScreen()
            }

            composable("admin_audit") {
                ComingSoonScreen("Audit log")
            }
        }
    }
}

@Composable
private fun BottomBar(
    navController: NavHostController,
    tabs: List<Tab>
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        tabs.forEach { tab ->
            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
            NavigationBarItem(
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) },
                selected = selected,
                onClick = {
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Composable
fun ComingSoonScreen(
    title: String,
    onLogOut: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Coming soon",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (onLogOut != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onLogOut) {
                Text("Log out")
            }
        }
    }
}
