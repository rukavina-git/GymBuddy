package com.rukavina.gymbuddy.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rukavina.gymbuddy.data.repository.AppPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppPreferencesViewModel @Inject constructor(
    private val repository: AppPreferencesRepository
) : ViewModel() {

    val showQuoteOfTheDay: StateFlow<Boolean> = repository.showQuoteOfTheDay
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    fun setShowQuoteOfTheDay(enabled: Boolean) {
        viewModelScope.launch {
            repository.setShowQuoteOfTheDay(enabled)
        }
    }
}
