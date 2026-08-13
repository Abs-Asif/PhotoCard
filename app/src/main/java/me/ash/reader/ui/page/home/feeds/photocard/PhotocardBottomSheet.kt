package me.ash.reader.ui.page.home.feeds.photocard

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import me.ash.reader.domain.model.article.ArticleWithFeed
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotocardBottomSheet(
    articleWithFeed: ArticleWithFeed,
    onDismissRequest: () -> Unit,
    onEditClick: (String?, String, String, Long) -> Unit, // passes code, title, img, date_timestamp to launch design suite
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var loadingText by remember { mutableStateOf("Fetching article content...") }
    var generatedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var currentCode by remember { mutableStateOf<String?>(null) }
    var currentConfig by remember { mutableStateOf<PhotocardConfig?>(null) }
    var currentReplacements by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var currentTitle by remember { mutableStateOf("") }
    var currentImgUrl by remember { mutableStateOf("") }

    LaunchedEffect(articleWithFeed) {
        scope.launch {
            // Step A: Fetch og tags
            loadingText = "Fetching og:image and og:title..."
            val fallbackImg = articleWithFeed.article.img?.toString()
            val extracted = PhotocardGenerator.fetchOgTags(
                url = articleWithFeed.article.link,
                fallbackTitle = articleWithFeed.article.title,
                fallbackImgUrl = fallbackImg
            )

            currentTitle = extracted.title
            currentImgUrl = extracted.imageUrl

            // Step B: Match config
            loadingText = "Applying Photocard design rules..."
            val matched = PhotocardManager.findMatchingConfig(context, articleWithFeed.feed.url)
            val config = matched?.second ?: PhotocardConfig(
                source = SourceConfig(url = articleWithFeed.feed.url)
            )
            val code = matched?.first
            val replacements = matched?.third ?: emptyMap()

            currentCode = code
            currentConfig = config
            currentReplacements = replacements

            // Step C: Generate Bitmap
            loadingText = "Generating Photocard..."
            val artDate = articleWithFeed.article.date
            val bmp = PhotocardGenerator.generateBitmap(
                context = context,
                code = code,
                config = config,
                replacements = replacements,
                ogTitle = extracted.title,
                ogImageUrl = extracted.imageUrl,
                articleDate = artDate
            )
            generatedBitmap = bmp
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f) // Covers approximately 2/3 of the screen
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Instant Photocard Creator",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (generatedBitmap == null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = loadingText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black)
                    ) {
                        Image(
                            bitmap = generatedBitmap!!.asImageBitmap(),
                            contentDescription = "Photocard Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Save Button
                Button(
                    onClick = {
                        generatedBitmap?.let { bmp ->
                            val uri = PhotocardGenerator.saveBitmapToGallery(context, bmp)
                            if (uri != null) {
                                Toast.makeText(context, "Photocard saved to gallery!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Failed to save to gallery", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = generatedBitmap != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Rounded.Save, contentDescription = "Save")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Save", fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.width(16.dp))

                // 2. Edit Button (Small icon only)
                IconButton(
                    onClick = {
                        onDismissRequest()
                        onEditClick(currentCode, currentTitle, currentImgUrl, articleWithFeed.article.date.time)
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit in Design Suite",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // 3. Share Button
                Button(
                    onClick = {
                        generatedBitmap?.let { bmp ->
                            PhotocardGenerator.shareBitmap(context, bmp)
                        }
                    },
                    enabled = generatedBitmap != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Rounded.Share, contentDescription = "Share")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Share", fontSize = 14.sp)
                }
            }
        }
    }
}
