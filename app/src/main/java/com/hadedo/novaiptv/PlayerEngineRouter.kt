package com.hadedo.novaiptv

enum class PlayerEngine {
    MEDIA3_EXOPLAYER,
    LIB_VLC,
    NATIVE_EXTERNAL
}

data class PlaybackConfig(
    val engine: PlayerEngine,
    val enableLowLatency: Boolean,
    val preferHighestBitrate: Boolean,
    val tuningConfig: AdaptiveTuningConfig? = null
)

fun EngineType.toPlayerEngine(): PlayerEngine = when (this) {
    EngineType.LIBVLC_ONLY -> PlayerEngine.LIB_VLC
    EngineType.MEDIA3_ONLY, EngineType.AUTO_HYBRID -> PlayerEngine.MEDIA3_EXOPLAYER
}

fun ExternalPlayerMode.toPlayerEngine(): PlayerEngine = when (this) {
    ExternalPlayerMode.EXTERNAL_CHOOSER -> PlayerEngine.NATIVE_EXTERNAL
    ExternalPlayerMode.INTERNAL -> PlayerEngine.MEDIA3_EXOPLAYER
}

object PlayerEngineRouter {

    fun resolvePlaybackConfig(
        isSportsSection: Boolean,
        preferredEngineType: EngineType = EngineType.AUTO_HYBRID,
        externalPlayerMode: ExternalPlayerMode = ExternalPlayerMode.INTERNAL,
        userLowLatencyEnabled: Boolean = true,
        manualOverrideEngine: PlayerEngine? = null,
        media3FailedFallback: Boolean = false,
        tuningConfig: AdaptiveTuningConfig? = null
    ): PlaybackConfig {
        val effectiveLowLatency = if (media3FailedFallback) {
            false
        } else if (isSportsSection) {
            userLowLatencyEnabled
        } else {
            userLowLatencyEnabled && (tuningConfig?.tier != PerformanceTier.LOW_TIER)
        }

        if (externalPlayerMode == ExternalPlayerMode.EXTERNAL_CHOOSER && manualOverrideEngine == null && !media3FailedFallback) {
            return PlaybackConfig(
                engine = PlayerEngine.NATIVE_EXTERNAL,
                enableLowLatency = effectiveLowLatency,
                preferHighestBitrate = isSportsSection,
                tuningConfig = tuningConfig
            )
        }

        if (manualOverrideEngine != null) {
            return PlaybackConfig(
                engine = manualOverrideEngine,
                enableLowLatency = effectiveLowLatency,
                preferHighestBitrate = isSportsSection,
                tuningConfig = tuningConfig
            )
        }

        if (media3FailedFallback) {
            return PlaybackConfig(
                engine = PlayerEngine.LIB_VLC,
                enableLowLatency = false,
                preferHighestBitrate = true,
                tuningConfig = tuningConfig
            )
        }

        return PlaybackConfig(
            engine = preferredEngineType.toPlayerEngine(),
            enableLowLatency = effectiveLowLatency,
            preferHighestBitrate = isSportsSection,
            tuningConfig = tuningConfig
        )
    }
}
