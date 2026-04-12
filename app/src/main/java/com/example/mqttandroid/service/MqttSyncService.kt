package com.example.mqttandroid.service

import android.content.Context
import android.content.Intent
import android.app.Service
import android.os.IBinder
import com.example.mqttandroid.MqttAndroidApplication
import com.example.mqttandroid.data.model.StoredMessage
import com.example.mqttandroid.domain.IncomingMessageProcessor
import com.example.mqttandroid.mqtt.MqttClientGateway
import com.example.mqttandroid.mqtt.MqttConnectionConfig
import com.example.mqttandroid.mqtt.MqttConnectionState
import com.example.mqttandroid.mqtt.MqttReconnectPolicy
import com.example.mqttandroid.mqtt.PahoMqttClientGateway
import com.example.mqttandroid.notifications.MqttNotificationChannels
import com.example.mqttandroid.notifications.MqttNotificationFactory
import com.example.mqttandroid.speech.MqttEventSpeaker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MqttSyncService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val reconnectPolicy = MqttReconnectPolicy()
    private lateinit var notificationFactory: MqttNotificationFactory
    private lateinit var eventSpeaker: MqttEventSpeaker
    private lateinit var mqttGateway: MqttClientGateway
    private var syncJob: Job? = null

    companion object {
        private const val ACTION_START = "com.example.mqttandroid.action.START"
        private const val ACTION_STOP = "com.example.mqttandroid.action.STOP"

        fun start(context: Context) {
            val intent = Intent(context, MqttSyncService::class.java).setAction(ACTION_START)
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, MqttSyncService::class.java).setAction(ACTION_STOP)
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        MqttNotificationChannels.register(this)
        notificationFactory = MqttNotificationFactory(this)
        eventSpeaker = MqttEventSpeaker(this)
        mqttGateway = PahoMqttClientGateway(applicationContext)
        notificationFactory.updateForeground(this, MqttConnectionState.Connecting)

        mqttGateway.setListener { message ->
            handleIncomingMessage(
                topic = message.topic,
                payload = message.payload,
                qos = message.qos,
                retained = message.retained
            )
        }

        serviceScope.launch {
            mqttGateway.connectionState.collectLatest { state ->
                notificationFactory.updateForeground(this@MqttSyncService, state)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                syncJob?.cancel()
                serviceScope.launch {
                    mqttGateway.disconnect()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
                return START_NOT_STICKY
            }

            else -> {
                startSync()
                return START_STICKY
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        syncJob?.cancel()
        serviceScope.launch { mqttGateway.disconnect() }
        eventSpeaker.shutdown()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startSync() {
        syncJob?.cancel()
        syncJob = serviceScope.launch {
            val appContainer = (application as MqttAndroidApplication).appContainer
            val brokerConfig = appContainer.brokerRepository.getBrokerConfig()
            if (brokerConfig == null || !brokerConfig.syncEnabled) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return@launch
            }

            val activeTopics = appContainer.topicRepository.getTopics()
                .filter { it.subscriptionEnabled }
            if (activeTopics.isEmpty()) {
                notificationFactory.updateForeground(
                    this@MqttSyncService,
                    MqttConnectionState.Error("No active topics configured")
                )
                return@launch
            }

            var attempt = 0
            while (true) {
                try {
                    mqttGateway.connect(
                        MqttConnectionConfig(
                            serverUri = brokerConfig.serverUri,
                            clientId = brokerConfig.clientId,
                            username = brokerConfig.username,
                            password = brokerConfig.password,
                            cleanSession = brokerConfig.cleanSession,
                            keepAliveSeconds = brokerConfig.keepAliveSeconds,
                            autoReconnect = brokerConfig.autoReconnect
                        )
                    )
                    activeTopics.forEach { topic ->
                        mqttGateway.subscribe(topic.topic, topic.qos)
                        appContainer.topicRepository.setLastError(topic.id, null)
                    }
                    break
                } catch (error: Throwable) {
                    attempt += 1
                    activeTopics.forEach { topic ->
                        appContainer.topicRepository.setLastError(topic.id, error.message)
                    }
                    notificationFactory.updateForeground(
                        this@MqttSyncService,
                        MqttConnectionState.Error(error.message ?: "Connection failed")
                    )
                    delay(reconnectPolicy.delayForAttempt(attempt))
                }
            }
        }
    }

    private suspend fun handleIncomingMessage(
        topic: String,
        payload: ByteArray,
        qos: Int,
        retained: Boolean
    ) {
        val appContainer = (application as MqttAndroidApplication).appContainer
        val processor = IncomingMessageProcessor(
            topicLookup = { incomingTopic -> appContainer.topicRepository.findMatchingTopic(incomingTopic) },
            saveMessage = { storedMessage: StoredMessage -> appContainer.messageRepository.save(storedMessage) }
        )

        val processed = processor.process(topic, payload, qos, retained) ?: return
        val preview = payload.toPreviewString()
        notificationFactory.notifyIncomingMessage(
            topicId = processed.topic.id,
            topicLabel = processed.topic.displayName,
            payloadPreview = preview,
            notificationsEnabled = processed.topic.notificationsEnabled
        )
        eventSpeaker.announceIncomingEvent(
            topicLabel = processed.topic.displayName,
            topic = processed.topic.topic
        )
    }
}

private fun ByteArray.toPreviewString(): String {
    return runCatching { decodeToString() }.getOrDefault("<binary payload>")
}
