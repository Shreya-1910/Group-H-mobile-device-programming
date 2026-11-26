package com.mdpgroup08.scpd.reminders

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.mdpgroup08.scpd.ScpdApplication
import com.mdpgroup08.scpd.data.model.BookingStatus
import com.mdpgroup08.scpd.data.model.EventStatus
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/** Schedules reminders. An interface, so tests can use a fake instead of WorkManager. */
interface ReminderScheduler {
    fun schedule(eventId: String, remindAt: Long)
    fun cancel(eventId: String)
    /** R4-S21 EX1: post the reminder again as soon as the phone is online. */
    fun repostWhenOnline(eventId: String)
}

class WorkManagerReminderScheduler(context: Context) : ReminderScheduler {
    private val workManager = WorkManager.getInstance(context)

    override fun schedule(eventId: String, remindAt: Long) {
        val delay = (remindAt - System.currentTimeMillis()).coerceAtLeast(0)
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(ReminderWorker.KEY_EVENT_ID to eventId))
            .build()
        // REPLACE: if the event time was corrected, the reminder moves with it
        workManager.enqueueUniqueWork("reminder_$eventId", ExistingWorkPolicy.REPLACE, request)
    }

    override fun cancel(eventId: String) {
        workManager.cancelUniqueWork("reminder_$eventId")
        workManager.cancelUniqueWork("reminder_repost_$eventId")
    }

    override fun repostWhenOnline(eventId: String) {
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInputData(workDataOf(ReminderWorker.KEY_EVENT_ID to eventId))
            .build()
        workManager.enqueueUniqueWork("reminder_repost_$eventId", ExistingWorkPolicy.REPLACE, request)
    }
}

/** Runs 3 hours before an event and shows the reminder, checking first that it's still needed. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val eventId = inputData.getString(KEY_EVENT_ID) ?: return Result.success()
        val container = (applicationContext as ScpdApplication).container
        return try {
            val event = container.eventRepository.getEvent(eventId).first() ?: return Result.success()
            val booking = container.bookingRepository.getMyBooking(eventId).first()
            val now = System.currentTimeMillis()

            // Only for a booking that still stands, and never after the event has started (R5-S17 EX2)
            val stillNeeded = booking?.status == BookingStatus.CONFIRMED &&
                    event.status == EventStatus.APPROVED && now < event.startTime
            if (stillNeeded && ReminderNotifier.canNotify(applicationContext)) {
                val button = ReminderRules.buttonFor(event, container.networkMonitor.isOnline.value, now)
                ReminderNotifier.showReminder(applicationContext, event, button)
                if (button == ReminderButton.OFFLINE) container.reminderScheduler.repostWhenOnline(eventId)
            }
            Result.success()
        } catch (e: Exception) {
            // Couldn't read the event or booking: skip rather than show something wrong
            Result.success()
        }
    }

    companion object {
        const val KEY_EVENT_ID = "eventId"
    }
}