package com.rukavina.gymbuddy.di

import com.rukavina.gymbuddy.data.auth.AuthClient
import com.rukavina.gymbuddy.data.auth.DefaultSignInSupport
import com.rukavina.gymbuddy.data.auth.FirebaseAuthClient
import com.rukavina.gymbuddy.data.auth.SignInSupport
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Sign-in screens' Firebase and on-phone data access (Plan 7 step 04). */
@Module
@InstallIn(SingletonComponent::class)
interface AuthModule {
    @Binds
    fun bindAuthClient(impl: FirebaseAuthClient): AuthClient

    @Binds
    fun bindSignInSupport(impl: DefaultSignInSupport): SignInSupport
}
