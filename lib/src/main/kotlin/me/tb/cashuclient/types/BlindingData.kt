/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE.txt file.
 */
 
 package me.tb.cashuclient.types

import fr.acinq.bitcoin.PrivateKey
import fr.acinq.bitcoin.PublicKey
import me.tb.cashuclient.hashToCurve
import me.tb.cashuclient.randomBytes

public interface BlindingData {
    public val amount: ULong
    public val secret: Secret
    public val blindedSecret: PublicKey
    public val blindingFactor: PrivateKey
}

internal fun createBlindingData(): Triple<Secret, PublicKey, PrivateKey> {
    val secret = Secret()
    val blindingFactorBytes: ByteArray = randomBytes(32)
    val blindingFactor: PrivateKey = PrivateKey(blindingFactorBytes)
    val blindedSecret: PublicKey = hashToCurve(secret.value) + blindingFactor.publicKey()
    return Triple(secret, blindedSecret, blindingFactor)
}
