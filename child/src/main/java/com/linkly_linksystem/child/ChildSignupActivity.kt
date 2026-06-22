package com.linkly_linksystem.child

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.FirebaseDatabase

class ChildSignupActivity : AppCompatActivity() {

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

    private var currentStep = 1 // 1: Profile, 2: Location, 3: Usage, 4: Overlay

    private lateinit var editChildName: AppCompatEditText
    private lateinit var editChildAge: AppCompatEditText
    private lateinit var btnNextStep: MaterialButton

    // Walkthrough Layout Containers
    private lateinit var flashCardProfile: View
    private lateinit var flashCardLocation: View
    private lateinit var flashCardUsage: View
    private lateinit var flashCardOverlay: View

    // Step dots
    private lateinit var dotStep1: View
    private lateinit var dotStep2: View
    private lateinit var dotStep3: View
    private lateinit var dotStep4: View

    // Status Indicators inside cards
    private lateinit var viewLocationStatusIndicator: View
    private lateinit var textLocationStatusBadge: TextView
    private lateinit var viewUsageStatusIndicator: View
    private lateinit var textUsageStatusBadge: TextView
    private lateinit var viewOverlayStatusIndicator: View
    private lateinit var textOverlayStatusBadge: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if already signed up, skip directly to MainActivity and ensure service is running
        val prefs = getSharedPreferences("LinklyChildPrefs", MODE_PRIVATE)
        if (prefs.getBoolean("is_signed_up", false)) {
            startProtectionService()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_child_signup)

        // Bind layouts
        flashCardProfile = findViewById(R.id.flashCardProfile)
        flashCardLocation = findViewById(R.id.flashCardLocation)
        flashCardUsage = findViewById(R.id.flashCardUsage)
        flashCardOverlay = findViewById(R.id.flashCardOverlay)

        dotStep1 = findViewById(R.id.dotStep1)
        dotStep2 = findViewById(R.id.dotStep2)
        dotStep3 = findViewById(R.id.dotStep3)
        dotStep4 = findViewById(R.id.dotStep4)

        editChildName = findViewById(R.id.editChildName)
        editChildAge = findViewById(R.id.editChildAge)
        btnNextStep = findViewById(R.id.btnNextStep)

        // Status monitors inside cards
        viewLocationStatusIndicator = findViewById(R.id.viewLocationStatusIndicator)
        textLocationStatusBadge = findViewById(R.id.textLocationStatusBadge)
        viewUsageStatusIndicator = findViewById(R.id.viewUsageStatusIndicator)
        textUsageStatusBadge = findViewById(R.id.textUsageStatusBadge)
        viewOverlayStatusIndicator = findViewById(R.id.viewOverlayStatusIndicator)
        textOverlayStatusBadge = findViewById(R.id.textOverlayStatusBadge)

        // Bind interactive triggers
        findViewById<MaterialButton>(R.id.btnGrantLocation).setOnClickListener {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                1001
            )
        }

        findViewById<MaterialButton>(R.id.btnGrantUsage).setOnClickListener {
            Toast.makeText(this, "Select 'Linkly Child' in the next settings panel", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            startActivity(intent)
        }

        findViewById<MaterialButton>(R.id.btnGrantOverlay).setOnClickListener {
            Toast.makeText(this, "Allow Draw Over Other Apps for 'Linkly Child'", Toast.LENGTH_LONG).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }

        btnNextStep.setOnClickListener {
            handleNextNavigation()
        }

        // Live refresh state
        refreshPermissionStates()
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionStates()
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
                // Safe fall-back
            }
        }
    }

    private fun handleNextNavigation() {
        when (currentStep) {
            1 -> {
                val name = editChildName.text.toString().trim()
                val age = editChildAge.text.toString().trim()

                if (name.isEmpty() || age.isEmpty()) {
                    Toast.makeText(this, "Please enter your Nickname and Age first", Toast.LENGTH_SHORT).show()
                    return
                }

                // Cache temp values
                val prefs = getSharedPreferences("LinklyChildPrefs", MODE_PRIVATE)
                prefs.edit().putString("child_name", name).putString("child_age", age).apply()

                // Move to Location Flash Card
                currentStep = 2
                updateUiForCurrentStep()
            }
            2 -> {
                // Flash Card 2: Location
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Please enable Location Access to proceed safety sync", Toast.LENGTH_SHORT).show()
                    return
                }
                currentStep = 3
                updateUiForCurrentStep()
            }
            3 -> {
                // Flash Card 3: App Activity Monitoring
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
                if (mode != AppOpsManager.MODE_ALLOWED) {
                    Toast.makeText(this, "App usage permission required for child analytics", Toast.LENGTH_SHORT).show()
                    return
                }
                currentStep = 4
                updateUiForCurrentStep()
            }
            4 -> {
                // Flash Card 4: Screen Interception Lock Overlay
                if (!Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "Overlay permission is mandatory for remote lockdown safety features", Toast.LENGTH_SHORT).show()
                    return
                }
                finalizeOnboardingSetup()
            }
        }
    }

    private fun updateUiForCurrentStep() {
        // Toggle card visibilities
        flashCardProfile.visibility = if (currentStep == 1) View.VISIBLE else View.GONE
        flashCardLocation.visibility = if (currentStep == 2) View.VISIBLE else View.GONE
        flashCardUsage.visibility = if (currentStep == 3) View.VISIBLE else View.GONE
        flashCardOverlay.visibility = if (currentStep == 4) View.VISIBLE else View.GONE

        // Update dot background indicator states
        dotStep1.setBackgroundColor(Color.parseColor(if (currentStep >= 1) "#4C51BF" else "#1F2937"))
        dotStep2.setBackgroundColor(Color.parseColor(if (currentStep >= 2) "#4C51BF" else "#1F2937"))
        dotStep3.setBackgroundColor(Color.parseColor(if (currentStep >= 3) "#4C51BF" else "#1F2937"))
        dotStep4.setBackgroundColor(Color.parseColor(if (currentStep >= 4) "#4C51BF" else "#1F2937"))

        // Update Button text/color based on step
        when (currentStep) {
            1 -> {
                btnNextStep.text = "Start Walkthrough"
                btnNextStep.setBackgroundColor(Color.parseColor("#4C51BF"))
            }
            2 -> {
                btnNextStep.text = "Confirm Location Setup"
                btnNextStep.setBackgroundColor(Color.parseColor("#4C51BF"))
            }
            3 -> {
                btnNextStep.text = "Confirm App Analytics"
                btnNextStep.setBackgroundColor(Color.parseColor("#D53F8C"))
            }
            4 -> {
                btnNextStep.text = "Activate Protective System"
                btnNextStep.setBackgroundColor(Color.parseColor("#48BB78"))
            }
        }

        refreshPermissionStates()
    }

    private fun refreshPermissionStates() {
        // 1. Location state
        val locationGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (locationGranted) {
            viewLocationStatusIndicator.setBackgroundColor(Color.parseColor("#48BB78"))
            textLocationStatusBadge.text = "ACCESS ENABLED"
            textLocationStatusBadge.setTextColor(Color.parseColor("#48BB78"))
        } else {
            viewLocationStatusIndicator.setBackgroundColor(Color.parseColor("#EF4444"))
            textLocationStatusBadge.text = "ACCESS DISABLED"
            textLocationStatusBadge.setTextColor(Color.parseColor("#EF4444"))
        }

        // 2. Usage statistics state
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
        val usageGranted = mode == AppOpsManager.MODE_ALLOWED
        if (usageGranted) {
            viewUsageStatusIndicator.setBackgroundColor(Color.parseColor("#48BB78"))
            textUsageStatusBadge.text = "STATS SYNC ACTIVE"
            textUsageStatusBadge.setTextColor(Color.parseColor("#48BB78"))
        } else {
            viewUsageStatusIndicator.setBackgroundColor(Color.parseColor("#EF4444"))
            textUsageStatusBadge.text = "STATS DISABLED"
            textUsageStatusBadge.setTextColor(Color.parseColor("#EF4444"))
        }

        // 3. System alert overlay window state
        val overlayGranted = Settings.canDrawOverlays(this)
        if (overlayGranted) {
            viewOverlayStatusIndicator.setBackgroundColor(Color.parseColor("#48BB78"))
            textOverlayStatusBadge.text = "SHIELD CONNECTED"
            textOverlayStatusBadge.setTextColor(Color.parseColor("#48BB78"))
        } else {
            viewOverlayStatusIndicator.setBackgroundColor(Color.parseColor("#EF4444"))
            textOverlayStatusBadge.text = "OVERLAY DISABLED"
            textOverlayStatusBadge.setTextColor(Color.parseColor("#EF4444"))
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        refreshPermissionStates()
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission granted!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun finalizeOnboardingSetup() {
        val prefs = getSharedPreferences("LinklyChildPrefs", MODE_PRIVATE)
        val name = prefs.getString("child_name", "Child Device")
        val age = prefs.getString("child_age", "--")

        // Generate pairing code
        val charPool = ('A'..'Z') + ('0'..'9')
        val uniqueToken = (1..6).map { charPool.random() }.joinToString("")

        val handshakePayload = hashMapOf(
            "childDeviceId" to deviceUniqueId,
            "childName" to name,
            "childAge" to age,
            "timestamp" to System.currentTimeMillis()
        )

        // Submit child codes token handshake to Firebase Realtime Database
        database.getReference("child_codes/$uniqueToken").setValue(handshakePayload)
            .addOnSuccessListener {
                // Finalize setup local caches
                prefs.edit()
                    .putString("pairing_code", uniqueToken)
                    .putBoolean("is_signed_up", true)
                    .apply()

                // Sync status permissions directly
                val permissionsMap = hashMapOf(
                    "isLocationGranted" to true,
                    "isUsageStatsGranted" to true,
                    "isOverlayGranted" to true
                )
                database.getReference("devices/$deviceUniqueId/permissions").setValue(permissionsMap)

                // Launch persistent background protection loop service
                startProtectionService()

                Toast.makeText(this@ChildSignupActivity, "Companion Onboarding Complete! Pairing key: $uniqueToken", Toast.LENGTH_LONG).show()

                val intent = Intent(this@ChildSignupActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this@ChildSignupActivity, "Network Error: Verify active internet access.", Toast.LENGTH_LONG).show()
            }
    }
}
