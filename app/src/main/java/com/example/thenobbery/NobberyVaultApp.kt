package com.example.thenobbery

import android.app.Application
import timber.log.Timber

class NobberyVaultApp : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
