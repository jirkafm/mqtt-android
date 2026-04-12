package com.example.mqttandroid.ui.setup

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SetupTopicHistoryStateTest {
    @Test
    fun toggleExpandedTopicCollapsesCurrentTopicAndClearsMessages() {
        val state = SetupUiState(
            expandedTopicId = 7L,
            expandedTopicMessages = listOf(
                TopicMessageUiModel(
                    id = 11L,
                    payloadPreview = "door opened",
                    receivedAtLabel = "14:27:31",
                    isRead = false
                )
            )
        )

        val updated = toggleExpandedTopic(state, topicId = 7L)

        assertNull(updated.expandedTopicId)
        assertEquals(emptyList(), updated.expandedTopicMessages)
    }

    @Test
    fun toggleExpandedTopicSwitchesTopicAndClearsStaleMessages() {
        val state = SetupUiState(
            expandedTopicId = 7L,
            expandedTopicMessages = listOf(
                TopicMessageUiModel(
                    id = 11L,
                    payloadPreview = "door opened",
                    receivedAtLabel = "14:27:31",
                    isRead = false
                )
            )
        )

        val updated = toggleExpandedTopic(state, topicId = 9L)

        assertEquals(9L, updated.expandedTopicId)
        assertEquals(emptyList(), updated.expandedTopicMessages)
    }
}
