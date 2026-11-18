package app.revanced.patches.materialyoutok

import app.revanced.patcher.patch.resourcePatch
import org.w3c.dom.Element
import java.io.File
import java.nio.charset.StandardCharsets

private val pngSignature = byteArrayOf(
    0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
)

/**
 * Apktool can expose bitmap payloads from some APKs with a .png suffix even
 * when the encoded file is actually WebP/JPEG/GIF. AAPT2 selects its decoder
 * by extension and rejects those files as invalid PNGs. Keep the Android
 * resource name unchanged, but restore the extension that matches the bytes.
 */
private fun normalizeMislabeledPngResources(resources: File): Int {
    check(resources.isDirectory) {
        "Decoded Android resources directory is missing: ${resources.path}"
    }

    var renamedCount = 0
    resources.walkTopDown()
        .filter {
            it.isFile &&
                it.name.endsWith(".png", ignoreCase = true) &&
                !it.name.endsWith(".9.png", ignoreCase = true)
        }
        .toList()
        .forEach { file ->
            val header = file.inputStream().use { it.readNBytes(12) }

            val actualExtension = when {
                header.size >= pngSignature.size &&
                    header.copyOfRange(0, pngSignature.size).contentEquals(pngSignature) -> null

                header.size >= 12 &&
                    String(header, 0, 4, StandardCharsets.US_ASCII) == "RIFF" &&
                    String(header, 8, 4, StandardCharsets.US_ASCII) == "WEBP" -> "webp"

                header.size >= 3 &&
                    (header[0].toInt() and 0xFF) == 0xFF &&
                    (header[1].toInt() and 0xFF) == 0xD8 &&
                    (header[2].toInt() and 0xFF) == 0xFF -> "jpg"

                header.size >= 6 && (
                    String(header, 0, 6, StandardCharsets.US_ASCII) == "GIF87a" ||
                        String(header, 0, 6, StandardCharsets.US_ASCII) == "GIF89a"
                    ) -> "gif"

                else -> {
                    val hex = header.joinToString(" ") {
                        "%02x".format(it.toInt() and 0xFF)
                    }
                    error(
                        "Resource is named ${file.name}, but its content is not PNG, " +
                            "WebP, JPEG, or GIF (header: $hex)."
                    )
                }
            }

            if (actualExtension != null) {
                val correctedFile = File(
                    file.parentFile,
                    "${file.nameWithoutExtension}.$actualExtension"
                )
                check(!correctedFile.exists()) {
                    "Cannot rename ${file.path}: ${correctedFile.name} already exists."
                }
                check(file.renameTo(correctedFile)) {
                    "Could not correct image extension for ${file.path}."
                }
                renamedCount++
            }
        }

    return renamedCount
}

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
            icon.size >= pngSignature.size &&
                icon.copyOfRange(0, pngSignature.size).contentEquals(pngSignature)
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
        val normalizedImages = normalizeMislabeledPngResources(res)
        if (normalizedImages > 0) {
            println(
                "Corrected extensions for $normalizedImages mislabeled bitmap resources " +
                    "before AAPT2 resource compilation."
            )
        }

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
