package com.mdpgroup08.scpd.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mdpgroup08.scpd.ScpdApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Handles "Can't make it" on the reminder. All the decisions are in CantMakeItHandler. */
class CantMakeItReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getStringExtra(EXTRA_EVENT_ID) ?: return
        val container = (context.applicationContext as ScpdApplication).container
        val pending = goAsync()  // a receiver has about 10 seconds

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val handler = CantMakeItHandler(
                    container.bookingRepository, container.eventRepository, container.networkMonitor
                )
                val result = withTimeoutOrNull(9_000) { handler.handle(eventId) } ?: CantMakeItResult.Failed
                ReminderNotifier.showResult(context, eventId, result)
                when (result) {
                    is CantMakeItResult.Cancelled -> container.reminderScheduler.cancel(eventId)
                    CantMakeItResult.Offline -> container.reminderScheduler.repostWhenOnline(eventId)
                    else -> {}
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_EVENT_ID = "eventId"
    }
}