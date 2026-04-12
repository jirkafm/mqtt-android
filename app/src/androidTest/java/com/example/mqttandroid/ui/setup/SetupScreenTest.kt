package com.example.mqttandroid.ui.setup

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performClick
import com.example.mqttandroid.setup.initialSetupUiState
import com.example.mqttandroid.ui.theme.MqttAndroidTheme
import org.junit.Rule
import org.junit.Test

class SetupScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun setupScreenShowsBrokerAndTopicInputs() {
        composeRule.setContent {
            MqttAndroidTheme {
                SetupScreen(
                    state = initialSetupUiState(defaultClientId = "android-test-id"),
                    onHostChanged = {},
                    onPortChanged = {},
                    onClientIdChanged = {},
                    onUsernameChanged = {},
                    onPasswordChanged = {},
                    onTlsChanged = {},
                    onSaveBroker = {},
                    onTopicChanged = {},
                    onTopicDisplayNameChanged = {},
                    onTopicQosChanged = {},
                    onAddTopic = {},
                    onDeleteTopic = {},
                    onToggleTopicHistory = {},
                    onMarkTopicRead = {},
                    onClearTopicHistory = {},
                    onStartSync = {},
                    onStopSync = {}
                )
            }
        }

        composeRule.onNodeWithText("Broker settings").assertIsDisplayed()
        composeRule.onNodeWithText("Host").assertIsDisplayed()
        composeRule.onNodeWithText("Port").assertIsDisplayed()
        composeRule.onNodeWithText("1883").assertIsDisplayed()
        composeRule.onNodeWithText("Client ID").assertIsDisplayed()
        composeRule.onNodeWithText("android-test-id").assertIsDisplayed()
        composeRule.onNodeWithText("Save broker").assertIsDisplayed()
        composeRule.onNodeWithText("Connect").assertIsDisplayed()
        composeRule.onAllNodesWithText("Start listener").assertCountEquals(0)
        composeRule.onNodeWithText("Topic subscriptions").assertExists()
        composeRule.onNodeWithText("Add topic").assertExists()
    }

    @Test
    fun setupScreenShowsConnectingFeedback() {
        composeRule.setContent {
            MqttAndroidTheme {
                SetupScreen(
                    state = initialSetupUiState(defaultClientId = "android-test-id")
                        .copy(isConnecting = true),
                    onHostChanged = {},
                    onPortChanged = {},
                    onClientIdChanged = {},
                    onUsernameChanged = {},
                    onPasswordChanged = {},
                    onTlsChanged = {},
                    onSaveBroker = {},
                    onTopicChanged = {},
                    onTopicDisplayNameChanged = {},
                    onTopicQosChanged = {},
                    onAddTopic = {},
                    onDeleteTopic = {},
                    onToggleTopicHistory = {},
                    onMarkTopicRead = {},
                    onClearTopicHistory = {},
                    onStartSync = {},
                    onStopSync = {}
                )
            }
        }

        composeRule.onNodeWithText("Connecting...").assertIsDisplayed()
        composeRule.onNodeWithTag("connect-progress").assertIsDisplayed()
        composeRule.onNodeWithText("Save broker").assertIsNotEnabled()
        composeRule.onNodeWithText("Add topic").assertIsNotEnabled()
        composeRule.onNodeWithText("Connecting...").assertIsNotEnabled()
    }

    @Test
    fun expandedTopicShowsMessageHistoryAndActions() {
        composeRule.setContent {
            MqttAndroidTheme {
                SetupScreen(
                    state = initialSetupUiState(defaultClientId = "android-test-id").copy(
                        savedTopics = listOf(
                            SavedTopicUiModel(
                                id = 1,
                                topic = "alerts/frontdoor",
                                displayName = "Front Door",
                                qos = 1,
                                unreadCount = 5,
                                lastError = null
                            )
                        ),
                        expandedTopicId = 1,
                        expandedTopicMessages = listOf(
                            TopicMessageUiModel(
                                id = 11,
                                payloadPreview = "door opened",
                                receivedAtLabel = "14:27:31",
                                isRead = false
                            )
                        )
                    ),
                    onHostChanged = {},
                    onPortChanged = {},
                    onClientIdChanged = {},
                    onUsernameChanged = {},
                    onPasswordChanged = {},
                    onTlsChanged = {},
                    onSaveBroker = {},
                    onTopicChanged = {},
                    onTopicDisplayNameChanged = {},
                    onTopicQosChanged = {},
                    onAddTopic = {},
                    onDeleteTopic = {},
                    onToggleTopicHistory = {},
                    onMarkTopicRead = {},
                    onClearTopicHistory = {},
                    onStartSync = {},
                    onStopSync = {}
                )
            }
        }

        composeRule.onNodeWithText("door opened").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Mark all as read").assertIsDisplayed()
        composeRule.onNodeWithText("Clear all messages").assertIsDisplayed()
    }

    @Test
    fun topicHistoryButtonsInvokeCallbacksForExpandedTopic() {
        var toggledTopicId: Long? = null
        var markedTopicId: Long? = null
        var clearedTopicId: Long? = null

        composeRule.setContent {
            MqttAndroidTheme {
                SetupScreen(
                    state = initialSetupUiState(defaultClientId = "android-test-id").copy(
                        savedTopics = listOf(
                            SavedTopicUiModel(
                                id = 1,
                                topic = "alerts/frontdoor",
                                displayName = "Front Door",
                                qos = 1,
                                unreadCount = 5,
                                lastError = null
                            )
                        ),
                        expandedTopicId = 1,
                        expandedTopicMessages = listOf(
                            TopicMessageUiModel(
                                id = 11,
                                payloadPreview = "door opened",
                                receivedAtLabel = "14:27:31",
                                isRead = false
                            )
                        )
                    ),
                    onHostChanged = {},
                    onPortChanged = {},
                    onClientIdChanged = {},
                    onUsernameChanged = {},
                    onPasswordChanged = {},
                    onTlsChanged = {},
                    onSaveBroker = {},
                    onTopicChanged = {},
                    onTopicDisplayNameChanged = {},
                    onTopicQosChanged = {},
                    onAddTopic = {},
                    onDeleteTopic = {},
                    onToggleTopicHistory = { toggledTopicId = it },
                    onMarkTopicRead = { markedTopicId = it },
                    onClearTopicHistory = { clearedTopicId = it },
                    onStartSync = {},
                    onStopSync = {}
                )
            }
        }

        composeRule.onNodeWithText("door opened").performScrollTo()
        composeRule.onNodeWithText("Hide").performClick()
        composeRule.onNodeWithText("Mark all as read").performClick()
        composeRule.onNodeWithText("Clear all messages").performClick()

        org.junit.Assert.assertEquals(1L, toggledTopicId)
        org.junit.Assert.assertEquals(1L, markedTopicId)
        org.junit.Assert.assertEquals(1L, clearedTopicId)
    }
}
