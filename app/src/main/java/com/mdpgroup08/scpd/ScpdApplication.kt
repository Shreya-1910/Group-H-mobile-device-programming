package com.mdpgroup08.scpd

import android.app.Application

/**
 * Created once by Android when the app starts.
 * Holds the AppContainer so every ViewModel shares the same repositories.
 */

class ScpdApplication: Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer()
    }
}
