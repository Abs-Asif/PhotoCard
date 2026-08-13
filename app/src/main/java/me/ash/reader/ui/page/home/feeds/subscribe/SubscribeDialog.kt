package me.ash.reader.ui.page.home.feeds.subscribe

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import me.ash.reader.R
import me.ash.reader.ui.component.FeedIcon
import me.ash.reader.ui.component.base.ClipboardTextField
import me.ash.reader.ui.ext.collectAsStateValue

@OptIn(
    androidx.compose.ui.ExperimentalComposeUiApi::class,
    ExperimentalAnimationApi::class
)
@Composable
fun SubscribeDialog(
    subscribeViewModel: SubscribeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val subscribeState = subscribeViewModel.subscribeState.collectAsStateValue()

    if (subscribeState is SubscribeState.Visible) {

        DisposableEffect(Unit) {
            onDispose {
                subscribeViewModel.cancelSearch()
            }
        }

        AlertDialog(
            modifier = Modifier.padding(horizontal = 44.dp),
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnClickOutside = false
            ),
            onDismissRequest = {
                focusManager.clearFocus()
                subscribeViewModel.hideDrawer()
            },
            icon = {
                FeedIcon(
                    feedName = null,
                    iconUrl = null,
                    placeholderIcon = Icons.Rounded.Key,
                )
            },
            title = {
                Text(
                    text = when (subscribeState) {
                        is SubscribeState.Fetching -> "Downloading..."
                        else -> "Add Secret key"
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            text = {
                AnimatedContent(
                    targetState = subscribeState,
                    transitionSpec = {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut()) using null
                    },
                    contentKey = { it is SubscribeState.Configure }
                ) { state ->
                    when (state) {
                        is SubscribeState.Input -> {
                            val errorText = when (state) {
                                is SubscribeState.Fetching -> ""
                                is SubscribeState.Idle -> state.errorMessage ?: ""
                            }

                            Column {
                                Text(
                                    text = "Enter the secret key code (e.g. 28376) to download and extract the Photocard design.",
                                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                ClipboardTextField(
                                    state = state.linkState,
                                    modifier = Modifier.fillMaxWidth(),
                                    readOnly = state is SubscribeState.Fetching,
                                    placeholder = "Secret key code",
                                    errorText = errorText,
                                    imeAction = ImeAction.Search,
                                    onConfirm = {
                                        val code = state.linkState.text.toString().trim()
                                        if (code.isNotEmpty()) {
                                            subscribeViewModel.downloadPhotocard(code) { success, err ->
                                                if (success) {
                                                    Toast.makeText(context, "Photocard design downloaded successfully!", Toast.LENGTH_LONG).show()
                                                } else {
                                                    Toast.makeText(context, "Failed to download: $err", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }
                                    },
                                )
                            }
                        }
                        else -> {}
                    }
                }
            },
            confirmButton = {
                when (subscribeState) {
                    is SubscribeState.Input -> {
                        val enabled =
                            subscribeState is SubscribeState.Idle && subscribeState.linkState.text.isNotBlank()
                        TextButton(
                            enabled = enabled,
                            onClick = {
                                focusManager.clearFocus()
                                val code = subscribeState.linkState.text.toString().trim()
                                if (code.isNotEmpty()) {
                                    subscribeViewModel.downloadPhotocard(code) { success, err ->
                                        if (success) {
                                            Toast.makeText(context, "Photocard design downloaded successfully!", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Failed to download: $err", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            }
                        ) {
                            Text(
                                text = "Download",
                            )
                        }
                    }
                    else -> {}
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        focusManager.clearFocus()
                        subscribeViewModel.hideDrawer()
                    }
                ) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }
}
