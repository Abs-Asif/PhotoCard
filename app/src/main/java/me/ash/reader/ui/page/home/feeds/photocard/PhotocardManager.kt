package me.ash.reader.ui.page.home.feeds.photocard

import android.content.Context
import android.graphics.Typeface
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PhotocardManager {
    private val client = OkHttpClient()

    private fun getBaseDir(context: Context): File {
        val dir = File(context.filesDir, "photocard_sources")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getPhotocardDir(context: Context, code: String): File {
        val dir = File(getBaseDir(context), code)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun listInstalledCodes(context: Context): List<String> {
        val base = getBaseDir(context)
        return base.listFiles()?.filter { it.isDirectory }?.map { it.name } ?: emptyList()
    }

    fun deleteCode(context: Context, code: String) {
        val dir = getPhotocardDir(context, code)
        dir.deleteRecursively()
    }

    suspend fun downloadAndExtractZip(
        context: Context,
        code: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val cleanCode = code.trim().removePrefix("/").removeSuffix(".zip")
        val url = "https://raw.githubusercontent.com/Abs-Asif/PhotoCard/main/sources/$cleanCode.zip"
        val request = Request.Builder().url(url).build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("Failed to download zip from $url: ${response.code}"))
            }

            val body = response.body ?: return@withContext Result.failure(IOException("Empty response body"))
            val targetDir = getPhotocardDir(context, cleanCode)
            targetDir.deleteRecursively()
            targetDir.mkdirs()

            ZipInputStream(body.byteStream()).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val file = File(targetDir, entry.name)
                    if (entry.isDirectory) {
                        file.mkdirs()
                    } else {
                        file.parentFile?.mkdirs()
                        FileOutputStream(file).use { fos ->
                            val buffer = ByteArray(4096)
                            var len: Int
                            while (zis.read(buffer).also { len = it } > 0) {
                                fos.write(buffer, 0, len)
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getPhotocardConfig(context: Context, code: String): Pair<PhotocardConfig, Map<String, String>>? {
        val dir = getPhotocardDir(context, code)
        val cardFile = File(dir, "card.json")
        val replaceFile = File(dir, "replace.json")

        if (!cardFile.exists()) return null

        try {
            val cardJsonStr = cardFile.readText()
            val config = parseCardJson(cardJsonStr)

            val replacements = if (replaceFile.exists()) {
                parseReplaceJson(replaceFile.readText())
            } else {
                emptyMap()
            }

            return Pair(config, replacements)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun findMatchingConfig(context: Context, articleFeedUrl: String?): Triple<String, PhotocardConfig, Map<String, String>>? {
        if (articleFeedUrl == null) return null
        val codes = listInstalledCodes(context)
        for (code in codes) {
            val configPair = getPhotocardConfig(context, code) ?: continue
            val config = configPair.first
            val configUrl = config.source.url.trim().lowercase().removeSuffix("/")
            val feedUrl = articleFeedUrl.trim().lowercase().removeSuffix("/")
            if (configUrl.isNotEmpty() && (configUrl == feedUrl || feedUrl.contains(configUrl) || configUrl.contains(feedUrl))) {
                return Triple(code, config, configPair.second)
            }
        }
        return null
    }

    fun getFontTypeface(context: Context, code: String, fontName: String): Typeface? {
        if (fontName.isEmpty()) return null
        val dir = getPhotocardDir(context, code)
        // Find any file matching fontName
        val fontFile = File(dir, fontName)
        if (fontFile.exists()) {
            try {
                return Typeface.createFromFile(fontFile)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        // Try listing files and matching name case insensitively or matching extension
        val files = dir.listFiles() ?: return null
        for (file in files) {
            if (file.name.equals(fontName, ignoreCase = true)) {
                try {
                    return Typeface.createFromFile(file)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return null
    }

    fun getBackgroundImageFile(context: Context, code: String): File? {
        val dir = getPhotocardDir(context, code)
        // Look for any 1000x1000 image, or common image files
        val extensions = listOf(".png", ".jpg", ".jpeg", ".webp")
        val files = dir.listFiles() ?: return null
        for (file in files) {
            if (extensions.any { file.name.lowercase().endsWith(it) } && !file.name.equals("card.json", true) && !file.name.equals("replace.json", true)) {
                return file
            }
        }
        return null
    }

    fun parseCardJson(jsonStr: String): PhotocardConfig {
        val obj = JSONObject(jsonStr)
        val sourceObj = obj.optJSONObject("source") ?: JSONObject()
        val designObj = obj.optJSONObject("design") ?: JSONObject()

        val backgroundObj = designObj.optJSONObject("background") ?: JSONObject()
        val imageObj = designObj.optJSONObject("image") ?: JSONObject()
        val titleObj = designObj.optJSONObject("title") ?: JSONObject()
        val dateObj = designObj.optJSONObject("date") ?: JSONObject()
        val borderObj = designObj.optJSONObject("border") ?: JSONObject()

        return PhotocardConfig(
            source = SourceConfig(
                type = sourceObj.optString("type", "rss"),
                url = sourceObj.optString("url", "")
            ),
            design = DesignConfig(
                background = BackgroundConfig(
                    width = backgroundObj.optInt("width", 1000),
                    height = backgroundObj.optInt("height", 1000)
                ),
                image = ImageConfig(
                    x = imageObj.optDouble("x", 100.0).toFloat(),
                    y = imageObj.optDouble("y", 100.0).toFloat(),
                    width = imageObj.optDouble("width", 800.0).toFloat(),
                    height = imageObj.optDouble("height", 450.0).toFloat(),
                    cornerRadius = imageObj.optDouble("cornerRadius", 16.0).toFloat(),
                    zIndex = imageObj.optString("zIndex", "above_background")
                ),
                title = TextConfig(
                    x = titleObj.optDouble("x", 100.0).toFloat(),
                    y = titleObj.optDouble("y", 600.0).toFloat(),
                    width = titleObj.optDouble("width", 800.0).toFloat(),
                    color = titleObj.optString("color", "#FFFFFF"),
                    fontSize = titleObj.optDouble("fontSize", 36.0).toFloat(),
                    fontFamily = titleObj.optString("fontFamily", ""),
                    alignment = titleObj.optString("alignment", "left")
                ),
                date = DateConfig(
                    x = dateObj.optDouble("x", 100.0).toFloat(),
                    y = dateObj.optDouble("y", 800.0).toFloat(),
                    color = dateObj.optString("color", "#CCCCCC"),
                    fontSize = dateObj.optDouble("fontSize", 24.0).toFloat(),
                    fontFamily = dateObj.optString("fontFamily", ""),
                    format = dateObj.optString("format", "Sunday, 12 July 2026")
                ),
                border = BorderConfig(
                    thickness = borderObj.optDouble("thickness", 0.0).toFloat(),
                    color = borderObj.optString("color", "#000000")
                )
            )
        )
    }

    fun parseReplaceJson(jsonStr: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val obj = JSONObject(jsonStr)
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = obj.optString(key)
        }
        return map
    }

    fun serializeCardConfig(config: PhotocardConfig): String {
        val obj = JSONObject()
        val sourceObj = JSONObject().apply {
            put("type", config.source.type)
            put("url", config.source.url)
        }
        val backgroundObj = JSONObject().apply {
            put("width", config.design.background.width)
            put("height", config.design.background.height)
        }
        val imageObj = JSONObject().apply {
            put("x", config.design.image.x.toDouble())
            put("y", config.design.image.y.toDouble())
            put("width", config.design.image.width.toDouble())
            put("height", config.design.image.height.toDouble())
            put("cornerRadius", config.design.image.cornerRadius.toDouble())
            put("zIndex", config.design.image.zIndex)
        }
        val titleObj = JSONObject().apply {
            put("x", config.design.title.x.toDouble())
            put("y", config.design.title.y.toDouble())
            put("width", config.design.title.width.toDouble())
            put("color", config.design.title.color)
            put("fontSize", config.design.title.fontSize.toDouble())
            put("fontFamily", config.design.title.fontFamily)
            put("alignment", config.design.title.alignment)
        }
        val dateObj = JSONObject().apply {
            put("x", config.design.date.x.toDouble())
            put("y", config.design.date.y.toDouble())
            put("color", config.design.date.color)
            put("fontSize", config.design.date.fontSize.toDouble())
            put("fontFamily", config.design.date.fontFamily)
            put("format", config.design.date.format)
        }
        val borderObj = JSONObject().apply {
            put("thickness", config.design.border.thickness.toDouble())
            put("color", config.design.border.color)
        }
        val designObj = JSONObject().apply {
            put("background", backgroundObj)
            put("image", imageObj)
            put("title", titleObj)
            put("date", dateObj)
            put("border", borderObj)
        }
        obj.put("source", sourceObj)
        obj.put("design", designObj)
        return obj.toString(2)
    }

    fun serializeReplaceJson(replacements: Map<String, String>): String {
        val obj = JSONObject()
        for ((key, value) in replacements) {
            obj.put(key, value)
        }
        return obj.toString(2)
    }

    fun writeZip(
        outputFile: File,
        cardJsonStr: String,
        replaceJsonStr: String,
        backgroundImageFile: File?,
        fontFiles: List<File>
    ) {
        ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
            // Write card.json
            zos.putNextEntry(ZipEntry("card.json"))
            zos.write(cardJsonStr.toByteArray())
            zos.closeEntry()

            // Write replace.json
            zos.putNextEntry(ZipEntry("replace.json"))
            zos.write(replaceJsonStr.toByteArray())
            zos.closeEntry()

            // Write background image
            backgroundImageFile?.let { bgFile ->
                if (bgFile.exists()) {
                    zos.putNextEntry(ZipEntry(bgFile.name))
                    FileInputStream(bgFile).use { fis ->
                        fis.copyTo(zos)
                    }
                    zos.closeEntry()
                }
            }

            // Write font files
            for (fontFile in fontFiles) {
                if (fontFile.exists()) {
                    zos.putNextEntry(ZipEntry(fontFile.name))
                    FileInputStream(fontFile).use { fis ->
                        fis.copyTo(zos)
                    }
                    zos.closeEntry()
                }
            }
        }
    }
}
