package app.revanced.patches.materialyoutok

import app.revanced.patcher.patch.resourcePatch
import org.w3c.dom.Element
import java.io.File

@Suppress("unused")
val materialYouTokIconPatch = resourcePatch(
    name = "MaterialYouTok icon",
    description = "Replaces the app launcher icon and name with MaterialYouTok.",
) {
    compatibleWith("com.zhiliaoapp.musically")

    execute {
        val icon = object {}.javaClass
            .getResourceAsStream("/materialyoutok/ic_launcher.png")
            ?.use { it.readBytes() }
            ?: error("MaterialYouTok launcher image is missing from the patches artifact.")

        require(
            icon.size >= 8 && icon.copyOfRange(0, 8).contentEquals(
                byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
            )
        ) { "MaterialYouTok launcher image is not a valid PNG." }

        val androidNs = "http://schemas.android.com/apk/res/android"
        val refs = linkedSetOf<String>()

        document("AndroidManifest.xml").use { doc ->
            val app = doc.getElementsByTagName("application").item(0) as? Element
                ?: error("AndroidManifest.xml has no application element.")

            app.setAttributeNS(androidNs, "android:label", "MaterialYouTok")
            listOf("android:icon", "android:roundIcon").forEach { attr ->
                val value = app.getAttribute(attr)
                if (value.startsWith("@")) refs.add(value)
            }

            listOf("activity", "activity-alias").forEach { tag ->
                val nodes = doc.getElementsByTagName(tag)
                for (i in 0 until nodes.length) {
                    val element = nodes.item(i) as Element
                    val categories = element.getElementsByTagName("category")
                    val isLauncher = (0 until categories.length).any {
                        (categories.item(it) as Element).getAttribute("android:name") ==
                            "android.intent.category.LAUNCHER"
                    }
                    if (!isLauncher) continue

                    // Make launcher aliases use the same visible app name and image.
                    element.setAttributeNS(androidNs, "android:label", "MaterialYouTok")
                    listOf("android:icon", "android:roundIcon").forEach { attr ->
                        val value = element.getAttribute(attr)
                        if (value.startsWith("@")) refs.add(value)
                    }
                }
            }
        }

        require(refs.isNotEmpty()) {
            "No launcher icon resource references were found in AndroidManifest.xml."
        }

        val res = get("res")
        refs.forEach { ref ->
            val body = ref.removePrefix("@")
            val type = body.substringBefore('/')
            val name = body.substringAfter('/', "")
            require(type.isNotBlank() && name.isNotBlank()) {
                "Unsupported launcher icon resource reference: $ref"
            }

            // Remove all density/adaptive variants so Android cannot keep selecting the old icon.
            res.listFiles()
                ?.filter { it.isDirectory && it.name.startsWith(type) }
                ?.forEach { dir ->
                    dir.listFiles()
                        ?.filter { it.isFile && it.nameWithoutExtension == name }
                        ?.forEach { it.delete() }
                }

            val outDir = File(res, "$type-xxxhdpi")
            check(outDir.exists() || outDir.mkdirs()) {
                "Could not create launcher resource directory: ${outDir.path}"
            }
            val output = File(outDir, "$name.png")
            output.writeBytes(icon)
            check(output.isFile && output.length() == icon.size.toLong()) {
                "Failed to write MaterialYouTok launcher icon to ${output.path}"
            }
        }
    }
}
