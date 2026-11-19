package com.hadedo.novaiptv

import android.app.ActivityManager
import android.content.Context
import android.os.Build

enum class PerformanceTier {
    LOW_TIER,
    MID_TIER,
    HIGH_TIER
}

enum class PerformanceMode(
    val labelAr: String,
    val labelEn: String,
    val tierOverride: PerformanceTier?
) {
    AUTO_SMART("تلقائي ذكي (موازنة العتاد والنظام)", "Smart Auto (Device & OS Balanced)", null),
    ECO_LOW("اقتصادي (أجهزة ضعيفة / أندرويد قديم)", "Eco (Low-End / Legacy Android)", PerformanceTier.LOW_TIER),
    BALANCED("متوازن (استقرار وسلاسة يومية)", "Balanced (Daily Stability)", PerformanceTier.MID_TIER),
    ULTRA_HIGH("فائق (أجهزة قوية / أندرويد حديث)", "Ultra (Flagship / Modern Android)", PerformanceTier.HIGH_TIER)
}

data class AdaptiveTuningConfig(
    val tier: PerformanceTier,
    val minBufferMs: Int,
    val maxBufferMs: Int,
    val bufferForPlaybackMs: Int,
    val bufferForPlaybackAfterRebufferMs: Int,
    val connectTimeoutMs: Int,
    val readTimeoutMs: Int,
    val vlcNetworkCachingMs: Int,
    val vlcLiveCachingMs: Int,
    val preferHardwareDecoder: Boolean,
    val enableHeavyVisualEffects: Boolean
)

object DevicePerformanceManager {

    fun detectTier(context: Context): PerformanceTier {
        return try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager?.getMemoryInfo(memInfo)

            val totalRamGb = if (memInfo.totalMem > 0L) {
                memInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
            } else {
                4.0
            }
            val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
            val sdk = Build.VERSION.SDK_INT
            val isLowRamDevice = actManager?.isLowRamDevice == true

            when {
                isLowRamDevice || sdk < Build.VERSION_CODES.Q || totalRamGb < 3.5 || cores <= 4 ->
                    PerformanceTier.LOW_TIER

                sdk >= Build.VERSION_CODES.TIRAMISU && totalRamGb >= 7.5 && cores >= 8 ->
                    PerformanceTier.HIGH_TIER

                else ->
                    PerformanceTier.MID_TIER
            }
        } catch (_: Exception) {
            PerformanceTier.MID_TIER
        }
    }

    fun buildTuningConfig(
        context: Context,
        isVodOrSeries: Boolean,
        performanceMode: PerformanceMode = PerformanceMode.AUTO_SMART,
        lowLatency: Boolean = true
    ): AdaptiveTuningConfig {
        val tier = performanceMode.tierOverride ?: detectTier(context)

        val base = when (tier) {
            PerformanceTier.LOW_TIER -> AdaptiveTuningConfig(
                tier = tier,
                minBufferMs = if (isVodOrSeries) 8_000 else 4_000,
                maxBufferMs = if (isVodOrSeries) 20_000 else 10_000,
                bufferForPlaybackMs = 1_200,
                bufferForPlaybackAfterRebufferMs = 2_500,
                connectTimeoutMs = 8_000,
                readTimeoutMs = 15_000,
                vlcNetworkCachingMs = if (isVodOrSeries) 2_200 else 1_500,
                vlcLiveCachingMs = 1_500,
                preferHardwareDecoder = true,
                enableHeavyVisualEffects = false
            )

            PerformanceTier.MID_TIER -> AdaptiveTuningConfig(
                tier = tier,
                minBufferMs = if (isVodOrSeries) 15_000 else 8_000,
                maxBufferMs = if (isVodOrSeries) 40_000 else 22_000,
                bufferForPlaybackMs = 1_800,
                bufferForPlaybackAfterRebufferMs = 3_500,
                connectTimeoutMs = 10_000,
                readTimeoutMs = 22_000,
                vlcNetworkCachingMs = if (isVodOrSeries) 3_500 else 2_200,
                vlcLiveCachingMs = 2_200,
                preferHardwareDecoder = true,
                enableHeavyVisualEffects = true
            )

            PerformanceTier.HIGH_TIER -> AdaptiveTuningConfig(
                tier = tier,
                minBufferMs = if (isVodOrSeries) 25_000 else 12_000,
                maxBufferMs = if (isVodOrSeries) 75_000 else 35_000,
                bufferForPlaybackMs = 2_200,
                bufferForPlaybackAfterRebufferMs = 4_000,
                connectTimeoutMs = 12_000,
                readTimeoutMs = 30_000,
                vlcNetworkCachingMs = if (isVodOrSeries) 5_000 else 3_000,
                vlcLiveCachingMs = 3_000,
                preferHardwareDecoder = true,
                enableHeavyVisualEffects = true
            )
        }

        return if (!isVodOrSeries && lowLatency) {
            base.copy(
                minBufferMs = (base.minBufferMs * 0.75f).toInt().coerceAtLeast(2_500),
                bufferForPlaybackMs = (base.bufferForPlaybackMs * 0.75f).toInt().coerceAtLeast(800),
                vlcLiveCachingMs = (base.vlcLiveCachingMs * 0.75f).toInt().coerceAtLeast(1_000)
            )
        } else {
            base
        }
    }
}
