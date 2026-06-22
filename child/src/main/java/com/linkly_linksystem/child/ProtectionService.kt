package com.linkly_linksystem.child

import android.app.*
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Process
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ProtectionService : Service() {

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

    private lateinit var connectivityManager: ConnectivityManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private val handler = Handler(Looper.getMainLooper())
    private var offlineRunnable: Runnable? = null

    private var baseLat = 13.0489
    private var baseLng = 80.0970

    private var isListeningToLocation = false
    private var locationManager: android.location.LocationManager? = null

    private val locationListener = object : android.location.LocationListener {
        override fun onLocationChanged(location: android.location.Location) {
            baseLat = location.latitude
            baseLng = location.longitude
        }
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }


    // Geofencing limits
    private var safeZoneLat: Double? = null
    private var safeZoneLng: Double? = null
    private var safeZoneRadius: Double? = null
    private var wasInsideSafeZone: Boolean? = null

    // Screen time properties
    private var screenTimeSeconds = 0
    private var screenTimeLimitMins = 60
    private var lastSyncedUsedMins = -1
    private var isLimitAlertTriggered = false
    private var lastSyncedBatteryLevel = -1

    private val NOTIFICATION_ID = 8899
    private val CHANNEL_ID = "protection_service_channel"

    private val telemetryRunnable = object : Runnable {
        override fun run() {
            updateActualLocation()
            updateActualForegroundApp()
            updateAppScreenStats()

            // Increment screen time seconds if device active
            val pm = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            val isInteractive = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                pm.isInteractive
            } else {
                @Suppress("DEPRECATION")
                pm.isScreenOn
            }

            if (isInteractive) {
                screenTimeSeconds += 8
                val currentUsedMins = screenTimeSeconds / 60
                if (currentUsedMins != lastSyncedUsedMins) {
                    lastSyncedUsedMins = currentUsedMins
                    database.getReference("devices/$deviceUniqueId/screentime_used").setValue(currentUsedMins)
                }
                checkScreentimeLimit()
            }

            // Query device battery level percentage
            val batteryManager = getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
            val currentBattery = batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
            
            // Sync to Firebase if level changed
            if (currentBattery != lastSyncedBatteryLevel) {
                lastSyncedBatteryLevel = currentBattery
                database.getReference("devices/$deviceUniqueId/telemetry/batteryLevel").setValue(currentBattery)
            }

            handler.postDelayed(this, 8000) // Sync every 8 seconds in the background
        }
    }

    private fun checkScreentimeLimit() {
        val currentUsedMins = screenTimeSeconds / 60
        if (currentUsedMins >= screenTimeLimitMins && screenTimeLimitMins > 0) {
            if (!isLimitAlertTriggered) {
                isLimitAlertTriggered = true

                // 1. Post danger alert to parent
                val alertId = database.getReference("devices/$deviceUniqueId/alerts").push().key ?: "screentime_breached"
                val alertPayload = hashMapOf(
                    "message" to "🚨 Screentime Limit Reached: Child has used 100% of their daily screen time limit ($screenTimeLimitMins mins)!",
                    "category" to "danger",
                    "timestamp" to System.currentTimeMillis()
                )
                database.getReference("devices/$deviceUniqueId/alerts/$alertId").setValue(alertPayload)

                // 2. Transmit lockdown status & force overlay activity
                database.getReference("devices/$deviceUniqueId/status/isLockedByParent").setValue(true)
                launchLockscreenOverlay()

                // 3. Inform child locally
                handler.post {
                    Toast.makeText(this@ProtectionService, "Daily screen time limit reached! Device locked by parent.", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            if (currentUsedMins < screenTimeLimitMins) {
                isLimitAlertTriggered = false
            }
        }
    }

    private val packageReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            val dataUri = intent.data
            val packageName = dataUri?.schemeSpecificPart ?: return
            
            val pm = context.packageManager
            val appName = try {
                val appInfo = pm.getApplicationInfo(packageName, 0)
                appInfo.loadLabel(pm).toString()
            } catch (e: Exception) {
                packageName
            }

            when (action) {
                Intent.ACTION_PACKAGE_ADDED -> {
                    val encodedPkg = packageName.replace(".", "_")
                    database.getReference("devices/$deviceUniqueId/installed_apps/$encodedPkg").setValue(
                        hashMapOf(
                            "packageName" to packageName,
                            "appName" to appName
                        )
                    )
                    
                    // Alert log inside Firebase safety log
                    val alertId = database.getReference("devices/$deviceUniqueId/alerts").push().key ?: "install_alert"
                    val alertPayload = hashMapOf(
                        "message" to "📥 New App Installed: $appName ($packageName)",
                        "category" to "warning",
                        "timestamp" to System.currentTimeMillis()
                    )
                    database.getReference("devices/$deviceUniqueId/alerts/$alertId").setValue(alertPayload)
                }
                Intent.ACTION_PACKAGE_REMOVED -> {
                    val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                    if (!isReplacing) {
                        val encodedPkg = packageName.replace(".", "_")
                        database.getReference("devices/$deviceUniqueId/installed_apps/$encodedPkg").removeValue()
                        
                        // Alert log inside Firebase safety log
                        val alertId = database.getReference("devices/$deviceUniqueId/alerts").push().key ?: "uninstall_alert"
                        val alertPayload = hashMapOf(
                            "message" to "🗑️ App Uninstalled: $packageName",
                            "category" to "danger",
                            "timestamp" to System.currentTimeMillis()
                        )
                        database.getReference("devices/$deviceUniqueId/alerts/$alertId").setValue(alertPayload)
                    }
                }
            }
        }
    }

    private fun getInstalledApps(context: Context): List<Map<String, String>> {
        val pm = context.packageManager
        val packages = pm.getInstalledPackages(PackageManager.GET_META_DATA)
        val list = mutableListOf<Map<String, String>>()
        for (pkgInfo in packages) {
            val isSystem = (pkgInfo.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            if (!isSystem || pm.getLaunchIntentForPackage(pkgInfo.packageName) != null) {
                val appLabel = pkgInfo.applicationInfo.loadLabel(pm).toString()
                list.add(mapOf(
                    "packageName" to pkgInfo.packageName,
                    "appName" to appLabel
                ))
            }
        }
        return list
    }

    private fun syncInstalledAppsList() {
        val context = this
        val appsList = getInstalledApps(context)
        val appsMap = hashMapOf<String, Any>()
        for (app in appsList) {
            val pkg = app["packageName"] ?: continue
            val label = app["appName"] ?: "Unknown App"
            val encodedPkg = pkg.replace(".", "_")
            appsMap[encodedPkg] = hashMapOf(
                "packageName" to pkg,
                "appName" to label
            )
        }
        database.getReference("devices/$deviceUniqueId/installed_apps").setValue(appsMap)
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                createNotification(),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }

        // --- 📶 CONNECTIVITY MONITORING (Offline Auto-Lock Rule) ---
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                handler.post {
                    cancelOfflineLockTimer()
                    syncConnectionRecovered()
                }
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                handler.post {
                    startOfflineLockTimer()
                }
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback!!)

        // --- 🛡️ GEOFENCE CONFIG SYNC ---
        database.getReference("devices/$deviceUniqueId/safe_zone")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        safeZoneLat = snapshot.child("latitude").getValue(Double::class.java)
                        safeZoneLng = snapshot.child("longitude").getValue(Double::class.java)
                        safeZoneRadius = snapshot.child("radius").getValue(Double::class.java)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        // --- 🔒 LOCKDOWN SIGNAL SYNC ---
        database.getReference("devices/$deviceUniqueId/status/isLockedByParent")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val isLocked = snapshot.getValue(Boolean::class.java) ?: false
                    if (isLocked) {
                        launchLockscreenOverlay()
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        // Start real-time background sync loop
        startLocationUpdates()
        handler.post(telemetryRunnable)

        // Register package monitor BroadCastReceiver dynamically
        val filter = android.content.IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }
        registerReceiver(packageReceiver, filter)

        // Run startup package scan sync
        syncInstalledAppsList()

        // Fetch persisted screentime used on startup to avoid reset
        database.getReference("devices/$deviceUniqueId/screentime_used").get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val usedMins = snapshot.getValue(Int::class.java) ?: 0
                    screenTimeSeconds = usedMins * 60
                    lastSyncedUsedMins = usedMins
                    checkScreentimeLimit()
                }
            }

        // Listen to screentime limit updates
        database.getReference("devices/$deviceUniqueId/screentime_limit")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    screenTimeLimitMins = snapshot.getValue(Int::class.java) ?: 60
                    checkScreentimeLimit()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(telemetryRunnable)
        networkCallback?.let {
            connectivityManager.unregisterNetworkCallback(it)
        }
        try {
            locationManager?.removeUpdates(locationListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            unregisterReceiver(packageReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Linkly Protective Shield Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Linkly active monitoring in background"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Linkly Protection Active")
            .setContentText("Linkly is monitoring device diagnostics.")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun launchLockscreenOverlay() {
        try {
            val intent = Intent(this, LockOverlayActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
            startActivity(intent)
        } catch (e: Exception) {
            try {
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

                // Ensure channel exists
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val name = "Device Administration Alerts"
                    val importance = NotificationManager.IMPORTANCE_HIGH
                    val channel = NotificationChannel("lockdown_channel", name, importance).apply {
                        description = "Critical security and lockdown notifications"
                    }
                    val notificationManager = getSystemService(NotificationManager::class.java)
                    notificationManager.createNotificationChannel(channel)
                }

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
            } catch (ex: Exception) {
                // Secure catch for background start restrictions
            }
        }
    }

    // --- 🚨 OFFLINE LOCK COUNTDOWN SYSTEM ---
    private fun startOfflineLockTimer() {
        // Cache rules violation alert to sync
        val alertId = database.getReference("devices/$deviceUniqueId/alerts").push().key ?: "offline_alert"
        val alertPayload = hashMapOf(
            "message" to "⚠️ Rule Violation: Internet connection was turned OFF!",
            "category" to "danger",
            "timestamp" to System.currentTimeMillis()
        )
        database.getReference("devices/$deviceUniqueId/alerts/$alertId").setValue(alertPayload)

        offlineRunnable = Runnable {
            // Force lock screen launch
            database.getReference("devices/$deviceUniqueId/status/isLockedByParent").setValue(true)
            launchLockscreenOverlay()
        }

        // 15 seconds for testing countdown
        handler.postDelayed(offlineRunnable!!, 15000)
    }

    private fun cancelOfflineLockTimer() {
        offlineRunnable?.let {
            handler.removeCallbacks(it)
            offlineRunnable = null
        }
    }

    private fun syncConnectionRecovered() {
        val alertId = database.getReference("devices/$deviceUniqueId/alerts").push().key ?: "online_alert"
        val alertPayload = hashMapOf(
            "message" to "🔌 Internet connection recovered successfully.",
            "category" to "success",
            "timestamp" to System.currentTimeMillis()
        )
        database.getReference("devices/$deviceUniqueId/alerts/$alertId").setValue(alertPayload)
    }

    private fun startLocationUpdates() {
        if (isListeningToLocation) return

        if (locationManager == null) {
            locationManager = getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                // Get last known location for immediate baseline if possible
                val providers = locationManager?.getProviders(true)
                var bestLocation: android.location.Location? = null
                if (providers != null) {
                    for (provider in providers) {
                        val l = locationManager?.getLastKnownLocation(provider) ?: continue
                        if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                            bestLocation = l
                        }
                    }
                }
                bestLocation?.let {
                    baseLat = it.latitude
                    baseLng = it.longitude
                }

                var registered = false
                if (locationManager?.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) == true) {
                    locationManager?.requestLocationUpdates(
                        android.location.LocationManager.GPS_PROVIDER,
                        1000L,
                        0f,
                        locationListener
                    )
                    registered = true
                }
                if (locationManager?.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) == true) {
                    locationManager?.requestLocationUpdates(
                        android.location.LocationManager.NETWORK_PROVIDER,
                        1000L,
                        0f,
                        locationListener
                    )
                    registered = true
                }
                isListeningToLocation = registered
            } catch (e: SecurityException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- 📡 REAL-TIME TELEMETRY SENSORS ---
    private fun updateActualLocation() {
        // Attempt to start location updates if permissions were granted later
        startLocationUpdates()

        val locationPayload = hashMapOf(
            "latitude" to baseLat,
            "longitude" to baseLng,
            "timestamp" to System.currentTimeMillis()
        )
        database.getReference("devices/$deviceUniqueId/location").setValue(locationPayload)

        // Geofencing trigger evaluation
        val szLat = safeZoneLat
        val szLng = safeZoneLng
        val szRadius = safeZoneRadius

        if (szLat != null && szLng != null && szRadius != null) {
            val results = FloatArray(1)
            android.location.Location.distanceBetween(baseLat, baseLng, szLat, szLng, results)
            val distance = results[0]
            val isCurrentlyInside = distance <= szRadius

            val prevInside = wasInsideSafeZone
            if (prevInside == null) {
                wasInsideSafeZone = isCurrentlyInside
            } else if (prevInside != isCurrentlyInside) {
                wasInsideSafeZone = isCurrentlyInside
                val alertId = database.getReference("devices/$deviceUniqueId/alerts").push().key ?: "geofence_state"

                if (isCurrentlyInside) {
                    val alertPayload = hashMapOf(
                        "message" to "🛡️ Safe Zone Entered: Child is back in the safe zone.",
                        "category" to "success",
                        "timestamp" to System.currentTimeMillis()
                    )
                    database.getReference("devices/$deviceUniqueId/alerts/$alertId").setValue(alertPayload)
                } else {
                    val alertPayload = hashMapOf(
                        "message" to "🚨 Safe Zone Breach: Child left the designated safe zone!",
                        "category" to "danger",
                        "timestamp" to System.currentTimeMillis()
                    )
                    database.getReference("devices/$deviceUniqueId/alerts/$alertId").setValue(alertPayload)
                }
            }
        }
    }

    private fun updateActualForegroundApp() {
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

        var activeApp = "com.linkly_linksystem.child"

        if (usageGranted) {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val time = System.currentTimeMillis()
            val appList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 30, time)
            if (!appList.isNullOrEmpty()) {
                val sortedApp = appList.sortedByDescending { it.lastTimeUsed }
                // Avoid capturing the launcher or system background components constantly
                val primaryApp = sortedApp.firstOrNull { 
                    it.packageName != "com.google.android.apps.nexuslauncher" && 
                    it.packageName != "com.android.systemui" 
                }
                if (primaryApp != null) {
                    activeApp = primaryApp.packageName
                }
            }
        } else {
            // Simulated rotators when permission not granted
            val appList = listOf("com.android.chrome", "com.google.android.youtube", "com.zhiliaoapp.musically", "com.roblox.client")
            activeApp = appList.random()
        }

        database.getReference("devices/$deviceUniqueId/telemetry/foregroundApp").setValue(activeApp)
    }

    private fun updateAppScreenStats() {
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
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val endTime = System.currentTimeMillis()
            val startTime = endTime - java.util.concurrent.TimeUnit.DAYS.toMillis(1) // Last 24 Hours

            val usageStatsList = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )

            if (!usageStatsList.isNullOrEmpty()) {
                val usageStatsMap = hashMapOf<String, Any>()
                val sortedList = usageStatsList.filter { it.totalTimeInForeground > 0 }
                    .sortedByDescending { it.totalTimeInForeground }
                    .take(15)

                for (stats in sortedList) {
                    val pkg = stats.packageName
                    val encodedPkg = pkg.replace(".", "_")
                    val totalMinutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(stats.totalTimeInForeground)
                    if (totalMinutes > 0) {
                        usageStatsMap[encodedPkg] = hashMapOf(
                            "packageName" to pkg,
                            "timeUsedMins" to totalMinutes
                        )
                    }
                }
                if (usageStatsMap.isNotEmpty()) {
                    database.getReference("devices/$deviceUniqueId/usage_stats").setValue(usageStatsMap)
                }
            }
        } else {
            // Fallback simulated usage stats for debugging/testing
            val fallbackMap = hashMapOf<String, Any>(
                "com_android_chrome" to hashMapOf("packageName" to "com.android.chrome", "timeUsedMins" to 25L),
                "com_google_android_youtube" to hashMapOf("packageName" to "com.google.android.youtube", "timeUsedMins" to 45L),
                "com_zhiliaoapp_musically" to hashMapOf("packageName" to "com.zhiliaoapp.musically", "timeUsedMins" to 30L),
                "com_roblox_client" to hashMapOf("packageName" to "com.roblox.client", "timeUsedMins" to 60L)
            )
            database.getReference("devices/$deviceUniqueId/usage_stats").setValue(fallbackMap)
        }
    }
}
