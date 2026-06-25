package com.coffeemark.app.ui.brewguide

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.coffeemark.app.CoffeemarkApp
import com.coffeemark.app.data.entity.RecipeEntity
import com.coffeemark.app.data.entity.RecipeStepEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class BrewGuideState(
    val recipe: RecipeEntity? = null,
    val steps: List<RecipeStepEntity> = emptyList(),
    val currentStepIndex: Int = 0,
    val totalElapsedMs: Long = 0L,
    val stepElapsedMs: Long = 0L,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false,
    val isLoading: Boolean = true,
    val soundStatus: String = "🔊 提示音就绪",
    val countdownNumber: Int = 0  // 0=不在倒计时, 3/2/1=开场倒计时
) {
    val currentStep: RecipeStepEntity?
        get() = if (currentStepIndex in steps.indices) steps[currentStepIndex] else null

    val currentStepRemainingMs: Long
        get() {
            val step = currentStep ?: return 0
            return (step.duration * 1000L - stepElapsedMs).coerceAtLeast(0)
        }

    val totalTargetWater: Double
        get() = steps.sumOf { it.waterAmount }

    val stepProgress: Float
        get() {
            val step = currentStep ?: return 0f
            val total = step.duration * 1000L
            if (total == 0L) return 1f
            return (stepElapsedMs.toFloat() / total).coerceAtMost(1f)
        }
}

class BrewGuideViewModel(private val recipeId: String) : ViewModel() {

    private val recipeDao = CoffeemarkApp.instance.database.recipeDao()
    private val stepDao = CoffeemarkApp.instance.database.recipeStepDao()

    private val _state = MutableStateFlow(BrewGuideState())
    val state: StateFlow<BrewGuideState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var soundOk = true
    private var pendingStart = false

    // 引导开始的时间戳（用于精确计时）
    private var guideStartRealtime: Long = 0L
    private var currentStepStartRealtime: Long = 0L

    init {
        viewModelScope.launch {
            val recipe = recipeDao.getById(recipeId)
            val steps = stepDao.getByRecipeIdOnce(recipeId)
            _state.update { it.copy(recipe = recipe, steps = steps, isLoading = false) }
            if (pendingStart) {
                pendingStart = false
                doStart()
            }
        }
    }

    /** 初始化音效（AudioTrack 纯 PCM 正弦波，不依赖任何引擎） */
    fun initSound() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                playTone(800.0, 30)
                soundOk = true
                _state.update { it.copy(soundStatus = "🔊 提示音就绪") }
            } catch (e: Exception) {
                soundOk = false
                _state.update { it.copy(soundStatus = "🔇 提示音不可用") }
            }
        }
    }

    fun start() {
        if (_state.value.isLoading) {
            // 数据还没加载完，等 init 完成后自动调用 doStart()
            pendingStart = true
            return
        }
        doStart()
    }

    private fun doStart() {
        if (_state.value.steps.isEmpty()) return
        // 开场 3-2-1 倒计时（在 timerJob 内处理，不阻塞 UI）
        timerJob?.cancel()
        timerJob = viewModelScope.launch(Dispatchers.Main) {
            // ── 3-2-1 倒计时 ──
            for (n in 3 downTo 1) {
                _state.update { it.copy(countdownNumber = n) }
                beep(800.0, 60)
                delay(900) // 间隔 ~1秒
            }
            _state.update { it.copy(countdownNumber = 0) }
            beep(1200.0, 120) // 最后一声"叮"
            delay(400)

            // ── 正式开始 ──
            guideStartRealtime = System.currentTimeMillis()
            currentStepStartRealtime = guideStartRealtime
            _state.update { it.copy(isRunning = true, currentStepIndex = 0, stepElapsedMs = 0L, totalElapsedMs = 0L) }
            playStepStart()
            startTimerLoop()
        }
    }

    private suspend fun CoroutineScope.startTimerLoop() {
        while (isActive && _state.value.isRunning) {
            delay(100)
            val now = System.currentTimeMillis()
            val totalElapsed = now - guideStartRealtime
            val stepElapsed = now - currentStepStartRealtime

            _state.update { it.copy(totalElapsedMs = totalElapsed, stepElapsedMs = stepElapsed) }

            val currentStep = _state.value.currentStep
            if (currentStep != null && stepElapsed >= currentStep.duration * 1000L) {
                advanceStep()
            }
        }
    }

    private suspend fun advanceStep() {
        val s = _state.value
        val nextIndex = s.currentStepIndex + 1

        if (nextIndex >= s.steps.size) {
            // 全部完成：先响结束音 + 完成旋律，再标记完成，最后取消 timer
            playStepEnd()
            delay(100)
            playCompletionSound()
            _state.update { it.copy(isRunning = false, isFinished = true) }
            timerJob?.cancel()
            return
        }

        // 先响结束音 → 稍停 → 开始音 → 再切步
        playStepEnd()
        delay(100)
        playStepStart()

        currentStepStartRealtime = System.currentTimeMillis()
        _state.update { it.copy(currentStepIndex = nextIndex, stepElapsedMs = 0L) }
    }

    // ── 内置音效（AudioTrack 纯 PCM 正弦波）──

    private suspend fun playStepStart() {
        if (!soundOk) return
        withContext(Dispatchers.IO) {
            try {
                playTone(600.0, 80)
                delay(60)
                playTone(900.0, 120)
            } catch (_: Exception) {}
        }
    }

    private suspend fun playStepEnd() {
        if (!soundOk) return
        withContext(Dispatchers.IO) {
            try { playTone(1200.0, 150) } catch (_: Exception) {}
        }
    }

    /** 单声短鸣（用于倒计时） */
    private suspend fun beep(freq: Double, ms: Int) {
        if (!soundOk) return
        withContext(Dispatchers.IO) {
            try { playTone(freq, ms) } catch (_: Exception) {}
        }
    }

    /** 冲煮完成：上行旋律（C-E-G-C 琶音） */
    private suspend fun playCompletionSound() {
        if (!soundOk) return
        withContext(Dispatchers.IO) {
            try {
                // C5 - E5 - G5 - C6 上行，每音 ~150ms
                playTone(523.25, 160)
                Thread.sleep(80)
                playTone(659.25, 160)
                Thread.sleep(80)
                playTone(783.99, 160)
                Thread.sleep(80)
                playTone(1046.50, 350)
            } catch (_: Exception) {}
        }
    }

    /** 播放指定频率和时长的正弦波（必须在 IO 线程调用） */
    private fun playTone(frequencyHz: Double, durationMs: Int) {
        val sampleRate = 44100
        val numSamples = (durationMs * sampleRate / 1000.0).toInt().coerceAtLeast(1)
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val angle = 2.0 * Math.PI * i * frequencyHz / sampleRate
            val envelope = Math.exp(-3.0 * i / numSamples)
            samples[i] = (Short.MAX_VALUE * 0.84 * Math.sin(angle) * envelope).toInt().toShort()
        }
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build())
            .setAudioFormat(AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build())
            .setBufferSizeInBytes(numSamples * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        audioTrack.write(samples, 0, numSamples)
        audioTrack.play()
        Thread.sleep((durationMs + 30).toLong())
        audioTrack.stop()
        audioTrack.release()
    }

    fun getPrefillData(): BrewGuidePrefillData {
        val s = _state.value
        val recipe = s.recipe ?: return BrewGuidePrefillData()
        return BrewGuidePrefillData(
            recipeId = recipe.id,
            totalWater = recipe.totalWater,
            waterTemp = recipe.waterTemp,
            grindSize = recipe.grindSize,
            device = recipe.device,
            totalDuration = (s.totalElapsedMs / 1000).toInt(),
            groundWeight = recipe.beanWeight
        )
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    class Factory(private val recipeId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = BrewGuideViewModel(recipeId) as T
    }
}

data class BrewGuidePrefillData(
    val recipeId: String = "",
    val totalWater: Double = 0.0,
    val waterTemp: Int = 92,
    val grindSize: com.coffeemark.app.data.enums.GrindSize = com.coffeemark.app.data.enums.GrindSize.MEDIUM,
    val device: String = "",
    val totalDuration: Int = 0,
    val groundWeight: Double = 0.0
)
