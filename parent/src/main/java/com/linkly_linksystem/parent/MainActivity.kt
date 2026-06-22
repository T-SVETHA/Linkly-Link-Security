package com.linkly_linksystem.parent

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// Safe Zone Data Model
data class SafeZone(
    val id: String = "",
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val radius: Double = 0.0
)

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
    
    private val sharedPrefs by lazy {
        getSharedPreferences("ParentAppPrefs", Context.MODE_PRIVATE)
    }

    private var targetsChildHardwareId = "child_device_9988"

    // Safe Zone Variables
    private var childLastLat: Double? = null
    private var childLastLng: Double? = null
    private val safeZonesList = mutableListOf<SafeZone>()

    // Map WebView References
    private lateinit var mapWebView: android.webkit.WebView
    private var isMapLoaded = false
    private var isUpdatingFromFirebase = false

    // Layout Views declarations (Class Properties)
    private lateinit var textDashboardSubtitle: TextView
    private lateinit var textDeviceConnectionState: TextView
    private lateinit var statusConnectionIndicator: View
    private lateinit var cardPairing: MaterialCardView
    private lateinit var textProfileChildName: TextView
    private lateinit var textProfileChildAge: TextView
    private lateinit var indicatorLocation: View
    private lateinit var labelLocationState: TextView
    private lateinit var indicatorUsageStats: View
    private lateinit var labelUsageStatsState: TextView
    private lateinit var textDeviceLocation: TextView
    private lateinit var textLocationTimestamp: TextView
    private lateinit var textActiveApp: TextView
    private lateinit var layoutAlertsFeed: LinearLayout
    private lateinit var editBypassPin: AppCompatEditText
    private lateinit var switchLockdown: SwitchMaterial
    private lateinit var textSafeZoneStatus: TextView
    private lateinit var layoutSafeZonesList: LinearLayout
    private lateinit var layoutInstalledAppsList: LinearLayout
    private lateinit var layoutAppScreenStats: LinearLayout
    private lateinit var layoutInstallationEvents: LinearLayout
    private lateinit var textDeviceBattery: TextView

    // Track active database listeners to prevent memory leaks and support child switching
    private val activeListeners = mutableListOf<Pair<DatabaseReference, ValueEventListener>>()

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Notification permission denied. Alerts will not appear in the status bar.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // View Bindings
        textDashboardSubtitle = findViewById(R.id.textDashboardSubtitle)
        textDeviceConnectionState = findViewById(R.id.textDeviceConnectionState)
        statusConnectionIndicator = findViewById(R.id.statusConnectionIndicator)
        switchLockdown = findViewById(R.id.switchLockdown)
        val btnRemoteUnlock = findViewById<MaterialButton>(R.id.btnRemoteUnlock)
        cardPairing = findViewById(R.id.cardPairing)
        val editPairingCode = findViewById<AppCompatEditText>(R.id.editPairingCode)
        val btnLinkDevice = findViewById<MaterialButton>(R.id.btnLinkDevice)

        // Navigation Screens
        val screenDashboard = findViewById<View>(R.id.screen_dashboard)
        val screenMap = findViewById<View>(R.id.screen_map)
        val screenApps = findViewById<View>(R.id.screen_apps)
        val screenAlerts = findViewById<View>(R.id.screen_alerts)
        val screenProfile = findViewById<View>(R.id.screen_profile)
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // Dynamic Permissions View References
        indicatorLocation = findViewById(R.id.indicatorLocation)
        labelLocationState = findViewById(R.id.labelLocationState)
        indicatorUsageStats = findViewById(R.id.indicatorUsageStats)
        labelUsageStatsState = findViewById(R.id.labelUsageStatsState)

        // Profile Display Bindings
        textProfileChildName = findViewById(R.id.textProfileChildName)
        textProfileChildAge = findViewById(R.id.textProfileChildAge)

        // Telemetry GPS Locations
        textDeviceLocation = findViewById(R.id.textDeviceLocation)
        textLocationTimestamp = findViewById(R.id.textLocationTimestamp)

        // Safe Zone Setup Panel Bindings
        val editSafeZoneName = findViewById<AppCompatEditText>(R.id.editSafeZoneName)
        val editSafeZoneLat = findViewById<AppCompatEditText>(R.id.editSafeZoneLat)
        val editSafeZoneLng = findViewById<AppCompatEditText>(R.id.editSafeZoneLng)
        val editSafeZoneRadius = findViewById<AppCompatEditText>(R.id.editSafeZoneRadius)
        val btnSaveSafeZone = findViewById<MaterialButton>(R.id.btnSaveSafeZone)
        textSafeZoneStatus = findViewById(R.id.textSafeZoneStatus)
        layoutSafeZonesList = findViewById(R.id.layoutSafeZonesList)
        layoutInstalledAppsList = findViewById(R.id.layoutInstalledAppsList)
        layoutAppScreenStats = findViewById(R.id.layoutAppScreenStats)
        layoutInstallationEvents = findViewById(R.id.layoutInstallationEvents)
        textDeviceBattery = findViewById(R.id.textDeviceBattery)

        // App Blocker Display
        textActiveApp = findViewById(R.id.textActiveApp)

        // WhatsApp alerts list container
        layoutAlertsFeed = findViewById(R.id.layoutAlertsFeed)

        // Bypass PIN controls
        editBypassPin = findViewById(R.id.editBypassPin)
        val btnUpdateBypassPin = findViewById<MaterialButton>(R.id.btnUpdateBypassPin)

        // Map WebView Initialization
        mapWebView = findViewById(R.id.mapWebView)
        setupMapWebView()

        // --- 🗂️ TAB CONTROLLER ROUTER ---
        bottomNavigation.setOnItemSelectedListener { item ->
            // Hide all layouts
            screenDashboard.visibility = View.GONE
            screenMap.visibility = View.GONE
            screenApps.visibility = View.GONE
            screenAlerts.visibility = View.GONE
            screenProfile.visibility = View.GONE

            when (item.itemId) {
                R.id.nav_dashboard -> {
                    screenDashboard.visibility = View.VISIBLE
                    true
                }
                R.id.nav_map -> {
                    screenMap.visibility = View.VISIBLE
                    true
                }
                R.id.nav_apps -> {
                    screenApps.visibility = View.VISIBLE
                    true
                }
                R.id.nav_alerts -> {
                    screenAlerts.visibility = View.VISIBLE
                    true
                }
                R.id.nav_profile -> {
                    screenProfile.visibility = View.VISIBLE
                    true
                }
                else -> false
            }
        }

        // Initialize Spinner & Bind Listeners
        setupChildrenSpinner()

        // --- 🛰️ REMOTE SWITCH ACTION TRIGGER ---
        switchLockdown.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingFromFirebase) return@setOnCheckedChangeListener
            // Extract lockdown ref dynamically matching selected child
            val lockStatusRef = database.getReference("devices/$targetsChildHardwareId/status/isLockedByParent")
            lockStatusRef.setValue(isChecked)
                .addOnSuccessListener {
                    val statusAlertText = if (isChecked) "Lockdown Signal Transmitted!" else "Remote Unlock Signal Sent!"
                    Toast.makeText(this@MainActivity, statusAlertText, Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this@MainActivity, "Database Synchronization Failed", Toast.LENGTH_SHORT).show()
                }
        }

        // REMOTE UNLOCK ACTION TRIGGER
        btnRemoteUnlock.setOnClickListener {
            database.getReference("devices/$targetsChildHardwareId/status/isLockedByParent")
                .setValue(false)
                .addOnSuccessListener {
                    Toast.makeText(this@MainActivity, "Unlock signal sent successfully.", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this@MainActivity, "Database Synchronization Failed", Toast.LENGTH_SHORT).show()
                }
        }

        // --- 🛰️ ADMINISTRATIVE BYPASS PIN SYNC UPDATE ---
        btnUpdateBypassPin.setOnClickListener {
            val enteredPin = editBypassPin.text.toString().trim()
            if (enteredPin.length != 4) {
                Toast.makeText(this, "Administrative override code must be exactly 4 digits", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            database.getReference("devices/$targetsChildHardwareId/status/bypassPin")
                .setValue(enteredPin)
                .addOnSuccessListener {
                    Toast.makeText(this, "Administrative bypass override updated in Firebase!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to upload security PIN changes.", Toast.LENGTH_SHORT).show()
                }
        }

        // --- 🤝 HANDSHAKE & DEVICE SYNC VALIDATION ---
        btnLinkDevice.setOnClickListener {
            val code = editPairingCode.text.toString().trim().uppercase()
            if (code.length != 6) {
                Toast.makeText(this, "Please enter a valid 6-character pairing code", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            database.getReference("child_codes/$code").get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val childDeviceId = snapshot.child("childDeviceId").getValue(String::class.java)
                        val childName = snapshot.child("childName").getValue(String::class.java) ?: "Child Device"
                        val childAge = snapshot.child("childAge").getValue(String::class.java) ?: "--"

                        if (childDeviceId != null) {
                            // Persist newly linked child
                            savePairedChild(childDeviceId, childName)
                            
                            // Reconfigure profiles node details in Firebase
                            val profileUpdates = hashMapOf(
                                "name" to childName,
                                "age" to childAge
                            )
                            database.getReference("devices/$childDeviceId/profile").setValue(profileUpdates)

                            // Link successfully status
                            database.getReference("devices/$childDeviceId/status/isLinked").setValue(true)
                                .addOnSuccessListener {
                                    Toast.makeText(this@MainActivity, "Device ($childName) linked successfully!", Toast.LENGTH_SHORT).show()
                                    editPairingCode.text?.clear()
                                    
                                    // Refresh children selection spinner
                                    setupChildrenSpinner()
                                }
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "Invalid or expired token.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this@MainActivity, "Network Error: Link validation failed.", Toast.LENGTH_SHORT).show()
                }
        }

        // --- 🛡️ SAVE NEW CUSTOM GEOCIRCLE SAFE ZONE BOUNDARY ---
        btnSaveSafeZone.setOnClickListener {
            val nameText = editSafeZoneName.text.toString().trim()
            val latText = editSafeZoneLat.text.toString().trim()
            val lngText = editSafeZoneLng.text.toString().trim()
            val radiusText = editSafeZoneRadius.text.toString().trim()

            if (nameText.isEmpty() || latText.isEmpty() || lngText.isEmpty() || radiusText.isEmpty()) {
                Toast.makeText(this, "Please fill in Name, Coordinates and Radius values.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val latVal = latText.toDoubleOrNull()
            val lngVal = lngText.toDoubleOrNull()
            val radiusVal = radiusText.toDoubleOrNull()

            if (latVal == null || lngVal == null || radiusVal == null) {
                Toast.makeText(this, "Please enter valid numeric coordinates.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val zoneId = UUID.randomUUID().toString()
            val newZone = SafeZone(zoneId, nameText, latVal, lngVal, radiusVal)

            database.getReference("devices/$targetsChildHardwareId/safe_zones/$zoneId")
                .setValue(newZone)
                .addOnSuccessListener {
                    Toast.makeText(this@MainActivity, "Safe Zone '$nameText' added successfully!", Toast.LENGTH_SHORT).show()
                    editSafeZoneName.text?.clear()
                    editSafeZoneLat.text?.clear()
                    editSafeZoneLng.text?.clear()
                    editSafeZoneRadius.text?.clear()
                }
                .addOnFailureListener {
                    Toast.makeText(this@MainActivity, "Failed to save safe zone boundary.", Toast.LENGTH_SHORT).show()
                }
        }

        // --- 🔔 RUNTIME NOTIFICATION PERMISSION FOR ANDROID 13+ ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // --- 🤝 MULTI-CHILD PREFERENCES HANDLER ---
    private fun getPairedChildren(): List<Pair<String, String>> {
        val pairedSet = sharedPrefs.getStringSet("paired_children", null) ?: emptySet()
        val list = mutableListOf<Pair<String, String>>()
        
        // Always include default child
        list.add(Pair("child_device_9988", "Simulated Child"))
        
        for (item in pairedSet) {
            val parts = item.split(":", limit = 2)
            if (parts.size == 2) {
                val deviceId = parts[0]
                val deviceName = parts[1]
                if (deviceId != "child_device_9988") {
                    list.add(Pair(deviceId, deviceName))
                }
            }
        }
        return list
    }

    private fun savePairedChild(deviceId: String, deviceName: String) {
        val pairedSet = sharedPrefs.getStringSet("paired_children", emptySet()) ?: emptySet()
        val updatedSet = pairedSet.toMutableSet().apply {
            add("$deviceId:$deviceName")
        }
        sharedPrefs.edit().putStringSet("paired_children", updatedSet).apply()
    }

    private fun setupChildrenSpinner() {
        val spinnerChildren = findViewById<Spinner>(R.id.spinnerChildren)
        val childrenList = getPairedChildren()
        val displayNames = childrenList.map { it.second }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            displayNames
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        spinnerChildren.adapter = adapter

        val currentIndex = childrenList.indexOfFirst { it.first == targetsChildHardwareId }
        if (currentIndex != -1) {
            spinnerChildren.setSelection(currentIndex)
        }

        spinnerChildren.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedChild = childrenList[position]
                if (targetsChildHardwareId != selectedChild.first) {
                    targetsChildHardwareId = selectedChild.first
                    
                    // Re-bind listeners for newly selected child
                    clearActiveListeners()
                    bindChildData(targetsChildHardwareId)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Initial binding if listeners list empty
        if (activeListeners.isEmpty()) {
            bindChildData(targetsChildHardwareId)
        }
    }

    // --- 📡 REAL-TIME DATABASING VALUE-LISTENERS RE-BINDING ENGINE ---
    private fun addManagedListener(ref: DatabaseReference, listener: ValueEventListener) {
        ref.addValueEventListener(listener)
        activeListeners.add(Pair(ref, listener))
    }

    private fun clearActiveListeners() {
        for ((ref, listener) in activeListeners) {
            ref.removeEventListener(listener)
        }
        activeListeners.clear()
    }

    private fun bindChildData(childId: String) {
        // --- 🛰️ LINK WATCHER ---
        val systemLinkCheckRef = database.getReference("devices/$childId/status/isLinked")
        val linkCheckListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isDeviceLinked = snapshot.getValue(Boolean::class.java) ?: false
                if (isDeviceLinked) {
                    textDeviceConnectionState.text = "Hardware Active & Monitoring"
                    statusConnectionIndicator.setBackgroundColor(Color.parseColor("#10B981"))
                    cardPairing.visibility = View.GONE
                } else {
                    textDeviceConnectionState.text = "Waiting for Device Link..."
                    statusConnectionIndicator.setBackgroundColor(Color.parseColor("#EF4444"))
                    cardPairing.visibility = View.VISIBLE
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        addManagedListener(systemLinkCheckRef, linkCheckListener)

        // --- 🛰️ REAL-TIME PROFILE SYNC ---
        val profileRef = database.getReference("devices/$childId/profile")
        val profileListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val name = snapshot.child("name").getValue(String::class.java) ?: "Unlinked Device"
                    val age = snapshot.child("age").getValue(String::class.java) ?: "--"
                    
                    textDashboardSubtitle.text = "Monitoring Node: $name (Age $age)"
                    textProfileChildName.text = name
                    textProfileChildAge.text = "Age: $age | Hardware ID: $childId"
                } else {
                    textDashboardSubtitle.text = "Real-Time Family Supervision Node"
                    textProfileChildName.text = "Waiting for Pairing..."
                    textProfileChildAge.text = "Age: -- | Hardware ID: $childId"
                }
                updateMapState() // Refresh marker title
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        addManagedListener(profileRef, profileListener)

        // --- 🛰️ REAL-TIME PERMISSIONS SYNC ---
        val permissionsRef = database.getReference("devices/$childId/permissions")
        val permissionsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val locationGranted = snapshot.child("isLocationGranted").getValue(Boolean::class.java) ?: false
                val usageGranted = snapshot.child("isUsageStatsGranted").getValue(Boolean::class.java) ?: false

                if (locationGranted) {
                    indicatorLocation.setBackgroundColor(Color.parseColor("#10B981"))
                    labelLocationState.text = "GRANTED"
                    labelLocationState.setTextColor(Color.parseColor("#10B981"))
                } else {
                    indicatorLocation.setBackgroundColor(Color.parseColor("#EF4444"))
                    labelLocationState.text = "DISABLED"
                    labelLocationState.setTextColor(Color.parseColor("#EF4444"))
                }

                if (usageGranted) {
                    indicatorUsageStats.setBackgroundColor(Color.parseColor("#10B981"))
                    labelUsageStatsState.text = "GRANTED"
                    labelUsageStatsState.setTextColor(Color.parseColor("#10B981"))
                } else {
                    indicatorUsageStats.setBackgroundColor(Color.parseColor("#EF4444"))
                    labelUsageStatsState.text = "DISABLED"
                    labelUsageStatsState.setTextColor(Color.parseColor("#EF4444"))
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        addManagedListener(permissionsRef, permissionsListener)

        // --- 🛰️ GPS LOCATION SYNC ---
        val locationRef = database.getReference("devices/$childId/location")
        val locationListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lat = snapshot.child("latitude").getValue(Double::class.java)
                val lng = snapshot.child("longitude").getValue(Double::class.java)
                val time = snapshot.child("timestamp").getValue(Long::class.java)

                if (lat != null && lng != null) {
                    childLastLat = lat
                    childLastLng = lng
                    textDeviceLocation.text = String.format(Locale.US, "Latitude: %.5f | Longitude: %.5f", lat, lng)
                    if (time != null) {
                        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                        textLocationTimestamp.text = "Last Synced: " + sdf.format(Date(time))
                    }
                    updateMapState()
                } else {
                    textDeviceLocation.text = "Location Telemetry: Offline"
                    textLocationTimestamp.text = "No Coordinates Synced."
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        addManagedListener(locationRef, locationListener)

        // --- 🛡️ PLURAL SAFE ZONES BOUNDARIES SYNC ---
        val safeZonesRef = database.getReference("devices/$childId/safe_zones")
        val safeZonesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                safeZonesList.clear()
                for (zoneSnapshot in snapshot.children) {
                    val id = zoneSnapshot.child("id").getValue(String::class.java) ?: zoneSnapshot.key ?: ""
                    val name = zoneSnapshot.child("name").getValue(String::class.java) ?: "Unnamed"
                    val latVal = zoneSnapshot.child("latitude").getValue(Double::class.java) ?: 0.0
                    val lngVal = zoneSnapshot.child("longitude").getValue(Double::class.java) ?: 0.0
                    val radiusVal = zoneSnapshot.child("radius").getValue(Double::class.java) ?: 0.0
                    
                    safeZonesList.add(SafeZone(id, name, latVal, lngVal, radiusVal))
                }
                
                updateSafeZonesListUI()
                updateMapState()
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        addManagedListener(safeZonesRef, safeZonesListener)

        // --- 🔋 BATTERY MONITOR SYNC ---
        val batteryRef = database.getReference("devices/$childId/telemetry/batteryLevel")
        val batteryListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val battery = snapshot.getValue(Int::class.java)
                if (battery != null) {
                    textDeviceBattery.text = "🔋 $battery%"
                    if (battery <= 20) {
                        textDeviceBattery.setTextColor(Color.parseColor("#EF4444"))
                    } else if (battery <= 50) {
                        textDeviceBattery.setTextColor(Color.parseColor("#F59E0B"))
                    } else {
                        textDeviceBattery.setTextColor(Color.parseColor("#10B981"))
                    }
                } else {
                    textDeviceBattery.text = "🔋 --%"
                    textDeviceBattery.setTextColor(Color.parseColor("#64748B"))
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        addManagedListener(batteryRef, batteryListener)

        // --- ⏳ SCREEN TIME MONITOR SYNC ---
        val textScreenTimeUsed = findViewById<TextView>(R.id.textScreenTimeUsed)
        val textScreenTimeLimit = findViewById<TextView>(R.id.textScreenTimeLimit)
        val progressScreenTime = findViewById<com.google.android.material.progressindicator.LinearProgressIndicator>(R.id.progressScreenTime)
        val editScreenTimeLimit = findViewById<AppCompatEditText>(R.id.editScreenTimeLimit)
        val btnSaveScreenTimeLimit = findViewById<MaterialButton>(R.id.btnSaveScreenTimeLimit)

        var usedMins = 0
        var limitMins = 60

        fun updateScreenTimeUI() {
            textScreenTimeUsed.text = "Used: $usedMins mins"
            textScreenTimeLimit.text = "Limit: $limitMins mins"
            val pct = if (limitMins > 0) (usedMins * 100) / limitMins else 0
            progressScreenTime.progress = pct.coerceIn(0, 100)
            
            if (pct >= 100) {
                progressScreenTime.setIndicatorColor(Color.parseColor("#EF4444")) // Red
            } else if (pct >= 80) {
                progressScreenTime.setIndicatorColor(Color.parseColor("#F59E0B")) // Amber
            } else {
                progressScreenTime.setIndicatorColor(Color.parseColor("#3B82F6")) // Blue
            }
        }

        val screenUsedRef = database.getReference("devices/$childId/screentime_used")
        val screenUsedListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                usedMins = snapshot.getValue(Int::class.java) ?: 0
                updateScreenTimeUI()
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        addManagedListener(screenUsedRef, screenUsedListener)

        val screenLimitRef = database.getReference("devices/$childId/screentime_limit")
        val screenLimitListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                limitMins = snapshot.getValue(Int::class.java) ?: 60
                updateScreenTimeUI()
                if (!editScreenTimeLimit.isFocused) {
                    editScreenTimeLimit.setText(limitMins.toString())
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        addManagedListener(screenLimitRef, screenLimitListener)

        btnSaveScreenTimeLimit.setOnClickListener {
            val limitText = editScreenTimeLimit.text.toString().trim()
            if (limitText.isEmpty()) {
                Toast.makeText(this@MainActivity, "Please enter a valid screen time limit.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val limitVal = limitText.toIntOrNull()
            if (limitVal == null || limitVal < 0) {
                Toast.makeText(this@MainActivity, "Please enter a valid positive number.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            database.getReference("devices/$targetsChildHardwareId/screentime_limit")
                .setValue(limitVal)
                .addOnSuccessListener {
                    Toast.makeText(this@MainActivity, "Daily Screen Time Limit set to $limitVal minutes.", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this@MainActivity, "Failed to update Screen Time Limit in Firebase.", Toast.LENGTH_SHORT).show()
                }
        }

        // --- 📥 INSTALLED APPS TRACKER ---
        val installedAppsRef = database.getReference("devices/$childId/installed_apps")
        val installedAppsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                layoutInstalledAppsList.removeAllViews()
                
                if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                    val emptyText = TextView(this@MainActivity).apply {
                        text = "No installed apps reported yet."
                        textSize = 13f
                        setTextColor(Color.parseColor("#94A3B8"))
                        gravity = android.view.Gravity.CENTER
                        setPadding(0, 16, 0, 16)
                    }
                    layoutInstalledAppsList.addView(emptyText)
                    return
                }
                
                for (appSnapshot in snapshot.children) {
                    val appName = appSnapshot.child("appName").getValue(String::class.java) ?: "Unknown App"
                    val packageName = appSnapshot.child("packageName").getValue(String::class.java) ?: ""
                    
                    val itemView = LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(0, 6, 0, 6)
                        }
                        gravity = android.view.Gravity.CENTER_VERTICAL
                        setPadding(16, 10, 16, 10)
                        
                        val shape = android.graphics.drawable.GradientDrawable().apply {
                            setColor(Color.parseColor("#F8FAFC"))
                            setCornerRadius(8f)
                        }
                        background = shape
                    }
                    
                    val iconCard = com.google.android.material.card.MaterialCardView(this@MainActivity).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            (36 * resources.displayMetrics.density).toInt(),
                            (36 * resources.displayMetrics.density).toInt()
                        ).apply {
                            setMargins(0, 0, (12 * resources.displayMetrics.density).toInt(), 0)
                        }
                        cardElevation = 0f
                        radius = 18 * resources.displayMetrics.density
                        setCardBackgroundColor(Color.parseColor("#E0F2FE"))
                        
                        val img = ImageView(this@MainActivity).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.MATCH_PARENT
                            ).apply {
                                setPadding(8, 8, 8, 8)
                            }
                            setImageResource(android.R.drawable.sym_def_app_icon)
                            setColorFilter(Color.parseColor("#0284C7"))
                        }
                        addView(img)
                    }
                    
                    val textContainer = LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    }
                    
                    val titleText = TextView(this@MainActivity).apply {
                        text = appName
                        textSize = 13f
                        setTextColor(Color.parseColor("#1E293B"))
                        setTypeface(null, android.graphics.Typeface.BOLD)
                    }
                    
                    val pkgText = TextView(this@MainActivity).apply {
                        text = packageName
                        textSize = 10f
                        setTextColor(Color.parseColor("#64748B"))
                    }
                    
                    textContainer.addView(titleText)
                    textContainer.addView(pkgText)
                    
                    itemView.addView(iconCard)
                    itemView.addView(textContainer)
                    layoutInstalledAppsList.addView(itemView)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        addManagedListener(installedAppsRef, installedAppsListener)

        // --- 🛰️ FOREGROUND APP MONITOR SYNC ---
        val foregroundAppRef = database.getReference("devices/$childId/telemetry/foregroundApp")
        val foregroundAppListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentApp = snapshot.getValue(String::class.java)
                if (currentApp != null) {
                    textActiveApp.text = currentApp.substringAfterLast(".")
                } else {
                    textActiveApp.text = "No active screen monitored."
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        addManagedListener(foregroundAppRef, foregroundAppListener)

        // --- ⏳ APP SCREEN STATS VALUE SYNC ---
        val usageStatsRef = database.getReference("devices/$childId/usage_stats")
        val usageStatsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                layoutAppScreenStats.removeAllViews()

                if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                    val emptyTextView = TextView(this@MainActivity).apply {
                        text = "No active usage stats synchronized from child companion."
                        textSize = 12f
                        setTextColor(Color.parseColor("#94A3B8"))
                        gravity = android.view.Gravity.CENTER
                        setPadding(0, 16, 0, 16)
                    }
                    layoutAppScreenStats.addView(emptyTextView)
                    return
                }

                val statsList = mutableListOf<Triple<String, String, Long>>()
                for (appSnapshot in snapshot.children) {
                    val pkg = appSnapshot.child("packageName").getValue(String::class.java) ?: appSnapshot.key ?: ""
                    val timeMins = appSnapshot.child("timeUsedMins").getValue(Long::class.java) ?: 0L
                    val friendlyName = pkg.substringAfterLast(".").replaceFirstChar { it.uppercase() }
                    statsList.add(Triple(pkg, friendlyName, timeMins))
                }

                val sortedList = statsList.sortedByDescending { it.third }

                for (stats in sortedList) {
                    val pkgName = stats.first
                    val friendlyName = stats.second
                    val mins = stats.third

                    val cardView = com.google.android.material.card.MaterialCardView(this@MainActivity).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(0, 0, 0, 12)
                        }
                        radius = 10f
                        cardElevation = 0f
                        setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#E2E8F0")))
                        strokeWidth = 2
                        setCardBackgroundColor(Color.WHITE)
                    }

                    val itemLayout = LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(16, 12, 16, 12)
                    }

                    val titleText = TextView(this@MainActivity).apply {
                        text = friendlyName
                        textSize = 13f
                        setTextColor(Color.parseColor("#1E293B"))
                        setTypeface(null, android.graphics.Typeface.BOLD)
                    }

                    val subText = TextView(this@MainActivity).apply {
                        text = "$pkgName | Duration: $mins min"
                        textSize = 11f
                        setTextColor(Color.parseColor("#64748B"))
                        setPadding(0, 2, 0, 0)
                    }

                    itemLayout.addView(titleText)
                    itemLayout.addView(subText)
                    cardView.addView(itemLayout)
                    layoutAppScreenStats.addView(cardView)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        addManagedListener(usageStatsRef, usageStatsListener)

        // --- 🛰️ EDUCATIONAL GAME ANALYTICS SYNC ---
        val educationStatsRef = database.getReference("devices/$childId/education_stats")
        val educationListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val correct = snapshot.child("correctAnswers").getValue(Int::class.java) ?: 0
                val total = snapshot.child("totalQuestions").getValue(Int::class.java) ?: 0
                
                // Fetch score progress UI references
                val labelMathAccuracy = findViewById<TextView>(R.id.labelMathAccuracy)
                val progressMath = findViewById<LinearProgressIndicator>(R.id.progressMath)
                val labelLogicAccuracy = findViewById<TextView>(R.id.labelLogicAccuracy)
                val progressLogic = findViewById<LinearProgressIndicator>(R.id.progressLogic)

                if (total > 0) {
                    val accuracyPercentage = (correct * 100) / total
                    labelMathAccuracy.text = "Mathematics Score Accuracy: $accuracyPercentage%"
                    progressMath.progress = accuracyPercentage

                    val logicScore = if (accuracyPercentage > 50) accuracyPercentage - 10 else accuracyPercentage + 20
                    val boundedLogic = logicScore.coerceIn(0, 100)
                    labelLogicAccuracy.text = "Logic & Reasoning: $boundedLogic%"
                    progressLogic.progress = boundedLogic
                } else {
                    labelMathAccuracy.text = "Mathematics Score Accuracy: 0%"
                    progressMath.progress = 0
                    labelLogicAccuracy.text = "Logic & Reasoning: 0%"
                    progressLogic.progress = 0
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        addManagedListener(educationStatsRef, educationListener)

        // --- 🛰️ WHATSAPP-STYLE ALERTS SYNC FEED ---
        val alertsRef = database.getReference("devices/$childId/alerts")
        val alertsListener = object : ValueEventListener {
            private var initialLoadDone = false

            override fun onDataChange(snapshot: DataSnapshot) {
                layoutAlertsFeed.removeAllViews()
                layoutInstallationEvents.removeAllViews()

                if (!snapshot.exists()) {
                    val emptyText = TextView(this@MainActivity).apply {
                        text = "No safety or warning logs recorded yet."
                        textSize = 14f
                        setTextColor(Color.parseColor("#94A3B8"))
                        gravity = android.view.Gravity.CENTER
                        setPadding(0, 32, 0, 0)
                    }
                    layoutAlertsFeed.addView(emptyText)

                    val emptyInstallText = TextView(this@MainActivity).apply {
                        text = "No installation events recorded in the last 24 hours."
                        textSize = 12f
                        setTextColor(Color.parseColor("#94A3B8"))
                        gravity = android.view.Gravity.CENTER
                        setPadding(0, 16, 0, 16)
                    }
                    layoutInstallationEvents.addView(emptyInstallText)
                    
                    initialLoadDone = true
                    return
                }

                val inflater = LayoutInflater.from(this@MainActivity)

                for (alertSnapshot in snapshot.children) {
                    val message = alertSnapshot.child("message").getValue(String::class.java) ?: ""
                    val category = alertSnapshot.child("category").getValue(String::class.java) ?: "warning"
                    val timestamp = alertSnapshot.child("timestamp").getValue(Long::class.java) ?: 0

                    // 1. Render Normal Safety Log in Alerts Feed
                    val alertItemView = inflater.inflate(R.layout.item_alert_notification, layoutAlertsFeed, false)
                    val cardCategoryIconBg = alertItemView.findViewById<MaterialCardView>(R.id.cardCategoryIconBg)
                    val imgAlertCategory = alertItemView.findViewById<ImageView>(R.id.imgAlertCategory)
                    val textAlertTimestamp = alertItemView.findViewById<TextView>(R.id.textAlertTimestamp)
                    val textAlertMessage = alertItemView.findViewById<TextView>(R.id.textAlertMessage)
                    val alertStatusDot = alertItemView.findViewById<View>(R.id.alertStatusDot)

                    textAlertMessage.text = message

                    if (timestamp > 0) {
                        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                        textAlertTimestamp.text = sdf.format(Date(timestamp))
                    }

                    when (category) {
                        "danger" -> {
                            cardCategoryIconBg.setCardBackgroundColor(Color.parseColor("#FEE2E2"))
                            imgAlertCategory.setImageResource(android.R.drawable.stat_sys_warning)
                            imgAlertCategory.setColorFilter(Color.parseColor("#EF4444"))
                            alertStatusDot.setBackgroundColor(Color.parseColor("#EF4444"))
                        }
                        "warning" -> {
                            cardCategoryIconBg.setCardBackgroundColor(Color.parseColor("#FEF3C7"))
                            imgAlertCategory.setImageResource(android.R.drawable.stat_notify_chat)
                            imgAlertCategory.setColorFilter(Color.parseColor("#F59E0B"))
                            alertStatusDot.setBackgroundColor(Color.parseColor("#F59E0B"))
                        }
                        else -> {
                            cardCategoryIconBg.setCardBackgroundColor(Color.parseColor("#D1FAE5"))
                            imgAlertCategory.setImageResource(android.R.drawable.ic_dialog_info)
                            imgAlertCategory.setColorFilter(Color.parseColor("#10B981"))
                            alertStatusDot.setBackgroundColor(Color.parseColor("#10B981"))
                        }
                    }

                    layoutAlertsFeed.addView(alertItemView, 0)

                    // 2. Render App Install / Uninstall Specific Event Row (last 24 hours)
                    if (message.contains("Installed", ignoreCase = true) || message.contains("Uninstalled", ignoreCase = true)) {
                        val isUninstall = message.contains("uninstalled", ignoreCase = true)
                        val eventItem = LinearLayout(this@MainActivity).apply {
                            orientation = LinearLayout.HORIZONTAL
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                setMargins(0, 6, 0, 6)
                            }
                            gravity = android.view.Gravity.CENTER_VERTICAL
                            setPadding(16, 10, 16, 10)

                            val shape = android.graphics.drawable.GradientDrawable().apply {
                                setColor(Color.parseColor(if (isUninstall) "#FEF2F2" else "#ECFDF5"))
                                setCornerRadius(8f)
                            }
                            background = shape
                        }

                        val iconCard = com.google.android.material.card.MaterialCardView(this@MainActivity).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                (32 * resources.displayMetrics.density).toInt(),
                                (32 * resources.displayMetrics.density).toInt()
                            ).apply {
                                setMargins(0, 0, (12 * resources.displayMetrics.density).toInt(), 0)
                            }
                            cardElevation = 0f
                            radius = 16 * resources.displayMetrics.density
                            setCardBackgroundColor(Color.parseColor(if (isUninstall) "#FEE2E2" else "#D1FAE5"))

                            val img = ImageView(this@MainActivity).apply {
                                layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.MATCH_PARENT
                                ).apply {
                                    setPadding(6, 6, 6, 6)
                                }
                                setImageResource(if (isUninstall) android.R.drawable.ic_menu_delete else android.R.drawable.sym_def_app_icon)
                                setColorFilter(Color.parseColor(if (isUninstall) "#EF4444" else "#10B981"))
                            }
                            addView(img)
                        }

                        val textContainer = LinearLayout(this@MainActivity).apply {
                            orientation = LinearLayout.VERTICAL
                            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        }

                        val titleText = TextView(this@MainActivity).apply {
                            text = message
                            textSize = 12f
                            setTextColor(Color.parseColor("#1E293B"))
                            setTypeface(null, android.graphics.Typeface.BOLD)
                        }

                        val timeText = TextView(this@MainActivity).apply {
                            if (timestamp > 0) {
                                val sdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
                                text = sdf.format(Date(timestamp))
                            } else {
                                text = "Just Now"
                            }
                            textSize = 10f
                            setTextColor(Color.parseColor("#64748B"))
                            setPadding(0, 2, 0, 0)
                        }

                        textContainer.addView(titleText)
                        textContainer.addView(timeText)

                        eventItem.addView(iconCard)
                        eventItem.addView(textContainer)
                        layoutInstallationEvents.addView(eventItem, 0)
                    }
                }

                // If no app installation events recorded, add placeholder
                if (layoutInstallationEvents.childCount == 0) {
                    val emptyInstallText = TextView(this@MainActivity).apply {
                        text = "No installation events recorded in the last 24 hours."
                        textSize = 12f
                        setTextColor(Color.parseColor("#94A3B8"))
                        gravity = android.view.Gravity.CENTER
                        setPadding(0, 16, 0, 16)
                    }
                    layoutInstallationEvents.addView(emptyInstallText)
                }

                // Trigger real-time push notification if this is a new live warning alert
                if (!initialLoadDone) {
                    initialLoadDone = true
                    return
                }

                val lastAlertSnapshot = snapshot.children.lastOrNull()
                if (lastAlertSnapshot != null) {
                    val message = lastAlertSnapshot.child("message").getValue(String::class.java) ?: ""
                    val category = lastAlertSnapshot.child("category").getValue(String::class.java) ?: "warning"
                    triggerLocalNotification(category.uppercase(Locale.getDefault()) + " SAFETY ALERT", message)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        addManagedListener(alertsRef, alertsListener)

        // --- 🛰️ REMOTE SWITCH SYNC ---
        val lockStatusRef = database.getReference("devices/$childId/status/isLockedByParent")
        val lockStatusListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isCurrentlyLocked = snapshot.getValue(Boolean::class.java) ?: false

                isUpdatingFromFirebase = true
                switchLockdown.isChecked = isCurrentlyLocked
                isUpdatingFromFirebase = false
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Dashboard synchronization dropped.", Toast.LENGTH_SHORT).show()
            }
        }
        addManagedListener(lockStatusRef, lockStatusListener)

        // --- 🛰️ ADMINISTRATIVE BYPASS PIN SYNC ---
        val bypassPinRef = database.getReference("devices/$childId/status/bypassPin")
        val bypassPinListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentPin = snapshot.getValue(String::class.java) ?: "1234"
                if (!editBypassPin.isFocused) {
                    editBypassPin.setText(currentPin)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        addManagedListener(bypassPinRef, bypassPinListener)
    }

    // --- 🛡️ DYNAMIC LIST & MAP STATE GEOFENCE CALCULATORS ---
    private fun updateSafeZonesListUI() {
        layoutSafeZonesList.removeAllViews()
        
        if (safeZonesList.isEmpty()) {
            val emptyText = TextView(this).apply {
                text = "No active safe zones configured."
                textSize = 13f
                setTextColor(Color.parseColor("#64748B"))
                gravity = android.view.Gravity.CENTER
                setPadding(0, 24, 0, 24)
            }
            layoutSafeZonesList.addView(emptyText)
            return
        }

        for (zone in safeZonesList) {
            val itemView = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 6, 0, 6)
                }
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(16, 12, 16, 12)
                setBackgroundColor(Color.parseColor("#F8FAFC"))
                
                // Add soft visual border using material design styling
                val shape = android.graphics.drawable.GradientDrawable().apply {
                    setColor(Color.parseColor("#F1F5F9"))
                    setCornerRadius(10f)
                }
                background = shape
            }

            val textContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val titleText = TextView(this).apply {
                text = zone.name
                textSize = 14f
                setTextColor(Color.parseColor("#0F172A"))
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            val detailsText = TextView(this).apply {
                text = String.format(Locale.US, "Radius: %.0fm | Coordinates: %.4f, %.4f", zone.radius, zone.latitude, zone.longitude)
                textSize = 11f
                setTextColor(Color.parseColor("#64748B"))
                setPadding(0, 2, 0, 0)
            }

            textContainer.addView(titleText)
            textContainer.addView(detailsText)

            val deleteButton = ImageView(this).apply {
                setImageResource(android.R.drawable.ic_menu_delete)
                setColorFilter(Color.parseColor("#EF4444"))
                setPadding(8, 8, 8, 8)
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    database.getReference("devices/$targetsChildHardwareId/safe_zones/${zone.id}")
                        .removeValue()
                        .addOnSuccessListener {
                            Toast.makeText(this@MainActivity, "Safe zone '${zone.name}' deleted.", Toast.LENGTH_SHORT).show()
                        }
                }
            }

            itemView.addView(textContainer)
            itemView.addView(deleteButton)
            layoutSafeZonesList.addView(itemView)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupMapWebView() {
        mapWebView.settings.javaScriptEnabled = true
        mapWebView.settings.domStorageEnabled = true
        mapWebView.webViewClient = object : android.webkit.WebViewClient() {
            override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                super.onPageFinished(view, url)
                isMapLoaded = true
                updateMapState()
            }
        }

        val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="initial-scale=1.0, user-scalable=no" />
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                <style>
                    html, body, #map {
                        height: 100%;
                        margin: 0;
                        padding: 0;
                        background: #F8FAFC;
                    }
                </style>
            </head>
            <body>
                <div id="map"></div>
                <script>
                    var map = L.map('map').setView([13.0489, 80.0970], 15);
                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                        maxZoom: 19,
                        attribution: '© OpenStreetMap contributors'
                    }).addTo(map);

                    var childMarker = null;
                    var safeZoneCircles = [];

                    function updateChildLocation(lat, lng, name) {
                        var latLng = [lat, lng];
                        if (childMarker) {
                            childMarker.setLatLng(latLng);
                            childMarker.getPopup().setContent(name);
                        } else {
                            childMarker = L.marker(latLng).addTo(map)
                                .bindPopup(name).openPopup();
                        }
                        map.setView(latLng, map.getZoom());
                    }

                    function clearSafeZones() {
                        safeZoneCircles.forEach(function(c) { map.removeLayer(c); });
                        safeZoneCircles = [];
                    }

                    function addSafeZone(lat, lng, radius, name) {
                        var circle = L.circle([lat, lng], {
                            color: '#10B981',
                            fillColor: '#10B981',
                            fillOpacity: 0.15,
                            radius: radius
                        }).addTo(map).bindPopup(name);
                        safeZoneCircles.push(circle);
                    }
                </script>
            </body>
            </html>
        """.trimIndent()

        mapWebView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }

    private fun updateMapState() {
        if (!isMapLoaded) return

        val childLat = childLastLat
        val childLng = childLastLng

        // 1. Draw Child's Position Marker and Center Camera
        if (childLat != null && childLng != null) {
            val name = textProfileChildName.text.toString().ifEmpty { "Child Device" }
            val escapedName = name.replace("'", "\\'")
            mapWebView.evaluateJavascript("javascript:updateChildLocation($childLat, $childLng, '$escapedName')", null)
        }

        // 2. Draw Circles for all active Safe Zones
        val jsCode = StringBuilder("javascript:clearSafeZones();")
        for (zone in safeZonesList) {
            val escapedZoneName = zone.name.replace("'", "\\'")
            jsCode.append("addSafeZone(${zone.latitude}, ${zone.longitude}, ${zone.radius}, '$escapedZoneName');")
        }
        mapWebView.evaluateJavascript(jsCode.toString(), null)

        // 3. Evaluate Geofences
        recalculateGeofencingStatus()
    }

    private fun recalculateGeofencingStatus() {
        val childLat = childLastLat
        val childLng = childLastLng

        if (childLat == null || childLng == null) {
            textSafeZoneStatus.text = "UNCONFIGURED (Waiting for telemetry)"
            textSafeZoneStatus.setTextColor(Color.parseColor("#64748B"))
            return
        }

        if (safeZonesList.isEmpty()) {
            textSafeZoneStatus.text = "UNCONFIGURED (No safe zones created)"
            textSafeZoneStatus.setTextColor(Color.parseColor("#64748B"))
            return
        }

        var insideAnyZone = false
        var activeZoneName = ""

        for (zone in safeZonesList) {
            val results = FloatArray(1)
            android.location.Location.distanceBetween(
                childLat, childLng,
                zone.latitude, zone.longitude,
                results
            )
            val distance = results[0]
            if (distance <= zone.radius) {
                insideAnyZone = true
                activeZoneName = zone.name
                break
            }
        }

        if (insideAnyZone) {
            textSafeZoneStatus.text = "SAFE (Child inside '$activeZoneName')"
            textSafeZoneStatus.setTextColor(Color.parseColor("#10B981"))
        } else {
            textSafeZoneStatus.text = "⚠️ BREACHED (Child outside all safe zones)"
            textSafeZoneStatus.setTextColor(Color.parseColor("#EF4444"))
        }
    }

    // --- 🗺️ MAP WEBVIEW LIFECYCLE OVERRIDES ---
    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        clearActiveListeners()
        try {
            mapWebView.destroy()
        } catch (e: Exception) {
            // Safe cleanup
        }
    }

    @SuppressLint("MissingPermission")
    private fun triggerLocalNotification(title: String, body: String) {
        val channelId = "parent_alerts_channel"
        val channelName = "System Protection Alerts"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Channel for Parent alerts and safety warnings"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(this, 0, intent, pendingIntentFlags)

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_warning) // 100% safe system icon asset
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            val notificationId = kotlin.random.Random.nextInt(1, 100000)
            NotificationManagerCompat.from(this).notify(notificationId, builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}