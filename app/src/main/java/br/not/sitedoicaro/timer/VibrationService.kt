package br.not.sitedoicaro.timer

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.*
import br.not.sitedoicaro.timer.MainActivity.Companion.DEFAULT_CODE

/**
 * Vibrates the phone.
 *
 * Can execute while the device is asleep because it starts in the foreground.
 */
class VibrationService : Service() {
    companion object {
        /** Notification channel for the vibration service. */
        const val NOTIFICATION_CHANNEL_VIBRATION = "VIBRATION_NOTIFICATION_CHANNEL"
        /** Vibrations will repeat only once. */
        private const val RUN_ONCE: Int = -1
    }

    /** Creates a simple binder that is required for the service. */
    override fun onBind(p0: Intent?): IBinder {
        return Binder()
    }

    /** Start the service in the foreground. */
    override fun onCreate() {
        super.onCreate()
        // Create the notification to enable the foreground service
        val notificationBuilder: Notification.Builder
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationBuilder = Notification.Builder(applicationContext, NOTIFICATION_CHANNEL_VIBRATION)
        } else {
            @Suppress("DEPRECATION")
            notificationBuilder = Notification.Builder(applicationContext)
            @Suppress("DEPRECATION")
            notificationBuilder.setPriority(Notification.PRIORITY_LOW)
        }
        notificationBuilder.setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_vibration))
                .setSmallIcon(R.drawable.ic_timer)
                .setContentIntent(PendingIntent.getActivity(applicationContext, DEFAULT_CODE, Intent(applicationContext, VibrationService::class.java), DEFAULT_CODE))
        // Move the service to the foreground
        startForeground(DEFAULT_CODE, notificationBuilder.build())
    }

    /** Prepare the vibration type and execute it. */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            // Vibration pattern
            val pattern: LongArray? = longArrayOf(0, 500)
            // Vibrate with separate APIs according to the device's Android version
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                VibrationEffect.createWaveform(pattern, VibrationService.RUN_ONCE)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, VibrationService.RUN_ONCE)
            }
        } catch (_: KotlinNullPointerException) {
            // Sometimes the app crashes at this function when the timer is stopped so this mitigates those errors
        }
        return super.onStartCommand(intent, flags, startId)
    }
}
