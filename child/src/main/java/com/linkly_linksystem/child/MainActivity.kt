package com.linkly_linksystem.child

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    private val database by lazy {
        try {
            FirebaseDatabase.getInstance()
        } catch (e: Exception) {
            try {
                FirebaseDatabase.getInstance("https://linkly-link-system-default-rtdb.firebaseio.com/")
            } catch (ex: Exception) {
                FirebaseDatabase.getInstance("https://linkly-link-system.firebaseio.com/")
            }
        }
    }
    private val deviceUniqueId = "child_device_9988"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Ensure the background Protective Service is started
        startProtectionService()

        val statusDot = findViewById<View>(R.id.statusDot)
        val textSystemStatus = findViewById<TextView>(R.id.textSystemStatus)
        val textInstruction = findViewById<TextView>(R.id.textInstruction)
        val textPairingCode = findViewById<TextView>(R.id.textPairingCode)
        val btnGenerateCode = findViewById<MaterialButton>(R.id.btnGenerateCode)
        val btnViewUsageStats = findViewById<MaterialButton>(R.id.btnViewUsageStats)
        val btnLaunchGame = findViewById<MaterialButton>(R.id.btnLaunchGame)

        // Make dot visually round immediately
        statusDot.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#EF4444")) // Default unlinked (crimson)
        }

        // Cache parameters load
        val prefs = getSharedPreferences("LinklyChildPrefs", MODE_PRIVATE)
        val cachedCode = prefs.getString("pairing_code", "------")
        textPairingCode.text = cachedCode

        btnViewUsageStats.setOnClickListener {
            val intent = Intent(this, UsageStatsActivity::class.java)
            startActivity(intent)
        }

        btnLaunchGame.setOnClickListener {
            val intent = Intent(this, EducationalGameActivity::class.java)
            startActivity(intent)
        }

        // 🛰️ LINK WATCHER
        val systemLinkCheckRef = database.getReference("devices/$deviceUniqueId/status/isLinked")
        systemLinkCheckRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isDeviceLinked = snapshot.getValue(Boolean::class.java) ?: false

                val shape = GradientDrawable().apply { shape = GradientDrawable.OVAL }
                if (isDeviceLinked) {
                    shape.setColor(Color.parseColor("#48BB78")) // Emerald Green
                    statusDot.background = shape
                    textSystemStatus.text = "Shield Protection Active"
                    textSystemStatus.setTextColor(Color.parseColor("#48BB78"))
                    textInstruction.text = "This child device is fully linked. You can generate additional pairing codes below to link more parent devices."
                    btnGenerateCode.visibility = View.VISIBLE
                } else {
                    shape.setColor(Color.parseColor("#EF4444")) // Crimson Red
                    statusDot.background = shape
                    textSystemStatus.text = "Shield Unlinked"
                    textSystemStatus.setTextColor(Color.parseColor("#EF4444"))
                    btnGenerateCode.visibility = View.VISIBLE
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // 🎲 REGENERATE CODE TRIGGER
        btnGenerateCode.setOnClickListener {
            val name = prefs.getString("child_name", "Child Device")
            val age = prefs.getString("child_age", "--")

            val charPool = ('A'..'Z') + ('0'..'9')
            val uniqueToken = (1..6).map { charPool.random() }.joinToString("")

            val handshakePayload = hashMapOf(
                "childDeviceId" to deviceUniqueId,
                "childName" to name,
                "childAge" to age,
                "timestamp" to System.currentTimeMillis()
            )

            database.getReference("child_codes/$uniqueToken").setValue(handshakePayload)
                .addOnSuccessListener {
                    textPairingCode.text = uniqueToken
                    textInstruction.text = "Keep this screen open and input this key into your Parent App dashboard now."
                    prefs.edit().putString("pairing_code", uniqueToken).apply()
                    Toast.makeText(this@MainActivity, "Pairing token generated successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this@MainActivity, "Network Error: Couldn't reach Firebase.", Toast.LENGTH_LONG).show()
                }
        }

        // --- 🔒 LOCKDOWN FULL-SCREEN INTENT SETUP ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Device Administration Alerts"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("lockdown_channel", name, importance).apply {
                description = "Critical security and lockdown notifications"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        database.getReference("devices/$deviceUniqueId/status/isLockedByParent")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val isLocked = snapshot.getValue(Boolean::class.java) ?: false
                    if (isLocked) {
                        showLockdownNotification()
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        // --- ⏳ SCREEN TIME PROGRESS DATA SYNC ---
        val screenTimeUsedRef = database.getReference("devices/$deviceUniqueId/screentime_used")
        val screenTimeLimitRef = database.getReference("devices/$deviceUniqueId/screentime_limit")

        var usedMins = 0
        var limitMins = 60

        fun updateChildScreenTimeUI() {
            val textChildScreenTime = findViewById<TextView>(R.id.textChildScreenTime)
            val progressChildScreenTime = findViewById<com.google.android.material.progressindicator.LinearProgressIndicator>(R.id.progressChildScreenTime)

            textChildScreenTime.text = "$usedMins mins used out of $limitMins mins limit"
            
            val pct = if (limitMins > 0) (usedMins * 100) / limitMins else 0
            progressChildScreenTime.progress = pct.coerceIn(0, 100)
            
            if (pct >= 100) {
                progressChildScreenTime.setIndicatorColor(Color.parseColor("#EF4444")) // Crimson
            } else if (pct >= 80) {
                progressChildScreenTime.setIndicatorColor(Color.parseColor("#F59E0B")) // Amber
            } else {
                progressChildScreenTime.setIndicatorColor(Color.parseColor("#10B981")) // Emerald
            }
        }

        screenTimeUsedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                usedMins = snapshot.getValue(Int::class.java) ?: 0
                updateChildScreenTimeUI()
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        screenTimeLimitRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                limitMins = snapshot.getValue(Int::class.java) ?: 60
                updateChildScreenTimeUI()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun showLockdownNotification() {
        try {
            val intent = Intent(this, LockOverlayActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
            startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(this, LockOverlayActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(this, "lockdown_channel")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setContentTitle("Device Locked")
                .setContentText("An administrator has locked this device.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setFullScreenIntent(pendingIntent, true)
                .setAutoCancel(true)
                .setOngoing(true)

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.notify(99, builder.build())
        }
    }

    private fun startProtectionService() {
        try {
            val serviceIntent = Intent(this, ProtectionService::class.java)
            ContextCompat.startForegroundService(this, serviceIntent)
        } catch (e: Exception) {
            try {
                val serviceIntent = Intent(this, ProtectionService::class.java)
                startService(serviceIntent)
            } catch (ex: Exception) {
                // Safe catch-all fallback
            }
        }
    }
}