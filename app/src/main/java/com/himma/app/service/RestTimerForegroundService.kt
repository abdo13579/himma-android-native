package com.himma.app.service

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.himma.app.MainActivity
import com.himma.app.R
import com.himma.app.domain.model.WorkoutMath
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RestTimerForegroundService : Service() {

    @Inject
    lateinit var stateHolder: RestTimerStateHolder

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tickerJob: Job? = null

    private var endElapsedMs: Long = 0L
    private var endWallTimeMs: Long = 0L
    private var totalSeconds: Int = 0
    private var activeExerciseName: String = ""

    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    private val alarmManager by lazy {
        getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val seconds = intent.getIntExtra(EXTRA_SECONDS, 90).coerceIn(5, 600)
                val name = intent.getStringExtra(EXTRA_EXERCISE_NAME).orEmpty()
                startCountdown(seconds, name)
            }
            ACTION_ADJUST -> {
                val delta = intent.getIntExtra(EXTRA_DELTA_SECONDS, 0)
                adjustCountdown(delta)
            }
            ACTION_ALARM_FIRED -> {
                completeTimer()
            }
            ACTION_SKIP -> {
                stopTimerAndService()
            }
            else -> {
                if (endElapsedMs <= SystemClock.elapsedRealtime()) {
                    stopTimerAndService()
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        tickerJob?.cancel()
        cancelWakeupAlarm()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startCountdown(seconds: Int, exerciseName: String) {
        notificationManager.cancel(NOTIFICATION_ID_COMPLETE)
        totalSeconds = seconds
        activeExerciseName = exerciseName
        val nowElapsed = SystemClock.elapsedRealtime()
        val nowWall = System.currentTimeMillis()
        endElapsedMs = nowElapsed + seconds * 1000L
        endWallTimeMs = nowWall + seconds * 1000L

        stateHolder.update(
            RestTimerState(
                isRunning = true,
                exerciseName = activeExerciseName,
                totalSeconds = totalSeconds,
                remainingSeconds = seconds,
            ),
        )

        startForeground(NOTIFICATION_ID_PROGRESS, buildProgressNotification(seconds))
        scheduleWakeupAlarm(endElapsedMs)
        startTickerLoop()
    }

    private fun adjustCountdown(deltaSeconds: Int) {
        if (endElapsedMs == 0L) {
            stopTimerAndService()
            return
        }
        val nowElapsed = SystemClock.elapsedRealtime()
        val newRemainingMs = (endElapsedMs - nowElapsed + deltaSeconds * 1000L).coerceAtMost(600_000L)
        if (newRemainingMs <= 0L) {
            completeTimer()
            return
        }
        endElapsedMs = nowElapsed + newRemainingMs
        endWallTimeMs = System.currentTimeMillis() + newRemainingMs
        val remainingSec = ((newRemainingMs + 999L) / 1000L).toInt()
        if (remainingSec > totalSeconds) {
            totalSeconds = remainingSec
        }
        stateHolder.update(
            RestTimerState(
                isRunning = true,
                exerciseName = activeExerciseName,
                totalSeconds = totalSeconds,
                remainingSeconds = remainingSec,
            ),
        )
        scheduleWakeupAlarm(endElapsedMs)
        postProgressNotification(remainingSec)
    }

    private fun startTickerLoop() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            var lastReportedSecond = -1
            while (isActive) {
                val remainingMs = endElapsedMs - SystemClock.elapsedRealtime()
                if (remainingMs <= 0L) {
                    completeTimer()
                    break
                }
                val remainingSec = ((remainingMs + 999L) / 1000L).toInt().coerceAtLeast(1)
                if (remainingSec != lastReportedSecond) {
                    lastReportedSecond = remainingSec
                    stateHolder.update(
                        RestTimerState(
                            isRunning = true,
                            exerciseName = activeExerciseName,
                            totalSeconds = totalSeconds,
                            remainingSeconds = remainingSec,
                        ),
                    )
                    postProgressNotification(remainingSec)
                }
                delay(250L)
            }
        }
    }

    private fun completeTimer() {
        val completedExerciseName = activeExerciseName
        tickerJob?.cancel()
        cancelWakeupAlarm()
        endElapsedMs = 0L
        stateHolder.clear()

        triggerAlertVibration()
        postCompletionNotification(completedExerciseName)

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopTimerAndService() {
        tickerJob?.cancel()
        cancelWakeupAlarm()
        endElapsedMs = 0L
        stateHolder.clear()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun postProgressNotification(remainingSec: Int) {
        if (!canPostNotifications()) return
        notificationManager.notify(NOTIFICATION_ID_PROGRESS, buildProgressNotification(remainingSec))
    }

    private fun postCompletionNotification(exerciseName: String) {
        if (!canPostNotifications()) return
        val label = exerciseName.ifBlank { getString(R.string.rest_timer_label) }
        val notification = NotificationCompat.Builder(this, CHANNEL_COMPLETE)
            .setSmallIcon(R.drawable.ic_timer_notification)
            .setContentTitle(getString(R.string.rest_notif_done_title))
            .setContentText(getString(R.string.rest_notif_done_body, label))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(createLaunchIntent())
            .build()
        notificationManager.notify(NOTIFICATION_ID_COMPLETE, notification)
    }

    private fun buildProgressNotification(remainingSec: Int): Notification {
        val title = getString(
            R.string.rest_notif_ongoing_title,
            activeExerciseName.ifBlank { getString(R.string.rest_timer_label) },
        )
        val body = getString(
            R.string.rest_notif_ongoing_body,
            WorkoutMath.formatTimer(remainingSec),
        )

        return NotificationCompat.Builder(this, CHANNEL_PROGRESS)
            .setSmallIcon(R.drawable.ic_timer_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(endWallTimeMs)
            .setContentIntent(createLaunchIntent())
            .addAction(
                0,
                getString(R.string.rest_timer_minus_15),
                createActionPendingIntent(ACTION_ADJUST, -15, 101),
            )
            .addAction(
                0,
                getString(R.string.rest_timer_plus_15),
                createActionPendingIntent(ACTION_ADJUST, 15, 102),
            )
            .addAction(
                0,
                getString(R.string.rest_timer_skip),
                createActionPendingIntent(ACTION_SKIP, 0, 103),
            )
            .build()
    }

    private fun createLaunchIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            100,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createActionPendingIntent(
        action: String,
        deltaSeconds: Int,
        requestCode: Int,
    ): PendingIntent {
        val intent = Intent(this, RestTimerForegroundService::class.java).apply {
            this.action = action
            if (deltaSeconds != 0) {
                putExtra(EXTRA_DELTA_SECONDS, deltaSeconds)
            }
        }
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun scheduleWakeupAlarm(triggerAtElapsedMs: Long) {
        val alarmIntent = createAlarmPendingIntent()
        try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAtElapsedMs,
                alarmIntent,
            )
        } catch (_: SecurityException) {
            // Safe fallback: coroutine loop still handles foreground countdown.
        }
    }

    private fun cancelWakeupAlarm() {
        alarmManager.cancel(createAlarmPendingIntent())
    }

    private fun createAlarmPendingIntent(): PendingIntent {
        val intent = Intent(this, RestTimerForegroundService::class.java).apply {
            action = ACTION_ALARM_FIRED
        }
        return PendingIntent.getService(
            this,
            104,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun triggerAlertVibration() {
        val pattern = longArrayOf(0L, 220L, 120L, 280L)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
        }
    }

    private fun canPostNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun ensureNotificationChannels() {
        val progressChannel = NotificationChannel(
            CHANNEL_PROGRESS,
            getString(R.string.rest_channel_progress_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.rest_channel_progress_desc)
            setShowBadge(false)
        }

        val alertSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val completeChannel = NotificationChannel(
            CHANNEL_COMPLETE,
            getString(R.string.rest_channel_complete_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = getString(R.string.rest_channel_complete_desc)
            enableVibration(true)
            setSound(alertSoundUri, audioAttributes)
        }

        notificationManager.createNotificationChannel(progressChannel)
        notificationManager.createNotificationChannel(completeChannel)
    }

    companion object {
        const val ACTION_START = "com.himma.app.action.REST_START"
        const val ACTION_ADJUST = "com.himma.app.action.REST_ADJUST"
        const val ACTION_SKIP = "com.himma.app.action.REST_SKIP"
        const val ACTION_ALARM_FIRED = "com.himma.app.action.REST_ALARM_FIRED"

        const val EXTRA_SECONDS = "extra_seconds"
        const val EXTRA_DELTA_SECONDS = "extra_delta_seconds"
        const val EXTRA_EXERCISE_NAME = "extra_exercise_name"

        private const val CHANNEL_PROGRESS = "himma_rest_progress"
        private const val CHANNEL_COMPLETE = "himma_rest_complete"
        private const val NOTIFICATION_ID_PROGRESS = 4001
        private const val NOTIFICATION_ID_COMPLETE = 4002
    }
}
