package me.tb

import fr.acinq.bitcoin.PrivateKey
import fr.acinq.bitcoin.PublicKey
import kotlin.test.Test
import kotlin.test.assertTrue

class KeysetTest {
    @Test
    fun `Build keyset from json object`() {
        val jsonString = """{"1":"03ba786a2c0745f8c30e490288acd7a72dd53d65afd292ddefa326a4a3fa14c566","2":"03361cd8bd1329fea797a6add1cf1990ffcf2270ceb9fc81eeee0e8e9c1bd0cdf5","4":"036e378bcf78738ddf68859293c69778035740e41138ab183c94f8fee7572214c7"}"""
        val keyset = Keyset.fromJson(jsonString)
        println(keyset.deriveKeysetId().value)
    }

    @Test
    fun `Other test`() {
        val publicKey = "02C591A8FF19AC9C4E4E5793673B83123437E975285E7B442F4EE2654DFFCA5E2D".lowercase()
        assertTrue(PublicKey.fromHex(publicKey).isValid())
    }

    @Test
    fun `Other test private key`() {
        val privateKey = PrivateKey.fromHex("BCF69F7AFF3273B864F9DD76896FACE8E3D3CF69A133585C8177816F14FC9B55")
        val publicKey = privateKey.publicKey()
        assertTrue(publicKey.isValid())
    }
}
