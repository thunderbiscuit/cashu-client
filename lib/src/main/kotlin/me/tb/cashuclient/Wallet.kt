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
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import me.tb.cashuclient.db.DBBolt11Payment
import me.tb.cashuclient.db.DBProof
import me.tb.cashuclient.db.DBSettings
import me.tb.cashuclient.mint.PreMintBundle
import me.tb.cashuclient.split.PreSplitBundle
import me.tb.cashuclient.melt.CheckFeesRequest
import me.tb.cashuclient.melt.CheckFeesResponse
import me.tb.cashuclient.mint.InvoiceResponse
import me.tb.cashuclient.melt.MeltRequest
import me.tb.cashuclient.melt.MeltResponse
import me.tb.cashuclient.melt.PreMeltBundle
import me.tb.cashuclient.mint.MintRequest
import me.tb.cashuclient.mint.MintResponse
import me.tb.cashuclient.types.Proof
import me.tb.cashuclient.split.SplitResponse
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

public class Wallet(
    public var activeKeyset: Keyset? = null,
    private val mintUrl: String
) {
    public val inactiveKeysets: MutableList<Keyset> = mutableListOf()

    /**
     * A factory function to create a client for communication with the mint. This pattern allows
     * us to close the client after each request as there is no need to keep a client open for the
     * lifetime of the wallet given that calls are expected to be infrequent.
     */
    private fun createClient(): HttpClient {
        return HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(
                    Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                    }
                )
            }
            // TODO: Is this the logging level we want? Better configuration would probably be nice.
            install(Logging) {
                logger = Logger.DEFAULT
            }
        }
    }

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

    // ---------------------------------------------------------------------------------------------
    // MINT
    // ---------------------------------------------------------------------------------------------

    /**
     * Request newly minted tokens from the mint. The mint returns a list of [me.tb.cashuclient.types.BlindedSignature]s,
     * which the client must unblind and add to its database.
     *
     * @param amount The amount of token we want to mint.
     */
    public fun mint(amount: Satoshi): Unit = runBlocking(Dispatchers.IO) {
        val client = createClient()

        // Ask the mint for a bolt11 invoice
        val invoiceResponse: InvoiceResponse = requestFundingInvoice(amount, client)

        // Use it to build a mint request
        val preMintBundle: PreMintBundle = PreMintBundle.create(amount.toULong())
        val mintingRequest: MintRequest = preMintBundle.buildMintRequest()

        val response = async {
            client.post("$mintUrl/mint") {
                method = HttpMethod.Post
                url {
                    parameters.append("hash", invoiceResponse.hash)
                }
                contentType(ContentType.Application.Json)
                setBody(mintingRequest)
            }
        }.await()
        client.close()

        // println("Mint response: ${response.body<String>()}")
        val mintResponse: MintResponse = response.body()

        processMintResponse(preMintBundle, mintResponse)
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
    private fun requestFundingInvoice(amount: Satoshi, client: HttpClient): InvoiceResponse = runBlocking(Dispatchers.IO) {
        // Part 1: call the mint and get a bolt11 invoice
        val response = async {
            client.request("$mintUrl/mint") {
                method = HttpMethod.Get
                url { parameters.append("amount", amount.sat.toString()) }
            }
        }.await()
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
     * The wallet processes the mint's response by unblinding the signatures and adding the [Proof]s to its database.
     */
    @OptIn(ExperimentalEncodingApi::class)
    private fun processMintResponse(preMintBundle: PreMintBundle, mintResponse: MintResponse): Unit {
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

            // Note: The secret is initially a ByteArray and is converted to a Base64 string for storage in the database,
            //       and the mints currently use the utf-8 bytes out of this string instead of the actual ByteArray.
            val proof: Proof = Proof(
                amount = promise.amount,
                secret = Base64.encode(preMintItem.secret.value),
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

        val client = createClient()

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
