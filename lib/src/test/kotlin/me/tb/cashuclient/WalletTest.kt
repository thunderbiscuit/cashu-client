/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE file.
 */

package me.tb.cashuclient

import fr.acinq.bitcoin.Satoshi
import fr.acinq.lightning.payment.PaymentRequest
import kotlinx.coroutines.test.runTest
import me.tb.cashuclient.mint.MintQuoteData
import me.tb.cashuclient.types.EcashUnit
import me.tb.cashuclient.types.Keyset
import me.tb.cashuclient.types.KeysetId
import me.tb.cashuclient.types.PaymentMethod
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class WalletTest {
    // @Test
    // fun `Wallet correctly processes mint response`() {
    //     // Assume the mint has the following keys:
    //     // Amount 1 private key:   7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f
    //     // Amount 1 public key:  03142715675faf8da1ecc4d51e0b9e539fa0d52fdd96ed60dbe99adb15d6b05ad9
    //
    //     // Step 1: set up pre-mint bundle
    //     val preMintItem = PreMintItem.create(
    //         amount = 1uL,
    //         secret = Secret("test_message"),
    //         blindingFactorBytes = Hex.decode("0000000000000000000000000000000000000000000000000000000000000001")
    //     )
    //     val preMintBundle = PreMintBundle.create(16uL)
    //     println("Blinded secret is ${preMintBundle.preMintItems.first().blindedSecret}")
    //     val jsonString = """{"1":"03142715675faf8da1ecc4d51e0b9e539fa0d52fdd96ed60dbe99adb15d6b05ad9"}"""
    //     val keyset = Keyset.fromJson(jsonString)
    //
    //     // Step 2: create mint response
    //     val mockMint = MockMint(
    //         PrivateKey.fromHex("7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f")
    //     )
    //     val blindedSignature = mockMint.createBlindSignature(PublicKey.fromHex("02a9acc1e48c25eeeb9289b5031cc57da9fe72f3fe2861d264bdc074209b107ba2"))
    //     println("Blinded signature is $blindedSignature")
    //     val blindedSignatureObject: BlindedSignature = BlindedSignature(
    //         amount = 1uL,
    //         blindedKey = blindedSignature.toHex(),
    //         id = 1
    //     )
    //     val mintResponse: MintResponse = MintResponse(listOf(blindedSignatureObject))
    //
    //     // Step 3: create wallet and process mint response
    //     val wallet = Wallet(
    //         activeKeyset = keyset,
    //         mintUrl = "mockmint"
    //     )
    //     wallet.processMintResponse(preMintBundle, mintResponse)
    // }

    // Taken from https://github.com/cashubtc/cashu/blob/5c820f9469272b645e4014752270ca6926a6dfcb/tests/test_crypto.py#L80-L106
    // @Test
    // fun `Wallet correctly processes mint response 2`() {
    //     // Assume the mint has the following keys:
    //     // Amount 1 public key:  020000000000000000000000000000000000000000000000000000000000000001
    //
    //     // Step 1: set up pre-mint bundle
    //     val preMintItem = PreMintItem.create(
    //         amount = 1uL,
    //         secret = Secret("test_message"),
    //         blindingFactorBytes = Hex.decode("0000000000000000000000000000000000000000000000000000000000000001")
    //     )
    //     val preMintBundle = PreMintBundle(listOf(preMintItem))
    //     println("Blinded secret is ${preMintBundle.preMintItems.first().blindedSecret}")
    //     val jsonString = """{"1":"020000000000000000000000000000000000000000000000000000000000000001"}"""
    //     val keyset = Keyset.fromJson(jsonString)
    //
    //     // Step 2: create mint response
    //     val blindedSignature = "02a9acc1e48c25eeeb9289b5031cc57da9fe72f3fe2861d264bdc074209b107ba2"
    //     val blindedSignatureObject: BlindedSignature = BlindedSignature(
    //         amount = 1uL,
    //         blindedKey = blindedSignature,
    //         id = 1
    //     )
    //     val mintResponse: MintResponse = MintResponse(listOf(blindedSignatureObject))
    //
    //     // Step 3: create wallet and process mint response
    //     val wallet = Wallet(
    //         activeKeyset = keyset,
    //         mintUrl = "mockmint"
    //     )
    //     wallet.processMintResponse(preMintBundle, mintResponse)
    // }

    // @Test
    // fun `Wallet can request new tokens`() {
    //     // val wallet = Wallet(mintUrl = "https://mutinynet-cashu.thesimplekid.space")
    //     val wallet = Wallet(mintUrl = "https://testnut.cashu.space")
    //     wallet.getActiveKeyset()
    //
    //     // Note that this endpoint does not require actual payment of the lightning invoice.
    //     wallet.mint(Satoshi(10000))
    // }

    @Test
    fun `Wallet successfully updates active keyset from mint after requesting it`() = runTest {
        val jsonString = """{"1":"03142715675faf8da1ecc4d51e0b9e539fa0d52fdd96ed60dbe99adb15d6b05ad9"}"""
        val smallKeyset = Keyset.fromJson(jsonString)
        val wallet = Wallet(activeKeyset = smallKeyset, mintUrl = "https://testnut.cashu.space", unit = EcashUnit.SAT)

        // At this point the wallet keyset should not be the same as the smallKeyset
        assertEquals<Keyset>(
            expected = Keyset.fromJson(jsonString),
            actual = wallet.activeKeyset!!
        )

        // Now we request the active keyset from the mint
        wallet.getActiveKeyset()

        // At this point the wallet keyset should not be the same anymore
        assertEquals<Boolean>(
            expected = false,
            actual = smallKeyset == wallet.activeKeyset
        )
    }

    @Test
    fun `Wallet adds old keyset to list of inactive keysets`() = runTest {
        val jsonString = """{"1":"03142715675faf8da1ecc4d51e0b9e539fa0d52fdd96ed60dbe99adb15d6b05ad9"}"""
        val smallKeyset = Keyset.fromJson(jsonString)
        val wallet = Wallet(activeKeyset = smallKeyset, mintUrl = "https://testnut.cashu.space", unit = EcashUnit.SAT)

        // Now we request the active keyset from the mint
        wallet.getActiveKeyset()

        assertEquals<Keyset>(
            expected = smallKeyset,
            actual = wallet.inactiveKeysets.first()
        )
    }

    @Test
    fun `Wallet can request specific keyset from mint`() = runTest {
        val jsonString = """{"1":"03142715675faf8da1ecc4d51e0b9e539fa0d52fdd96ed60dbe99adb15d6b05ad9"}"""
        val smallKeyset = Keyset.fromJson(jsonString)
        val wallet = Wallet(
            activeKeyset = smallKeyset,
            mintUrl = "https://testnut.cashu.space",
            unit = EcashUnit.SAT
        )

        val specificKeyset = wallet.getSpecificKeyset(KeysetId("009a1f293253e41e"))

        assertEquals<String>(
            expected = "009a1f293253e41e",
            actual = specificKeyset.keysetId.value
        )
    }

    @Ignore("There are no mainnet or testnet mints that implement the v1 endpoints yet")
    @Test
    fun `Wallet can request a mint quote`() = runTest {
        // val wallet: Wallet = Wallet(mintUrl = "https://8333.space:3338", unit = EcashUnit.SAT)
        // val wallet: Wallet = Wallet(mintUrl = "https://legend.lnbits.com/cashu/api", unit = EcashUnit.SAT)
        val wallet = Wallet(mintUrl = "https://testnut.cashu.space", unit = EcashUnit.SAT)
        // val wallet: Wallet = Wallet(mintUrl = "https://mutinynet.moksha.cash:3338", unit = EcashUnit.SAT)
        val quote = wallet.requestMintQuote(Satoshi(5000), paymentMethod = PaymentMethod.BOLT11)


        println(quote)
    }

    @Ignore("There are no mainnet or testnet mints that implement the v1 endpoints yet")
    @Test
    fun `Wallet can check the status quote for mint`() = runTest {
        // mutinynet.mocksha.cash uses signet, which the ACINQ library doesn't support yet
        // val wallet: Wallet = Wallet(mintUrl = "https://mutinynet.moksha.cash:3338", unit = EcashUnit.SAT)

        // testnut.cashu.space returns invalid payment requests, which we don't handle yet
        // val wallet: Wallet = Wallet(mintUrl = "https://testnut.cashu.space", unit = EcashUnit.SAT)

        // 8333.space:3338 doesn't implement the v1 endpoints yet
        val wallet: Wallet = Wallet(mintUrl = "https://8333.space:3338", unit = EcashUnit.SAT)

        val quoteData: MintQuoteData = wallet.requestMintQuote(
            amount = Satoshi(100),
            paymentMethod = PaymentMethod.BOLT11
        )
        println(quoteData)
        val quoteId = quoteData.quote.quoteId

        val status = wallet.checkMintQuoteStatus(quoteId)
        println(status)
    }

    @Ignore("Find a way to produce a valid testnet payment request for unit tests")
    @Test
    fun `Wallet can request a melt quote`() = runTest {
        val wallet: Wallet = Wallet(mintUrl = "https://testnut.cashu.space", unit = EcashUnit.SAT)
        val paymentRequest = PaymentRequest.read("LNBC10U1P3PJ257PP5YZTKWJCZ5FTL5LAXKAV23ZMZEKAW37ZK6KMV80PK4XAEV5QHTZ7QDPDWD3XGER9WD5KWM36YPRX7U3QD36KUCMGYP282ETNV3SHJCQZPGXQYZ5VQSP5USYC4LK9CHSFP53KVCNVQ456GANH60D89REYKDNGSMTJ6YW3NHVQ9QYYSSQJCEWM5CJWZ4A6RFJX77C490YCED6PEMK0UPKXHY89CMM7SCT66K8GNEANWYKZGDRWRFJE69H9U5U0W57RRCSYSAS7GADWMZXC8C6T0SPJAZUP6").get()
        val quote = wallet.requestMeltQuote(paymentRequest)
        println(quote)
    }

    @Test
    fun `Wallet can request mint info`() = runTest {
        val wallet: Wallet = Wallet(mintUrl = "https://testnut.cashu.space", unit = EcashUnit.SAT)
        val mintInfo = wallet.getInfo()
        // println(mintInfo)

        assertEquals("Cashu mint", mintInfo.name)
    }

    // TODO: Fix this test: from what I understand the test completes before the BD has finished its work,
    //       so we're getting a "kotlinx.coroutines.JobCancellationException: Parent job is Completed" exception.
    // @Test
    // fun `Wallet can request minting of new tokens`() {
    //     val wallet = Wallet(mintUrl = "https://mutinynet-cashu.thesimplekid.space")
    //     // val wallet = Wallet(mintUrl = "https://testnut.cashu.space")
    //     wallet.mint(Satoshi(10000))
    // }

    // @Test
    // fun `Payment requests are correctly parsed`() {
    //     // testnut.cashu.space
    //     val pr0: String = "lnbc100000n1pjtsfkdpp58xvj83w64n2g3tpke69m0hn3n0n2fe7l7mcava72d8hn0jyyyv9qdq4gdshx6r4ypjx2ur0wd5hgd704plmuk4hdagrl7xvuw8l7g05pxsce7z9xe3e7ln6ze6a30mh4l4qpvk5z59r8cca0tfe5yxp5qr8pgpyec8fg50yvngjcr3nfapcptqykcl"
    //     // val paymentRequest1 = PaymentRequest.read(pr0) // java.lang.NullPointerException
    //
    //     // https://mutinynet-cashu.thesimplekid.space
    //     val pr1: String = "lntbs100u1pjts2mjsp5ng9k8crcxu6e7e9avr0m8804pyzmea2cujmqs4gx3gy4tmqtdqsspp54qfg2y5tr9hxetxcjd5clkl8yzzutd46rd2dadthtzchq75h7gwshp5uwcvgs5clswpfxhm7nyfjmaeysn6us0yvjdexn9yjkv3k7zjhp2sxqyjw5qcqp2rzjqdn2hj8tfknpuvdg6tz9yrf3e27ltrx9y58c24jh89lnm43yjwfc5q6srvqqqqgqqyqqqqqpqqqqqzsqqc9qxpqysgqdn70nx7gxedqlr0cuumywg08tr05q0hay4e5ql0lphncyy5yx77zsl8vjhgsmxm67px7qf3xdd6z3s9gp4c0an5mkmxd4v0m2cd590cprezhdm"
    //     val paymentRequest = PaymentRequest.read(pr1) // java.lang.NumberFormatException: For input string: "s100"
    // }
}
