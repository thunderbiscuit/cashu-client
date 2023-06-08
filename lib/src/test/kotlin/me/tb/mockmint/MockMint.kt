package me.tb.mockmint

import fr.acinq.bitcoin.PrivateKey
import fr.acinq.bitcoin.PublicKey

public class MockMint(private val privateKey: PrivateKey) {
    public fun createBlindSignature(B_: PublicKey): PublicKey {
        return B_.times(privateKey)
    }
}
