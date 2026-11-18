package com.example.predasafe.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.predasafe.data.model.AlertData
import com.example.predasafe.data.model.CameraData
import com.example.predasafe.data.model.DetectionData
import com.example.predasafe.data.model.NotificationData
import com.example.predasafe.data.model.StatusData
import com.example.predasafe.data.repository.FirebaseRepository
import com.example.predasafe.util.NotificationHelper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * AndroidViewModel so we have access to Application context for posting notifications.
 * All StateFlows use SharingStarted.Eagerly so Firebase listeners NEVER stop
 * when the UI temporarily leaves the screen.
 */
class PredaSafeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FirebaseRepository()
    private val appContext = application.applicationContext

    init {
        // Ensure notification channel exists before we might need it
        NotificationHelper.createChannel(appContext)

        // Start all Firebase listeners
        repository.observeStatus()
        repository.observeDetection()
        repository.observeAlerts()
        repository.observeCamera()
        repository.observeNotification()

        // Watch /notification trigger and fire a local notification when it's true
        viewModelScope.launch {
            repository.notificationFlow.collect { notif ->
                if (notif != null && notif.trigger) {
                    Log.d("FirebaseDebug", "NOTIFICATION TRIGGER: ${notif.title} / ${notif.body}")
                    NotificationHelper.showNotification(appContext, notif.title, notif.body)
                    // Reset trigger in Firebase so it doesn't fire again on next launch
                    repository.resetNotificationTrigger()
                }
            }
        }
    }

    // ── Exposed StateFlows ────────────────────────────────────────────────────

    val statusFlow: StateFlow<StatusData?> = repository.statusFlow
        .onEach { Log.d("FirebaseDebug", "UI status: $it") }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val detectionState: StateFlow<DetectionData?> = repository.detectionFlow
        .onEach { Log.d("FirebaseDebug", "UI detection: $it") }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val alertsState: StateFlow<List<AlertData>> = repository.alertsFlow
        .onEach { Log.d("FirebaseDebug", "UI alerts count=${it.size}") }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val cameraState: StateFlow<CameraData?> = repository.cameraFlow
        .onEach { Log.d("FirebaseDebug", "UI camera: $it") }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val notificationState: StateFlow<NotificationData?> = repository.notificationFlow
        .onEach { Log.d("FirebaseDebug", "UI notification: $it") }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /**
     * True when motion is active OR an animal was detected in the last 60 seconds.
     * Used to show the REC badge on the camera feed.
     *
     * NOTE: Python backend sends timestamps in UNIX seconds.
     *       System.currentTimeMillis() is in milliseconds → multiply by 1000.
     */
    val shouldShowCamera: StateFlow<Boolean> = combine(
        statusFlow,
        detectionState
    ) { status, detection ->
        val hasMotion = status?.motion == true
        val now = System.currentTimeMillis()
        val detectionMs = (detection?.timestamp ?: 0L) * 1000L
        val recentDetection = detection != null && (now - detectionMs) < 60_000L
        hasMotion || recentDetection
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
}
