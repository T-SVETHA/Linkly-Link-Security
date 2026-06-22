package com.linkly_linksystem.child

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import java.util.*
import java.util.concurrent.TimeUnit

class UsageStatsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usage_stats)
    }

    override fun onResume() {
        super.onResume()
        if (hasUsageStatsPermission()) {
            displayUsageStats()
        } else {
            requestUsageStatsPermission()
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    packageName
                )
            }
        } catch (e: Exception) {
            AppOpsManager.MODE_IGNORED
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun requestUsageStatsPermission() {
        Toast.makeText(this, "Usage Access Permission Required", Toast.LENGTH_LONG).show()
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        startActivity(intent)
    }

    private fun displayUsageStats() {
        val layoutUsageList = findViewById<LinearLayout>(R.id.layoutUsageList)
        layoutUsageList.removeAllViews()

        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - TimeUnit.DAYS.toMillis(1) // Last 24 Hours

        val usageStatsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        if (usageStatsList.isNullOrEmpty()) {
            val emptyTextView = TextView(this).apply {
                text = "No usage statistics available. Ensure usage access is enabled in Settings."
                textSize = 15f
                setTextColor(android.graphics.Color.parseColor("#64748B"))
                gravity = android.view.Gravity.CENTER
                setPadding(16, 48, 16, 16)
            }
            layoutUsageList.addView(emptyTextView)
            return
        }

        // Filter and sort descending by total time in foreground
        val sortedList = usageStatsList.filter { it.totalTimeInForeground > 0 }
            .sortedByDescending { it.totalTimeInForeground }

        if (sortedList.isEmpty()) {
            val emptyTextView = TextView(this).apply {
                text = "No active usage recorded in the last 24 hours."
                textSize = 15f
                setTextColor(android.graphics.Color.parseColor("#64748B"))
                gravity = android.view.Gravity.CENTER
                setPadding(16, 48, 16, 16)
            }
            layoutUsageList.addView(emptyTextView)
            return
        }

        for (stats in sortedList) {
            val cardView = MaterialCardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 16)
                }
                radius = 12f
                cardElevation = 0f
                setStrokeColor(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E2E8F0")))
                strokeWidth = 2
                setCardBackgroundColor(android.graphics.Color.WHITE)
            }

            val itemLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(24, 20, 24, 20)
            }

            val textPackageName = TextView(this).apply {
                text = stats.packageName
                textSize = 15f
                setTextColor(android.graphics.Color.parseColor("#1E293B"))
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            val textTimeUsed = TextView(this).apply {
                val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(stats.totalTimeInForeground)
                text = "Foreground Duration: $totalMinutes min"
                textSize = 13f
                setTextColor(android.graphics.Color.parseColor("#64748B"))
                setPadding(0, 4, 0, 0)
            }

            itemLayout.addView(textPackageName)
            itemLayout.addView(textTimeUsed)
            cardView.addView(itemLayout)
            layoutUsageList.addView(cardView)
        }
    }
}
