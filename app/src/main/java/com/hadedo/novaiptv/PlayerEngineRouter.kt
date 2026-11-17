package com.hadedo.novaiptv

enum class PlayerEngine {
    MEDIA3_EXOPLAYER,
    LIB_VLC,
    NATIVE_EXTERNAL
}

data class PlaybackConfig(
    val engine: PlayerEngine,
    val enableLowLatency: Boolean,
    val preferHighestBitrate: Boolean
)

object PlayerEngineRouter {

    fun resolvePlaybackConfig(
        isSportsSection: Boolean,
        manualOverrideEngine: PlayerEngine? = null,
        media3FailedFallback: Boolean = false
    ): PlaybackConfig {
        if (manualOverrideEngine != null) {
            return PlaybackConfig(
                engine = manualOverrideEngine,
                enableLowLatency = isSportsSection,
                preferHighestBitrate = isSportsSection
            )
        }

        if (media3FailedFallback) {
            return PlaybackConfig(
                engine = PlayerEngine.LIB_VLC,
                enableLowLatency = false,
                preferHighestBitrate = true
            )
        }

        return PlaybackConfig(
            engine = PlayerEngine.MEDIA3_EXOPLAYER,
            enableLowLatency = isSportsSection,
            preferHighestBitrate = isSportsSection
        )
    }
}
