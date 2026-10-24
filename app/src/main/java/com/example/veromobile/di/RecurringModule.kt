package com.example.veromobile.di

import com.example.veromobile.core.AppTime
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RecurringClock

@Module
@InstallIn(SingletonComponent::class)
object RecurringModule {
    @Provides @RecurringClock fun clock(): Clock = Clock.system(AppTime.zone)
}
