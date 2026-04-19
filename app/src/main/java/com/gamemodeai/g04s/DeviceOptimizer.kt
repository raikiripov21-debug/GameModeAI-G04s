package com.gamemodeai.g04s

import android.os.Process
import kotlinx.coroutines.*
import java.io.File

/** Motorola G04s Device Optimizer — low-end adaptive calibration, completely independent */
class DeviceOptimizer {
    private var adaptiveDelayMs = 4_000L
    private val DELAY_MIN_MS    = 3_000L
    private val DELAY_MAX_MS    = 5_500L
    private val THERMAL_SAFE_C  = 38.0f
    private val ANTI_SPAM_MS    = 8_000L
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    @Volatile private var active = false
    private var cycle = 0

    fun onGameStarted() { active = true; adaptiveDelayMs = DELAY_MIN_MS; cycle = 0; scope.launch { loop() } }
    fun onGameStopped() { active = false; scope.coroutineContext.cancelChildren(); Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT) }

    private suspend fun loop() {
        while (active) {
            val temp = readThermalC()
            if (temp < THERMAL_SAFE_C) {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                if (++cycle % 5 == 0) System.gc()
                adaptiveDelayMs = when {
                    temp < 32f -> DELAY_MIN_MS
                    temp < 35f -> 3_500L
                    temp < 38f -> 4_500L
                    else       -> DELAY_MAX_MS
                }
            } else {
                adaptiveDelayMs = DELAY_MAX_MS
                delay(ANTI_SPAM_MS)
            }
            delay(adaptiveDelayMs)
        }
    }
    private fun readThermalC() = try { val f = File("/sys/class/thermal/thermal_zone0/temp"); if (f.exists()) f.readText().trim().toFloat() / 1000f else 32f } catch (e: Exception) { 32f }
}
