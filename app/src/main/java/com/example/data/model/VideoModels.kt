package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ColorGradingConfig(
    val filterName: String = "Normal",
    val brightness: Float = 0.0f,     // -0.5 to 0.5
    val contrast: Float = 1.0f,       // 0.5 to 2.0
    val saturation: Float = 1.0f,     // 0.0 to 3.0
    val temperature: Float = 0.0f,    // -1.0 to 1.0 (blue to yellow)
    val vignette: Float = 0.0f,       // 0.0 to 1.0
    val hueShift: Float = 0.0f,       // -180 to 180
    val explanation: String = ""
)

@JsonClass(generateAdapter = true)
data class VideoClip(
    val id: String,
    val name: String,
    val durationMs: Long = 4000L,
    val startMs: Long = 0L,
    val drawableName: String, // img_clip_city, img_clip_forest, img_clip_sunset
    val grading: ColorGradingConfig = ColorGradingConfig()
)

@JsonClass(generateAdapter = true)
data class VideoTransition(
    val id: String,
    val fromClipId: String,
    val toClipId: String,
    val type: String = "None", // None, Glow Dissolve, Cosmic Wipe, Zoom Blur, Analog Glitch
    val durationMs: Long = 1000L
)

@JsonClass(generateAdapter = true)
data class ProjectData(
    val clips: List<VideoClip>,
    val transitions: List<VideoTransition>,
    val bpm: Int = 120,
    val audioTrackName: String = "No Track",
    val audioVolume: Float = 0.8f
)
