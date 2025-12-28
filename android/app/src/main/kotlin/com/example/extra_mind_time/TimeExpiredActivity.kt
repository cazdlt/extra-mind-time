package com.example.extra_mind_time

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class TimeExpiredActivity : Activity() {

    private lateinit var messageTextView: TextView
    private lateinit var iconView: TextView
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

        // Get data from intent
        targetPackageName = intent.getStringExtra("packageName")
        targetAppName = intent.getStringExtra("appName")
        val message = intent.getStringExtra("message") ?: "Your time is up! How much more time do you need?"

        // Get settings from SharedPreferences
        val prefs = getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)

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

        // Get background color (same as mindful popup)
        val backgroundColorValue = prefs.getLong("flutter.background_color", -7637753)
        val backgroundColor = backgroundColorValue.toInt()

        // Create the layout programmatically
        createLayout(message, backgroundColor)
    }

    private fun createLayout(message: String, backgroundColor: Int) {
        // Set activity background
        try {
            this.window.decorView.setBackgroundColor(backgroundColor)
        } catch (e: Exception) {
            this.window.decorView.setBackgroundColor(Color.RED)
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

        // Icon (alarm)
        iconView =
            TextView(this).apply {
                text = "⏰"
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

        // Message
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
                        .apply { bottomMargin = 48 }
                alpha = 0.95f
            }
        rootLayout.addView(messageTextView)

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

        // Stop using button
        val stopButton =
            Button(this).apply {
                text = "🛑 Stop Using App"
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
                    goToHomeScreen()
                }
            }
        buttonsLayout.addView(stopButton)

        // Time limit buttons
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
                    setOnClickListener {
                        selectTimeLimit(minutes)
                    }
                }
            buttonsLayout.addView(timeLimitButton)
        }

        rootLayout.addView(buttonsLayout)

        setContentView(rootLayout)

        // Try setting background after setContentView as well
        try {
            window.decorView.setBackgroundColor(backgroundColor)
            rootLayout.setBackgroundColor(backgroundColor)
        } catch (e: Exception) {
            window.decorView.setBackgroundColor(Color.RED)
            rootLayout.setBackgroundColor(Color.RED)
        }

        // Start animation
        startAnimation()
    }

    private fun startAnimation() {
        val scaleUp =
            ObjectAnimator.ofFloat(iconView, View.SCALE_X, 1f, 1.2f).apply {
                duration = 800
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
            }
        val scaleUpY =
            ObjectAnimator.ofFloat(iconView, View.SCALE_Y, 1f, 1.2f).apply {
                duration = 800
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
            }
        scaleUp.start()
        scaleUpY.start()
    }

    private fun selectTimeLimit(minutes: Int) {
        val intent = Intent("com.example.extra_mind_time.TIME_LIMIT_EXTENDED")
        intent.putExtra("packageName", targetPackageName)
        intent.putExtra("appName", targetAppName)
        intent.putExtra("extraMinutes", minutes)
        sendBroadcast(intent)
        finish()
    }

    private fun goToHomeScreen() {
        val homeIntent =
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        startActivity(homeIntent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBackPressed() {
        // Prevent back button from closing the time expired screen
        // User must select an option
    }
}
