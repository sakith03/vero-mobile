package com.example.veromobile.di

import com.example.veromobile.core.AppTime
import com.example.veromobile.data.local.AppDatabase
import com.example.veromobile.data.local.InvoiceDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class InvoiceClock

@Module
@InstallIn(SingletonComponent::class)
object InvoiceModule {
    @Provides fun dao(db: AppDatabase): InvoiceDao = db.invoiceDao()
    @Provides @InvoiceClock fun clock(): Clock = Clock.system(AppTime.zone)
}
