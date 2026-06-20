package com.example.data.network

import com.example.BuildConfig
import com.example.data.model.ColorGradingConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiPart(val text: String? = null)

@JsonClass(generateAdapter = true)
data class GeminiContent(val parts: List<GeminiPart>)

@JsonClass(generateAdapter = true)
data class GeminiSchemaProperty(val type: String, val description: String? = null)

@JsonClass(generateAdapter = true)
data class GeminiResponseSchema(
    val type: String,
    val properties: Map<String, GeminiSchemaProperty>? = null,
    val required: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponseFormat(
    val mimeType: String,
    val responseSchema: GeminiResponseSchema? = null
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    val responseMimeType: String? = null,
    val responseSchema: GeminiResponseSchema? = null,
    val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GeminiGenerateRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null,
    val generationConfig: GeminiGenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(val content: GeminiContent)

@JsonClass(generateAdapter = true)
data class GeminiGenerateResponse(val candidates: List<GeminiCandidate>?)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiGenerateRequest
    ): GeminiGenerateResponse
}

@JsonClass(generateAdapter = true)
data class AiVideoGradingResult(
    val filterName: String,
    val brightness: Float,
    val contrast: Float,
    val saturation: Float,
    val temperature: Float,
    val vignette: Float,
    val hueShift: Float,
    val suggestedTransition: String, // "None", "Glow Dissolve", "Cosmic Wipe", "Zoom Blur", "Analog Glitch"
    val explanation: String
) {
    fun toColorGradingConfig() = ColorGradingConfig(
        filterName = filterName,
        brightness = brightness.coerceIn(-0.5f, 0.5f),
        contrast = contrast.coerceIn(0.5f, 2.0f),
        saturation = saturation.coerceIn(0.0f, 3.0f),
        temperature = temperature.coerceIn(-1.0f, 1.0f),
        vignette = vignette.coerceIn(0.0f, 1.0f),
        hueShift = hueShift.coerceIn(-180.0f, 180.0f),
        explanation = explanation
    )
}

object GeminiApiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val api: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    suspend fun autoGradeVideo(prompt: String): AiVideoGradingResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            // Fallback mock grading output if no API key is specified
            return@withContext getMockGrading(prompt)
        }

        val systemPrompt = """
            You are a expert cinematic colorist and AI video editor coordinator.
            Analyze the user's natural language request describing the desired visual style, color grading mood, or transition effect.
            Return a JSON object matching the requested schema.
            The parameters should reflect professional color grading practice:
            - filterName: string title representing the custom filter look.
            - brightness: float between -0.4 and 0.4.
            - contrast: float between 0.6 and 1.8 (1.0 is neutral).
            - saturation: float between 0.0 and 2.5 (1.0 is neutral).
            - temperature: float between -1.0 (very cool/blue) and 1.0 (very warm/orange/amber).
            - vignette: float between 0.0 and 1.0.
            - hueShift: float between -180.0 and 180.0.
            - suggestedTransition: MUST be exactly one of: "None", "Glow Dissolve", "Cosmic Wipe", "Zoom Blur", "Analog Glitch".
            - explanation: brief explanation of the aesthetic choices.
        """.trimIndent()

        val responseSchema = GeminiResponseSchema(
            type = "OBJECT",
            properties = mapOf(
                "filterName" to GeminiSchemaProperty("STRING", "Visual name of this customized look"),
                "brightness" to GeminiSchemaProperty("NUMBER", "Brightness offset (-0.4 to 0.4)"),
                "contrast" to GeminiSchemaProperty("NUMBER", "Contrast modifier (0.6 to 1.8)"),
                "saturation" to GeminiSchemaProperty("NUMBER", "Saturation modifier (0.0 to 2.5)"),
                "temperature" to GeminiSchemaProperty("NUMBER", "Color temperature tint (-1.0 to 1.0)"),
                "vignette" to GeminiSchemaProperty("NUMBER", "Edge darkness intensity (0.0 to 1.0)"),
                "hueShift" to GeminiSchemaProperty("NUMBER", "Hue shift rotation offset (-180.0 to 180.0)"),
                "suggestedTransition" to GeminiSchemaProperty("STRING", "E.g., Glow Dissolve, Cosmic Wipe, Zoom Blur, Analog Glitch"),
                "explanation" to GeminiSchemaProperty("STRING", "Brief styling commentary")
            ),
            required = listOf("filterName", "brightness", "contrast", "saturation", "temperature", "vignette", "suggestedTransition", "explanation")
        )

        val request = GeminiGenerateRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = "Analyze: $prompt")))),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt))),
            generationConfig = GeminiGenerationConfig(
                responseMimeType = "application/json",
                responseSchema = responseSchema,
                temperature = 0.2f
            )
        )

        try {
            val response = api.generateContent(apiKey, request)
            val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (responseText != null) {
                moshi.adapter(AiVideoGradingResult::class.java).fromJson(responseText)
                    ?: getMockGrading(prompt)
            } else {
                getMockGrading(prompt)
            }
        } catch (e: Exception) {
            getMockGrading(prompt)
        }
    }

    private fun getMockGrading(prompt: String): AiVideoGradingResult {
        // High-quality local grading solver based on user keywords
        val lower = prompt.lowercase()
        return when {
            lower.contains("cyber") || lower.contains("neon") || lower.contains("synth") -> {
                AiVideoGradingResult(
                    filterName = "AI Cyber-Neon",
                    brightness = 0.05f,
                    contrast = 1.3f,
                    saturation = 1.6f,
                    temperature = -0.5f,
                    vignette = 0.4f,
                    hueShift = 25.0f,
                    suggestedTransition = "Analog Glitch",
                    explanation = "[Local AI Simulation] Boosted blues and pinks, elevated contrast for high neon reflections with analog glitching transitions."
                )
            }
            lower.contains("warm") || lower.contains("sunset") || lower.contains("amber") || lower.contains("golden") -> {
                AiVideoGradingResult(
                    filterName = "AI Golden Sunset",
                    brightness = 0.0f,
                    contrast = 1.1f,
                    saturation = 1.4f,
                    temperature = 0.7f,
                    vignette = 0.3f,
                    hueShift = -5.0f,
                    suggestedTransition = "Glow Dissolve",
                    explanation = "[Local AI Simulation] Warmed up chromatic balance towards amber/golden hues with cinematic blooming transitions."
                )
            }
            lower.contains("retro") || lower.contains("vhs") || lower.contains("vintage") || lower.contains("old") -> {
                AiVideoGradingResult(
                    filterName = "AI Retro VHS",
                    brightness = 0.03f,
                    contrast = 0.85f,
                    saturation = 0.7f,
                    temperature = 0.2f,
                    vignette = 0.5f,
                    hueShift = 10.0f,
                    suggestedTransition = "Analog Glitch",
                    explanation = "[Local AI Simulation] Faded black levels, lowered saturation, and shifted tint to emulate 1970s analog tape format."
                )
            }
            lower.contains("teal") || lower.contains("orange") || lower.contains("blockbuster") || lower.contains("movie") -> {
                AiVideoGradingResult(
                    filterName = "AI Teal & Orange",
                    brightness = -0.05f,
                    contrast = 1.35f,
                    saturation = 1.3f,
                    temperature = -0.2f,
                    vignette = 0.45f,
                    hueShift = -15.0f,
                    suggestedTransition = "Cosmic Wipe",
                    explanation = "[Local AI Simulation] Applied classic blockbuster contrast splitting skin-tones toward amber and shadows toward rich cyan."
                )
            }
            lower.contains("mono") || lower.contains("noir") || lower.contains("black") || lower.contains("gray") -> {
                AiVideoGradingResult(
                    filterName = "AI Cinematic Noir",
                    brightness = -0.05f,
                    contrast = 1.6f,
                    saturation = 0.0f,
                    temperature = 0.0f,
                    vignette = 0.6f,
                    hueShift = 0.0f,
                    suggestedTransition = "Zoom Blur",
                    explanation = "[Local AI Simulation] Entirely desaturated palette with stark shadow weights and a heavy vignette reminiscent of film-noir camera styling."
                )
            }
            else -> {
                AiVideoGradingResult(
                    filterName = "AI Modern Cinematic",
                    brightness = 0.02f,
                    contrast = 1.15f,
                    saturation = 1.2f,
                    temperature = 0.1f,
                    vignette = 0.25f,
                    hueShift = 0.0f,
                    suggestedTransition = "Zoom Blur",
                    explanation = "[Local AI Simulation] Subtle balancing of contrasts and soft highlights to deliver a clean polished commercial look."
                )
            }
        }
    }
}
