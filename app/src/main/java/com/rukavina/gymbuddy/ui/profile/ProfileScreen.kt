package com.rukavina.gymbuddy.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.rukavina.gymbuddy.navigation.NavRoutes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    rootNavController: NavHostController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show message in snackbar
    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    // Observe logout event and navigate
    LaunchedEffect(Unit) {
        viewModel.logoutEvent.collect {
            rootNavController.navigate(NavRoutes.Login) {
                popUpTo(NavRoutes.Main) { inclusive = true }
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Your Profile") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),

                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Image
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        .clickable { viewModel.onImageClick() },
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.profileImageUri != null) {
                        AsyncImage(
                            model = uiState.profileImageUri,
                            contentDescription = "Profile image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Default icon",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Text fields
                ProfileTextField("First Name", uiState.firstName) {
                    viewModel.onFieldChanged(ProfileField.FirstName, it)
                }
                ProfileTextField("Last Name", uiState.lastName) {
                    viewModel.onFieldChanged(ProfileField.LastName, it)
                }
                ProfileTextField("Email", uiState.email) {
                    viewModel.onFieldChanged(ProfileField.Email, it)
                }
                ProfileTextField("Age", uiState.age, KeyboardType.Number) {
                    viewModel.onFieldChanged(ProfileField.Age, it)
                }
                ProfileTextField("Weight (kg)", uiState.weight, KeyboardType.Number) {
                    viewModel.onFieldChanged(ProfileField.Weight, it)
                }
                ProfileTextField("Height (cm)", uiState.height, KeyboardType.Number) {
                    viewModel.onFieldChanged(ProfileField.Height, it)
                }
                ProfileTextField("Gender", uiState.gender) {
                    viewModel.onFieldChanged(ProfileField.Gender, it)
                }
                ProfileTextField("Goal", uiState.goal) {
                    viewModel.onFieldChanged(ProfileField.Goal, it)
                }

                Button(
                    onClick = { viewModel.onSaveClick() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save")
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { viewModel.logout() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Log Out", color = Color.White)
                }
            }
        }
    }
}