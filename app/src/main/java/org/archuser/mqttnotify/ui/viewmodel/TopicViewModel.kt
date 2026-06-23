package org.archuser.mqttnotify.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.archuser.mqttnotify.domain.model.TopicSubscriptionConfig
import org.archuser.mqttnotify.domain.repo.MessageRepository
import org.archuser.mqttnotify.domain.repo.TopicRepository

@HiltViewModel
class TopicViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val topicRepository: TopicRepository,
    private val messageRepository: MessageRepository
) : ViewModel() {

    private val channelId: Long = savedStateHandle.get<String>("channelId")?.toLongOrNull() ?: 0L

    private val _state = MutableStateFlow(ChannelUiState())
    val state: StateFlow<ChannelUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            topicRepository.observeChannels().collect { channels ->
                _state.value = _state.value.copy(channels = channels)
            }
        }

        if (channelId == 0L) {
            viewModelScope.launch {
                messageRepository.observeRecentMessages().collect { messages ->
                    _state.value = _state.value.copy(messages = messages)
                }
            }
        } else {
            viewModelScope.launch {
                topicRepository.getChannel(channelId)?.let { channel ->
                    _state.value = _state.value.copy(editing = channel, form = ChannelForm.from(channel))
                    messageRepository.observeMessagesForTopic(channel.topicFilter).collect { messages ->
                        _state.value = _state.value.copy(messages = messages)
                    }
                }
            }
        }
    }

    fun updateForm(form: ChannelForm) {
        _state.value = _state.value.copy(form = form, validationError = null)
    }

    fun saveChannel(onSaved: () -> Unit = {}) {
        val form = _state.value.form.normalized()
        val validationError = validateTopicFilter(form.topicFilter)
            ?: if (form.name.isBlank()) "Channel name cannot be empty" else null
        if (validationError != null) {
            _state.value = _state.value.copy(validationError = validationError)
            return
        }

        viewModelScope.launch {
            val current = _state.value.editing
            topicRepository.upsertTopic(
                TopicSubscriptionConfig(
                    id = current?.id ?: 0L,
                    brokerId = 0L,
                    displayName = form.name,
                    topicFilter = form.topicFilter,
                    qos = 1,
                    enabled = true,
                    notifyEnabled = form.notifyEnabled,
                    retainedAsNew = false,
                    hideRetained = form.hideRetained
                )
            )
            onSaved()
        }
    }

    fun deleteChannel(id: Long) {
        viewModelScope.launch {
            topicRepository.getChannel(id)?.let { channel ->
                messageRepository.deleteMessagesForTopic(channel.topicFilter)
            }
            topicRepository.deleteTopic(id)
        }
    }

    fun clearChannelHistory() {
        val channel = _state.value.editing ?: return
        viewModelScope.launch {
            messageRepository.deleteMessagesForTopic(channel.topicFilter)
        }
    }

    fun markAllRead() {
        val channel = _state.value.editing ?: return
        viewModelScope.launch {
            messageRepository.resetUnreadForTopicAcrossBrokers(channel.topicFilter)
        }
    }

    fun deleteMessage(id: Long) {
        viewModelScope.launch {
            messageRepository.deleteMessage(id)
        }
    }

    private fun validateTopicFilter(filter: String): String? {
        if (filter.isBlank()) return "Topic filter cannot be empty"
        if (filter.contains("//") || filter.startsWith("/") || filter.endsWith("/")) {
            return "Empty topic levels are not supported"
        }
        val levels = filter.split("/")
        val hashIndex = levels.indexOf("#")
        if (hashIndex != -1 && hashIndex != levels.lastIndex) return "# must be the final topic level"
        if (levels.any { level -> level.contains("#") && level != "#" }) return "# must occupy a full topic level"
        if (levels.any { level -> level.contains("+") && level != "+" }) return "+ must occupy a full topic level"
        return null
    }
}

data class ChannelUiState(
    val channels: List<TopicSubscriptionConfig> = emptyList(),
    val editing: TopicSubscriptionConfig? = null,
    val form: ChannelForm = ChannelForm(),
    val messages: List<org.archuser.mqttnotify.domain.model.InboundMessageRecord> = emptyList(),
    val validationError: String? = null
)

data class ChannelForm(
    val name: String = "",
    val topicFilter: String = "",
    val notifyEnabled: Boolean = true,
    val hideRetained: Boolean = true
) {
    fun normalized(): ChannelForm = copy(name = name.trim(), topicFilter = topicFilter.trim())

    companion object {
        fun from(channel: TopicSubscriptionConfig): ChannelForm = ChannelForm(
            name = channel.displayName,
            topicFilter = channel.topicFilter,
            notifyEnabled = channel.notifyEnabled,
            hideRetained = channel.hideRetained
        )
    }
}
