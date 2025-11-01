package com.example.extra_mind_time

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
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
    private lateinit var openButton: Button
    private var targetPackageName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make the activity full screen
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)

        // Get the target package name from intent
        targetPackageName = intent.getStringExtra("packageName")
        val recheckIntervalMinutes = intent.getLongExtra("recheckIntervalMinutes", 30)

        // Get settings from SharedPreferences
        val prefs = getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
        val delaySeconds = prefs.getLong("flutter.delay_seconds", 5L).toInt()
        val mindfulMessage =
            prefs.getString(
                "flutter.mindful_message",
                "Take a moment to breathe and be present."
            )
                ?: "Take a moment to breathe and be present."

        // Get background color with test colors for debugging
        val backgroundColorValue = prefs.getLong("flutter.background_color", -7637753) // Colors.deepPurple.value

        // Test: Apply some known colors to verify the system works
        val testColors: Map<Long, String> = mapOf(
            -7637753L to "Deep Purple",
            -16738680L to "Blue",
            -16711936L to "Green",
            -65536L to "Red",
            -16777216L to "Black"
        )

        android.util.Log.d("DelayActivity", "Background color value from SharedPreferences: $backgroundColorValue")
        android.util.Log.d("DelayActivity", "Background color hex: ${String.format("#%08X", backgroundColorValue)}")
        android.util.Log.d("DelayActivity", "Color name: ${testColors[backgroundColorValue] ?: "Unknown"}")

        // Convert to Android color format
        val backgroundColor = backgroundColorValue.toInt()

        // Check if we're getting the default value
        val isDefault = backgroundColorValue == -7637753L
        android.util.Log.d("DelayActivity", "Using default color: $isDefault")

        // Convert to Android color format
        val backgroundColor = backgroundColorValue.toInt()
        android.util.Log.d("DelayActivity", "Final background color: $backgroundColor")

        // Create the layout programmatically
        createLayout(mindfulMessage, backgroundColor, isDefault)

        // Start the countdown
        startCountdown(delaySeconds)
    }

    private fun createLayout(message: String, backgroundColor: Int, isDefault: Boolean) {
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

                // Apply background color with verification
                try {
                    setBackgroundColor(backgroundColor)
                    android.util.Log.d("DelayActivity", "Background color applied successfully: $backgroundColor")

                    // Test: Set a known test color to verify the system works
                    if (isDefault) {
                        android.util.Log.w(
                            "DelayActivity",
                            "Using default color - check if SharedPreferences is being saved correctly"
                        )
                        // Uncomment to test with a different color:
                        // setBackgroundColor(-16711936) // Green for testing
                    }
                } catch (e: Exception) {
                    android.util.Log.e("DelayActivity", "Failed to apply background color: $e")
                    // Fallback to a test color
                    setBackgroundColor(-65536) // Red as fallback
                    android.util.Log.w("DelayActivity", "Applied fallback red color")
                }

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
                text = "Take this moment to reflect"
                textSize = 14f
                setTextColor(0xB3FFFFFF.toInt())
                gravity = android.view.Gravity.CENTER
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                fontFeatureSettings = "smcp"
            }
        rootLayout.addView(infoTextView)

        // Recheck interval info
        val intervalTextView =
            TextView(this).apply {
                text = "Next reminder in ${recheckIntervalMinutes} minutes"
                textSize = 12f
                setTextColor(0x99FFFFFF.toInt())
                gravity = android.view.Gravity.CENTER
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                        .apply { topMargin = 8 }
            }
        rootLayout.addView(intervalTextView)

        // Buttons container
        val buttonsLayout =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
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

        // Close button - goes to home screen
        closeButton =
            Button(this).apply {
                text = "🙏 Stay Mindful"
                textSize = 16f
                setTextColor(0xFFFFFFFF.toInt())
                layoutParams =
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        .apply { rightMargin = 16 }
                setBackgroundColor(0x88000000.toInt())
                setPadding(24, 32, 24, 32)
                setOnClickListener {
                    countDownTimer?.cancel()
                    goToHomeScreen()
                }
            }
        buttonsLayout.addView(closeButton)

        // Open button - closes overlay and allows app to open
        openButton =
            Button(this).apply {
                text = "✨ Continue"
                textSize = 16f
                setTextColor(0xFFFFFFFF.toInt())
                layoutParams =
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        .apply { leftMargin = 16 }
                setBackgroundColor(0xAAFFFFFF.toInt())
                setTextColor(0xFF4A148C.toInt())
                setPadding(24, 32, 24, 32)
                isEnabled = false
                alpha = 0.5f
                setOnClickListener {
                    countDownTimer?.cancel()
                    notifyDelayScreenFinished(isContinue = true)
                    finish()
                }
            }
        buttonsLayout.addView(openButton)

        rootLayout.addView(buttonsLayout)

        setContentView(rootLayout)

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

                    // Enable the open button when timer finishes
                    openButton.isEnabled = true
                    openButton.alpha = 1.0f
                }
            }
                .start()
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

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }

    override fun onBackPressed() {
        // Prevent back button from closing the delay screen
        // User must wait for the timer to finish
    }

    private fun notifyDelayScreenFinished(isContinue: Boolean = false) {
        // Send broadcast to notify service that delay screen is finished
        val intent = Intent("com.example.extra_mind_time.DELAY_SCREEN_FINISHED")
        intent.putExtra("packageName", targetPackageName)
        intent.putExtra("isStayingMindful", !isContinue) // true when user clicked "Stay Mindful"
        sendBroadcast(intent)
    }
}
