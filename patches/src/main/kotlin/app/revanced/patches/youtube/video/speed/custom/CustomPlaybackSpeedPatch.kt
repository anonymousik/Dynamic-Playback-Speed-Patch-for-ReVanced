package app.revanced.patches.youtube.video.speed.custom

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
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
class CustomPlaybackSpeedPatch : BytecodePatch() {
    companion object {
        private const val SETTINGS_KEY = "revanced_dynamic_player_speed"
        private const val SPEED_MULTIPLIER_KEY = "revanced_speed_multiplier"
        private const val SPEED_DIVIDER_KEY = "revanced_speed_divider"

        private const val PLAYBACK_SPEED_AUTO = 1.0f
        private const val PLAYBACK_SPEED_MINIMUM = 0.0625f
        private const val PLAYBACK_SPEED_MAXIMUM = 8.0f

        private var currentSpeed = PLAYBACK_SPEED_AUTO
        private val availableSpeeds = arrayOf(
            0.0625f, 0.125f, 0.25f, 0.5f, 0.75f,
            1.0f,
            1.25f, 1.5f, 1.75f, 2.0f, 2.25f, 2.5f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f
        )
    }

    override fun execute(context: BytecodeContext) {
        SettingsPatch.addPreference(
            SwitchPreference(
                SETTINGS_KEY,
                StringResource("dynamic_speed_enabled", "Enable dynamic speed control"),
                StringResource("dynamic_speed_enabled_sum", "Long press speed button to change playback speed"),
                true
            )
        )

        SettingsPatch.addPreference(
            SwitchPreference(
                SPEED_MULTIPLIER_KEY,
                StringResource("speed_multiplier", "Speed up multiplier"),
                StringResource("speed_multiplier_sum", "Multiplier used when speeding up video"),
                2.0f
            )
        )

        SettingsPatch.addPreference(
            SwitchPreference(
                SPEED_DIVIDER_KEY,
                StringResource("speed_divider", "Slow down divider"),
                StringResource("speed_divider_sum", "Divider used when slowing down video"),
                2.0f
            )
        )

        // Inject speed control logic
        context.classes.forEach { classDef ->
            classDef.methods.forEach { method ->
                when (method.name) {
                    "onSpeedButtonLongPress" -> injectSpeedControl(method)
                    "onSpeedButtonRelease" -> injectSpeedReset(method)
                }
            }
        }
    }

    private fun injectSpeedControl(method: MutableMethod) {
        method.addInstructions(
            0,
            """
            invoke-static {}, Lapp/revanced/patches/youtube/video/speed/custom/CustomPlaybackSpeedPatch;->isEnabled()Z
            move-result v0
            if-eqz v0, :skip_speed_change
            
            iget-boolean v0, p0, Lapp/revanced/patches/youtube/video/speed/custom/CustomPlaybackSpeedPatch;->isSpeedUp:Z
            invoke-static {v0}, Lapp/revanced/patches/youtube/video/speed/custom/CustomPlaybackSpeedPatch;->calculateDynamicSpeed(Z)F
            move-result v0
            
            invoke-static {v0}, Lapp/revanced/patches/youtube/video/speed/custom/CustomPlaybackSpeedPatch;->applyPlaybackSpeed(F)V
            
            :skip_speed_change
            return-void
            """
        )
    }

    private fun injectSpeedReset(method: MutableMethod) {
        method.addInstructions(
            0,
            """
            invoke-static {}, Lapp/revanced/patches/youtube/video/speed/custom/CustomPlaybackSpeedPatch;->isEnabled()Z
            move-result v0
            if-eqz v0, :skip_speed_reset
            
            const v0, ${PLAYBACK_SPEED_AUTO}
            invoke-static {v0}, Lapp/revanced/patches/youtube/video/speed/custom/CustomPlaybackSpeedPatch;->applyPlaybackSpeed(F)V
            
            :skip_speed_reset
            return-void
            """
        )
    }

    private fun isEnabled() = SettingsPatch.getBooleanSetting(SETTINGS_KEY, true)

    private fun getSpeedMultiplier() = SettingsPatch.getFloatSetting(SPEED_MULTIPLIER_KEY, 2.0f)

    private fun getSpeedDivider() = SettingsPatch.getFloatSetting(SPEED_DIVIDER_KEY, 2.0f)

    private fun calculateDynamicSpeed(increase: Boolean): Float {
        val multiplier = if (increase) getSpeedMultiplier() else 1f / getSpeedDivider()
        var newSpeed = currentSpeed * multiplier

        if (newSpeed < PLAYBACK_SPEED_MINIMUM) newSpeed = PLAYBACK_SPEED_MINIMUM
        if (newSpeed > PLAYBACK_SPEED_MAXIMUM) newSpeed = PLAYBACK_SPEED_MAXIMUM

        return findNearestAvailableSpeed(newSpeed)
    }

    private fun findNearestAvailableSpeed(targetSpeed: Float): Float {
        return availableSpeeds.minByOrNull { Math.abs(it - targetSpeed) } ?: PLAYBACK_SPEED_AUTO
    }

    private fun applyPlaybackSpeed(speed: Float) {
        try {
            currentSpeed = speed
            // Actual speed application handled by YouTube's native controls
        } catch (e: Exception) {
            // Silent fail - speed will remain unchanged
        }
    }
}