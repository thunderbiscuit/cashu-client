/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE file.
 */
 
package me.tb.cashuclient

import fr.acinq.bitcoin.PrivateKey
import fr.acinq.bitcoin.PublicKey
import fr.acinq.bitcoin.Satoshi
import fr.acinq.lightning.payment.PaymentRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import me.tb.cashuclient.db.DBBolt11Payment
import me.tb.cashuclient.db.DBProof
import me.tb.cashuclient.db.DBSettings
import me.tb.cashuclient.db.getAmountByHash
import me.tb.cashuclient.types.CheckFeesRequest
import me.tb.cashuclient.types.CheckFeesResponse
import me.tb.cashuclient.types.InvoiceResponse
import me.tb.cashuclient.types.MintingRequest
import me.tb.cashuclient.types.MintingResponse
import me.tb.cashuclient.types.Proof
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

public class Wallet(
    public var activeKeyset: Keyset? = null,
    private val mintUrl: String
) {
    public val inactiveKeysets: MutableList<Keyset> = mutableListOf()

    /**
     * Rotate the active [Keyset] for the wallet.
     */
    private fun addKeyset(keyset: Keyset) {
        val currentActiveKeyset = this.activeKeyset
        if (currentActiveKeyset != null) inactiveKeysets.add(currentActiveKeyset)
        this.activeKeyset = keyset
    }

    // TODO: This method doesn't handle mint errors yet.
    /**
     * Query the mint for the active [Keyset] and set it as the active keyset.*
     */
    public fun getActiveKeyset(): Unit = runBlocking(Dispatchers.IO) {
        val keyset = async {
            val client = HttpClient(OkHttp)
            val keysetJson = client.get("$mintUrl/keys").bodyAsText()
            client.close()
            Keyset.fromJson(keysetJson)
        }
        addKeyset(keyset.await())
    }

    // TODO: This method doesn't handle mint errors yet.
    /**
     * Query the mint for the [Keyset] associated with a given [KeysetId].
     */
    public fun getSpecificKeyset(keysetId: KeysetId): Keyset = runBlocking(Dispatchers.IO) {
        val urlSafeKeysetId = base64ToBase64UrlSafe(keysetId.value)
        val oldKeyset = async {
            val client = HttpClient(OkHttp)
            val keysetJson = client.get("$mintUrl/keys/$urlSafeKeysetId").bodyAsText()
            client.close()
            Keyset.fromJson(keysetJson)
        }
        oldKeyset.await()
    }

    // TODO: This method doesn't handle mint errors yet.
    // TODO: Make sure we sanitize the logs
    // TODO: I think this method could return Unit instead of InvoiceResponse and the client simply moves on to the next
    //       step. The bolt11 invoice is stored in the database, ready to be given out to the user to pay.
    // TODO: Test entries to the database
    /**
     * Initiate minting request with the mint for a given amount. The mint will return a bolt11 invoice the client must pay
     * in order to proceed to the next step and request newly minted tokens.
     */
    public fun requestFundingInvoice(amount: Satoshi): InvoiceResponse = runBlocking(Dispatchers.IO) {
        val client = HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(
                    Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                    }
                )
            }
            install(Logging) {
                logger = Logger.DEFAULT
            }
        }

        // Part 1: call the mint and get a bolt11 invoice
        val response = async {
            client.request("$mintUrl/mint") {
            method = HttpMethod.Get
            url {
                parameters.append("amount", amount.sat.toString())
            }
        }}.await()
        client.close()

        val fundingInvoiceResponse: InvoiceResponse = response.body()

        // Part 2: add information to database
        DBSettings.db
        transaction {
            SchemaUtils.create(DBBolt11Payment)

            // TODO: Think of what to do if the bolt11 invoice is already in the database
            DBBolt11Payment.insert {
                it[pr] = fundingInvoiceResponse.pr
                it[hash] = fundingInvoiceResponse.hash
                it[value] = amount.sat.toULong()
            }
        }

        fundingInvoiceResponse
    }

    /**
     * Request newly minted tokens from the mint. The request requires the client provides the payment ID agreed upon between
     * the client and the mint (also called the hash, for better or worse, because it's not the preimage hash of the payment at all).
     *
     * The mint will return a list of [me.tb.cashuclient.types.BlindedSignature]s, which the client must unblind and add to its database.
     *
     * @param hash The payment ID agreed upon between the client and the mint.
     */
    public fun requestNewTokens(hash: String): Unit = runBlocking(Dispatchers.IO) {
        val client = HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(
                    Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                    }
                )
            }
            install(Logging) {
                logger = Logger.DEFAULT
            }
        }

        val amount = getAmountByHash(hash)
        val preMintBundle: PreMintBundle = PreMintBundle.create(amount)
        val mintingRequest: MintingRequest = preMintBundle.buildMintingRequest()

        val response = async {
            client.post("$mintUrl/mint") {
                method = HttpMethod.Post
                url {
                    parameters.append("hash", hash)
                }
                contentType(ContentType.Application.Json)
                setBody(mintingRequest)
            }}.await()
        client.close()

        // println("Mint response: ${response.body<String>()}")
        val mintResponse: MintingResponse = response.body()

        processMintResponse(preMintBundle, mintResponse)
    }

    /**
     * The wallet processes the mint's response by unblinding the signatures and adding the [Proof]s to its database.
     */
    private fun processMintResponse(preMintBundle: PreMintBundle, mintResponse: MintingResponse) {
        require(preMintBundle.preMintItems.size == mintResponse.promises.size) {
            "The number of outputs in the mint request and promises in the mint response must be the same."
        }
        val scopedActiveKeyset = this.activeKeyset ?: throw Exception("The wallet must have an active keyset for the mint.")

        (preMintBundle.preMintItems zip mintResponse.promises).forEach { (preMintItem, promise) ->
            // Unblinding is done like so: C = C_ - rK
            val r: PrivateKey = preMintItem.blindingFactor
            val K: PublicKey = scopedActiveKeyset.getKey(preMintItem.amount)
            val rK: PublicKey = K.times(r)
            val unblindedKey: PublicKey = PublicKey.fromHex(promise.blindedKey).minus(rK)

            val proof: Proof = Proof(
                amount = promise.amount,
                secret = r.toHex(),
                C = unblindedKey.toHex(),
                id = scopedActiveKeyset.keysetId.value,
                script = null
            )

            println("Adding proof to database: $proof")

            DBSettings.db
            transaction {
                SchemaUtils.create(DBProof)

                DBProof.insert {
                    it[amount] = proof.amount
                    it[secret] = proof.secret
                    it[C] = proof.C
                    it[id] = proof.id
                    it[script] = null
                }
            }
        }
    }

    public fun checkFees(paymentRequest: PaymentRequest): CheckFeesResponse = runBlocking(Dispatchers.IO) {
        val checkFeesRequest: CheckFeesRequest = CheckFeesRequest(paymentRequest)

        val client = HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(
                    Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                    }
                )
            }
            install(Logging) {
                logger = Logger.DEFAULT
            }
        }

        val response = async {
            client.post("$mintUrl/mint") {
                method = HttpMethod.Post
                contentType(ContentType.Application.Json)
                setBody(checkFeesRequest)
            }}.await()
        client.close()

        val responseString: String = response.body<String>()
        println("Response from mint: $responseString")
        val maximumFees: CheckFeesResponse = response.body<CheckFeesResponse>()
        println("Maximum fees: $maximumFees")
        maximumFees
    }
}
