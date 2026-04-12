package com.example.mqttandroid.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.mqttandroid.data.repository.MessageRepository
import com.example.mqttandroid.data.repository.BrokerRepository
import com.example.mqttandroid.data.repository.TopicRepository
import com.example.mqttandroid.data.model.BrokerConfig
import com.example.mqttandroid.data.model.StoredMessage
import com.example.mqttandroid.domain.MqttConnectionVerifier
import com.example.mqttandroid.setup.initialSetupUiState
import com.example.mqttandroid.service.MqttSyncService
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.net.URI
import java.util.Locale

data class SavedTopicUiModel(
    val id: Long,
    val topic: String,
    val displayName: String,
    val qos: Int,
    val unreadCount: Int,
    val lastError: String?
)

data class TopicMessageUiModel(
    val id: Long,
    val payloadPreview: String,
    val receivedAtLabel: String,
    val isRead: Boolean
)

data class SetupUiState(
    val host: String = "",
    val port: String = "",
    val clientId: String = "",
    val username: String = "",
    val password: String = "",
    val tlsEnabled: Boolean = false,
    val topic: String = "",
    val topicDisplayName: String = "",
    val topicQos: String = "0",
    val savedTopics: List<SavedTopicUiModel> = emptyList(),
    val expandedTopicId: Long? = null,
    val expandedTopicMessages: List<TopicMessageUiModel> = emptyList(),
    val syncEnabled: Boolean = false,
    val isConnecting: Boolean = false,
    val statusUpdatedAtEpochMillis: Long? = null,
    val statusMessage: String? = null
)

@Composable
fun SetupScreenRoute(
    brokerRepository: BrokerRepository,
    topicRepository: TopicRepository,
    messageRepository: MessageRepository,
    defaultClientId: String,
    connectionVerifier: MqttConnectionVerifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember(defaultClientId) { mutableStateOf(initialSetupUiState(defaultClientId)) }
    val brokerConfig by brokerRepository.observeBrokerConfig().collectAsState(initial = null)
    val topicSummaries by topicRepository.observeTopicSummaries().collectAsState(initial = emptyList())

    fun currentSetupConfig(syncEnabled: Boolean): BrokerConfig? {
        val host = state.host.trim()
        val port = state.port.trim()
        val clientId = state.clientId.trim()
        if (host.isBlank() || port.isBlank() || clientId.isBlank()) {
            state = state.withStatus("Host, port, and client ID are required.")
            return null
        }

        val numericPort = port.toIntOrNull()
        if (numericPort == null) {
            state = state.withStatus("Port must be a number.")
            return null
        }

        return BrokerConfig(
            serverUri = buildServerUri(host, numericPort, state.tlsEnabled),
            clientId = clientId,
            username = state.username.trim().ifBlank { null },
            password = state.password.ifBlank { null },
            tlsEnabled = state.tlsEnabled,
            cleanSession = false,
            keepAliveSeconds = 60,
            syncEnabled = syncEnabled,
            updatedAtEpochMillis = System.currentTimeMillis()
        )
    }

    LaunchedEffect(brokerConfig) {
        val parsed = brokerConfig?.let(::parseBrokerConfig)
        state = state.copy(
            host = parsed?.host ?: state.host,
            port = parsed?.port ?: state.port,
            clientId = brokerConfig?.clientId ?: state.clientId,
            username = brokerConfig?.username.orEmpty(),
            password = brokerConfig?.password.orEmpty(),
            tlsEnabled = parsed?.tlsEnabled ?: state.tlsEnabled,
            syncEnabled = brokerConfig?.syncEnabled ?: state.syncEnabled
        )
    }

    LaunchedEffect(topicSummaries) {
        state = state.copy(
            savedTopics = topicSummaries.map { summary ->
                SavedTopicUiModel(
                    id = summary.id,
                    topic = summary.topic,
                    displayName = summary.displayName,
                    qos = summary.qos,
                    unreadCount = summary.unreadCount,
                    lastError = null
                )
            },
            expandedTopicId = state.expandedTopicId
                ?.takeIf { expandedId -> topicSummaries.any { it.id == expandedId } }
        )
    }

    LaunchedEffect(state.expandedTopicId) {
        val topicId = state.expandedTopicId ?: run {
            state = state.copy(expandedTopicMessages = emptyList())
            return@LaunchedEffect
        }
        messageRepository.observeMessages(topicId).collectLatest { messages ->
            state = state.copy(
                expandedTopicMessages = messages.map { it.toUiModel() }
            )
        }
    }

    SetupScreen(
        state = state,
        onHostChanged = { state = state.copy(host = it) },
        onPortChanged = { state = state.copy(port = it) },
        onClientIdChanged = { state = state.copy(clientId = it) },
        onUsernameChanged = { state = state.copy(username = it) },
        onPasswordChanged = { state = state.copy(password = it) },
        onTlsChanged = { state = state.copy(tlsEnabled = it) },
        onSaveBroker = {
            scope.launch {
                val config = currentSetupConfig(syncEnabled = state.syncEnabled) ?: return@launch
                brokerRepository.saveBrokerConfig(config)
                if (config.syncEnabled) {
                    MqttSyncService.start(context)
                }
                state = state.withStatus("Broker settings saved.")
            }
        },
        onTopicChanged = { state = state.copy(topic = it) },
        onTopicDisplayNameChanged = { state = state.copy(topicDisplayName = it) },
        onTopicQosChanged = {
            state = state.copy(topicQos = sanitizeTopicQosInput(state.topicQos, it))
        },
        onAddTopic = {
            scope.launch {
                val topic = state.topic.trim()
                if (topic.isBlank()) {
                    state = state.withStatus("Topic is required.")
                    return@launch
                }

                val qos = state.topicQos.trim().toIntOrNull()
                if (qos == null || qos !in 0..2) {
                    state = state.withStatus("QoS must be 0, 1, or 2.")
                    return@launch
                }

                topicRepository.saveTopic(
                    com.example.mqttandroid.data.model.TopicSubscription(
                        topic = topic,
                        displayName = state.topicDisplayName.trim().ifBlank { topic },
                        qos = qos
                    )
                )

                state = state.copy(
                    topic = "",
                    topicDisplayName = "",
                    topicQos = "0"
                ).withStatus("Topic saved.")
                if (state.syncEnabled) {
                    MqttSyncService.start(context)
                }
            }
        },
        onDeleteTopic = { topicId ->
            scope.launch {
                topicRepository.deleteTopic(topicId)
                state = state.copy(
                    expandedTopicId = if (state.expandedTopicId == topicId) null else state.expandedTopicId,
                    expandedTopicMessages = if (state.expandedTopicId == topicId) emptyList() else state.expandedTopicMessages
                ).withStatus("Topic removed.")
                if (state.syncEnabled) {
                    MqttSyncService.start(context)
                }
            }
        },
        onToggleTopicHistory = { topicId ->
            state = toggleExpandedTopic(state, topicId)
        },
        onMarkTopicRead = { topicId ->
            scope.launch {
                messageRepository.markTopicRead(topicId)
                state = state.withStatus("Marked topic as read.")
            }
        },
        onClearTopicHistory = { topicId ->
            scope.launch {
                messageRepository.clearTopicHistory(topicId)
                state = state.withStatus("Cleared topic messages.")
            }
        },
        onStartSync = {
            scope.launch {
                val config = currentSetupConfig(syncEnabled = true) ?: return@launch
                val topics = topicRepository.getTopics().filter { it.subscriptionEnabled }
                if (topics.isEmpty()) {
                    state = state.withStatus("Add at least one topic before connecting.")
                    return@launch
                }

                state = state.copy(isConnecting = true).withStatus("Connecting...")
                runCatching {
                    connectionVerifier.verify(config, topics)
                }.onSuccess {
                    brokerRepository.saveBrokerConfig(config)
                    MqttSyncService.start(context)
                    state = state.copy(
                        isConnecting = false,
                        syncEnabled = true
                    ).withStatus("Connected.")
                }.onFailure { error ->
                    state = state.copy(
                        isConnecting = false,
                        syncEnabled = false
                    ).withStatus(error.message ?: "Connection failed.")
                }
            }
        },
        onStopSync = {
            scope.launch {
                val existing = brokerConfig
                if (existing != null) {
                    brokerRepository.saveBrokerConfig(existing.copy(syncEnabled = false))
                }
                MqttSyncService.stop(context)
                state = state.copy(syncEnabled = false).withStatus("Listener stopped.")
            }
        }
    )
}

@Composable
fun SetupScreen(
    state: SetupUiState,
    onHostChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
    onClientIdChanged: (String) -> Unit,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onTlsChanged: (Boolean) -> Unit,
    onSaveBroker: () -> Unit,
    onTopicChanged: (String) -> Unit,
    onTopicDisplayNameChanged: (String) -> Unit,
    onTopicQosChanged: (String) -> Unit,
    onAddTopic: () -> Unit,
    onDeleteTopic: (Long) -> Unit,
    onToggleTopicHistory: (Long) -> Unit,
    onMarkTopicRead: (Long) -> Unit,
    onClearTopicHistory: (Long) -> Unit,
    onStartSync: () -> Unit,
    onStopSync: () -> Unit
) {
    val controlsEnabled = !state.isConnecting

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("mqtt-android", style = MaterialTheme.typography.headlineMedium)
        state.statusMessage?.let { message ->
            val timestamp = state.statusUpdatedAtEpochMillis
            val rendered = if (timestamp != null) {
                formatStatusLine(message, timestamp)
            } else {
                message
            }
            Text(rendered, style = MaterialTheme.typography.bodyMedium)
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Broker settings", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = state.host,
                    onValueChange = onHostChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Host") },
                    singleLine = true,
                    enabled = controlsEnabled
                )
                OutlinedTextField(
                    value = state.port,
                    onValueChange = onPortChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Port") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = controlsEnabled
                )
                OutlinedTextField(
                    value = state.clientId,
                    onValueChange = onClientIdChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Client ID") },
                    singleLine = true,
                    enabled = controlsEnabled
                )
                OutlinedTextField(
                    value = state.username,
                    onValueChange = onUsernameChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Username") },
                    singleLine = true,
                    enabled = controlsEnabled
                )
                OutlinedTextField(
                    value = state.password,
                    onValueChange = onPasswordChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    enabled = controlsEnabled
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("TLS")
                    Switch(
                        checked = state.tlsEnabled,
                        onCheckedChange = onTlsChanged,
                        enabled = controlsEnabled
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onSaveBroker, enabled = controlsEnabled) {
                        Text("Save broker")
                    }
                    Button(
                        onClick = if (state.syncEnabled) onStopSync else onStartSync,
                        enabled = controlsEnabled
                    ) {
                        if (state.isConnecting) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(18.dp)
                                    .testTag("connect-progress"),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Connecting...")
                        } else {
                            Text(if (state.syncEnabled) "Disconnect" else "Connect")
                        }
                    }
                }
                Text(
                    if (state.syncEnabled) "Listener running" else "Listener stopped",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Topic subscriptions", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = state.topic,
                    onValueChange = onTopicChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Topic") },
                    singleLine = true,
                    enabled = controlsEnabled
                )
                OutlinedTextField(
                    value = state.topicDisplayName,
                    onValueChange = onTopicDisplayNameChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Display name") },
                    singleLine = true,
                    enabled = controlsEnabled
                )
                OutlinedTextField(
                    value = state.topicQos,
                    onValueChange = onTopicQosChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("QoS") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = controlsEnabled
                )
                Button(onClick = onAddTopic, enabled = controlsEnabled) {
                    Text("Add topic")
                }

                if (state.savedTopics.isEmpty()) {
                    Text("No topics configured yet.")
                } else {
                    state.savedTopics.forEach { topic ->
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(topic.displayName, style = MaterialTheme.typography.titleMedium)
                                Text(topic.topic, style = MaterialTheme.typography.bodyMedium)
                                Text("QoS ${topic.qos}", style = MaterialTheme.typography.bodySmall)
                                Text("${topic.unreadCount} unread", style = MaterialTheme.typography.bodySmall)
                                topic.lastError?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onToggleTopicHistory(topic.id) },
                                    enabled = controlsEnabled
                                ) {
                                    Text(if (state.expandedTopicId == topic.id) "Hide" else "History")
                                }
                                Button(
                                    onClick = { onDeleteTopic(topic.id) },
                                    enabled = controlsEnabled
                                ) {
                                    Text("Remove")
                                }
                            }
                        }
                        if (state.expandedTopicId == topic.id) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { onMarkTopicRead(topic.id) },
                                        enabled = controlsEnabled
                                    ) {
                                        Text("Mark all as read")
                                    }
                                    Button(
                                        onClick = { onClearTopicHistory(topic.id) },
                                        enabled = controlsEnabled
                                    ) {
                                        Text("Clear all messages")
                                    }
                                }

                                if (state.expandedTopicMessages.isEmpty()) {
                                    Text("No messages received yet.")
                                } else {
                                    state.expandedTopicMessages.forEach { message ->
                                        Card(modifier = Modifier.fillMaxWidth()) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    message.receivedAtLabel,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                                Text(
                                                    message.payloadPreview,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Text(
                                                    if (message.isRead) "Read" else "Unread",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class ParsedBrokerConfig(
    val host: String,
    val port: String,
    val tlsEnabled: Boolean
)

private fun parseBrokerConfig(config: BrokerConfig): ParsedBrokerConfig? {
    val uri = runCatching { URI(config.serverUri) }.getOrNull() ?: return null
    val host = uri.host ?: return null
    val port = uri.port.takeIf { it != -1 }?.toString().orEmpty()
    return ParsedBrokerConfig(
        host = host,
        port = port,
        tlsEnabled = config.tlsEnabled || uri.scheme.equals("ssl", ignoreCase = true)
    )
}

private fun buildServerUri(host: String, port: Int, tlsEnabled: Boolean): String {
    val scheme = if (tlsEnabled) "ssl" else "tcp"
    return "$scheme://$host:$port"
}

internal fun sanitizeTopicQosInput(current: String, input: String): String {
    if (input.isEmpty()) return ""
    if (input.length != 1) return current
    return input.takeIf { it == "0" || it == "1" || it == "2" } ?: current
}

internal fun toggleExpandedTopic(
    state: SetupUiState,
    topicId: Long
): SetupUiState = if (state.expandedTopicId == topicId) {
    state.copy(
        expandedTopicId = null,
        expandedTopicMessages = emptyList()
    )
} else {
    state.copy(
        expandedTopicId = topicId,
        expandedTopicMessages = emptyList()
    )
}

private fun SetupUiState.withStatus(
    message: String,
    now: Long = System.currentTimeMillis()
): SetupUiState = copy(
    statusMessage = message,
    statusUpdatedAtEpochMillis = now
)

internal fun formatStatusLine(
    message: String,
    statusUpdatedAtEpochMillis: Long,
    zoneId: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault()
): String {
    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss", locale)
    val time = Instant.ofEpochMilli(statusUpdatedAtEpochMillis).atZone(zoneId)
    return "${formatter.format(time)} $message"
}

private fun StoredMessage.toUiModel(
    zoneId: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault()
) = TopicMessageUiModel(
    id = id,
    payloadPreview = payload.toDisplayString(),
    receivedAtLabel = formatMessageTimestamp(receivedAtEpochMillis, zoneId, locale),
    isRead = isRead
)

private fun formatMessageTimestamp(
    epochMillis: Long,
    zoneId: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault()
): String {
    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss", locale)
    return formatter.format(Instant.ofEpochMilli(epochMillis).atZone(zoneId))
}

private fun ByteArray.toDisplayString(): String =
    runCatching { decodeToString() }.getOrDefault("<binary payload>")
