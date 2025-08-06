package com.rukavina.gymbuddy.ui.profile

import android.net.Uri

data class ProfileUiState(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val age: String = "",
    val weight: String = "",
    val height: String = "",
    val gender: String = "",
    val goal: String = "",
    val profileImageUri: Uri? = null,
    val isLoading: Boolean = false,
    val message: String? = null
)