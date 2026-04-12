package com.example.mqttandroid.mqtt

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class PahoMqttClientGateway(
    private val appContext: Context
) : MqttClientGateway {
    private val callbackScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutableConnectionState =
        MutableStateFlow<MqttConnectionState>(MqttConnectionState.Disconnected)

    private var client: MqttAsyncClient? = null
    private var listener: (suspend (MqttIncomingMessage) -> Unit)? = null
    private var currentConfig: MqttConnectionConfig? = null

    override val connectionState: StateFlow<MqttConnectionState> = mutableConnectionState

    override suspend fun connect(config: MqttConnectionConfig) {
        currentConfig = config
        mutableConnectionState.value = MqttConnectionState.Connecting

        val existing = client
        if (existing != null && existing.isConnected) {
            mutableConnectionState.value = MqttConnectionState.Connected(config.serverUri)
            return
        }

        val mqttClient = existing ?: createClient(config).also { client = it }
        mqttClient.awaitConnect(config.toConnectOptions())
        mutableConnectionState.value = MqttConnectionState.Connected(config.serverUri)
    }

    override suspend fun disconnect() {
        val mqttClient = client ?: run {
            mutableConnectionState.value = MqttConnectionState.Disconnected
            return
        }
        if (mqttClient.isConnected) {
            mqttClient.awaitDisconnect()
        }
        mqttClient.close()
        client = null
        mutableConnectionState.value = MqttConnectionState.Disconnected
    }

    override suspend fun subscribe(topic: String, qos: Int) {
        val mqttClient = requireConnectedClient()
        mqttClient.awaitSubscribe(topic, qos)
    }

    override suspend fun unsubscribe(topic: String) {
        val mqttClient = requireConnectedClient()
        mqttClient.awaitUnsubscribe(topic)
    }

    override fun setListener(listener: suspend (MqttIncomingMessage) -> Unit) {
        this.listener = listener
    }

    private fun createClient(config: MqttConnectionConfig): MqttAsyncClient {
        return MqttAsyncClient(
            config.serverUri,
            config.clientId,
            MemoryPersistence()
        ).apply {
            setCallback(object : MqttCallbackExtended {
                override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                    val targetUri = serverURI ?: currentConfig?.serverUri.orEmpty()
                    mutableConnectionState.value = MqttConnectionState.Connected(targetUri)
                }

                override fun connectionLost(cause: Throwable?) {
                    mutableConnectionState.value = if (cause == null) {
                        MqttConnectionState.Disconnected
                    } else {
                        MqttConnectionState.Error(cause.message ?: "Connection lost")
                    }
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    val currentListener = listener ?: return
                    val safeTopic = topic ?: return
                    val safeMessage = message ?: return
                    callbackScope.launch {
                        currentListener(
                            MqttIncomingMessage(
                                topic = safeTopic,
                                payload = safeMessage.payload ?: ByteArray(0),
                                qos = safeMessage.qos,
                                retained = safeMessage.isRetained
                            )
                        )
                    }
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) = Unit
            })
        }
    }

    private fun requireConnectedClient(): MqttAsyncClient {
        val mqttClient = client ?: throw IllegalStateException("MQTT client is not connected")
        if (!mqttClient.isConnected) {
            throw IllegalStateException("MQTT client is not connected")
        }
        return mqttClient
    }

    private suspend fun MqttAsyncClient.awaitConnect(options: MqttConnectOptions) {
        suspendCancellableCoroutine { continuation ->
            connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    continuation.resume(Unit)
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    continuation.resumeWithException(exception ?: IllegalStateException("MQTT connect failed"))
                }
            })
        }
    }

    private suspend fun MqttAsyncClient.awaitDisconnect() {
        suspendCancellableCoroutine { continuation ->
            disconnect(null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    continuation.resume(Unit)
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    continuation.resumeWithException(exception ?: IllegalStateException("MQTT disconnect failed"))
                }
            })
        }
    }

    private suspend fun MqttAsyncClient.awaitSubscribe(topic: String, qos: Int) {
        suspendCancellableCoroutine { continuation ->
            subscribe(topic, qos, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    continuation.resume(Unit)
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    continuation.resumeWithException(exception ?: IllegalStateException("MQTT subscribe failed"))
                }
            })
        }
    }

    private suspend fun MqttAsyncClient.awaitUnsubscribe(topic: String) {
        suspendCancellableCoroutine { continuation ->
            unsubscribe(topic, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    continuation.resume(Unit)
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    continuation.resumeWithException(exception ?: IllegalStateException("MQTT unsubscribe failed"))
                }
            })
        }
    }
}

internal fun MqttConnectionConfig.toConnectOptionsForTest(): MqttConnectOptions = toConnectOptions()

private fun MqttConnectionConfig.toConnectOptions() = MqttConnectOptions().apply {
    isCleanSession = this@toConnectOptions.cleanSession
    keepAliveInterval = this@toConnectOptions.keepAliveSeconds
    isAutomaticReconnect = this@toConnectOptions.autoReconnect
    val normalizedUsername = this@toConnectOptions.username?.trim().takeUnless { it.isNullOrEmpty() }
    if (normalizedUsername != null) {
        userName = normalizedUsername
        password = this@toConnectOptions.password?.toCharArray()
    }
}
