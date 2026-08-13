package me.ash.reader.ui.page.home.feeds.subscribe

import android.content.Context
import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rometools.rome.feed.synd.SyndFeed
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.InputStream
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.ash.reader.R
import me.ash.reader.domain.model.group.Group
import me.ash.reader.domain.service.AccountService
import me.ash.reader.domain.service.OpmlService
import me.ash.reader.domain.service.RssService
import me.ash.reader.infrastructure.android.AndroidStringsHelper
import me.ash.reader.infrastructure.di.ApplicationScope
import me.ash.reader.infrastructure.rss.RssHelper
import me.ash.reader.ui.ext.formatUrl
import me.ash.reader.ui.page.home.feeds.photocard.PhotocardManager

@HiltViewModel
class SubscribeViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val opmlService: OpmlService,
    val rssService: RssService,
    private val rssHelper: RssHelper,
    private val androidStringsHelper: AndroidStringsHelper,
    @ApplicationScope private val applicationScope: CoroutineScope,
    accountService: AccountService,
) : ViewModel() {

    private val _subscribeUiState = MutableStateFlow(SubscribeUiState())
    val subscribeUiState: StateFlow<SubscribeUiState> = _subscribeUiState.asStateFlow()

    private val _subscribeState: MutableStateFlow<SubscribeState> =
        MutableStateFlow(SubscribeState.Hidden)
    val subscribeState = _subscribeState.asStateFlow()

    val groupsFlow = MutableStateFlow<List<Group>>(emptyList())

    init {
        viewModelScope.launch {
            accountService.currentAccountFlow.collectLatest {
                rssService.get().pullGroups().collect { groupsFlow.value = it }
            }
        }
    }

    fun reset() {
        cancelSearch()
    }

    fun downloadPhotocard(code: String, onFinished: (Boolean, String?) -> Unit = { _, _ -> }) {
        val currentState = _subscribeState.value
        if (currentState !is SubscribeState.Idle) return

        val job = viewModelScope.launch {
            _subscribeState.value = SubscribeState.Fetching(
                linkState = currentState.linkState,
                job = coroutineContext[Job] ?: Job()
            )

            val result = PhotocardManager.downloadAndExtractZip(context, code)
            if (result.isSuccess) {
                _subscribeState.value = SubscribeState.Hidden
                onFinished(true, null)
            } else {
                val errorMsg = "Nothing is found"
                _subscribeState.value = SubscribeState.Idle(
                    linkState = currentState.linkState,
                    errorMessage = errorMsg
                )
                onFinished(false, errorMsg)
            }
        }

        _subscribeState.value = SubscribeState.Fetching(linkState = currentState.linkState, job = job)
    }

    fun cancelSearch() {
        _subscribeState.value.let {
            if (it is SubscribeState.Fetching && it.job.isActive) {
                it.job.cancel()
            }
        }
    }

    fun inputNewGroup(content: String) {
        _subscribeUiState.update { it.copy(newGroupContent = content) }
    }

    fun handleSharedUrlFromIntent(url: String) {
        viewModelScope.launch {
            _subscribeState.update { SubscribeState.Idle(linkState = TextFieldState(url)) }
            delay(50)
            downloadPhotocard(url)
        }
    }

    fun showDrawer() {
        _subscribeState.value = SubscribeState.Idle()
    }

    fun hideDrawer() {
        cancelSearch()
        _subscribeState.value = SubscribeState.Hidden
    }

    // Unused but kept for compatibility or stubs
    fun importFromInputStream(inputStream: InputStream) {}
    fun selectedGroup(groupId: String) {}
    fun addNewGroup() {}
    fun toggleParseFullContentPreset() {}
    fun toggleOpenInBrowserPreset() {}
    fun toggleAllowNotificationPreset() {}
    fun searchFeed() {}
    fun subscribe() {}
    fun showNewGroupDialog() {}
    fun hideNewGroupDialog() {}
    fun showRenameDialog() {}
    fun hideRenameDialog() {}
    fun inputNewName(content: String) {}
    fun renameFeed() {}
}

data class SubscribeUiState(
    val newGroupDialogVisible: Boolean = false,
    val newGroupContent: String = "",
    val newName: String = "",
    val renameDialogVisible: Boolean = false,
)

sealed interface SubscribeState {
    object Hidden : SubscribeState

    sealed interface Visible

    sealed interface Input : SubscribeState, Visible {
        val linkState: TextFieldState
    }

    data class Idle(
        override val linkState: TextFieldState = TextFieldState(),
        val importFromOpmlEnabled: Boolean = false,
        val errorMessage: String? = null,
    ) : SubscribeState, Input

    data class Fetching(override val linkState: TextFieldState, val job: Job) :
        SubscribeState, Input

    data class Configure(
        val searchedFeed: SyndFeed,
        val feedLink: String,
        val groups: List<Group> = emptyList(),
        val notification: Boolean = true,
        val fullContent: Boolean = false,
        val browser: Boolean = false,
        val selectedGroupId: String,
    ) : SubscribeState, Visible
}
