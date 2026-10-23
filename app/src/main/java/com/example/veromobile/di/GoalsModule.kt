package com.example.veromobile.di

import com.example.veromobile.data.local.AppDatabase
import com.example.veromobile.data.local.GoalDao
import com.example.veromobile.data.repository.SavingsGoalRepositoryImpl
import com.example.veromobile.domain.repository.SavingsGoalRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.ZoneId
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GoalsClock

@Module
@InstallIn(SingletonComponent::class)
object GoalsDatabaseModule {
    @Provides fun provideGoalDao(db: AppDatabase): GoalDao = db.goalDao()

    @Provides @Singleton @GoalsClock
    fun provideClock(): Clock = Clock.system(ZoneId.of("Asia/Colombo"))
}

@Module
@InstallIn(SingletonComponent::class)
abstract class GoalsBindingModule {
    @Binds abstract fun bindSavingsGoalRepository(impl: SavingsGoalRepositoryImpl): SavingsGoalRepository
}
