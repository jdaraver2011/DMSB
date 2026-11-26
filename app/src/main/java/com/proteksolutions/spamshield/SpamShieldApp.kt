package com.proteksolutions.spamshield

import android.app.Application

class SpamShieldApp : Application() {
    override fun onCreate() {
        super.onCreate()
        BlockNotifier.createChannel(this)
    }
}
