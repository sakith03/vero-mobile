package com.example.veromobile

import android.app.Application
import com.example.veromobile.data.repository.FinancialSyncCoordinator
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class VeroMobile : Application() {
    @Inject lateinit var financialSync: FinancialSyncCoordinator

    override fun onCreate() {
        super.onCreate()
        financialSync.start()
    }
}
