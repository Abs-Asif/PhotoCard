package me.ash.reader.ui.page.home.feeds.photocard

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import java.util.regex.Pattern

object PhotocardGenerator {
    private val client = OkHttpClient()

    data class ExtractedContent(
        val title: String,
        val imageUrl: String
    )

    suspend fun fetchOgTags(url: String, fallbackTitle: String, fallbackImgUrl: String?): ExtractedContent = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext ExtractedContent(fallbackTitle, fallbackImgUrl ?: "")
            }
            val html = response.body?.string() ?: ""

            val ogTitle = extractMetaTag(html, "og:title") ?: extractTitleTag(html) ?: fallbackTitle
            val ogImage = extractMetaTag(html, "og:image") ?: fallbackImgUrl ?: ""

            ExtractedContent(ogTitle.trim(), ogImage.trim())
        } catch (e: Exception) {
            e.printStackTrace()
            ExtractedContent(fallbackTitle, fallbackImgUrl ?: "")
        }
    }

    private fun extractMetaTag(html: String, property: String): String? {
        val pattern = Pattern.compile("<meta[^>]+property=[\"']$property[\"'][^>]+content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE)
        var matcher = pattern.matcher(html)
        if (matcher.find()) {
            return matcher.group(1)
        }
        val patternAlternative = Pattern.compile("<meta[^>]+content=[\"']([^\"']+)[\"'][^>]+property=[\"']$property[\"']", Pattern.CASE_INSENSITIVE)
        matcher = patternAlternative.matcher(html)
        if (matcher.find()) {
            return matcher.group(1)
        }
        return null
    }

    private fun extractTitleTag(html: String): String? {
        val pattern = Pattern.compile("<title>([^<]+)</title>", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(html)
        if (matcher.find()) {
            return matcher.group(1)
        }
        return null
    }

    suspend fun generateBitmap(
        context: Context,
        code: String?,
        config: PhotocardConfig,
        replacements: Map<String, String>,
        ogTitle: String,
        ogImageUrl: String,
        articleDate: Date
    ): Bitmap = withContext(Dispatchers.IO) {
        val width = config.design.background.width
        val height = config.design.background.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Text Moderation
        var moderatedTitle = ogTitle
        replacements.forEach { (bad, good) ->
            moderatedTitle = moderatedTitle.replace(bad, good)
        }

        // 2. Fetch og:image Bitmap
        var imgBitmap: Bitmap? = null
        if (ogImageUrl.isNotEmpty()) {
            try {
                val req = Request.Builder().url(ogImageUrl).build()
                val res = client.newCall(req).execute()
                if (res.isSuccessful) {
                    val bytes = res.body?.bytes()
                    if (bytes != null) {
                        imgBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 3. Load background
        var bgBitmap: Bitmap? = null
        if (code != null) {
            val bgFile = PhotocardManager.getBackgroundImageFile(context, code)
            if (bgFile != null && bgFile.exists()) {
                try {
                    bgBitmap = BitmapFactory.decodeFile(bgFile.absolutePath)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Helper to draw background
        val drawBg = {
            if (bgBitmap != null) {
                val src = Rect(0, 0, bgBitmap.width, bgBitmap.height)
                val dest = Rect(0, 0, width, height)
                canvas.drawBitmap(bgBitmap, src, dest, Paint(Paint.ANTI_ALIAS_FLAG))
            } else {
                // Fallback elegant gradient background
                val paint = Paint().apply {
                    isAntiAlias = true
                    shader = LinearGradient(
                        0f, 0f, 0f, height.toFloat(),
                        Color.parseColor("#1A237E"), Color.parseColor("#121212"),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }
        }

        // Helper to draw image
        val drawImage = {
            if (imgBitmap != null) {
                canvas.save()
                val imgConf = config.design.image
                val destF = RectF(imgConf.x, imgConf.y, imgConf.x + imgConf.width, imgConf.y + imgConf.height)
                val path = Path()
                path.addRoundRect(destF, imgConf.cornerRadius, imgConf.cornerRadius, Path.Direction.CW)
                canvas.clipPath(path)
                canvas.drawBitmap(imgBitmap, null, destF, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                canvas.restore()
            }
        }

        // 4. Draw background & image based on zIndex
        if (config.design.image.zIndex == "below_background") {
            drawImage()
            drawBg()
        } else {
            drawBg()
            drawImage()
        }

        // 5. Draw Title Text
        val titleConf = config.design.title
        val titleColor = try {
            Color.parseColor(titleConf.color)
        } catch (e: Exception) {
            Color.WHITE
        }
        val tf = if (code != null) {
            PhotocardManager.getFontTypeface(context, code, titleConf.fontFamily)
        } else {
            null
        }

        val titlePaint = TextPaint().apply {
            color = titleColor
            textSize = titleConf.fontSize
            typeface = tf ?: Typeface.DEFAULT
            isAntiAlias = true
        }

        val alignment = when (titleConf.alignment.lowercase()) {
            "center" -> Layout.Alignment.ALIGN_CENTER
            "right" -> Layout.Alignment.ALIGN_OPPOSITE
            else -> Layout.Alignment.ALIGN_NORMAL
        }

        val staticLayout = StaticLayout.Builder.obtain(
            moderatedTitle,
            0,
            moderatedTitle.length,
            titlePaint,
            titleConf.width.toInt().coerceAtLeast(100)
        )
        .setAlignment(alignment)
        .setLineSpacing(0f, 1.1f)
        .setIncludePad(false)
        .build()

        canvas.save()
        canvas.translate(titleConf.x, titleConf.y)
        staticLayout.draw(canvas)
        canvas.restore()

        // 6. Draw Date Text
        val dateConf = config.design.date
        val dateText = PhotocardDateHelper.formatDate(articleDate, dateConf.format)
        val dateColor = try {
            Color.parseColor(dateConf.color)
        } catch (e: Exception) {
            Color.LTGRAY
        }
        val dateTf = if (code != null) {
            PhotocardManager.getFontTypeface(context, code, dateConf.fontFamily)
        } else {
            null
        }

        val datePaint = Paint().apply {
            color = dateColor
            textSize = dateConf.fontSize
            typeface = dateTf ?: Typeface.DEFAULT
            isAntiAlias = true
        }
        canvas.drawText(dateText, dateConf.x, dateConf.y, datePaint)

        // 7. Draw Border
        val borderConf = config.design.border
        if (borderConf.thickness > 0) {
            val borderPaint = Paint().apply {
                color = try { Color.parseColor(borderConf.color) } catch (e: Exception) { Color.BLACK }
                style = Paint.Style.STROKE
                strokeWidth = borderConf.thickness
                isAntiAlias = true
            }
            canvas.drawRect(
                borderConf.thickness / 2f,
                borderConf.thickness / 2f,
                width.toFloat() - borderConf.thickness / 2f,
                height.toFloat() - borderConf.thickness / 2f,
                borderPaint
            )
        }

        bitmap
    }

    fun saveBitmapToGallery(context: Context, bitmap: Bitmap): Uri? {
        val filename = "photocard_${System.currentTimeMillis()}.png"
        val resolver = context.contentResolver

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PhotoCard")
        }

        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return null
        resolver.openOutputStream(imageUri).use { out ->
            if (out != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        }
        return imageUri
    }

    fun shareBitmap(context: Context, bitmap: Bitmap) {
        try {
            val cachePath = File(context.cacheDir, "shared_images")
            cachePath.mkdirs()
            val file = File(cachePath, "shared_photocard.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)

            val intent = Intent(Intent.ACTION_SEND).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setDataAndType(uri, context.contentResolver.getType(uri))
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "image/png"
            }
            context.startActivity(Intent.createChooser(intent, "Share Photocard"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
