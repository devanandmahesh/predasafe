package com.example.predasafe.data.repository

import android.util.Log
import com.example.predasafe.data.model.AlertData
import com.example.predasafe.data.model.CameraData
import com.example.predasafe.data.model.DetectionData
import com.example.predasafe.data.model.NotificationData
import com.example.predasafe.data.model.StatusData
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository that provides real-time data streams from Firebase Realtime Database.
 *
 * Rules:
 * - Always pass the explicit database URL; google-services.json "firebase_url" is NOT
 *   auto-read by the Realtime Database SDK.
 * - All fields are mapped manually from snake_case Firebase keys to camelCase Kotlin fields.
 * - Each observe*() function is guarded so calling it multiple times is safe.
 */
class FirebaseRepository {

    private val database = FirebaseDatabase.getInstance(
        "https://predasafe-default-rtdb.firebaseio.com"
    )

    // ── Status (/status) ──────────────────────────────────────────────────────

    private val _statusFlow = MutableStateFlow<StatusData?>(null)
    val statusFlow: StateFlow<StatusData?> = _statusFlow.asStateFlow()
    private var statusListenerAttached = false

    fun observeStatus() {
        if (statusListenerAttached) return
        statusListenerAttached = true
        Log.d("FirebaseDebug", "observeStatus: attaching listener")

        database.getReference("status").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("FirebaseDebug", "STATUS RAW: ${snapshot.value}")
                val motion     = snapshot.child("motion").getValue(Boolean::class.java)   ?: false
                val distance   = snapshot.child("distance").getValue(Double::class.java)  ?: 0.0
                val lastUpdate = snapshot.child("last_update").getValue(Long::class.java) ?: 0L
                _statusFlow.value = StatusData(motion, distance, lastUpdate)
                Log.d("FirebaseDebug", "STATUS PARSED: motion=$motion distance=$distance")
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseDebug", "STATUS ERROR: ${error.message}", error.toException())
            }
        })
    }

    // ── Detection (/detection) ────────────────────────────────────────────────

    private val _detectionFlow = MutableStateFlow<DetectionData?>(null)
    val detectionFlow: StateFlow<DetectionData?> = _detectionFlow.asStateFlow()
    private var detectionListenerAttached = false

    fun observeDetection() {
        if (detectionListenerAttached) return
        detectionListenerAttached = true
        Log.d("FirebaseDebug", "observeDetection: attaching listener")

        database.getReference("detection").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("FirebaseDebug", "DETECTION RAW: ${snapshot.value}")
                if (!snapshot.exists()) {
                    _detectionFlow.value = null
                    return
                }
                val animal     = snapshot.child("animal").getValue(String::class.java)     ?: ""
                val confidence = snapshot.child("confidence").getValue(Double::class.java) ?: 0.0
                val timestamp  = snapshot.child("timestamp").getValue(Long::class.java)    ?: 0L
                _detectionFlow.value = DetectionData(animal, confidence, timestamp)
                Log.d("FirebaseDebug", "DETECTION PARSED: animal=$animal confidence=$confidence ts=$timestamp")
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseDebug", "DETECTION ERROR: ${error.message}", error.toException())
            }
        })
    }

    // ── Alerts (/alerts) ──────────────────────────────────────────────────────

    private val _alertsFlow = MutableStateFlow<List<AlertData>>(emptyList())
    val alertsFlow: StateFlow<List<AlertData>> = _alertsFlow.asStateFlow()
    private var alertsListenerAttached = false

    fun observeAlerts() {
        if (alertsListenerAttached) return
        alertsListenerAttached = true
        Log.d("FirebaseDebug", "observeAlerts: attaching listener")

        database.getReference("alerts").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("FirebaseDebug", "ALERTS RAW: ${snapshot.value}")
                val alerts = mutableListOf<AlertData>()
                for (child in snapshot.children) {
                    val id      = child.key                                                  ?: ""
                    val message = child.child("message").getValue(String::class.java)        ?: ""
                    val time    = child.child("time").getValue(Long::class.java)             ?: 0L
                    val type    = child.child("type").getValue(String::class.java)           ?: ""
                    alerts.add(AlertData(id, message, time, type))
                }
                alerts.sortByDescending { it.time }
                _alertsFlow.value = alerts
                Log.d("FirebaseDebug", "ALERTS PARSED: count=${alerts.size}")
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseDebug", "ALERTS ERROR: ${error.message}", error.toException())
            }
        })
    }

    // ── Camera (/camera) ──────────────────────────────────────────────────────

    private val _cameraFlow = MutableStateFlow<CameraData?>(null)
    val cameraFlow: StateFlow<CameraData?> = _cameraFlow.asStateFlow()
    private var cameraListenerAttached = false

    fun observeCamera() {
        if (cameraListenerAttached) return
        cameraListenerAttached = true
        Log.d("FirebaseDebug", "observeCamera: attaching listener")

        database.getReference("camera").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("FirebaseDebug", "CAMERA RAW: ${snapshot.value}")
                if (!snapshot.exists()) {
                    _cameraFlow.value = null
                    return
                }
                val imageUrl    = snapshot.child("image_url").getValue(String::class.java)    ?: ""
                val lastUpdated = snapshot.child("last_updated").getValue(Long::class.java)   ?: 0L
                _cameraFlow.value = CameraData(imageUrl, lastUpdated)
                Log.d("FirebaseDebug", "CAMERA PARSED: imageUrl=$imageUrl lastUpdated=$lastUpdated")
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseDebug", "CAMERA ERROR: ${error.message}", error.toException())
            }
        })
    }

    // ── Notification (/notification) ──────────────────────────────────────────

    private val _notificationFlow = MutableStateFlow<NotificationData?>(null)
    val notificationFlow: StateFlow<NotificationData?> = _notificationFlow.asStateFlow()
    private var notificationListenerAttached = false

    fun observeNotification() {
        if (notificationListenerAttached) return
        notificationListenerAttached = true
        Log.d("FirebaseDebug", "observeNotification: attaching listener")

        database.getReference("notification").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("FirebaseDebug", "NOTIFICATION RAW: ${snapshot.value}")
                if (!snapshot.exists()) return

                val title   = snapshot.child("title").getValue(String::class.java)   ?: ""
                val body    = snapshot.child("body").getValue(String::class.java)    ?: ""
                val time    = snapshot.child("time").getValue(Long::class.java)      ?: 0L
                val trigger = snapshot.child("trigger").getValue(Boolean::class.java) ?: false

                _notificationFlow.value = NotificationData(title, body, time, trigger)
                Log.d("FirebaseDebug", "NOTIFICATION PARSED: trigger=$trigger title=$title")
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseDebug", "NOTIFICATION ERROR: ${error.message}", error.toException())
            }
        })
    }

    /**
     * Resets the trigger flag to false in Firebase after the notification has been shown,
     * so the app does not fire the notification again on next app launch.
     */
    fun resetNotificationTrigger() {
        database.getReference("notification/trigger").setValue(false)
            .addOnFailureListener { e ->
                Log.e("FirebaseDebug", "Failed to reset notification trigger", e)
            }
    }
}
