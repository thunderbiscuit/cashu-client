/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE file.
 */
 
package me.tb

import java.security.SecureRandom

// TODO: Should we check for the characters in the secret string? Not sure if there are corner cases here.
//       Maybe only accepting base64 encoded strings would be better?
//       It also feels like we should require secrets to be a certain size, otherwise they'd be easy to brute force?
//       The issue is that test vectors in other libraries use secrets that are quite short

/**
 * Secret used to generate a token. This is the x in NUT-00, the bytes we'll use in the [hashToCurve] function
 * to generate key Y.
 *
 * @param secret The secret to use. If null or blank, a random secret will be generated.
 */
public class Secret(secret: String?) {
    public val value: ByteArray = generateSecret(secret)

    // TODO: Should maybe look at building a second constructor that takes the secret
    //       as a String and keep the primary constructor building using randomly generated secrets.

    // init {
    //     require(secret.isNullOrBlank() || secret.length >= 32) { "Secret must be at least 32 bytes long" }
    // }

    private fun generateSecret(secret: String?): ByteArray {
        return if (secret.isNullOrBlank()) {
            val random = SecureRandom()
            val secretBytes = ByteArray(32)
            random.nextBytes(secretBytes)
            secretBytes
        } else {
            secret.toByteArray(Charsets.UTF_8)
        }
    }
}
