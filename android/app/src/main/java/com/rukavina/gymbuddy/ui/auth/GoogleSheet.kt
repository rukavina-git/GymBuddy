package com.rukavina.gymbuddy.ui.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CancellationException

/** Google's account sheet: a token and the chosen account's email. */
data class GoogleToken(val idToken: String, val email: String?)

sealed interface GoogleSheetResult {
    data class Token(val token: GoogleToken) : GoogleSheetResult
    data object Cancelled : GoogleSheetResult
    data class Failed(val offline: Boolean) : GoogleSheetResult
}

private const val TAG = "GoogleSheet"

/**
 * Android's account sheet (Figma "Android's account sheet"). [authorizedFirst]:
 * first only accounts that used GymBuddy before; with none, all accounts.
 * Needs an Activity context.
 */
suspend fun openGoogleSheet(context: Context, webClientId: String, authorizedFirst: Boolean): GoogleSheetResult {
    val manager = CredentialManager.create(context)
    suspend fun request(authorizedOnly: Boolean): GoogleToken {
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(authorizedOnly)
            .setAutoSelectEnabled(false)
            .setServerClientId(webClientId)
            .build()
        val result = manager.getCredential(context, GetCredentialRequest.Builder().addCredentialOption(option).build())
        val credential = GoogleIdTokenCredential.createFrom(result.credential.data)
        return GoogleToken(credential.idToken, credential.id)
    }
    return try {
        val token = if (authorizedFirst) {
            try {
                request(authorizedOnly = true)
            } catch (e: NoCredentialException) {
                request(authorizedOnly = false)
            }
        } else {
            request(authorizedOnly = false)
        }
        GoogleSheetResult.Token(token)
    } catch (e: CancellationException) {
        throw e
    } catch (e: GetCredentialCancellationException) {
        GoogleSheetResult.Cancelled
    } catch (e: NoCredentialException) {
        // No Google account on the phone, or Play services can't reach Google.
        Log.w(TAG, "no credential", e)
        GoogleSheetResult.Failed(offline = true)
    } catch (e: GetCredentialException) {
        Log.w(TAG, "sheet failed", e)
        GoogleSheetResult.Failed(offline = false)
    } catch (e: Exception) {
        Log.w(TAG, "token failed", e)
        GoogleSheetResult.Failed(offline = false)
    }
}
