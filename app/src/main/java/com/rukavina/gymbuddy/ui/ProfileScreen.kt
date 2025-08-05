package com.rukavina.gymbuddy.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.rukavina.gymbuddy.navigation.NavRoutes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(rootNavController: NavHostController) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Profile") }) }
    ) { paddingValues ->
        Column(modifier = Modifier
            .padding(paddingValues)
            .padding(16.dp)) {
            Text("Profile Screen")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                FirebaseAuth.getInstance().signOut()
                rootNavController.navigate(NavRoutes.Login) {
                    popUpTo(NavRoutes.Main) { inclusive = true }
                }
            }) {
                Text("Log Out")
            }
        }
    }
}