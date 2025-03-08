package app.revanced.patches.youtube.video.speed.custom

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.patch.annotation.Version
import app.revanced.patches.shared.settings.preference.impl.StringResource
import app.revanced.patches.shared.settings.preference.impl.SwitchPreference
import app.revanced.patches.youtube.misc.settings.SettingsPatch

@Patch(
    name = "Dynamic playback speed",
    description = "Adds ability to change playback speed dynamically by holding speed button.",
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
                "18.32.39",
                "18.33.40",
                "18.34.38",
                "18.35.36",
                "18.36.39"
            ]
        )
    ],
    dependencies = [SettingsPatch::class]
)
@Version("0.0.2")
class CustomPlaybackSpeedPatch : BytecodePatch(
    setOf(
        SpeedControlGestureFingerprint,
        SpeedResetFingerprint,
        SpeedValueFingerprint,
        SpeedLimiterFingerprint,
        SpeedMenuFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {
        // Add settings preferences
        SettingsPatch.addPreference(
            SwitchPreference(
                "revanced_dynamic_player_speed",
                StringResource("revanced_dynamic_speed_enabled", "Enable dynamic speed control"),
                StringResource("revanced_dynamic_speed_enabled_summary", "Hold speed button to change playback speed"),
                true
            )
        )

        SettingsPatch.addPreference(
            SwitchPreference(
                "revanced_speed_multiplier",
                StringResource("revanced_speed_multiplier", "Speed up multiplier"),
                StringResource("revanced_speed_multiplier_summary", "Multiplier used when speeding up video (1.1-4.0)"),
                2.0f
            )
        )

        SettingsPatch.addPreference(
            SwitchPreference(
                "revanced_speed_divider",
                StringResource("revanced_speed_divider", "Slow down divider"),
                StringResource("revanced_speed_divider_summary", "Divider used when slowing down video (1.1-4.0)"),
                2.0f
            )
        )

        // Implement fingerprints
        SpeedControlGestureFingerprint.result?.let { result ->
            result.mutableMethod.apply {
                addInstructions(
                    0,
                    """
                    invoke-static {}, Lapp/revanced/patches/youtube/video/speed/custom/CustomPlaybackSpeedPatch;->handleSpeedControl()V
                    """
                )
            }
        } ?: throw SpeedControlGestureFingerprint.exception

        SpeedResetFingerprint.result?.let { result ->
            result.mutableMethod.apply {
                addInstructions(
                    0,
                    """
                    invoke-static {}, Lapp/revanced/patches/youtube/video/speed/custom/CustomPlaybackSpeedPatch;->resetSpeed()V
                    """
                )
            }
        } ?: throw SpeedResetFingerprint.exception
    }

    companion object {
        private const val PLAYBACK_SPEED_DEFAULT = 1.0f
        private const val PLAYBACK_SPEED_MIN = 0.0625f
        private const val PLAYBACK_SPEED_MAX = 8.0f

        @JvmStatic
        private fun handleSpeedControl() {
            if (!isEnabled()) return
            
            val currentSpeed = getCurrentPlaybackSpeed()
            val newSpeed = calculateNewSpeed(currentSpeed)
            setPlaybackSpeed(newSpeed)
        }

        @JvmStatic
        private fun resetSpeed() {
            if (isEnabled()) {
                setPlaybackSpeed(PLAYBACK_SPEED_DEFAULT)
            }
        }

        private fun isEnabled() = 
            SettingsPatch.getBooleanSetting("revanced_dynamic_player_speed", true)

        private fun getSpeedMultiplier() = 
            SettingsPatch.getFloatSetting("revanced_speed_multiplier", 2.0f)
                .coerceIn(1.1f, 4.0f)

        private fun getSpeedDivider() = 
            SettingsPatch.getFloatSetting("revanced_speed_divider", 2.0f)
                .coerceIn(1.1f, 4.0f)

        private fun calculateNewSpeed(currentSpeed: Float): Float {
            val multiplier = if (isSpeedUpGesture()) {
                getSpeedMultiplier()
            } else {
                1f / getSpeedDivider()
            }
            
            return (currentSpeed * multiplier)
                .coerceIn(PLAYBACK_SPEED_MIN, PLAYBACK_SPEED_MAX)
        }
    }
}