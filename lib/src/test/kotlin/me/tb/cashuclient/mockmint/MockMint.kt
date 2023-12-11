/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE file.
 */

package me.tb.cashuclient.mockmint

import fr.acinq.bitcoin.PrivateKey
import fr.acinq.bitcoin.PublicKey

public class MockMint(private val privateKey: PrivateKey) {
    public fun createBlindSignature(B_: PublicKey): PublicKey {
        return B_.times(privateKey)
    }
}
