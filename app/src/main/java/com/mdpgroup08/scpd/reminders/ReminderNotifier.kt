package com.mdpgroup08.scpd.reminders

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mdpgroup08.scpd.MainActivity
import com.mdpgroup08.scpd.data.model.Event
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Builds and shows the 3-hour reminder and the result of "Can't make it" (R4-S21). */
object ReminderNotifier {
    private const val CHANNEL_ID = "event_reminders"

    /** Called once when the app starts. */
    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID, "Event reminders", NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "A reminder 3 hours before an event you booked" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** False if notifications are off or the permission was refused (R5-S17 EX5). */
    fun canNotify(context: Context): Boolean {
        val permissionOk = Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        return permissionOk && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun showReminder(context: Context, event: Event, button: ReminderButton) {
        val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(event.startTime))
        val text = when (button) {
            ReminderButton.CAN_CANCEL ->
                "Starts at $time. Can't make it? Cancel now so someone on the waitlist gets your place."
            ReminderButton.OFFLINE -> "Starts at $time. You are offline. Connect to the internet to cancel."
            ReminderButton.TOO_LATE -> "Starts at $time. Cancellation closed 2 hours before the event."
        }
        val builder = base(context, event.id, "Reminder: ${event.title}", text)
        if (button == ReminderButton.CAN_CANCEL) {
            builder.addAction(0, "Can't make it", cantMakeItIntent(context, event.id))
        }
        notify(context, event.id, builder)
    }

    fun showResult(context: Context, eventId: String, result: CantMakeItResult) {
        val (title, text) = when (result) {
            is CantMakeItResult.Cancelled -> "Booking cancelled" to "\"${result.title}\" is cancelled and your place has been released."
            CantMakeItResult.Offline -> "Not cancelled" to "You are offline. Connect to the internet to cancel."
            CantMakeItResult.TooLate -> "Too late to cancel" to "Cancellation closed 2 hours before the event. Your booking stays."
            CantMakeItResult.NotBooked -> "Already cancelled" to "This booking was already cancelled."
            CantMakeItResult.Failed -> "Couldn't cancel" to "Open the app and try again."
        }
        notify(context, eventId, base(context, eventId, title, text))
    }

    private fun base(context: Context, eventId: String, title: String, text: String) =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context, eventId))

    private fun openAppIntent(context: Context, eventId: String): PendingIntent = PendingIntent.getActivity(
        context, eventId.hashCode(),
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun cantMakeItIntent(context: Context, eventId: String): PendingIntent = PendingIntent.getBroadcast(
        context, eventId.hashCode(),
        Intent(context, CantMakeItReceiver::class.java).putExtra(CantMakeItReceiver.EXTRA_EVENT_ID, eventId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    /** One notification per event, so a result replaces its reminder instead of stacking. */
    @SuppressLint("MissingPermission") // checked in canNotify()
    private fun notify(context: Context, eventId: String, builder: NotificationCompat.Builder) {
        if (!canNotify(context)) return
        NotificationManagerCompat.from(context).notify(eventId.hashCode(), builder.build())
    }
}