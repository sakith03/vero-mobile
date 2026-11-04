package com.example.veromobile.ui.offline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.veromobile.data.connectivity.ConnectivityObserver
import com.example.veromobile.data.repository.CachedFinancialOverview
import com.example.veromobile.data.repository.FinancialCacheRepository
import com.example.veromobile.data.repository.FinancialSyncCoordinator
import com.example.veromobile.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OfflineViewModel @Inject constructor(
    auth: AuthRepository,
    private val cache: FinancialCacheRepository,
    private val sync: FinancialSyncCoordinator,
    val connectivity: ConnectivityObserver
) : ViewModel() {
    private val _overview = MutableStateFlow(CachedFinancialOverview())
    val overview: StateFlow<CachedFinancialOverview> = _overview.asStateFlow()

    init {
        viewModelScope.launch {
            auth.authState.flatMapLatest { user ->
                user?.uid?.let(cache::observe) ?: flowOf(CachedFinancialOverview())
            }.collect { _overview.value = it }
        }
    }

    fun retry() = sync.retry()
}
