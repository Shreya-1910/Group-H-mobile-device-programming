
package com.mdpgroup08.scpd

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.mdpgroup08.scpd.reminders.ReminderNotifier

class ScpdApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()

        container = AppContainer(this)

        ReminderNotifier.ensureChannel(this)

        // Test Room database creation and access.
        CoroutineScope(Dispatchers.IO).launch {
            try {
                container.checkInDao.getPending("__room_test__")
                android.util.Log.d("ROOM_TEST", "Room database read successful")
            } catch (e: Exception) {
                android.util.Log.e("ROOM_TEST", "Room database test failed", e)
            }
        }
    }
}

