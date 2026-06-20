package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.ColorGradingConfig
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.VideoEditorViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                VideoEditorWorkspaceScreen()
            }
        }
    }
}

@Composable
fun VideoEditorWorkspaceScreen() {
    val viewModel: VideoEditorViewModel = viewModel()
    
    val projectId by viewModel.projectId.collectAsState()
    val projectName by viewModel.projectName.collectAsState()
    val clips by viewModel.clips.collectAsState()
    val transitions by viewModel.transitions.collectAsState()
    val selectedClipIndex by viewModel.selectedClipIndex.collectAsState()
    
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playheadPercent by viewModel.playheadPercent.collectAsState()
    val playbackActiveClipIndex by viewModel.playbackActiveClipIndex.collectAsState()
    val activeTransitionType by viewModel.activeTransitionType.collectAsState()
    val transitionAlpha by viewModel.transitionAlpha.collectAsState()
    
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val aiFeedback by viewModel.aiFeedback.collectAsState()
    
    val isExporting by viewModel.isExporting.collectAsState()
    val exportProgress by viewModel.exportProgress.collectAsState()
    val exportStage by viewModel.exportStage.collectAsState()
    
    val savedProjects by viewModel.savedProjectsList.collectAsState()

    var showDrawer by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf(projectName) }
    var isRenaming by remember { mutableStateOf(false) }
    var aiPromptInput by remember { mutableStateOf("") }
    var activeInspectorTab by remember { mutableStateOf(0) } // 0 = Manual, 1 = AI Spectra

    // Synchronize rename text when project loads
    LaunchedEffect(projectName) {
        renameText = projectName
    }

    // Studio Theme Palette
    val studioBg = Color(0xFF0F0B1F)
    val studioSurface = Color(0xFF18132C)
    val studioAccentRose = Color(0xFFFF2994)
    val studioAccentTeal = Color(0xFF00FFEA)
    val studioMuted = Color(0xFF8B84A6)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = studioBg,
        topBar = {
            // Elegant Header
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(studioSurface)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Filled.Settings, // Cinematic gear representation
                            contentDescription = "Studio Logo",
                            tint = studioAccentTeal,
                            modifier = Modifier
                                .size(28.dp)
                                .padding(end = 6.dp)
                        )
                        
                        if (isRenaming) {
                            TextField(
                                value = renameText,
                                onValueChange = { renameText = it },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                                singleLine = true,
                                modifier = Modifier
                                    .width(180.dp)
                                    .testTag("project_name_edit_field")
                            )
                            IconButton(
                                onClick = {
                                    viewModel.renameProject(renameText)
                                    isRenaming = false
                                },
                                modifier = Modifier.testTag("save_rename_button")
                            ) {
                                Icon(Icons.Filled.Check, "Save Rename", tint = studioAccentTeal)
                            }
                        } else {
                            Row(
                                modifier = Modifier.clickable { isRenaming = true },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = projectName,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Rename Project",
                                    tint = studioAccentRose,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Project History Drawer Button
                        IconButton(
                            onClick = { showDrawer = true },
                            modifier = Modifier.testTag("open_projects_drawer")
                        ) {
                            Icon(Icons.Filled.Menu, "Browse Sessions", tint = Color.White)
                        }

                        // Project Refresh / New Session
                        IconButton(
                            onClick = { viewModel.loadDefaultProject() },
                            modifier = Modifier.testTag("new_project_button")
                        ) {
                            Icon(Icons.Filled.Refresh, "New Project", tint = studioMuted)
                        }

                        // Save current project state
                        Button(
                            onClick = { viewModel.saveActiveProject() },
                            colors = ButtonDefaults.buttonColors(containerColor = studioSurface),
                            shape = RoundedCornerShape(8.dp),
                            border = PaddingValues(0.dp).let { BorderStroke(1.dp, studioAccentRose) },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("save_project_button")
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = "Save", tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save", color = Color.White, fontSize = 12.sp)
                        }

                        // Export (High quality render)
                        Button(
                            onClick = { viewModel.triggerTimelineExport() },
                            colors = ButtonDefaults.buttonColors(containerColor = studioAccentRose),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("export_button")
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = "Export", tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
                
                // Active project status indicator band
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF07040E))
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "STUDIO RENDER ENGINE ACTIVE " + if (projectId != 0) "[ID #$projectId]" else "[Sandbox Mode]",
                        color = studioAccentTeal,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "CLIP ${selectedClipIndex + 1}/${clips.size} SELECTED",
                        color = studioAccentRose,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp)
            ) {
                // SECTION 1: MASTER PREVIEW PLAYER SCREEN
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .border(BorderStroke(1.dp, studioSurface), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column {
                        // Interactive Video Screen with overlay and crossfades
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            // Resolve what active clip (and overlay transitions) to display
                            val activePlaybackClip = clips.getOrNull(playbackActiveClipIndex) ?: clips.firstOrNull()
                            val nextPlaybackClip = clips.getOrNull(playbackActiveClipIndex + 1)
                            
                            if (activePlaybackClip != null) {
                                val currentDrawableId = getDrawableId(activePlaybackClip.drawableName)
                                val currentMatrix = createColorGradingMatrix(activePlaybackClip.grading)

                                Box(modifier = Modifier.fillMaxSize()) {
                                    // Primary Frame Render with real-time math-based color grade filter
                                    Image(
                                        painter = painterResource(id = currentDrawableId),
                                        contentDescription = "Graded Clip Frame",
                                        colorFilter = ColorFilter.colorMatrix(currentMatrix),
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    // Dynamic transition overlay emulation
                                    if (activeTransitionType != null && activeTransitionType != "None" && nextPlaybackClip != null) {
                                        val nextDrawableId = getDrawableId(nextPlaybackClip.drawableName)
                                        val nextMatrix = createColorGradingMatrix(nextPlaybackClip.grading)
                                        
                                        // Blended crossfade frame
                                        Image(
                                            painter = painterResource(id = nextDrawableId),
                                            contentDescription = "Next Graded Frame",
                                            colorFilter = ColorFilter.colorMatrix(nextMatrix),
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .alpha(transitionAlpha)
                                        )

                                        // Apply VFX Overlay Graphics on target boundaries
                                        when (activeTransitionType) {
                                            "Glow Dissolve" -> {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(Color.White.copy(alpha = (0.4f * (1.0f - kotlin.math.abs(transitionAlpha - 0.5f) * 2))))
                                                )
                                            }
                                            "Analog Glitch" -> {
                                                // Simulated digital scanning line sweep
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(4.dp)
                                                        .align(Alignment.Center)
                                                        .offset(y = (-100 + (transitionAlpha * 200)).dp)
                                                        .background(studioAccentTeal)
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(Color(0x33FF00A0))
                                                )
                                            }
                                            "Cosmic Wipe" -> {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(
                                                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                                                colors = listOf(Color.Transparent, studioAccentRose.copy(alpha = 0.5f), Color.Transparent),
                                                                startX = 0.0f,
                                                                endX = 1000f * transitionAlpha
                                                            )
                                                        )
                                                )
                                            }
                                            "Zoom Blur" -> {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .border(BorderStroke((10 * transitionAlpha).dp, studioAccentTeal.copy(alpha = 1f - transitionAlpha)))
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                Text("No Clips Active on Timeline", color = Color.White)
                            }

                            // Interactive overlays
                            // Playhead active FX text
                            if (activeTransitionType != null && activeTransitionType != "None") {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(12.dp)
                                        .background(studioAccentRose, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "TRANSITION: $activeTransitionType",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            // Dynamic Live Preview Tag
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(12.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isPlaying) Color.Green else Color.Red)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isPlaying) "4K MASTER PREVIEW" else "PAUSED",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Play Bar Controls Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(studioSurface)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val elapsedMs = ((playheadPercent / 100f) * clips.sumOf { it.durationMs }).toLong()
                            val totalMs = clips.sumOf { it.durationMs }
                            
                            Text(
                                text = formatTime(elapsedMs),
                                color = studioAccentTeal,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )

                            // Play / Pause Float Button
                            IconButton(
                                onClick = { viewModel.togglePlayback() },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(studioAccentRose, CircleShape)
                                    .testTag("play_pause_button")
                            ) {
                                Text(
                                    text = if (isPlaying) "⏸" else "▶",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = if (isPlaying) Modifier else Modifier.padding(start = 2.dp)
                                )
                            }

                            Text(
                                text = formatTime(totalMs),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // SECTION 2: SCRUBBER TIMELINE
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "TIMELINE STAGING",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    // Linear scrubbing support
                    Slider(
                        value = playheadPercent,
                        onValueChange = { viewModel.scrubPlayhead(it) },
                        valueRange = 0.0f..100.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = studioAccentRose,
                            activeTrackColor = studioAccentRose,
                            inactiveTrackColor = Color(0xFF231A3F)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("timeline_scrubber")
                    )

                    // Tracks Layout Box representing Video and Transition Timeline
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(studioSurface, RoundedCornerShape(8.dp))
                            .border(BorderStroke(1.dp, Color(0xFF261D4C)), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Visual track descriptions
                        Column(
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Text("VIDEO", color = studioAccentTeal, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(36.dp))
                            Text("VFX", color = studioAccentRose, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }

                        // Blocks for Clips and Transitions
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            clips.forEachIndexed { idx, clip ->
                                // Clip box
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (playbackActiveClipIndex == idx) Color(0xFF2E2259) else Color(0xFF1E183B))
                                        .border(
                                            BorderStroke(
                                                width = if (selectedClipIndex == idx) 2.dp else 1.dp,
                                                color = if (selectedClipIndex == idx) studioAccentTeal else Color(0xFF382F63)
                                            ),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable { viewModel.selectClip(idx) }
                                ) {
                                    val clipDrawableId = getDrawableId(clip.drawableName)
                                    // Tiny thumbnail thumbnail background
                                    Image(
                                        painter = painterResource(id = clipDrawableId),
                                        contentDescription = clip.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .alpha(0.3f)
                                    )
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(6.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = clip.name,
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "${clip.durationMs / 1000}s [${clip.grading.filterName}]",
                                            color = studioAccentTeal,
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                // If not last, draw editable transition fx dot
                                if (idx < clips.size - 1) {
                                    val transition = transitions.getOrNull(idx)
                                    val hasTransition = transition != null && transition.type != "None"

                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp)
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(if (hasTransition) studioAccentRose else Color(0xFF292244))
                                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)), CircleShape)
                                            .clickable {
                                                // Cycle transitions upon direct tap!
                                                val nextTransType = when (transition?.type) {
                                                    "None" -> "Glow Dissolve"
                                                    "Glow Dissolve" -> "Cosmic Wipe"
                                                    "Cosmic Wipe" -> "Zoom Blur"
                                                    "Zoom Blur" -> "Analog Glitch"
                                                    else -> "None"
                                                }
                                                viewModel.updateTransitionType(idx, nextTransType)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = when (transition?.type) {
                                                "Glow Dissolve" -> "☄️"
                                                "Cosmic Wipe" -> "🌌"
                                                "Zoom Blur" -> "🔍"
                                                "Analog Glitch" -> "⚡"
                                                else -> "➕"
                                            },
                                            fontSize = 11.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Text(
                        text = "💡 Tap transition nodes to cycle custom AI effects: Glow Dissolve, Cosmic Wipe, Zoom Blur, Analog Glitch.",
                        color = studioMuted,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )
                }

                // SECTION 3: INSPECTOR TAB PANE (Manual Color Grading & AI Assistant Console)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .border(BorderStroke(1.dp, Color(0xFF2E2454)), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = studioSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column {
                        // Inspector Tabs Selector
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF130E26))
                        ) {
                            TabButton(
                                text = "🎨 COLOR GRADING MATRIX",
                                active = activeInspectorTab == 0,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("manual_grader_tab_button"),
                                onClick = { activeInspectorTab = 0 }
                            )
                            TabButton(
                                text = "🧠 SPECTRA AI CONSOLE",
                                active = activeInspectorTab == 1,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("ai_console_tab_button"),
                                onClick = { activeInspectorTab = 1 }
                            )
                        }

                        val activeClip = clips.getOrNull(selectedClipIndex)
                        if (activeClip != null) {
                            if (activeInspectorTab == 0) {
                                // Tab 0: Manual LUT Slider Tuning
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "TUNING CLIP LOOK: ${activeClip.name}",
                                        color = studioAccentTeal,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )

                                    // Manual Slider Items
                                    LUTSlider(
                                        label = "Brightness",
                                        value = activeClip.grading.brightness,
                                        valueRange = -0.4f..0.4f,
                                        onValueChange = {
                                            viewModel.updateClipGrading(selectedClipIndex, activeClip.grading.copy(brightness = it))
                                        }
                                    )
                                    LUTSlider(
                                        label = "Contrast",
                                        value = activeClip.grading.contrast,
                                        valueRange = 0.6f..1.8f,
                                        onValueChange = {
                                            viewModel.updateClipGrading(selectedClipIndex, activeClip.grading.copy(contrast = it))
                                        }
                                    )
                                    LUTSlider(
                                        label = "Saturation",
                                        value = activeClip.grading.saturation,
                                        valueRange = 0.0f..2.5f,
                                        onValueChange = {
                                            viewModel.updateClipGrading(selectedClipIndex, activeClip.grading.copy(saturation = it))
                                        }
                                    )
                                    LUTSlider(
                                        label = "Color Temp (K)",
                                        value = activeClip.grading.temperature,
                                        valueRange = -1.0f..1.0f,
                                        onValueChange = {
                                            viewModel.updateClipGrading(selectedClipIndex, activeClip.grading.copy(temperature = it))
                                        }
                                    )
                                    LUTSlider(
                                        label = "Vignette Width",
                                        value = activeClip.grading.vignette,
                                        valueRange = 0.0f..1.0f,
                                        onValueChange = {
                                            viewModel.updateClipGrading(selectedClipIndex, activeClip.grading.copy(vignette = it))
                                        }
                                    )
                                    LUTSlider(
                                        label = "Hue Rotation Pivot",
                                        value = activeClip.grading.hueShift,
                                        valueRange = -180.0f..180.0f,
                                        onValueChange = {
                                            viewModel.updateClipGrading(selectedClipIndex, activeClip.grading.copy(hueShift = it))
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "INSTANT LUT GRADE PRESETS",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )

                                    // Horizontally scrollable professional quick grades
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(
                                            "Teal & Orange" to ColorGradingConfig(filterName = "Cinematic Blockbuster Teal & Orange", temperature = -0.1f, saturation = 1.35f, contrast = 1.25f, vignette = 0.45f, hueShift = -15.0f),
                                            "Cyber Dusk" to ColorGradingConfig(filterName = "Grid Cyberpunk Dusk", temperature = -0.55f, saturation = 1.7f, contrast = 1.4f, vignette = 0.35f, hueShift = 30.0f),
                                            "Retro VHS" to ColorGradingConfig(filterName = "Analog Retro VHS Tape", temperature = 0.25f, saturation = 0.65f, contrast = 0.85f, vignette = 0.5f, hueShift = 12.0f),
                                            "Noir Film" to ColorGradingConfig(filterName = "Monochrome Noir Shadow", temperature = 0.0f, saturation = 0.0f, contrast = 1.7f, vignette = 0.65f, hueShift = 0.0f),
                                            "Golden Glow" to ColorGradingConfig(filterName = "Golden Sunset Glow", temperature = 0.75f, saturation = 1.3f, contrast = 1.15f, vignette = 0.25f, hueShift = -5.0f)
                                        ).forEach { (label, config) ->
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFF281C4F), RoundedCornerShape(8.dp))
                                                    .border(BorderStroke(1.dp, Color(0xFF45357c)), RoundedCornerShape(8.dp))
                                                    .clickable { viewModel.updateClipGrading(selectedClipIndex, config) }
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text(text = label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Tab 1: AI Spectra Assistant Command Console
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "COMMAND SPECTRA AI CRITICAL CODES",
                                        color = studioAccentRose,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                    
                                    Text(
                                        text = "Send structural descriptions of any color aesthetic or mood. The AI will compute lookup curves and deploy transitions.",
                                        color = studioMuted,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )

                                    // Input bar
                                    TextField(
                                        value = aiPromptInput,
                                        onValueChange = { aiPromptInput = it },
                                        placeholder = { Text("E.g., Make it cold and eerie like a Nordic noir mystery", color = studioMuted, fontSize = 12.sp) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = studioAccentRose,
                                            unfocusedBorderColor = Color(0xFF33295B),
                                            focusedContainerColor = Color(0xFF0F0B1F),
                                            unfocusedContainerColor = Color(0xFF0F0B1F)
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("ai_instruction_input")
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Prompt triggers
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Try presets:",
                                            color = studioMuted,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            listOf("Cyber Glow", "Nordic Winter", "Vintage Warmth").forEach { preset ->
                                                Text(
                                                    text = preset,
                                                    color = studioAccentTeal,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier
                                                        .clickable { aiPromptInput = "Apply state: $preset" }
                                                        .border(BorderStroke(0.5.dp, studioAccentTeal), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(
                                        onClick = {
                                            viewModel.requestAiGrading(aiPromptInput)
                                            aiPromptInput = ""
                                        },
                                        enabled = !isAiLoading && aiPromptInput.isNotBlank(),
                                        colors = ButtonDefaults.buttonColors(containerColor = studioAccentTeal),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("ai_apply_button")
                                    ) {
                                        if (isAiLoading) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = studioBg)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("AI CONSTRUCTING LOOKS...", color = studioBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        } else {
                                            Icon(Icons.Filled.Settings, "Compute Look", tint = studioBg, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("AUTO-GRADE VIA SPECTRA AI", color = studioBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // AI Feedback Console panel
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF0A0716), RoundedCornerShape(8.dp))
                                            .border(BorderStroke(1.dp, Color(0xFF211742)), RoundedCornerShape(8.dp))
                                            .padding(12.dp)
                                    ) {
                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(bottom = 6.dp)
                                            ) {
                                                Icon(Icons.Filled.Info, "AI Logs", tint = studioAccentRose, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "NEURAL DIAGNOSTIC FEEDBACK:",
                                                    color = studioAccentRose,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                            Text(
                                                text = aiFeedback,
                                                color = Color(0xFFC0BBD6),
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Text("Please load a clip", color = Color.White)
                        }
                    }
                }
            }

            // MODAL OVERLAY 1: HISTORIAL PROJECT DRAWER (ROOM ENGINE)
            if (showDrawer) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .clickable { showDrawer = false },
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(300.dp)
                            .background(studioSurface)
                            .border(BorderStroke(1.dp, Color(0xFF382963)))
                            .clickable(enabled = false) {}
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Header of Drawer
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF110C24))
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("SAVED CUTS & DRAWS", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                IconButton(onClick = { showDrawer = false }) {
                                    Icon(Icons.Filled.Close, "Close", tint = studioAccentRose)
                                }
                            }

                            if (savedProjects.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(24.dp)
                                    ) {
                                        Icon(Icons.Filled.Search, "Empty DB", tint = studioMuted, modifier = Modifier.size(48.dp))
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "No saved projects found in local Room Database.",
                                            color = studioMuted,
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Click 'Save' on the top row to archive your workspace.",
                                            color = studioAccentTeal,
                                            fontSize = 10.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(savedProjects) { (id, projectPair) ->
                                        val (name, data) = projectPair
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(
                                                    BorderStroke(
                                                        2.dp,
                                                        if (projectId == id) studioAccentTeal else Color.Transparent
                                                    ),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    viewModel.loadProject(id, name, data)
                                                    showDrawer = false
                                                },
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF221A3D)),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    Text(
                                                        text = "${data.clips.size} clips • ${data.transitions.map { it.type }.filter { it != "None" }.size} FX",
                                                        color = studioAccentRose,
                                                        fontSize = 10.sp,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { viewModel.deleteProject(id) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Filled.Delete, "Delete Save", tint = studioMuted, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // MODAL OVERLAY 2: CINEMATIC RENDERING ENGINE PRO PROGRESS
            if (isExporting) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(studioBg.copy(alpha = 0.95f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        // Giant Spinning Lens aperture loader representing engine
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(120.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { exportProgress.toFloat() / 100.0f },
                                modifier = Modifier.fillMaxSize(),
                                color = studioAccentTeal,
                                strokeWidth = 6.dp,
                            )
                            CircularProgressIndicator(
                                modifier = Modifier.size(90.dp),
                                color = studioAccentRose,
                                strokeWidth = 3.dp,
                            )
                            Text(
                                text = "$exportProgress%",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "SPECTRA CINEMA ENG MASTER",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Active rendering step diagnostic
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(Color(0xFF080512), RoundedCornerShape(8.dp))
                                .border(BorderStroke(1.dp, Color(0xFF1E143F)), RoundedCornerShape(8.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Filled.Settings, "Stage Icon", tint = studioAccentTeal, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = exportStage,
                                color = studioAccentTeal,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(48.dp))
                        Text(
                            text = "⚠️ Do not lock your screen. Compensating for 10-bit color buffers...",
                            color = studioMuted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// Custom tabs selector
@Composable
fun TabButton(
    text: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(if (active) Color.Transparent else Color(0xFF0F0B1E))
            .clickable { onClick() }
            .drawBehind {
                if (active) {
                    drawLine(
                        color = Color(0xFFFF2994),
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 6f
                    )
                }
            }
            .padding(vertical = 12.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (active) Color.White else Color(0xFF6B648C),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )
    }
}

// LUT Custom control sliders
@Composable
fun LUTSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(
                text = String.format("%.2f", value),
                color = Color(0xFF00FFEA),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFFF2994),
                activeTrackColor = Color(0xFFFF2994),
                inactiveTrackColor = Color(0xFF281D4C)
            ),
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

// Map custom string to layout resource ID safely
private fun getDrawableId(name: String): Int {
    return when (name) {
        "img_clip_forest" -> R.drawable.img_clip_forest
        "img_clip_city" -> R.drawable.img_clip_city
        "img_clip_sunset" -> R.drawable.img_clip_sunset
        else -> R.drawable.img_clip_forest
    }
}

// Mathematical color matrix mapping
private fun createColorGradingMatrix(config: ColorGradingConfig): ColorMatrix {
    val c = config.contrast
    val s = config.saturation
    val bOffset = config.brightness * 255f
    
    // Calculate color temperature multiplier weights
    val tempRedScale = if (config.temperature > 0f) 1f + config.temperature * 0.2f else 1f
    val tempBlueScale = if (config.temperature < 0f) 1f - config.temperature * 0.2f else 1f

    val rScale = c * tempRedScale
    val gScale = c
    val bScale = c * tempBlueScale

    // Luminance constants for saturation split-matrix
    val rWeight = 0.299f
    val gWeight = 0.587f
    val bWeight = 0.114f

    return ColorMatrix(
        floatArrayOf(
            ((1f - s) * rWeight + s) * rScale, ((1f - s) * gWeight) * rScale, ((1f - s) * bWeight) * rScale, 0f, bOffset,
            ((1f - s) * rWeight) * gScale, ((1f - s) * gWeight + s) * gScale, ((1f - s) * bWeight) * gScale, 0f, bOffset,
            ((1f - s) * rWeight) * bScale, ((1f - s) * gWeight) * bScale, ((1f - s) * bWeight + s) * bScale, 0f, bOffset,
            0f, 0f, 0f, 1f, 0f
        )
    )
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val frames = (ms % 1000) / 40 // approximate frame rate count (25 fps representation)
    return String.format("%02d:%02d.%02d", minutes, seconds, frames)
}

// Deprecated fallback Greeting function to keep older testing suits fully functional and compiling
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
