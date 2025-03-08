package app.revanced.patches.youtube.video.speed.custom

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patcher.extensions.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object SpeedControlGestureFingerprint : MethodFingerprint(
    returnType = "V",
    access = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf(),
    opcodes = listOf(
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.IF_EQZ
    ),
    strings = listOf("menu_item_playback_speed"),
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass.endsWith("/YouTubePlayerOverlay;") &&
        methodDef.name == "onSpeedGestureDetected"
    }
)

internal object SpeedResetFingerprint : MethodFingerprint(
    returnType = "V",
    access = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf(),
    opcodes = listOf(
        Opcode.IGET_OBJECT,
        Opcode.CONST_HIGH16,
        Opcode.INVOKE_VIRTUAL
    ),
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass.endsWith("/YouTubePlayerOverlay;") &&
        methodDef.name == "onSpeedGestureReleased"
    }
)

internal object SpeedValueFingerprint : MethodFingerprint(
    returnType = "F",
    access = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf(),
    opcodes = listOf(
        Opcode.IGET,
        Opcode.RETURN
    ),
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass.endsWith("/YouTubePlayerController;") &&
        methodDef.name == "getPlaybackSpeed"
    }
)

internal object SpeedLimiterFingerprint : MethodFingerprint(
    returnType = "V",
    access = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("F"),
    opcodes = listOf(
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
        Opcode.IF_EQZ,
        Opcode.CONST_HIGH16,
        Opcode.GOTO,
        Opcode.CONST_HIGH16,
        Opcode.INVOKE_STATIC
    )
)

internal object SpeedMenuFingerprint : MethodFingerprint(
    returnType = "V",
    access = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Landroid/view/View;"),
    strings = listOf("menu_item_playback_speed")
)