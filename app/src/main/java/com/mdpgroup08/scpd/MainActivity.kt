package com.mdpgroup08.scpd

import android.app.KeyguardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.mdpgroup08.scpd.ui.navigation.AppNavHost
import com.mdpgroup08.scpd.ui.theme.SCPDTheme

class MainActivity : ComponentActivity() {

    private val lockLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != RESULT_OK) {
            // Did not unlock, close app
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SCPDTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavHost()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (lastBackgroundTime > 0 && System.currentTimeMillis() - lastBackgroundTime > LOCK_TIMEOUT_MS) {
            promptPinLock()
        }
        lastBackgroundTime = 0L
    }

    override fun onPause() {
        super.onPause()
        lastBackgroundTime = System.currentTimeMillis()
    }

    private fun promptPinLock() {
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (keyguardManager.isDeviceSecure) {
            val intent = keyguardManager.createConfirmDeviceCredentialIntent("App Locked", "Enter your phone PIN to continue")
            if (intent != null) {
                lockLauncher.launch(intent)
            }
        } else {
            // R6-S14 EX1: Phone has no screen lock -> told to set up screen lock first
            Toast.makeText(this, "Please set up a screen lock (PIN) in phone settings to use App Lock.", Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        // App Lock (R6-S14 AC1): 5 minutes timeout
        private const val LOCK_TIMEOUT_MS = 5 * 60 * 1000L
        private var lastBackgroundTime = 0L
    }
}
