package com.example.mqttandroid

import android.app.Application
import com.example.mqttandroid.data.AppContainer

class MqttAndroidApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}
