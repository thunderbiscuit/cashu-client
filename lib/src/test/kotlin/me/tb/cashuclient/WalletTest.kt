package me.tb.cashuclient

import fr.acinq.bitcoin.PrivateKey
import fr.acinq.bitcoin.PublicKey
import fr.acinq.secp256k1.Hex
import me.tb.cashuclient.mockmint.MockMint
import kotlin.test.Test
import kotlin.test.assertEquals

class WalletTest {
    @Test
    fun `Wallet correctly processes mint response`() {
        // Assume the mint has the following keys:
        // Amount 1 private key:   7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f
        // Amount 1 public key:  03142715675faf8da1ecc4d51e0b9e539fa0d52fdd96ed60dbe99adb15d6b05ad9

        // Step 1: set up pre-mint bundle
        val preMintItem = PreMintItem.create(
            amount = 1uL,
            secret = Secret("test_message"),
            blindingFactorBytes = Hex.decode("0000000000000000000000000000000000000000000000000000000000000001")
        )
        val preMintBundle = PreMintBundle(listOf(preMintItem))
        println("Blinded secret is ${preMintBundle.preMintItems.first().blindedSecret}")
        val jsonString = """{"1":"03142715675faf8da1ecc4d51e0b9e539fa0d52fdd96ed60dbe99adb15d6b05ad9"}"""
        val keyset = Keyset.fromJson(jsonString)

        // Step 2: create mint response
        val mockMint = MockMint(
            PrivateKey.fromHex("7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f")
        )
        val blindedSignature = mockMint.createBlindSignature(PublicKey.fromHex("02a9acc1e48c25eeeb9289b5031cc57da9fe72f3fe2861d264bdc074209b107ba2"))
        println("Blinded signature is $blindedSignature")
        val blindedSignatureObject: BlindedSignature = BlindedSignature(
            amount = 1uL,
            blindedKey = blindedSignature.toHex(),
            id = 1
        )
        val mintResponse: MintResponse = MintResponse(listOf(blindedSignatureObject))

        // Step 3: create wallet and process mint response
        val wallet = Wallet(
            activeKeyset = keyset,
            mintUrl = "mockmint"
        )
        wallet.processMintResponse(preMintBundle, mintResponse)
    }

    // Taken from https://github.com/cashubtc/cashu/blob/5c820f9469272b645e4014752270ca6926a6dfcb/tests/test_crypto.py#L80-L106
    @Test
    fun `Wallet correctly processes mint response 2`() {
        // Assume the mint has the following keys:
        // Amount 1 public key:  020000000000000000000000000000000000000000000000000000000000000001

        // Step 1: set up pre-mint bundle
        val preMintItem = PreMintItem.create(
            amount = 1uL,
            secret = Secret("test_message"),
            blindingFactorBytes = Hex.decode("0000000000000000000000000000000000000000000000000000000000000001")
        )
        val preMintBundle = PreMintBundle(listOf(preMintItem))
        println("Blinded secret is ${preMintBundle.preMintItems.first().blindedSecret}")
        val jsonString = """{"1":"020000000000000000000000000000000000000000000000000000000000000001"}"""
        val keyset = Keyset.fromJson(jsonString)

        // Step 2: create mint response
        val blindedSignature = "02a9acc1e48c25eeeb9289b5031cc57da9fe72f3fe2861d264bdc074209b107ba2"
        val blindedSignatureObject: BlindedSignature = BlindedSignature(
            amount = 1uL,
            blindedKey = blindedSignature,
            id = 1
        )
        val mintResponse: MintResponse = MintResponse(listOf(blindedSignatureObject))

        // Step 3: create wallet and process mint response
        val wallet = Wallet(
            activeKeyset = keyset,
            mintUrl = "mockmint"
        )
        wallet.processMintResponse(preMintBundle, mintResponse)
    }

    @Test
    fun `Wallet successfully updates active keyset from mint after requesting it`() {
        val jsonString = """{"1":"03142715675faf8da1ecc4d51e0b9e539fa0d52fdd96ed60dbe99adb15d6b05ad9"}"""
        val smallKeyset = Keyset.fromJson(jsonString)
        val wallet = Wallet(activeKeyset = smallKeyset, mintUrl = "https://8333.space:3338")

        // At this point the wallet keyset should not be the same as the TEST_KEYSET
        assertEquals<Boolean>(
            expected = false,
            actual = Keyset.fromJson(TEST_KEYSET) == wallet.activeKeyset
        )

        // Now we request the active keyset from the mint
        wallet.getActiveKeyset()

        assertEquals<Keyset>(
            expected = Keyset.fromJson(TEST_KEYSET),
            actual = wallet.activeKeyset!!
        )
    }

    @Test
    fun `Wallet adds old keyset to list of inactive keysets`() {
        val jsonString = """{"1":"03142715675faf8da1ecc4d51e0b9e539fa0d52fdd96ed60dbe99adb15d6b05ad9"}"""
        val smallKeyset = Keyset.fromJson(jsonString)
        val wallet = Wallet(activeKeyset = smallKeyset, mintUrl = "https://8333.space:3338")

        // Now we request the active keyset from the mint
        wallet.getActiveKeyset()

        assertEquals<Keyset>(
            expected = smallKeyset,
            actual = wallet.inactiveKeysets.first()
        )
    }

    @Test
    fun `Wallet can request specific keyset from mint`() {
        val jsonString = """{"1":"03142715675faf8da1ecc4d51e0b9e539fa0d52fdd96ed60dbe99adb15d6b05ad9"}"""
        val smallKeyset = Keyset.fromJson(jsonString)
        val wallet = Wallet(activeKeyset = smallKeyset, mintUrl = "https://8333.space:3338")

        val specificKeyset = wallet.getSpecificKeyset(KeysetId("I2yN+iRYfkzT"))
        println(specificKeyset.sortedKeyset)
        assertEquals(
            expected = Keyset.fromJson(TEST_KEYSET),
            actual = specificKeyset
        )
    }
}
