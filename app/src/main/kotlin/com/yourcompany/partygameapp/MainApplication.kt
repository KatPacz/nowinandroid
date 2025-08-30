// FILE: MainApplication.kt

package com.yourcompany.partygameapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MainApplication : Application() {
    // This class is required for Hilt to work.
    // No code is needed inside the class body.
}