package com.rukavina.gymbuddy.ui.auth

import com.rukavina.gymbuddy.data.auth.AuthClient
import com.rukavina.gymbuddy.data.auth.AuthError
import com.rukavina.gymbuddy.data.auth.AuthException
import com.rukavina.gymbuddy.data.auth.GoogleProbe

/** Records calls in order; [failWith] makes every Firebase call fail. */
internal class FakeAuthClient : AuthClient {
    val calls = mutableListOf<String>()
    var failWith: AuthError? = null
    var probeIsNew = false
    var googleIsNew = false
    var onCall: suspend () -> Unit = {}

    private suspend fun call(name: String) {
        calls += name
        onCall()
        failWith?.let { throw AuthException(it) }
    }

    override suspend fun probeGoogle(idToken: String): GoogleProbe {
        call("probe $idToken")
        return GoogleProbe("a@gmail.com", probeIsNew)
    }

    override suspend fun endProbe(delete: Boolean) {
        calls += "endProbe($delete)"
    }

    override suspend fun signInWithGoogle(idToken: String): Boolean {
        call("google $idToken")
        return googleIsNew
    }

    override suspend fun signInWithEmail(email: String, password: String) = call("email $email")
    override suspend fun createWithEmail(email: String, password: String) = call("create $email")
    override suspend fun sendPasswordReset(email: String) = call("reset $email")
}
