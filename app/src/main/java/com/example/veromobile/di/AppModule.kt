package com.example.veromobile.di

import android.content.Context
import androidx.room.Room
import com.example.veromobile.data.local.AppDatabase
import com.example.veromobile.data.local.ExpenseDao
import com.example.veromobile.data.local.GoalMigrations
import com.example.veromobile.data.local.IncomeDao
import com.example.veromobile.data.repository.ExpenseRepositoryImpl
import com.example.veromobile.data.repository.IncomeRepositoryImpl
import com.example.veromobile.domain.repository.ExpenseRepository
import com.example.veromobile.domain.repository.IncomeRepository
import com.example.veromobile.domain.session.DevUserSession
import com.example.veromobile.domain.session.UserSession
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDb(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "finance.db")
            .addMigrations(GoalMigrations.MIGRATION_1_2, GoalMigrations.MIGRATION_2_3)
            .build()

    @Provides
    fun provideExpenseDao(db: AppDatabase): ExpenseDao = db.expenseDao()

    @Provides
    fun provideIncomeDao(db: AppDatabase): IncomeDao = db.incomeDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingModule {
    @Binds abstract fun bindIncomeRepo(impl: IncomeRepositoryImpl): IncomeRepository
    @Binds abstract fun bindExpenseRepo(impl: ExpenseRepositoryImpl): ExpenseRepository
    @Binds abstract fun bindSession(impl: DevUserSession): UserSession
}
