package com.linkly_linksystem.child

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LockOverlayActivity : AppCompatActivity() {

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

    private var cachedBypassPin = "1234"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make overlay cover lockscreen, keep screen on, and bypass standard status indicators
        window.addFlags(
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        setContentView(R.layout.activity_lock_overlay)

        val editPinCode = findViewById<AppCompatEditText>(R.id.editPinCode)
        val btnUnlock = findViewById<MaterialButton>(R.id.btnUnlock)

        // Sync Parent-Configured Bypass PIN in Real-Time
        database.getReference("devices/$deviceUniqueId/status/bypassPin")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val remotePin = snapshot.getValue(String::class.java)
                    if (!remotePin.isNullOrEmpty()) {
                        cachedBypassPin = remotePin
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        // Remote Auto-Unlock Watcher
        database.getReference("devices/$deviceUniqueId/status/isLockedByParent")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val isLockActive = snapshot.getValue(Boolean::class.java) ?: false
                    if (!isLockActive) {
                        finish()
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        // Local PIN Unlock Action
        btnUnlock.setOnClickListener {
            val enteredPin = editPinCode.text.toString().trim()
            if (enteredPin == cachedBypassPin) {
                database.getReference("devices/$deviceUniqueId/status/isLockedByParent").setValue(false)
                finish()
            } else {
                Toast.makeText(this, "Access Denied: Invalid Administrative PIN", Toast.LENGTH_SHORT).show()
                editPinCode.text?.clear()
            }
        }

        // Disable standard back button via onBackPressedDispatcher to prevent easily bypassing the lock
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Ignore back presses
            }
        })

        // Enforce Full-Screen Immersive mode on startup
        applyImmersiveStickyFullscreen()
    }

    override fun onResume() {
        super.onResume()
        applyImmersiveStickyFullscreen()
    }

    private fun applyImmersiveStickyFullscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }
    }
}