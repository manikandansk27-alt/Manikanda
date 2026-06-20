package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.VideoEditorDatabase
import com.example.data.model.ColorGradingConfig
import com.example.data.model.ProjectData
import com.example.data.model.VideoClip
import com.example.data.model.VideoTransition
import com.example.data.network.GeminiApiClient
import com.example.data.repository.VideoProjectRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VideoEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val db = VideoEditorDatabase.getDatabase(application)
    private val repository = VideoProjectRepository(db.videoProjectDao)

    // Saved projects list
    val savedProjectsList: StateFlow<List<Pair<Int, Pair<String, ProjectData>>>> = repository.allProjects
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Project state variables
    private val _projectId = MutableStateFlow(0)
    val projectId = _projectId.asStateFlow()

    private val _projectName = MutableStateFlow("Untitled Cinema")
    val projectName = _projectName.asStateFlow()

    private val _clips = MutableStateFlow<List<VideoClip>>(emptyList())
    val clips = _clips.asStateFlow()

    private val _transitions = MutableStateFlow<List<VideoTransition>>(emptyList())
    val transitions = _transitions.asStateFlow()

    private val _selectedClipIndex = MutableStateFlow(0)
    val selectedClipIndex = _selectedClipIndex.asStateFlow()

    // Playback state
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _playheadPercent = MutableStateFlow(0.0f) // 0.0f to 100.0f
    val playheadPercent = _playheadPercent.asStateFlow()

    private val _playbackActiveClipIndex = MutableStateFlow(0)
    val playbackActiveClipIndex = _playbackActiveClipIndex.asStateFlow()

    // Is inside transition zone during playback
    private val _activeTransitionType = MutableStateFlow<String?>(null)
    val activeTransitionType = _activeTransitionType.asStateFlow()

    private val _transitionAlpha = MutableStateFlow(0.0f) // For overlay transitions
    val transitionAlpha = _transitionAlpha.asStateFlow()

    // AI states
    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading = _isAiLoading.asStateFlow()

    private val _aiFeedback = MutableStateFlow("Ask Spectra AI to automatically grade your timeline clips or set transitions.")
    val aiFeedback = _aiFeedback.asStateFlow()

    // Export overlay states
    private val _isExporting = MutableStateFlow(false)
    val isExporting = _isExporting.asStateFlow()

    private val _exportProgress = MutableStateFlow(0)
    val exportProgress = _exportProgress.asStateFlow()

    private val _exportStage = MutableStateFlow("")
    val exportStage = _exportStage.asStateFlow()

    private var playbackJob: Job? = null

    init {
        loadDefaultProject()
    }

    fun loadDefaultProject() {
        _projectId.value = 0
        _projectName.value = "New Cinematic Draft"
        val defaultClips = listOf(
            VideoClip(
                id = "clip_1",
                name = "1. Golden Sunrays",
                drawableName = "img_clip_forest",
                durationMs = 4000L,
                grading = ColorGradingConfig(
                    filterName = "Amber Dawn Overlay",
                    temperature = 0.5f,
                    saturation = 1.25f,
                    contrast = 1.1f,
                    brightness = 0.02f,
                    explanation = "Warmed atmospheric morning tones with elevated contrast saturation."
                )
            ),
            VideoClip(
                id = "clip_2",
                name = "2. Cyber Grid",
                drawableName = "img_clip_city",
                durationMs = 4000L,
                grading = ColorGradingConfig(
                    filterName = "Digital Twilight",
                    temperature = -0.4f,
                    saturation = 1.4f,
                    contrast = 1.2f,
                    brightness = -0.05f,
                    vignette = 0.35f,
                    explanation = "Cooled background matrix with electric blue saturation profiles."
                )
            ),
            VideoClip(
                id = "clip_3",
                name = "3. Oceanic Surge",
                drawableName = "img_clip_sunset",
                durationMs = 4000L,
                grading = ColorGradingConfig(
                    filterName = "Teal & Dusk Split",
                    temperature = 0.1f,
                    saturation = 1.3f,
                    contrast = 1.25f,
                    vignette = 0.4f,
                    hueShift = -10.0f,
                    explanation = "Classic Hollywood blockbuster split toning mimicking sunset glow."
                )
            )
        )
        val defaultTransitions = listOf(
            VideoTransition(id = "trans_1", fromClipId = "clip_1", toClipId = "clip_2", type = "Glow Dissolve", durationMs = 1000L),
            VideoTransition(id = "trans_2", fromClipId = "clip_2", toClipId = "clip_3", type = "Cosmic Wipe", durationMs = 1000L)
        )
        _clips.value = defaultClips
        _transitions.value = defaultTransitions
        _selectedClipIndex.value = 0
        _playbackActiveClipIndex.value = 0
        _playheadPercent.value = 0.0f
        _activeTransitionType.value = null
        _transitionAlpha.value = 0.0f
    }

    fun loadProject(id: Int, name: String, data: ProjectData) {
        _projectId.value = id
        _projectName.value = name
        _clips.value = data.clips
        _transitions.value = data.transitions
        _selectedClipIndex.value = 0
        _playbackActiveClipIndex.value = 0
        _playheadPercent.value = 0.0f
        _activeTransitionType.value = null
        _transitionAlpha.value = 0.0f
        stopPlayback()
    }

    fun deleteProject(id: Int) {
        viewModelScope.launch {
            repository.deleteProject(id)
            if (_projectId.value == id) {
                loadDefaultProject()
            }
        }
    }

    fun renameProject(newName: String) {
        _projectName.value = newName.ifBlank { "Untitled Project" }
    }

    fun selectClip(index: Int) {
        if (index in _clips.value.indices) {
            _selectedClipIndex.value = index
        }
    }

    fun updateClipGrading(index: Int, config: ColorGradingConfig) {
        val current = _clips.value.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(grading = config)
            _clips.value = current
        }
    }

    fun updateTransitionType(index: Int, type: String) {
        val current = _transitions.value.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(type = type)
            _transitions.value = current
        }
    }

    // Toggle Play / Pause playback simulation
    fun togglePlayback() {
        if (_isPlaying.value) {
            stopPlayback()
        } else {
            startPlayback()
        }
    }

    private fun startPlayback() {
        _isPlaying.value = true
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch(Dispatchers.Main) {
            val totalClips = _clips.value
            val totalTransitions = _transitions.value
            if (totalClips.isEmpty()) {
                _isPlaying.value = false
                return@launch
            }

            // Simple cycle playback loop
            var percent = _playheadPercent.value
            while (_isPlaying.value) {
                percent += 1.0f
                if (percent > 100.0f) {
                    percent = 0.0f
                }
                _playheadPercent.value = percent

                // Calculate active clip and transitions based on percent (0..100)
                resolvePlaybackStateAndEffects(percent, totalClips, totalTransitions)
                delay(30) // ~30 fps
            }
        }
    }

    private fun resolvePlaybackStateAndEffects(
        percent: Float,
        totalClips: List<VideoClip>,
        totalTransitions: List<VideoTransition>
    ) {
        val totalMs = totalClips.sumOf { it.durationMs }
        val currentMs = ((percent / 100.0f) * totalMs).toLong()

        var accumulatedMs = 0L
        var activeIdx = 0
        var transitionType: String? = null
        var tAlpha = 0.0f

        for (i in totalClips.indices) {
            val clip = totalClips[i]
            val clipStart = accumulatedMs
            val clipEnd = accumulatedMs + clip.durationMs
            if (currentMs in clipStart..clipEnd) {
                activeIdx = i

                // Check transition zones
                // Transition 0 coordinates clip 0 to 1, occurring at the end of clip 0
                val transitionDuration = 1000L // default transition range
                val transitionStartsAt = clipEnd - (transitionDuration / 2)
                val transitionEndsAt = clipEnd + (transitionDuration / 2)

                if (i < totalClips.size - 1) {
                    val matchingTrans = totalTransitions.getOrNull(i)
                    if (matchingTrans != null && matchingTrans.type != "None" && currentMs >= transitionStartsAt) {
                        transitionType = matchingTrans.type
                        // Linear alpha morph during the boundary overlap
                        val progress = (currentMs - transitionStartsAt).toFloat() / transitionDuration.toFloat()
                        tAlpha = progress.coerceIn(0.0f, 1.0f)
                    }
                }
                break
            }
            accumulatedMs += clip.durationMs
        }

        _playbackActiveClipIndex.value = activeIdx
        _activeTransitionType.value = transitionType
        _transitionAlpha.value = tAlpha
    }

    fun stopPlayback() {
        _isPlaying.value = false
        playbackJob?.cancel()
    }

    fun scrubPlayhead(percent: Float) {
        _playheadPercent.value = percent.coerceIn(0.0f, 100.0f)
        resolvePlaybackStateAndEffects(percent, _clips.value, _transitions.value)
    }

    // AI-powered video auto-grading & transitions trigger
    fun requestAiGrading(prompt: String) {
        if (prompt.isBlank()) return
        stopPlayback()
        _isAiLoading.value = true
        _aiFeedback.value = "Analyzing cinematic frames and estimating look curves..."

        viewModelScope.launch {
            try {
                val response = GeminiApiClient.autoGradeVideo(prompt)
                val updatedConfig = response.toColorGradingConfig()

                // Apply color grading to the currently selected clip on the workspace!
                val activeIdx = _selectedClipIndex.value
                val currentClips = _clips.value.toMutableList()
                if (activeIdx in currentClips.indices) {
                    val originalClipName = currentClips[activeIdx].name
                    currentClips[activeIdx] = currentClips[activeIdx].copy(
                        name = "${activeIdx + 1}. AI ${response.filterName}",
                        grading = updatedConfig
                    )
                    _clips.value = currentClips

                    // If there's an adjacent clip, update the transition suggestion recommended by Gemini!
                    if (response.suggestedTransition != "None" && activeIdx < _transitions.value.size) {
                        val currentTransitions = _transitions.value.toMutableList()
                        currentTransitions[activeIdx] = currentTransitions[activeIdx].copy(type = response.suggestedTransition)
                        _transitions.value = currentTransitions
                    }

                    _aiFeedback.value = "🎨 Spectra AI Grade Applied successfully to '$originalClipName' -> Look: [${response.filterName}].\n⚙️ Transition: Configured '${response.suggestedTransition}' cross-clip effect.\n💬 AI Explanation:\n${response.explanation}"
                } else {
                    _aiFeedback.value = "Spectra AI suggested '${response.filterName}' but no active clip was selected on your timeline."
                }
            } catch (e: Exception) {
                _aiFeedback.value = "Failed to synchronize with Spectra AI services: ${e.message}"
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    // Persist current editing session to SQLite database via Room
    fun saveActiveProject() {
        viewModelScope.launch {
            val name = _projectName.value
            val currentClips = _clips.value
            val currentTrans = _transitions.value
            val data = ProjectData(clips = currentClips, transitions = currentTrans)

            val currentId = _projectId.value
            val newId = repository.saveProject(currentId, name, data)
            _projectId.value = newId
            _aiFeedback.value = "💾 Project '$name' successfully saved and persisted to local Room Database."
        }
    }

    // Simulated premium rendering output engine
    fun triggerTimelineExport() {
        stopPlayback()
        _isExporting.value = true
        _exportProgress.value = 0
        _exportStage.value = "Initializing rendering pipelines..."

        viewModelScope.launch {
            val stages = listOf(
                "Analyzing timeline clip arrays..." to 10,
                "Rendering custom vector clip masks..." to 25,
                "Applying AI color grading convolutions..." to 45,
                "Blending AI neural transitions (${_transitions.value.map { it.type }.filter { it != "None" }.joinToString { it }.ifEmpty { "Dissolves" }})..." to 65,
                "Compiling 10-bit color pixel matrices..." to 80,
                "Muxing surround dynamic audio tracks..." to 90,
                "Finalizing high-quality MP4 master render..." to 98
            )

            for ((text, startPct) in stages) {
                _exportStage.value = text
                var current = _exportProgress.value
                while (current < startPct) {
                    current += (1..4).random()
                    _exportProgress.value = current.coerceAtMost(startPct)
                    delay((50..120).random().toLong())
                }
            }
            _exportProgress.value = 100
            _exportStage.value = "Render complete! AI Cinema Master successfully exported."
            delay(1500)
            _isExporting.value = false
        }
    }
}
