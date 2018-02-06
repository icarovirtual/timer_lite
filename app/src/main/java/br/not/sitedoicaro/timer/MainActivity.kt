package br.not.sitedoicaro.timer

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.databinding.DataBindingUtil
import android.databinding.ObservableBoolean
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.widget.NumberPicker
import br.not.sitedoicaro.timer.VibrationService.Companion.NOTIFICATION_CHANNEL_VIBRATION
import br.not.sitedoicaro.timer.databinding.ActivityMainBinding
import com.github.stephenvinouze.materialnumberpickercore.MaterialNumberPicker
import kotlinx.android.synthetic.main.activity_main.*


/** Controls starting and stopping the vibrations. */
class MainActivity : AppCompatActivity() {

    companion object {
        /** Default code for unnecessary values. */
        const val DEFAULT_CODE = 0
        /** Default delay in seconds. */
        const val DEFAULT_DELAY: Int = 10
        /** Delay to start the timer. */
        private const val DELAY_INITIAL: Long = 2 * 1000

        /** Value of the delay in seconds. */
        var delaySeconds: Int = 0
        /** Used to persist the vibration delay time. */
        lateinit var sharedPreferences: SharedPreferences

        /** Handles execution of the vibration broadcasts. */
        private var vibrationPendingIntent: PendingIntent? = null
    }

    /** Setup the views and start the service runnable. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Load the saved vibration delay times
        sharedPreferences = applicationContext.getSharedPreferences(MainActivity::class.java.name, Context.MODE_PRIVATE)
        delaySeconds = sharedPreferences.getInt(getString(R.string.settings_vibration_delay_tag), DEFAULT_DELAY)
        // Prepare the data binding
        val binding = DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)
        binding.presenter = Presenter()
        // Initialize the notification channels for Android Oreo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel(NOTIFICATION_CHANNEL_VIBRATION, getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_LOW)
            } else {
                null
            }
            if (channel != null) {
                channel.description = getString(R.string.notification_channel_description)
                notificationManager.createNotificationChannel(channel)
            }
        }
        // Initialize the alarm requirements
        vibrationPendingIntent = PendingIntent.getBroadcast(applicationContext, DEFAULT_CODE, Intent(applicationContext, VibrationBroadcastReceiver::class.java), DEFAULT_CODE)
        VibrationBroadcastReceiver.activity = this@MainActivity
    }

    /** Set an alarm for the next vibration broadcast. */
    fun scheduleNextVibration(triggerAtMillis: Long) {
        (applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager).set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtMillis, vibrationPendingIntent)
    }

    /** Cancels all alarms for vibration broadcasts. */
    fun cancelNextVibration() {
        (applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(vibrationPendingIntent)
    }

    /** Saves the vibration delay. */
    fun persistVibrationDelay(tag: String, value: Int) {
        sharedPreferences.edit().putInt(tag, value).apply()
    }

    /** Handles interactions with the layout. */
    inner class Presenter {

        /** Control whether the timer is started or not. */
        var isStarted: ObservableBoolean = ObservableBoolean(false)

        init {
            vibration_button.text = secondsFormatter().format(delaySeconds)
        }

        /** Start executing the vibration service. */
        fun onStart() {
            scheduleNextVibration(SystemClock.elapsedRealtime() + DELAY_INITIAL)
            isStarted.set(true)
        }

        /** Stop executing the vibration service. */
        fun onStop() {
            cancelNextVibration()
            isStarted.set(false)
        }

        /** Shows the time picker to set the delay value. */
        fun onDelayClick() {
            // Prepare the picker view
            val delayPicker = MaterialNumberPicker(
                    context = this@MainActivity,
                    minValue = 5,
                    maxValue = 60,
                    value = delaySeconds,
                    separatorColor = ContextCompat.getColor(this@MainActivity, R.color.colorAccent),
                    textColor = ContextCompat.getColor(this@MainActivity, R.color.light_on_dark_primary),
                    formatter = secondsFormatter()
            )
            // Show the picker view in a dialog
            AlertDialog.Builder(this@MainActivity)
                    .setView(delayPicker)
                    .setNegativeButton(getString(android.R.string.cancel), null)
                    .setPositiveButton(getString(android.R.string.ok),
                            /** Set and persist the new value and change button text. */
                            { _, _ ->
                                val tag = vibration_button.tag.toString()
                                val value = delayPicker.value
                                delaySeconds = value
                                vibration_button.text = secondsFormatter().format(value)
                                persistVibrationDelay(tag, value)
                            })
                    .show()
        }

        /** Format the number to be displayed asx seconds. */
        private fun secondsFormatter(): NumberPicker.Formatter {
            return NumberPicker.Formatter {
                return@Formatter "$it seconds"
            }
        }
    }

    /** Controls the vibration service. */
    class VibrationBroadcastReceiver : BroadcastReceiver() {

        companion object {
            /** Reference to the activity. */
            var activity: MainActivity? = null
        }

        /** Start the vibration. */
        override fun onReceive(context: Context?, intent: Intent?) {
            // Execute the vibration
            val serviceIntent = Intent(context, VibrationService::class.java)
            context!!.startService(serviceIntent)
            activity!!.scheduleNextVibration(SystemClock.elapsedRealtime() + delaySeconds * 1000)
        }
    }
}
