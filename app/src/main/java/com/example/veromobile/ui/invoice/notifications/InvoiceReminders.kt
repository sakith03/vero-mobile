package com.example.veromobile.ui.invoice.notifications

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.room.withTransaction
import com.example.veromobile.core.AppTime
import com.example.veromobile.data.local.AppDatabase
import com.example.veromobile.domain.session.UserSession
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.*

object InvoiceReminders {
    const val CHANNEL = "invoice_overdue"
    fun enabled(context: Context): Boolean =
        (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            (context.getSystemService(NotificationManager::class.java).getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE)

    fun openSettings(context: Context) {
        context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
    }
    fun checkNow(context: Context) { context.sendBroadcast(Intent(context, InvoiceReminderReceiver::class.java)) }
    fun schedule(context: Context) {
        val intent = PendingIntent.getBroadcast(context, 704, Intent(context, InvoiceReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val next = AppTime.today().plusDays(1).atStartOfDay(AppTime.zone).toInstant().toEpochMilli()
        context.getSystemService(AlarmManager::class.java).setInexactRepeating(AlarmManager.RTC_WAKEUP,
            next, AlarmManager.INTERVAL_DAY, intent)
    }
}

@AndroidEntryPoint
class InvoiceReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var db: AppDatabase
    @Inject lateinit var session: UserSession
    override fun onReceive(context: Context, intent: Intent) {
        InvoiceReminders.schedule(context)
        val result = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try { withTimeout(8_000) { notifyOverdue(context) } }
            catch (_: Exception) { Log.w("InvoiceReminder", "Overdue check deferred; it will retry on the next check") }
            finally { result.finish() }
        }
    }

    private suspend fun notifyOverdue(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(InvoiceReminders.CHANNEL, "Overdue invoices", NotificationManager.IMPORTANCE_DEFAULT))
        if (!InvoiceReminders.enabled(context)) return
        val owner = session.userId.takeIf { it.isNotBlank() } ?: return
        val dao = db.invoiceDao()
        dao.closedIds(owner).forEach { manager.cancel("invoice:$it", 1) }
        val today = LocalDate.now(AppTime.zone).toEpochDay()
        for (invoice in dao.newlyOverdue(owner, today)) {
            db.withTransaction {
                val current = dao.find(invoice.id, owner)
                if (session.userId != owner || current?.status != "EXPECTED" || current.dueDateEpochDay >= today || current.notifiedDueEpochDay == current.dueDateEpochDay) return@withTransaction
                val notification = NotificationCompat.Builder(context, InvoiceReminders.CHANNEL)
                    .setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Invoice is overdue")
                    .setContentText("${current.client} · ${current.project} was due ${LocalDate.ofEpochDay(current.dueDateEpochDay)}")
                    .setOnlyAlertOnce(true).setAutoCancel(true).build()
                manager.notify("invoice:${current.id}", 1, notification)
                // Stable notification tag prevents duplicate tray entries if interrupted before this commit.
                dao.markNotified(current.id, owner, current.dueDateEpochDay)
            }
        }
    }
}
