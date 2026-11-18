package com.example.predasafe.data.model

/**
 * Represents the motion/distance sensor status from /status/ node.
 */
data class StatusData(
    var motion: Boolean = false,
    var distance: Double = 0.0,
    var lastUpdate: Long = 0L
)

/**
 * Represents animal detection data from /detection/ node.
 */
data class DetectionData(
    var animal: String? = null,
    var confidence: Double = 0.0,
    var timestamp: Long = 0L
)

/**
 * Represents a single alert entry from /alerts/ node.
 */
data class AlertData(
    var id: String = "",
    var message: String = "",
    var time: Long = 0L,
    var type: String = ""
)

/**
 * Represents camera feed data from /camera/ node.
 */
data class CameraData(
    var imageUrl: String = "",
    var lastUpdated: Long = 0L
)

/**
 * Represents the push notification trigger from /notification/ node.
 * Backend sets trigger=true when a new event fires; app resets it to false after showing.
 */
data class NotificationData(
    var title: String = "",
    var body: String = "",
    var time: Long = 0L,
    var trigger: Boolean = false
)
