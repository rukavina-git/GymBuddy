package com.rukavina.gymbuddy.ui.profile.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.rukavina.gymbuddy.Constants
import com.rukavina.gymbuddy.ui.components.ValidatedTextField
import com.rukavina.gymbuddy.utils.validation.ValidatedFieldState
import com.rukavina.gymbuddy.utils.validation.Validators

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditNameScreen(
    navController: NavHostController,
    currentName: String,
    onSave: (String) -> Unit
) {
    // Treat "User" (the default) as empty for first-time users
    val isDefaultName = currentName.isBlank() || currentName == "User"
    val initialName = if (isDefaultName) "" else currentName

    val nameState = remember {
        ValidatedFieldState(
            initialValue = initialName,
            validators = listOf(
                Validators.required("Name"),
                Validators.nameCharsOnly()
            ),
            maxLength = Constants.Profile.MAX_NAME_LENGTH
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Name") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ValidatedTextField(
                state = nameState,
                label = "Name",
                placeholder = "Enter your name",
                capitalization = KeyboardCapitalization.Words
            )

            Button(
                onClick = {
                    nameState.touch()
                    if (nameState.isValid) {
                        onSave(nameState.value)
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = nameState.isValid
            ) {
                Text("Save")
            }
        }
    }
}
