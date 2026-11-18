package app.revanced.patches.materialyoutok

import app.revanced.patcher.patch.resourcePatch
import org.w3c.dom.Element
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * TikTok 47.2.41 contains zero-byte PNG placeholders in drawable-xxhdpi.
 * AAPT2 cannot compile an empty drawable. Keep each resource name and ID,
 * but give only those empty drawable PNGs a valid transparent 1x1 image.
 *
 * Do not touch res/raw: the APK contains many intentionally empty raw entries.
 * Do not rewrite any non-empty image.
 */
private fun replaceEmptyDrawablePngs(resources: File): Int {
    check(resources.isDirectory) {
        "Decoded Android resources directory is missing: ${resources.path}"
    }

    val drawableDirectories = resources.listFiles()
        ?.filter {
            it.isDirectory &&
                (it.name == "drawable" || it.name.startsWith("drawable-"))
        }
        .orEmpty()

    val emptyPngs = drawableDirectories.flatMap { directory ->
        directory.walkTopDown()
            .filter {
                it.isFile &&
                    it.length() == 0L &&
                    it.name.endsWith(".png", ignoreCase = true) &&
                    !it.name.endsWith(".9.png", ignoreCase = true)
            }
            .toList()
    }

    if (emptyPngs.isEmpty()) return 0

    val transparentPixel = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
    emptyPngs.forEach { file ->
        check(ImageIO.write(transparentPixel, "png", file)) {
            "No PNG writer is available to repair empty drawable resource: ${file.path}"
        }
        check(file.isFile && file.length() > 0L) {
            "Failed to replace empty drawable resource: ${file.path}"
        }
    }

    return emptyPngs.size
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

        val pngSignature = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        )
        require(
            icon.size >= pngSignature.size &&
                icon.copyOfRange(0, pngSignature.size).contentEquals(pngSignature)
        ) { "MaterialYouTok launcher image is not a valid PNG." }

        val androidNs = "http://schemas.android.com/apk/res/android"
        val refs = linkedSetOf<String>()

        document("AndroidManifest.xml").use { doc ->
            val app = doc.getElementsByTagName("application").item(0) as? Element
                ?: error("AndroidManifest.xml has no application element.")

            // The source APK is derived from Play split delivery. This metadata points
            // to an XML resource that belongs to a separate split package, not this APK.
            // A single-APK rebuild must not retain Play's split manifest reference.
            val splitMetadataNames = setOf(
                "com.android.vending.splits",
                "com.android.vending.splits.required"
            )
            val metadataNodes = app.getElementsByTagName("meta-data")
            var removedSplitMetadata = 0
            for (index in metadataNodes.length - 1 downTo 0) {
                val metadata = metadataNodes.item(index) as? Element ?: continue
                if (metadata.parentNode != app) continue
                val metadataName = metadata.getAttribute("android:name")
                if (metadataName !in splitMetadataNames) continue
                app.removeChild(metadata)
                println("Removed Play split metadata from standalone APK: $metadataName")
                removedSplitMetadata++
            }
            if (removedSplitMetadata > 0) {
                println("Removed $removedSplitMetadata Play split metadata entries.")
            }

            if (app.getAttribute("android:isSplitRequired").equals("true", ignoreCase = true)) {
                app.setAttributeNS(androidNs, "android:isSplitRequired", "false")
                println("Set android:isSplitRequired=false for standalone APK output.")
            }

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
        val repairedPlaceholders = replaceEmptyDrawablePngs(res)
        if (repairedPlaceholders > 0) {
            println(
                "Replaced $repairedPlaceholders zero-byte drawable PNG placeholder(s) " +
                    "with valid transparent 1x1 PNGs. res/raw and non-empty images were left unchanged."
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
