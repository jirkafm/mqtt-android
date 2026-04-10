# MQTT Mail Client Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a fresh Android app that connects to one MQTT broker, subscribes to multiple topics, stores message history per topic, posts notifications for new messages, and keeps syncing in the background via a foreground service.

**Architecture:** Scaffold a single-module Android app using Jetpack Compose, Room, and a foreground service. Isolate the MQTT connection in a dedicated `mqtt` package, persist all broker/topic/message state in Room, expose UI state through view models, and route incoming messages through storage before notification delivery.

**Tech Stack:** Kotlin, Android Gradle Plugin, Jetpack Compose, Room, Kotlin Coroutines/Flow, Android foreground services, BootReceiver, an Android-compatible MQTT client library, JUnit, AndroidX test libraries

---

### Task 1: Scaffold The Android Project

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/example/mqttmailclient/MainActivity.kt`
- Create: `app/src/main/java/com/example/mqttmailclient/MqttMailApplication.kt`
- Create: `app/src/main/java/com/example/mqttmailclient/ui/theme/Color.kt`
- Create: `app/src/main/java/com/example/mqttmailclient/ui/theme/Theme.kt`
- Create: `app/src/main/java/com/example/mqttmailclient/ui/theme/Type.kt`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/themes.xml`
- Create: `app/src/main/res/xml/backup_rules.xml`
- Create: `app/src/main/res/xml/data_extraction_rules.xml`

- [ ] **Step 1: Write the failing smoke test plan**

```kotlin
// app/src/test/java/com/example/mqttmailclient/AppSmokeTest.kt
package com.example.mqttmailclient

import org.junit.Test
import kotlin.test.assertTrue

class AppSmokeTest {
    @Test
    fun placeholder() {
        assertTrue(true)
    }
}
```

- [ ] **Step 2: Create the Gradle settings file**

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "mqtt-android"
include(":app")
```

- [ ] **Step 3: Create the root build script**

```kotlin
// build.gradle.kts
plugins {
    id("com.android.application") version "8.8.2" apply false
    id("org.jetbrains.kotlin.android") version "2.1.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.20" apply false
    id("com.google.devtools.ksp") version "2.1.20-1.0.32" apply false
}
```

- [ ] **Step 4: Create the app Gradle module**

```kotlin
// app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.example.mqttmailclient"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.mqttmailclient"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.02.00")

    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.1.20")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
```

- [ ] **Step 5: Create the base manifest and app entry points**

```xml
<!-- app/src/main/AndroidManifest.xml -->
<manifest package="com.example.mqttmailclient">
    <application
        android:name=".MqttMailApplication"
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.MqttMailClient">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

```kotlin
// app/src/main/java/com/example/mqttmailclient/MqttMailApplication.kt
package com.example.mqttmailclient

import android.app.Application

class MqttMailApplication : Application()
```

```kotlin
// app/src/main/java/com/example/mqttmailclient/MainActivity.kt
package com.example.mqttmailclient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.example.mqttmailclient.ui.theme.MqttMailTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MqttMailTheme {
                Surface {
                    Text("MQTT Mail")
                }
            }
        }
    }
}
```

- [ ] **Step 6: Run the unit test to verify the scaffold builds**

Run: `./gradlew testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Initialize git if the repository is still plain filesystem**

Run: `git init`
Expected: `Initialized empty Git repository`

- [ ] **Step 8: Commit the scaffold**

```bash
git add .
git commit -m "chore: scaffold Android compose app"
```

### Task 2: Add Persistence Models And Repository Contracts

**Files:**
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/java/com/example/mqttmailclient/data/db/MqttDatabase.kt`
- Create: `app/src/main/java/com/example/mqttmailclient/data/db/entity/BrokerConfigEntity.kt`
- Create: `app/src/main/java/com/example/mqttmailclient/data/db/entity/TopicSubscriptionEntity.kt`
- Create: `app/src/main/java/com/example/mqttmailclient/data/db/entity/ReceivedMessageEntity.kt`
- Create: `app/src/main/java/com/example/mqttmailclient/data/db/dao/BrokerConfigDao.kt`
- Create: `app/src/main/java/com/example/mqttmailclient/data/db/dao/TopicSubscriptionDao.kt`
- Create: `app/src/main/java/com/example/mqttmailclient/data/db/dao/ReceivedMessageDao.kt`
- Create: `app/src/main/java/com/example/mqttmailclient/data/repository/BrokerRepository.kt`
- Create: `app/src/main/java/com/example/mqttmailclient/data/repository/TopicRepository.kt`
- Create: `app/src/main/java/com/example/mqttmailclient/data/repository/MessageRepository.kt`
- Create: `app/src/test/java/com/example/mqttmailclient/data/repository/TopicRepositoryTest.kt`
- Create: `app/src/test/java/com/example/mqttmailclient/data/repository/MessageRepositoryTest.kt`

- [ ] **Step 1: Write the failing repository tests**

```kotlin
// app/src/test/java/com/example/mqttmailclient/data/repository/MessageRepositoryTest.kt
package com.example.mqttmailclient.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals

class MessageRepositoryTest {
    @Test
    fun clearHistoryRemovesOnlyMessagesForTargetTopic() {
        val repository = InMemoryMessageRepository()
        repository.save(topicId = 1L, payload = "one")
        repository.save(topicId = 2L, payload = "two")

        repository.clearTopicHistory(1L)

        assertEquals(0, repository.messagesFor(1L).size)
        assertEquals(1, repository.messagesFor(2L).size)
    }
}
```

```kotlin
// app/src/test/java/com/example/mqttmailclient/data/repository/TopicRepositoryTest.kt
package com.example.mqttmailclient.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals

class TopicRepositoryTest {
    @Test
    fun unreadCountReflectsUnreadMessagesForEachTopic() {
        val repository = TopicListAggregator()

        val summaries = repository.build(
            unreadCounts = mapOf(1L to 2, 2L to 1)
        )

        assertEquals(2, summaries.first { it.id == 1L }.unreadCount)
        assertEquals(1, summaries.first { it.id == 2L }.unreadCount)
    }
}
```

- [ ] **Step 2: Add Room and coroutine dependencies**

```kotlin
// app/build.gradle.kts dependency block additions
implementation("androidx.room:room-runtime:2.7.1")
implementation("androidx.room:room-ktx:2.7.1")
ksp("androidx.room:room-compiler:2.7.1")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
testImplementation("androidx.room:room-testing:2.7.1")
```

- [ ] **Step 3: Define the Room entities**

```kotlin
// app/src/main/java/com/example/mqttmailclient/data/db/entity/TopicSubscriptionEntity.kt
package com.example.mqttmailclient.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "topic_subscriptions")
data class TopicSubscriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val topic: String,
    val displayName: String,
    val qos: Int,
    val notificationsEnabled: Boolean,
    val subscriptionEnabled: Boolean,
    val lastError: String? = null
)
```

```kotlin
// app/src/main/java/com/example/mqttmailclient/data/db/entity/ReceivedMessageEntity.kt
package com.example.mqttmailclient.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "received_messages",
    foreignKeys = [
        ForeignKey(
            entity = TopicSubscriptionEntity::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("topicId"), Index("receivedAtEpochMillis")]
)
data class ReceivedMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val topicId: Long,
    val topic: String,
    val payload: String,
    val qos: Int,
    val retained: Boolean,
    val receivedAtEpochMillis: Long,
    val isRead: Boolean
)
```

- [ ] **Step 4: Create DAO contracts and database shell**

```kotlin
// app/src/main/java/com/example/mqttmailclient/data/db/dao/ReceivedMessageDao.kt
package com.example.mqttmailclient.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.mqttmailclient.data.db.entity.ReceivedMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceivedMessageDao {
    @Insert
    suspend fun insert(message: ReceivedMessageEntity): Long

    @Query("SELECT * FROM received_messages WHERE topicId = :topicId ORDER BY receivedAtEpochMillis DESC")
    fun observeTopicMessages(topicId: Long): Flow<List<ReceivedMessageEntity>>

    @Query("DELETE FROM received_messages WHERE topicId = :topicId")
    suspend fun clearTopicHistory(topicId: Long)

    @Query("UPDATE received_messages SET isRead = 1 WHERE topicId = :topicId AND isRead = 0")
    suspend fun markTopicRead(topicId: Long)
}
```

```kotlin
// app/src/main/java/com/example/mqttmailclient/data/db/MqttDatabase.kt
package com.example.mqttmailclient.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.mqttmailclient.data.db.dao.BrokerConfigDao
import com.example.mqttmailclient.data.db.dao.ReceivedMessageDao
import com.example.mqttmailclient.data.db.dao.TopicSubscriptionDao
import com.example.mqttmailclient.data.db.entity.BrokerConfigEntity
import com.example.mqttmailclient.data.db.entity.ReceivedMessageEntity
import com.example.mqttmailclient.data.db.entity.TopicSubscriptionEntity

@Database(
    entities = [
        BrokerConfigEntity::class,
        TopicSubscriptionEntity::class,
        ReceivedMessageEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class MqttDatabase : RoomDatabase() {
    abstract fun brokerConfigDao(): BrokerConfigDao
    abstract fun topicSubscriptionDao(): TopicSubscriptionDao
    abstract fun receivedMessageDao(): ReceivedMessageDao
}
```

- [ ] **Step 5: Implement repository interfaces with clear responsibilities**

```kotlin
// app/src/main/java/com/example/mqttmailclient/data/repository/MessageRepository.kt
package com.example.mqttmailclient.data.repository

import com.example.mqttmailclient.data.db.dao.ReceivedMessageDao
import com.example.mqttmailclient.data.db.entity.ReceivedMessageEntity
import kotlinx.coroutines.flow.Flow

class MessageRepository(
    private val messageDao: ReceivedMessageDao
) {
    fun observeMessages(topicId: Long): Flow<List<ReceivedMessageEntity>> =
        messageDao.observeTopicMessages(topicId)

    suspend fun save(message: ReceivedMessageEntity): Long =
        messageDao.insert(message)

    suspend fun clearTopicHistory(topicId: Long) =
        messageDao.clearTopicHistory(topicId)

    suspend fun markTopicRead(topicId: Long) =
        messageDao.markTopicRead(topicId)
}
```

- [ ] **Step 6: Run the repository tests**

Run: `./gradlew testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit the persistence layer**

```bash
git add app/build.gradle.kts app/src/main docs
git commit -m "feat: add Room persistence for broker topics and messages"
```

### Task 3: Build The MQTT Connection Layer

**Files:**
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/java/com/example/mqttmailclient/mqtt/MqttConnectionConfig.kt`
- Create: `app/src/main/java/com/example/mqttmailclient/mqtt/MqttIncomingMessage.kt`
- Create: `app/src/main/java/com/example/mqttmailclient/mqtt/MqttConnectionState.kt`
- Create: `app/src/main/java/com/example/mqttmailclient/mqtt/MqttClientGateway.kt`
- Create: `app/src/main/java/com/example/mqttmailclient/mqtt/PahoMqttClientGateway.kt`
- Create: `app/src/test/java/com/example/mqttmailclient/mqtt/MqttReconnectPolicyTest.kt`

- [ ] **Step 1: Write the failing reconnect policy test**

```kotlin
// app/src/test/java/com/example/mqttmailclient/mqtt/MqttReconnectPolicyTest.kt
package com.example.mqttmailclient.mqtt

import kotlin.test.Test
import kotlin.test.assertEquals

class MqttReconnectPolicyTest {
    @Test
    fun delayCapsAtMaximumBackoff() {
        val policy = MqttReconnectPolicy(
            baseDelayMillis = 1_000,
            maxDelayMillis = 60_000
        )

        assertEquals(60_000, policy.delayForAttempt(10))
    }
}
```

- [ ] **Step 2: Add the MQTT client dependency**

```kotlin
// app/build.gradle.kts dependency block additions
implementation("org.eclipse.paho:org.eclipse.paho.android.service:1.1.1")
implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
```

- [ ] **Step 3: Create the MQTT gateway contract**

```kotlin
// app/src/main/java/com/example/mqttmailclient/mqtt/MqttClientGateway.kt
package com.example.mqttmailclient.mqtt

import kotlinx.coroutines.flow.StateFlow

interface MqttClientGateway {
    val connectionState: StateFlow<MqttConnectionState>

    suspend fun connect(config: MqttConnectionConfig)
    suspend fun disconnect()
    suspend fun subscribe(topic: String, qos: Int)
    suspend fun unsubscribe(topic: String)
    fun setListener(listener: suspend (MqttIncomingMessage) -> Unit)
}
```

- [ ] **Step 4: Implement the reconnect policy and Paho-backed client**

```kotlin
// app/src/main/java/com/example/mqttmailclient/mqtt/MqttReconnectPolicy.kt
package com.example.mqttmailclient.mqtt

import kotlin.math.min
import kotlin.math.pow

class MqttReconnectPolicy(
    private val baseDelayMillis: Long = 1_000,
    private val maxDelayMillis: Long = 60_000
) {
    fun delayForAttempt(attempt: Int): Long {
        val multiplier = 2.0.pow(attempt.coerceAtLeast(0)).toLong()
        return min(baseDelayMillis * multiplier, maxDelayMillis)
    }
}
```

```kotlin
// app/src/main/java/com/example/mqttmailclient/mqtt/PahoMqttClientGateway.kt
package com.example.mqttmailclient.mqtt

class PahoMqttClientGateway(
    private val appContext: android.content.Context
) : MqttClientGateway {
    override val connectionState = kotlinx.coroutines.flow.MutableStateFlow<MqttConnectionState>(
        MqttConnectionState.Disconnected
    )

    private var listener: (suspend (MqttIncomingMessage) -> Unit)? = null

    override suspend fun connect(config: MqttConnectionConfig) {
        connectionState.value = MqttConnectionState.Connecting
        // Create Android client, build options, connect, then set Connected
    }

    override suspend fun disconnect() {
        connectionState.value = MqttConnectionState.Disconnected
    }

    override suspend fun subscribe(topic: String, qos: Int) = Unit

    override suspend fun unsubscribe(topic: String) = Unit

    override fun setListener(listener: suspend (MqttIncomingMessage) -> Unit) {
        this.listener = listener
    }
}
```

- [ ] **Step 5: Run the MQTT unit tests**

Run: `./gradlew testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit the MQTT layer**

```bash
git add app/build.gradle.kts app/src/main/java/com/example/mqttmailclient/mqtt app/src/test/java/com/example/mqttmailclient/mqtt
git commit -m "feat: add MQTT gateway and reconnect policy"
```

### Task 4: Add Notifications And The Foreground Sync Service

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/example/mqttmailclient/notifications/MqttNotificationChannels.kt`
- Create: `app/src/main/java/com/example/mqttmailclient/notifications/MqttNotificationFactory.kt`
- Create: `app/src/main/java/com/example/mqttmailclient/service/MqttSyncService.kt`
- Create: `app/src/test/java/com/example/mqttmailclient/notifications/MqttNotificationFactoryTest.kt`

- [ ] **Step 1: Write the failing notification decision test**

```kotlin
// app/src/test/java/com/example/mqttmailclient/notifications/MqttNotificationFactoryTest.kt
package com.example.mqttmailclient.notifications

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MqttNotificationFactoryTest {
    @Test
    fun topicNotificationRequiresTopicOptIn() {
        val factory = MqttNotificationDecision()

        assertTrue(factory.shouldNotify(notificationsEnabled = true, appInTopicScreen = false))
        assertFalse(factory.shouldNotify(notificationsEnabled = false, appInTopicScreen = false))
    }
}
```

- [ ] **Step 2: Register permissions and service metadata in the manifest**

```xml
<!-- app/src/main/AndroidManifest.xml additions -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<application ...>
    <service
        android:name=".service.MqttSyncService"
        android:enabled="true"
        android:exported="false"
        android:foregroundServiceType="dataSync" />
</application>
```

- [ ] **Step 3: Create the notification channels and builders**

```kotlin
// app/src/main/java/com/example/mqttmailclient/notifications/MqttNotificationChannels.kt
package com.example.mqttmailclient.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object MqttNotificationChannels {
    const val SYNC_STATUS = "sync_status"
    const val INCOMING_MESSAGES = "incoming_messages"

    fun register(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                SYNC_STATUS,
                "Sync status",
                NotificationManager.IMPORTANCE_LOW
            )
        )
        manager.createNotificationChannel(
            NotificationChannel(
                INCOMING_MESSAGES,
                "Incoming messages",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
    }
}
```

- [ ] **Step 4: Implement the foreground service skeleton**

```kotlin
// app/src/main/java/com/example/mqttmailclient/service/MqttSyncService.kt
package com.example.mqttmailclient.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.mqttmailclient.R
import com.example.mqttmailclient.notifications.MqttNotificationChannels

class MqttSyncService : Service() {
    override fun onCreate() {
        super.onCreate()
        MqttNotificationChannels.register(this)
        startForeground(
            1001,
            NotificationCompat.Builder(this, MqttNotificationChannels.SYNC_STATUS)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(getString(R.string.sync_connecting))
                .setContentText(getString(R.string.sync_connecting_description))
                .build()
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Load broker config and subscriptions, connect, and resubscribe
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
```

- [ ] **Step 5: Run the notification tests**

Run: `./gradlew testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit the service and notification layer**

```bash
git add app/src/main/AndroidManifest.xml app/src/main/java/com/example/mqttmailclient/notifications app/src/main/java/com/example/mqttmailclient/service app/src/test/java/com/example/mqttmailclient/notifications
git commit -m "feat: add foreground MQTT sync service"
```

### Task 5: Wire Incoming Messages Into Storage And Topic State

**Files:**
- Create: `app/src/main/java/com/example/mqttmailclient/domain/IncomingMessageProcessor.kt`
- Create: `app/src/main/java/com/example/mqttmailclient/domain/TopicSummary.kt`
- Create: `app/src/main/java/com/example/mqttmailclient/domain/ObserveTopicSummaries.kt`
- Modify: `app/src/main/java/com/example/mqttmailclient/service/MqttSyncService.kt`
- Create: `app/src/test/java/com/example/mqttmailclient/domain/IncomingMessageProcessorTest.kt`

- [ ] **Step 1: Write the failing message processing test**

```kotlin
// app/src/test/java/com/example/mqttmailclient/domain/IncomingMessageProcessorTest.kt
package com.example.mqttmailclient.domain

import kotlin.test.Test
import kotlin.test.assertTrue

class IncomingMessageProcessorTest {
    @Test
    fun incomingMessageIsStoredUnreadForMatchingTopic() {
        val processor = IncomingMessageProcessor(
            topicLookup = { "alerts/door" to 5L },
            saveMessage = { _, _, _, _, _ -> 10L }
        )

        val saved = processor.process(
            topic = "alerts/door",
            payload = "opened",
            qos = 1,
            retained = false
        )

        assertTrue(saved > 0)
    }
}
```

- [ ] **Step 2: Implement the message processor**

```kotlin
// app/src/main/java/com/example/mqttmailclient/domain/IncomingMessageProcessor.kt
package com.example.mqttmailclient.domain

class IncomingMessageProcessor(
    private val topicLookup: (String) -> Pair<String, Long>?,
    private val saveMessage: (Long, String, String, Int, Boolean) -> Long
) {
    fun process(
        topic: String,
        payload: String,
        qos: Int,
        retained: Boolean
    ): Long {
        val match = topicLookup(topic) ?: return -1L
        return saveMessage(match.second, topic, payload, qos, retained)
    }
}
```

- [ ] **Step 3: Create the topic summary model**

```kotlin
// app/src/main/java/com/example/mqttmailclient/domain/TopicSummary.kt
package com.example.mqttmailclient.domain

data class TopicSummary(
    val id: Long,
    val title: String,
    val latestMessagePreview: String?,
    val latestMessageAt: Long?,
    val unreadCount: Int,
    val notificationsEnabled: Boolean,
    val lastError: String?
)
```

- [ ] **Step 4: Integrate the processor into the sync service**

```kotlin
// app/src/main/java/com/example/mqttmailclient/service/MqttSyncService.kt excerpt
mqttGateway.setListener { message ->
    val storedId = incomingMessageProcessor.process(
        topic = message.topic,
        payload = message.payload,
        qos = message.qos,
        retained = message.retained
    )

    if (storedId > 0L) {
        notificationFactory.notifyIncomingMessage(message.topic, message.payload)
    }
}
```

- [ ] **Step 5: Run the domain tests**

Run: `./gradlew testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit the message routing logic**

```bash
git add app/src/main/java/com/example/mqttmailclient/domain app/src/main/java/com/example/mqttmailclient/service app/src/test/java/com/example/mqttmailclient/domain
git commit -m "feat: persist incoming MQTT messages and update topic state"
```

### Task 6: Build The Compose UI Flows

**Files:**
- Create: `app/src/main/java/com/example/mqttmailclient/ui/navigation/MqttMailNavGraph.kt`
- Create: `app/src/main/java/com/example/mqttmailclient/ui/account/AccountSettingsViewModel.kt`
- Create: `app/src/main/java/com/example/mqttmailclient/ui/account/AccountSettingsScreen.kt`
- Create: `app/src/main/java/com/example/mqttmailclient/ui/topics/TopicListViewModel.kt`
- Create: `app/src/main/java/com/example/mqttmailclient/ui/topics/TopicListScreen.kt`
- Create: `app/src/main/java/com/example/mqttmailclient/ui/topics/TopicDetailViewModel.kt`
- Create: `app/src/main/java/com/example/mqttmailclient/ui/topics/TopicDetailScreen.kt`
- Modify: `app/src/main/java/com/example/mqttmailclient/MainActivity.kt`
- Create: `app/src/androidTest/java/com/example/mqttmailclient/ui/TopicListScreenTest.kt`

- [ ] **Step 1: Write the failing UI test**

```kotlin
// app/src/androidTest/java/com/example/mqttmailclient/ui/TopicListScreenTest.kt
package com.example.mqttmailclient.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class TopicListScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun topicRowShowsUnreadCount() {
        composeRule.setContent {
            TopicListScreen(
                state = TopicListUiState(
                    topics = listOf(
                        TopicRowUiModel(
                            id = 1L,
                            title = "alerts/door",
                            latestPreview = "opened",
                            unreadCount = 2
                        )
                    )
                ),
                onTopicClick = {},
                onAddTopic = {},
                onOpenSettings = {}
            )
        }

        composeRule.onNodeWithText("2 unread").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Add navigation dependencies**

```kotlin
// app/build.gradle.kts dependency block additions
implementation("androidx.navigation:navigation-compose:2.9.0")
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
```

- [ ] **Step 3: Implement the topic list screen**

```kotlin
// app/src/main/java/com/example/mqttmailclient/ui/topics/TopicListScreen.kt
package com.example.mqttmailclient.ui.topics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TopicListScreen(
    state: TopicListUiState,
    onTopicClick: (Long) -> Unit,
    onAddTopic: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTopic) {
                Text("+")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(state.topics) { topic ->
                Column(
                    modifier = Modifier
                        .clickable { onTopicClick(topic.id) }
                        .padding(16.dp)
                ) {
                    Text(topic.title)
                    topic.latestPreview?.let { Text(it) }
                    Text("${topic.unreadCount} unread")
                }
            }
        }
    }
}
```

- [ ] **Step 4: Implement the topic detail clear-history flow**

```kotlin
// app/src/main/java/com/example/mqttmailclient/ui/topics/TopicDetailScreen.kt excerpt
Button(onClick = onClearHistory) {
    Text("Clear history")
}
```

- [ ] **Step 5: Replace the placeholder activity content with navigation**

```kotlin
// app/src/main/java/com/example/mqttmailclient/MainActivity.kt
setContent {
    MqttMailTheme {
        MqttMailNavGraph()
    }
}
```

- [ ] **Step 6: Run the instrumentation and unit tests**

Run: `./gradlew testDebugUnitTest connectedDebugAndroidTest`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit the UI**

```bash
git add app/src/main/java/com/example/mqttmailclient/ui app/src/androidTest/java/com/example/mqttmailclient/ui app/src/main/java/com/example/mqttmailclient/MainActivity.kt
git commit -m "feat: add account topic list and message history UI"
```

### Task 7: Add Startup Recovery, Deep Links, And Sync Controls

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/example/mqttmailclient/receiver/BootCompletedReceiver.kt`
- Create: `app/src/main/java/com/example/mqttmailclient/domain/SyncController.kt`
- Modify: `app/src/main/java/com/example/mqttmailclient/notifications/MqttNotificationFactory.kt`
- Create: `app/src/test/java/com/example/mqttmailclient/domain/SyncControllerTest.kt`

- [ ] **Step 1: Write the failing sync control test**

```kotlin
// app/src/test/java/com/example/mqttmailclient/domain/SyncControllerTest.kt
package com.example.mqttmailclient.domain

import kotlin.test.Test
import kotlin.test.assertTrue

class SyncControllerTest {
    @Test
    fun restartOnBootOnlyWhenSyncEnabled() {
        val controller = SyncController(
            isSyncEnabled = { true },
            startService = {}
        )

        assertTrue(controller.shouldRestartAfterBoot())
    }
}
```

- [ ] **Step 2: Add the boot receiver**

```kotlin
// app/src/main/java/com/example/mqttmailclient/receiver/BootCompletedReceiver.kt
package com.example.mqttmailclient.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.mqttmailclient.domain.SyncController

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        SyncController.from(context).restartAfterBootIfNeeded()
    }
}
```

- [ ] **Step 3: Register the boot receiver in the manifest**

```xml
<!-- app/src/main/AndroidManifest.xml additions -->
<receiver
    android:name=".receiver.BootCompletedReceiver"
    android:enabled="true"
    android:exported="false">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
    </intent-filter>
</receiver>
```

- [ ] **Step 4: Add deep links from notifications into topic detail**

```kotlin
// app/src/main/java/com/example/mqttmailclient/notifications/MqttNotificationFactory.kt excerpt
val intent = Intent(context, MainActivity::class.java)
    .putExtra("topic_id", topicId)
    .putExtra("open_topic_from_notification", true)
```

- [ ] **Step 5: Run the sync control tests**

Run: `./gradlew testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit the recovery wiring**

```bash
git add app/src/main/AndroidManifest.xml app/src/main/java/com/example/mqttmailclient/receiver app/src/main/java/com/example/mqttmailclient/domain app/src/main/java/com/example/mqttmailclient/notifications app/src/test/java/com/example/mqttmailclient/domain
git commit -m "feat: restore sync after reboot and add notification deep links"
```

### Task 8: Final Verification And Device Validation

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Create: `README.md`

- [ ] **Step 1: Add user-facing setup and battery guidance copy**

```xml
<!-- app/src/main/res/values/strings.xml additions -->
<string name="sync_connecting">Connecting to broker</string>
<string name="sync_connecting_description">MQTT Mail is starting the background connection.</string>
<string name="battery_notice">Some devices may still restrict always-on background networking.</string>
```

- [ ] **Step 2: Add the project README**

```markdown
# MQTT Mail Client

## Run

1. Open the project in Android Studio.
2. Sync Gradle.
3. Run the `app` configuration on a device or emulator with Google APIs.

## Validate

- Configure one broker account.
- Add two topic subscriptions.
- Publish a message to one topic and verify:
  - the message appears in the topic history
  - the topic list updates preview and unread count
  - a notification opens the correct topic
- Lock the device and publish another message.
- Reboot the device and verify sync resumes if it was enabled.
```

- [ ] **Step 3: Run the full verification suite**

Run: `./gradlew lint testDebugUnitTest connectedDebugAndroidTest`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Perform manual validation**

Run:

```bash
adb shell am start -n com.example.mqttmailclient/.MainActivity
```

Expected:

```text
Status: ok
```

Then verify these behaviors manually on device:

- App can save broker settings
- App can add multiple topics
- App receives and stores messages
- App can clear history for one topic only
- App keeps showing foreground sync notification while active

- [ ] **Step 5: Commit the final polish**

```bash
git add README.md app/src/main/res/values/strings.xml
git commit -m "docs: add setup and validation guidance"
```
