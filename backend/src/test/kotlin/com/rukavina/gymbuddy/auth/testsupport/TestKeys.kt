package com.rukavina.gymbuddy.auth.testsupport

import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

/**
 * One RSA key pair, generated once per test JVM, shared by
 * TestJwtBuilder (signs) and LocalJwtTokenVerifier (verifies) so tests
 * can hand-craft JWTs without ever calling Firebase.
 */
object TestKeys {
    private val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()

    val publicKey: RSAPublicKey = keyPair.public as RSAPublicKey
    val privateKey: RSAPrivateKey = keyPair.private as RSAPrivateKey
}
