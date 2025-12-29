package com.example.extra_mind_time

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView

class DelayActivity : Activity() {

    private var countDownTimer: CountDownTimer? = null
    private lateinit var messageTextView: TextView
    private lateinit var timerTextView: TextView
    private lateinit var secondsLabelTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var iconView: TextView
    private lateinit var closeButton: Button
    private lateinit var timeLimitButtonsLayout: LinearLayout
    private var targetPackageName: String? = null
    private var targetAppName: String? = null
    private var timeLimitOptions: List<Int> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make the activity full screen
        @Suppress("DEPRECATION")
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)

        // Get the target package name and app name from intent
        targetPackageName = intent.getStringExtra("packageName")
        targetAppName = intent.getStringExtra("appName")

        // Get settings from SharedPreferences
        val prefs = getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)

        val delaySeconds = prefs.getLong("flutter.delay_seconds", 5L).toInt()
        val mindfulMessage =
            prefs.getString(
                "flutter.mindful_message",
                "Take a moment to breathe and be present."
            )
                ?: "Take a moment to breathe and be present."

        // Get time limit options
        val timeLimitOptionsJson = prefs.getString("flutter.time_limit_options", null)
        timeLimitOptions = if (timeLimitOptionsJson != null) {
            try {
                val gson = com.google.gson.Gson()
                gson.fromJson(timeLimitOptionsJson, Array<Int>::class.java).toList()
            } catch (e: Exception) {
                listOf(2, 5, 10)
            }
        } else {
            listOf(2, 5, 10)
        }

        // Get background color
        val backgroundColorValue = prefs.getLong("flutter.background_color", -7637753) // Colors.deepPurple.value

        // Convert to Android color format
        val backgroundColor = backgroundColorValue.toInt()

        // Create the layout programmatically
        createLayout(mindfulMessage, backgroundColor)

        // Start the countdown
        startCountdown(delaySeconds)
    }

    private fun createLayout(message: String, backgroundColor: Int) {
        // Set activity background first
        try {
            this.window.decorView.setBackgroundColor(backgroundColor)
        } catch (e: Exception) {
            this.window.decorView.setBackgroundColor(-16711936) // Green fallback
        }

        // Create root layout
        val rootLayout =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                    )
                setPadding(64, 64, 64, 64)

                setBackgroundColor(backgroundColor)
                background = android.graphics.drawable.ColorDrawable(backgroundColor)
                gravity = android.view.Gravity.CENTER
            }

        // Icon (breathing indicator)
        iconView =
            TextView(this).apply {
                text = "🧘"
                textSize = 80f
                gravity = android.view.Gravity.CENTER
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                        .apply { bottomMargin = 48 }
            }
        rootLayout.addView(iconView)

        // Mindful message
        messageTextView =
            TextView(this).apply {
                text = message
                textSize = 24f
                setTextColor(0xFFFFFFFF.toInt())
                gravity = android.view.Gravity.CENTER
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                        .apply { bottomMargin = 64 }
                alpha = 0.95f
            }
        rootLayout.addView(messageTextView)

        // Progress bar
        progressBar =
            ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
                layoutParams = LinearLayout.LayoutParams(320, 320)
                max = 100
                progress = 100
                indeterminateDrawable?.setTint(0xFFFFFFFF.toInt())
                progressDrawable?.setTint(0xFFFFFFFF.toInt())
            }

        // Create a circular progress container
        val progressContainer =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                        .apply { bottomMargin = 64 }
                gravity = android.view.Gravity.CENTER
            }

        // Timer text (countdown number)
        timerTextView =
            TextView(this).apply {
                text = "5"
                textSize = 64f
                setTextColor(0xFFFFFFFF.toInt())
                gravity = android.view.Gravity.CENTER
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                setPadding(0, 100, 0, 0)
            }
        progressContainer.addView(timerTextView)

        // Seconds label
        secondsLabelTextView =
            TextView(this).apply {
                text = "seconds"
                textSize = 16f
                setTextColor(0xCCFFFFFF.toInt())
                gravity = android.view.Gravity.CENTER
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
            }
        progressContainer.addView(secondsLabelTextView)

        rootLayout.addView(progressContainer)

        // Info text
        val infoTextView =
            TextView(this).apply {
                text = "Take a deep breath"
                textSize = 14f
                setTextColor(0xB3FFFFFF.toInt())
                gravity = android.view.Gravity.CENTER
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
            }
        rootLayout.addView(infoTextView)

        // Buttons container
        val buttonsLayout =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                        .apply {
                            topMargin = 48
                            leftMargin = 32
                            rightMargin = 32
                        }
                gravity = Gravity.CENTER
            }

        // Stay Mindful button
        closeButton =
            Button(this).apply {
                text = "🙏 Stay Mindful"
                textSize = 16f
                setTextColor(0xFFFFFFFF.toInt())
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = 12 }
                setBackgroundColor(0x88000000.toInt())
                setPadding(24, 24, 24, 24)
                setOnClickListener {
                    countDownTimer?.cancel()
                    goToHomeScreen()
                }
            }
        buttonsLayout.addView(closeButton)

        // Time limit buttons container
        timeLimitButtonsLayout =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                gravity = Gravity.CENTER
                visibility = android.view.View.GONE
            }

        val timeLimitLabel = TextView(this).apply {
            text = "How much time do you need?"
            textSize = 16f
            setTextColor(0xCCFFFFFF.toInt())
            gravity = android.view.Gravity.CENTER
            layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
        }
        timeLimitButtonsLayout.addView(timeLimitLabel)

        // Create time limit option buttons
        for (minutes in timeLimitOptions) {
            val timeLimitButton =
                Button(this).apply {
                    text = "$minutes ${if (minutes == 1) "minute" else "minutes"}"
                    textSize = 16f
                    setTextColor(0xFF4A148C.toInt())
                    layoutParams =
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { bottomMargin = 8 }
                    setBackgroundColor(0xAAFFFFFF.toInt())
                    setPadding(24, 20, 24, 20)
                    isEnabled = false
                    alpha = 0.5f
                    setOnClickListener {
                        selectTimeLimit(minutes)
                    }
                }
            timeLimitButtonsLayout.addView(timeLimitButton)
        }

        buttonsLayout.addView(timeLimitButtonsLayout)

        rootLayout.addView(buttonsLayout)

        setContentView(rootLayout)

        // Try setting background after setContentView as well
        try {
            window.decorView.setBackgroundColor(backgroundColor)
            rootLayout.setBackgroundColor(backgroundColor)
        } catch (e: Exception) {
            window.decorView.setBackgroundColor(android.graphics.Color.GREEN)
            rootLayout.setBackgroundColor(android.graphics.Color.GREEN)
        }

        // Start breathing animation
        startBreathingAnimation()
    }

    private fun startBreathingAnimation() {
        val scaleUp =
            ObjectAnimator.ofFloat(iconView, View.SCALE_X, 1f, 1.1f).apply {
                duration = 1500
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
            }
        val scaleUpY =
            ObjectAnimator.ofFloat(iconView, View.SCALE_Y, 1f, 1.1f).apply {
                duration = 1500
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
            }
        scaleUp.start()
        scaleUpY.start()
    }

    private fun startCountdown(seconds: Int) {
        val totalMillis = seconds * 1000L

        countDownTimer =
            object : CountDownTimer(totalMillis, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    val secondsRemaining = (millisUntilFinished / 1000).toInt() + 1
                    timerTextView.text = secondsRemaining.toString()
                    secondsLabelTextView.text =
                        if (secondsRemaining == 1) "second" else "seconds"

                    // Update progress
                    val progress =
                        ((millisUntilFinished.toFloat() / totalMillis.toFloat()) *
                                100)
                            .toInt()
                    progressBar.progress = progress
                }

                override fun onFinish() {
                    timerTextView.text = "0"
                    secondsLabelTextView.text = "seconds"
                    progressBar.progress = 0

                    // Enable time limit buttons
                    timeLimitButtonsLayout.visibility = android.view.View.VISIBLE
                    for (i in 0 until timeLimitButtonsLayout.childCount) {
                        val child = timeLimitButtonsLayout.getChildAt(i)
                        if (child is Button) {
                            child.isEnabled = true
                            child.alpha = 1.0f
                        }
                    }
                }
            }
                .start()
    }

    private fun selectTimeLimit(minutes: Int) {
        android.util.Log.d("DelayActivity", "selectTimeLimit called: minutes=$minutes")
        countDownTimer?.cancel()
        notifyDelayScreenFinished(isContinue = true, timeLimitMinutes = minutes)
        finish()
    }

    private fun goToHomeScreen() {
        val homeIntent =
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        startActivity(homeIntent)
        notifyDelayScreenFinished(isContinue = false)
        finish()
    }

    override fun onStop() {
        super.onStop()
        // Ensure timer is cancelled and activity is finished when user leaves (Home button, etc.)
        // so that it starts fresh next time.
        android.util.Log.d("DelayActivity", "onStop called, cancelling timer and finishing activity")
        countDownTimer?.cancel()
        notifyDelayScreenFinished(isContinue = false)
        if (!isFinishing) {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        
        val intent = Intent("com.example.extra_mind_time.CLEAR_STUCK_STATE")
        sendBroadcast(intent)
    }

    override fun onBackPressed() {
        // Prevent back button from closing the delay screen
        // User must wait for the timer to finish
    }

    private fun notifyDelayScreenFinished(isContinue: Boolean = false, timeLimitMinutes: Int = 0) {
        // Call service directly instead of broadcast
        val service = AppMonitorService.getInstance()
        android.util.Log.d("DelayActivity", "Getting service instance: $service")
        if (service != null) {
            android.util.Log.d("DelayActivity", "Calling service.onDelayScreenFinished directly")
            service.onDelayScreenFinished(
                packageName = targetPackageName,
                appName = targetAppName,
                timeLimitMinutes = timeLimitMinutes,
                isStayingMindful = !isContinue
            )
        } else {
            android.util.Log.e("DelayActivity", "Service instance is null, cannot notify service")
        }
    }
}
