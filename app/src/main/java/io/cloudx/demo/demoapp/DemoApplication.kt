package io.cloudx.demo.demoapp

import android.app.Application
import io.cloudx.demo.demoapp.ads.CloudXStartup

class DemoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        CloudXStartup.initialize(this)
    }
}
