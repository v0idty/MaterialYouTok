package app.revanced.patches.materialyoutok

import app.revanced.patcher.patch.resourcePatch
import org.w3c.dom.Element
import java.io.File

@Suppress("unused")
val materialYouTokIconPatch = resourcePatch(
    name = "MaterialYouTok icon",
    description = "Replaces the app icon and name with MaterialYouTok.",
) {
    compatibleWith("com.zhiliaoapp.musically")

    execute {
        val icon = object {}.javaClass
            .getResourceAsStream("/materialyoutok/ic_launcher.png")!!
            .readBytes()
        val refs = linkedSetOf<String>()

        document("AndroidManifest.xml").use { doc ->
            val app = doc.getElementsByTagName("application").item(0) as Element
            app.setAttribute("android:label", "MaterialYouTok")
            listOf("android:icon", "android:roundIcon").forEach { attr ->
                val v = app.getAttribute(attr)
                if (v.startsWith("@")) refs.add(v)
            }
            listOf("activity", "activity-alias").forEach { tag ->
                val nodes = doc.getElementsByTagName(tag)
                for (i in 0 until nodes.length) {
                    val el = nodes.item(i) as Element
                    val cats = el.getElementsByTagName("category")
                    val isLauncher = (0 until cats.length).any {
                        (cats.item(it) as Element).getAttribute("android:name") ==
                            "android.intent.category.LAUNCHER"
                    }
                    if (!isLauncher) continue
                    if (el.hasAttribute("android:label")) el.setAttribute("android:label", "MaterialYouTok")
                    listOf("android:icon", "android:roundIcon").forEach { attr ->
                        val v = el.getAttribute(attr)
                        if (v.startsWith("@")) refs.add(v)
                    }
                }
            }
        }

        val res = get("res")
        refs.forEach { ref ->
            val body = ref.removePrefix("@")
            val type = body.substringBefore('/')
            val name = body.substringAfter('/')
            res.listFiles()?.filter { it.isDirectory && it.name.startsWith(type) }?.forEach { dir ->
                dir.listFiles()?.filter { it.isFile && it.nameWithoutExtension == name }?.forEach { it.delete() }
            }
            val outDir = File(res, "$type-xxxhdpi")
            outDir.mkdirs()
            File(outDir, "$name.png").writeBytes(icon)
        }
    }
}
