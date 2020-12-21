package ru.semper_viventem.trackrobot_android

import android.app.Application
import timber.log.Timber

class TheApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}