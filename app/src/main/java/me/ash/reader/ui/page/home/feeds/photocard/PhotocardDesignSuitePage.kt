package me.ash.reader.ui.page.home.feeds.photocard

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.ash.reader.R
import me.ash.reader.ui.component.base.FeedbackIconButton
import me.ash.reader.ui.component.base.RYScaffold
import me.ash.reader.ui.page.nav3.key.Route
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotocardDesignSuitePage(
    key: Route.PhotocardDesignSuite,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Configuration state
    var sourceUrl by remember { mutableStateOf(key.code?.let { "" } ?: "") }
    var sourceType by remember { mutableStateOf("rss") } // "rss" or "sitemap"
    var sourceCheckStatus by remember { mutableStateOf("Not checked") }

    // Design parameters
    var bgWidth by remember { mutableStateOf(1000) }
    var bgHeight by remember { mutableStateOf(1000) }

    var imgX by remember { mutableStateOf(100f) }
    var imgY by remember { mutableStateOf(100f) }
    var imgW by remember { mutableStateOf(800f) }
    var imgH by remember { mutableStateOf(450f) }
    var imgCornerRadius by remember { mutableStateOf(16f) }
    var imgZIndex by remember { mutableStateOf("above_background") }

    var titleX by remember { mutableStateOf(100f) }
    var titleY by remember { mutableStateOf(600f) }
    var titleW by remember { mutableStateOf(800f) }
    var titleColor by remember { mutableStateOf("#FFFFFF") }
    var titleSize by remember { mutableStateOf(36f) }
    var titleFontFamily by remember { mutableStateOf("") }
    var titleAlignment by remember { mutableStateOf("left") }

    var dateX by remember { mutableStateOf(100f) }
    var dateY by remember { mutableStateOf(800f) }
    var dateColor by remember { mutableStateOf("#CCCCCC") }
    var dateSize by remember { mutableStateOf(24f) }
    var dateFontFamily by remember { mutableStateOf("") }
    var dateFormatSelected by remember { mutableStateOf("Sunday, 12 July 2026") }

    var borderThickness by remember { mutableStateOf(0f) }
    var borderColor by remember { mutableStateOf("#000000") }

    // Text Moderation Replacements List
    var replacementsList by remember { mutableStateOf(listOf<Pair<String, String>>("ধর্ষক" to "ধ*র্ষক")) }
    var newBadWord by remember { mutableStateOf("") }
    var newGoodWord by remember { mutableStateOf("") }

    // Asset Picked states
    var bgImageFile by remember { mutableStateOf<File?>(null) }
    var fontFile by remember { mutableStateOf<File?>(null) }

    // Mocks / Prefill
    val mockTitle = if (key.prefillTitle.isNotEmpty()) key.prefillTitle else "পরিকল্পনা অনুযায়ী ফটোকর্ড ডিজাইন সোর্সিং ফিচার বাস্তবায়ন"
    val mockImgUrl = if (key.prefillImage.isNotEmpty()) key.prefillImage else "https://picsum.photos/800/450"
    val mockDateTimestamp = if (key.prefillDate > 0) key.prefillDate else System.currentTimeMillis()

    // Status / Progress
    var isCheckingSource by remember { mutableStateOf(false) }
    var isSavingZip by remember { mutableStateOf(false) }

    // File pickers launchers
    val bgImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val file = uri.toTempFile(context, "photocard_bg_", ".png")
            if (file != null) {
                bgImageFile = file
                Toast.makeText(context, "Background image selected!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val fontPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val file = uri.toTempFile(context, "photocard_font_", ".ttf")
            if (file != null) {
                fontFile = file
                titleFontFamily = file.name
                dateFontFamily = file.name
                Toast.makeText(context, "Font file selected!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Auto check source URL
    LaunchedEffect(sourceUrl) {
        if (sourceUrl.length > 5 && sourceUrl.startsWith("http")) {
            isCheckingSource = true
            scope.launch {
                val type = checkSourceType(sourceUrl)
                sourceType = type
                sourceCheckStatus = "Auto checked: $type"
                isCheckingSource = false
            }
        } else {
            sourceCheckStatus = "Invalid URL"
        }
    }

    RYScaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Photocard Design Suite") },
                navigationIcon = {
                    FeedbackIconButton(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = MaterialTheme.colorScheme.onSurface,
                        onClick = onBack
                    )
                },
                actions = {
                    if (isSavingZip) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        IconButton(
                            onClick = {
                                if (bgImageFile == null) {
                                    Toast.makeText(context, "Please select a background image first!", Toast.LENGTH_LONG).show()
                                    return@IconButton
                                }
                                isSavingZip = true
                                scope.launch {
                                    val config = PhotocardConfig(
                                        source = SourceConfig(type = sourceType, url = sourceUrl),
                                        design = DesignConfig(
                                            background = BackgroundConfig(width = bgWidth, height = bgHeight),
                                            image = ImageConfig(
                                                x = imgX, y = imgY, width = imgW, height = imgH,
                                                cornerRadius = imgCornerRadius, zIndex = imgZIndex
                                            ),
                                            title = TextConfig(
                                                x = titleX, y = titleY, width = titleW, color = titleColor,
                                                fontSize = titleSize, fontFamily = titleFontFamily, alignment = titleAlignment
                                            ),
                                            date = DateConfig(
                                                x = dateX, y = dateY, color = dateColor, fontSize = dateSize,
                                                fontFamily = dateFontFamily, format = dateFormatSelected
                                            ),
                                            border = BorderConfig(thickness = borderThickness, color = borderColor)
                                        )
                                    )
                                    val cardJsonStr = PhotocardManager.serializeCardConfig(config)
                                    val replaceMap = replacementsList.toMap()
                                    val replaceJsonStr = PhotocardManager.serializeReplaceJson(replaceMap)

                                    val exportsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                                    val exportFile = File(exportsDir, "photocard_design_export.zip")

                                    try {
                                        PhotocardManager.writeZip(
                                            outputFile = exportFile,
                                            cardJsonStr = cardJsonStr,
                                            replaceJsonStr = replaceJsonStr,
                                            backgroundImageFile = bgImageFile,
                                            fontFiles = listOfNotNull(fontFile)
                                        )
                                        Toast.makeText(context, "Successfully exported design to Downloads/photocard_design_export.zip!", Toast.LENGTH_LONG).show()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        Toast.makeText(context, "Failed to save ZIP: ${e.message}", Toast.LENGTH_LONG).show()
                                    } finally {
                                        isSavingZip = false
                                    }
                                }
                            }
                        ) {
                            Icon(imageVector = Icons.Rounded.Download, contentDescription = "Save as ZIP")
                        }
                    }
                }
            )
        },
        content = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Visual Interactive Canvas (covers 1:1 preview with visual drag and drop!)
                item {
                    Text(
                        text = "Visual Canvas Preview (Drag to position elements)",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF121212))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    ) {
                        val canvasWidth = maxWidth
                        val canvasHeight = maxHeight
                        val scaleX = canvasWidth.value / 1000f
                        val scaleY = canvasHeight.value / 1000f

                        // Draw default/fallback background if no image selected
                        if (bgImageFile != null) {
                            val bgBmp = remember(bgImageFile) { BitmapFactory.decodeFile(bgImageFile!!.absolutePath) }
                            if (bgBmp != null) {
                                Image(
                                    bitmap = bgBmp.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.FillBounds
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF1E2640))
                            )
                        }

                        // Relative absolute positioned blocks that are visually draggable!
                        // Block A: Photocard Image
                        Box(
                            modifier = Modifier
                                .offset(
                                    x = (imgX * scaleX).dp,
                                    y = (imgY * scaleY).dp
                                )
                                .size(
                                    width = (imgW * scaleX).dp,
                                    height = (imgH * scaleY).dp
                                )
                                .clip(RoundedCornerShape((imgCornerRadius * scaleX).dp))
                                .border(
                                    width = 1.dp,
                                    color = Color.Green.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape((imgCornerRadius * scaleX).dp)
                                )
                                .background(Color.DarkGray)
                                .pointerInput(canvasWidth) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        imgX = (imgX + dragAmount.x / scaleX).coerceIn(0f, 1000f - imgW)
                                        imgY = (imgY + dragAmount.y / scaleY).coerceIn(0f, 1000f - imgH)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Photocard Image\n(Drag me!)",
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                fontSize = 11.sp,
                                maxLines = 2
                            )
                        }

                        // Block B: Title Text
                        Box(
                            modifier = Modifier
                                .offset(
                                    x = (titleX * scaleX).dp,
                                    y = (titleY * scaleY).dp
                                )
                                .width((titleW * scaleX).dp)
                                .border(1.dp, Color.Cyan.copy(alpha = 0.6f))
                                .pointerInput(canvasWidth) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        titleX = (titleX + dragAmount.x / scaleX).coerceIn(0f, 1000f - titleW)
                                        titleY = (titleY + dragAmount.y / scaleY).coerceIn(0f, 1000f - 50f)
                                    }
                                }
                                .padding(4.dp)
                        ) {
                            Text(
                                text = mockTitle,
                                color = try { Color(android.graphics.Color.parseColor(titleColor)) } catch (e: Exception) { Color.White },
                                fontSize = (titleSize * scaleX).sp,
                                textAlign = when (titleAlignment.lowercase()) {
                                    "center" -> TextAlign.Center
                                    "right" -> TextAlign.Right
                                    else -> TextAlign.Left
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Block C: Date Text
                        Box(
                            modifier = Modifier
                                .offset(
                                    x = (dateX * scaleX).dp,
                                    y = (dateY * scaleY).dp
                                )
                                .border(1.dp, Color.Magenta.copy(alpha = 0.6f))
                                .pointerInput(canvasWidth) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        dateX = (dateX + dragAmount.x / scaleX).coerceIn(0f, 1000f - 150f)
                                        dateY = (dateY + dragAmount.y / scaleY).coerceIn(0f, 1000f - 30f)
                                    }
                                }
                                .padding(4.dp)
                        ) {
                            Text(
                                text = PhotocardDateHelper.formatDate(Date(mockDateTimestamp), dateFormatSelected),
                                color = try { Color(android.graphics.Color.parseColor(dateColor)) } catch (e: Exception) { Color.LightGray },
                                fontSize = (dateSize * scaleX).sp
                            )
                        }

                        // Canvas Border
                        if (borderThickness > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(
                                        width = (borderThickness * scaleX).dp,
                                        color = try { Color(android.graphics.Color.parseColor(borderColor)) } catch (e: Exception) { Color.Black }
                                    )
                            )
                        }
                    }
                }

                // Section 2: Essential Asset Selectors
                item {
                    Text(text = "Assets Selection", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { bgImagePicker.launch("image/*") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Rounded.Image, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = if (bgImageFile != null) "Change BG" else "Choose BG")
                        }

                        Button(
                            onClick = { fontPicker.launch("*/*") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Rounded.FontDownload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = if (fontFile != null) "Change Font" else "Choose Font")
                        }
                    }

                    if (bgImageFile != null) {
                        Text(
                            text = "BG file: ${bgImageFile!!.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    if (fontFile != null) {
                        Text(
                            text = "Font file: ${fontFile!!.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Section 3: Source Verification Check
                item {
                    Text(text = "Source Rules Configuration", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = sourceUrl,
                        onValueChange = { sourceUrl = it },
                        label = { Text("Source RSS/Sitemap URL") },
                        placeholder = { Text("https://example.com/rss") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Detected source: $sourceType",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (isCheckingSource) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        } else {
                            Text(
                                text = sourceCheckStatus,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Section 4: Design Rules Tuning (Coordinate sliders and options)
                item {
                    Text(text = "Layout Tuning Controls", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Layer position priority Z-index
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Photocard Image Placement priority:")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = imgZIndex == "above_background",
                                onClick = { imgZIndex = "above_background" }
                            )
                            Text(text = "Above BG")
                            Spacer(modifier = Modifier.width(8.dp))
                            RadioButton(
                                selected = imgZIndex == "below_background",
                                onClick = { imgZIndex = "below_background" }
                            )
                            Text(text = "Below BG")
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    // Sliders for Photocard Image dimensions
                    Text(text = "Photocard image Corner Radius: ${imgCornerRadius.roundToInt()} px")
                    Slider(
                        value = imgCornerRadius,
                        onValueChange = { imgCornerRadius = it },
                        valueRange = 0f..100f
                    )

                    Text(text = "Photocard image width: ${imgW.roundToInt()} px")
                    Slider(
                        value = imgW,
                        onValueChange = { imgW = it },
                        valueRange = 100f..1000f
                    )

                    Text(text = "Photocard image height: ${imgH.roundToInt()} px")
                    Slider(
                        value = imgH,
                        onValueChange = { imgH = it },
                        valueRange = 100f..1000f
                    )

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    // Title configs
                    Text(text = "Title Text size: ${titleSize.roundToInt()} px")
                    Slider(
                        value = titleSize,
                        onValueChange = { titleSize = it },
                        valueRange = 12f..100f
                    )

                    Text(text = "Title max bounding width: ${titleW.roundToInt()} px")
                    Slider(
                        value = titleW,
                        onValueChange = { titleW = it },
                        valueRange = 100f..1000f
                    )

                    OutlinedTextField(
                        value = titleColor,
                        onValueChange = { titleColor = it },
                        label = { Text("Title Text Color (HEX string)") },
                        placeholder = { Text("#FFFFFF") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Title text alignment:")
                        Row {
                            listOf("left", "center", "right").forEach { align ->
                                FilterChip(
                                    selected = titleAlignment == align,
                                    onClick = { titleAlignment = align },
                                    label = { Text(align) },
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                )
                            }
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    // Date config
                    Text(text = "Date Text size: ${dateSize.roundToInt()} px")
                    Slider(
                        value = dateSize,
                        onValueChange = { dateSize = it },
                        valueRange = 12f..100f
                    )

                    OutlinedTextField(
                        value = dateColor,
                        onValueChange = { dateColor = it },
                        label = { Text("Date Text Color (HEX string)") },
                        placeholder = { Text("#CCCCCC") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Date Format drop downs list
                    Text(text = "Date Format Representation:", fontWeight = FontWeight.Bold)
                    Column(modifier = Modifier.padding(top = 4.dp)) {
                        PhotocardDateHelper.FORMATS.forEach { fmt ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { dateFormatSelected = fmt }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = dateFormatSelected == fmt,
                                    onClick = { dateFormatSelected = fmt }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = fmt, fontSize = 13.sp)
                            }
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    // Card Border
                    Text(text = "Card Border thickness: ${borderThickness.roundToInt()} px")
                    Slider(
                        value = borderThickness,
                        onValueChange = { borderThickness = it },
                        valueRange = 0f..50f
                    )

                    OutlinedTextField(
                        value = borderColor,
                        onValueChange = { borderColor = it },
                        label = { Text("Card Border Color (HEX)") },
                        placeholder = { Text("#000000") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Section 5: Word replacement editor (text moderation replace.json)
                item {
                    Text(text = "Compliance Word Moderation (replace.json)", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newBadWord,
                            onValueChange = { newBadWord = it },
                            label = { Text("Word") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newGoodWord,
                            onValueChange = { newGoodWord = it },
                            label = { Text("Moderate") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                if (newBadWord.trim().isNotEmpty() && newGoodWord.trim().isNotEmpty()) {
                                    replacementsList = replacementsList + (newBadWord.trim() to newGoodWord.trim())
                                    newBadWord = ""
                                    newGoodWord = ""
                                }
                            }
                        ) {
                            Icon(imageVector = Icons.Rounded.Add, contentDescription = "Add")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(replacementsList) { (bad, good) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "$bad  →  $good", style = MaterialTheme.typography.bodyMedium)
                        IconButton(
                            onClick = {
                                replacementsList = replacementsList.filter { it.first != bad }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Remove",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    )
}

// Helpers for checkSourceType
suspend fun checkSourceType(url: String): String = withContext(Dispatchers.IO) {
    try {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            val content = response.body?.string() ?: ""
            if (content.contains("<sitemap") || content.contains("<urlset") || url.contains("sitemap")) {
                "sitemap"
            } else {
                "rss"
            }
        } else {
            if (url.contains("sitemap")) "sitemap" else "rss"
        }
    } catch (e: Exception) {
        if (url.contains("sitemap")) "sitemap" else "rss"
    }
}

// Uri extension helper to write file
fun Uri.toTempFile(context: Context, prefix: String, suffix: String): File? {
    try {
        val inputStream = context.contentResolver.openInputStream(this) ?: return null
        val tempFile = File.createTempFile(prefix, suffix, context.cacheDir)
        FileOutputStream(tempFile).use { out ->
            inputStream.copyTo(out)
        }
        return tempFile
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}
