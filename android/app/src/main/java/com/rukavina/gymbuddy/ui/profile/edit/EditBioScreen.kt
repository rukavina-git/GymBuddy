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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.rukavina.gymbuddy.Constants
import com.rukavina.gymbuddy.ui.components.ValidatedTextField
import com.rukavina.gymbuddy.utils.validation.ValidatedFieldState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBioScreen(
    navController: NavHostController,
    currentBio: String,
    onSave: (String) -> Unit
) {
    val bioState = remember {
        ValidatedFieldState(
            initialValue = currentBio,
            validators = emptyList(), // Bio is optional
            maxLength = Constants.Profile.MAX_BIO_LENGTH
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bio") },
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
                state = bioState,
                label = "Bio",
                placeholder = "Tell us about yourself",
                singleLine = false,
                minLines = 4,
                maxLines = 8
            )

            Button(
                onClick = {
                    onSave(bioState.value)
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}
