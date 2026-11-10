package com.mdpgroup08.scpd

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mdpgroup08.scpd.ui.auth.LoginScreen
import com.mdpgroup08.scpd.ui.student.MarketplaceScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var loggedIn by remember { mutableStateOf(false) }
                if (loggedIn) {
                    MarketplaceScreen()
                } else {
                    LoginScreen(onLoginSuccess = { loggedIn = true })
                }
            }
        }

    }
}