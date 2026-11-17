package app.revanced.patches.materialyoutok

import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXT = "Lapp/revanced/extension/materialyoutok/RegionSpoof;"

private val targets = mapOf(
    "getSimCountryIso" to "simCountryIso",
    "getNetworkCountryIso" to "networkCountryIso",
    "getSimOperator" to "simOperator",
    "getNetworkOperator" to "networkOperator",
)

private fun targetOf(ins: Any): String? {
    if (ins !is ReferenceInstruction || ins !is FiveRegisterInstruction) return null
    if (ins.opcode != Opcode.INVOKE_VIRTUAL) return null
    val ref = ins.reference as? MethodReference ?: return null
    if (ref.definingClass != "Landroid/telephony/TelephonyManager;") return null
    return targets[ref.name]
}

@Suppress("unused")
val materialYouTokRegionPatch = bytecodePatch(
    name = "MaterialYouTok region",
    description = "Spoofs SIM and network region (default: Belarus).",
) {
    compatibleWith("com.zhiliaoapp.musically")

    extendWith("extensions/extension.rve")

    execute {
        classDefs.toList().forEach { original ->
            val needsPatch = original.methods.any { m ->
                m.implementation?.instructions?.any { targetOf(it) != null } == true
            }
            if (!needsPatch) return@forEach

            val mutable = classDefs.getOrReplaceMutable(original)
            mutable.methods.forEach { method ->
                val impl = method.implementation ?: return@forEach
                impl.instructions.toList().forEachIndexed { index, ins ->
                    val replacement = targetOf(ins) ?: return@forEachIndexed
                    val reg = (ins as FiveRegisterInstruction).registerC
                    method.replaceInstruction(
                        index,
                        "invoke-static { v$reg }, $EXT->$replacement(Landroid/telephony/TelephonyManager;)Ljava/lang/String;",
                    )
                }
            }
        }
    }
}
