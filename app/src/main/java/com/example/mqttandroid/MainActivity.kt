package com.example.mqttandroid

import android.Manifest
import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.mqttandroid.ui.setup.SetupScreenRoute
import com.example.mqttandroid.ui.theme.MqttAndroidTheme

class MainActivity : ComponentActivity() {
    private val notificationsPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        val appContainer = (application as MqttAndroidApplication).appContainer
        setContent {
            MqttAndroidTheme {
                MqttAndroidApp(appContainer)
            }
        }
    }
}

@Composable
private fun MqttAndroidApp(appContainer: com.example.mqttandroid.data.AppContainer) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        SetupScreenRoute(
            brokerRepository = appContainer.brokerRepository,
            topicRepository = appContainer.topicRepository,
            messageRepository = appContainer.messageRepository,
            defaultClientId = appContainer.startupClientId,
            connectionVerifier = appContainer.connectionVerifier
        )
    }
}
