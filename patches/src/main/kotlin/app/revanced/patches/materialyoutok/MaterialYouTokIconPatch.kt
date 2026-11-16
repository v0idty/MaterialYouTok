package app.revanced.patches.materialyoutok

import app.revanced.patcher.patch.resourcePatch

@Suppress("unused")
val materialYouTokIconPatch = resourcePatch(
    name = "MaterialYouTok icon",
    description = "Replaces the app icon with MaterialYouTok.",
) {
    compatibleWith("com.zhiliaoapp.musically")

    execute {
        val icon = object {}.javaClass
            .getResourceAsStream("/materialyoutok/ic_launcher.png")!!
            .readBytes()

        listOf("mipmap-xxxhdpi", "mipmap-xxhdpi", "mipmap-xhdpi").forEach { dir ->
            val file = get("res/$dir/ic_launcher.png")
            if (file.exists()) file.writeBytes(icon)
        }
    }
}
