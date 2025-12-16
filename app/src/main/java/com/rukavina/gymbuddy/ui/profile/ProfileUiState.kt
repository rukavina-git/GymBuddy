package com.rukavina.gymbuddy.ui.profile

import android.net.Uri
import com.rukavina.gymbuddy.data.model.ActivityLevel
import com.rukavina.gymbuddy.data.model.FitnessGoal
import com.rukavina.gymbuddy.data.model.Gender
import com.rukavina.gymbuddy.data.model.PreferredUnits

data class ProfileUiState(
    val name: String = "",
    val email: String = "",
    val age: String = "",
    val weight: String = "",
    val height: String = "",
    val targetWeight: String = "",
    val gender: Gender? = null,
    val fitnessGoal: FitnessGoal? = null,
    val activityLevel: ActivityLevel? = null,
    val preferredUnits: PreferredUnits = PreferredUnits.METRIC,
    val bio: String = "",
    val profileImageUri: Uri? = null,
    val isLoading: Boolean = false,
    val message: String? = null
)