package com.rukavina.gymbuddy.ui

import androidx.lifecycle.ViewModel
import com.rukavina.gymbuddy.data.repository.AppPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appPreferencesRepository: AppPreferencesRepository
) : ViewModel() {
    val showQuoteOfTheDay: Flow<Boolean> = appPreferencesRepository.showQuoteOfTheDay
}
