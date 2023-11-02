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
import me.tb.cashuclient.melt.CheckFeesRequest
import me.tb.cashuclient.melt.CheckFeesResponse
import me.tb.cashuclient.mint.InvoiceResponse
import me.tb.cashuclient.melt.MeltRequest
import me.tb.cashuclient.melt.MeltResponse
import me.tb.cashuclient.melt.PreMeltBundle
import me.tb.cashuclient.mint.MintRequest
import me.tb.cashuclient.mint.MintResponse
import me.tb.cashuclient.split.PreSplitBundle
import me.tb.cashuclient.split.SplitResponse
import me.tb.cashuclient.types.BlindedSignaturesResponse
import me.tb.cashuclient.types.PreRequestBundle
import me.tb.cashuclient.types.Proof
import me.tb.cashuclient.types.SplitRequired
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

public typealias NewAvailableDenominations = List<ULong>

@Suppress("LocalVariableName")
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

        val mintResponse: MintResponse = response.body()

        processBlindedSignaturesResponse(preMintBundle, mintResponse)
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

    // ---------------------------------------------------------------------------------------------
    // MELT
    // ---------------------------------------------------------------------------------------------

    /**
     * Check the fees for a given payment request. This function is used internally as part of the [melt] call.
     */
    private fun checkFees(paymentRequest: PaymentRequest, client: HttpClient): CheckFeesResponse = runBlocking(Dispatchers.IO) {
        val checkFeesRequest: CheckFeesRequest = CheckFeesRequest(paymentRequest)

        val response = async {
            client.post("$mintUrl/mint") {
                method = HttpMethod.Post
                contentType(ContentType.Application.Json)
                setBody(checkFeesRequest)
            }
        }.await()

        val responseString: String = response.body<String>()
        println("Response from mint: $responseString")
        val maximumFees: CheckFeesResponse = response.body<CheckFeesResponse>()
        println("Maximum fees: $maximumFees")
        maximumFees
    }

    /**
     * Melting is exchanging tokens for lightning payments. The process is done in two communication rounds:
     * 1. Asking what the fees are likely to be for a given payment request.
     * 2. Sending the payment request and the fees to the mint.
     *
     * @param paymentRequest The lightning payment request.
     */
    private fun melt(paymentRequest: PaymentRequest): Unit = runBlocking(Dispatchers.IO) {
        val client = createClient()

        val fee: CheckFeesResponse = checkFees(paymentRequest, client)
        // TODO: Look into payment requests and make sure they always have an amount in the case of Cashu. I don't think
        //       they do.
        val paymentAmount: ULong = paymentRequest
            .amount
            ?.truncateToSatoshi()
            ?.toULong() ?: throw Exception("Payment request does not have an amount.")

        DBSettings.db
        val availableDenominations: List<ULong> = transaction {
            SchemaUtils.create(DBProof)
            DBProof
                .selectAll()
                .map { it[DBProof.amount] }
        }
        val totalBalance = availableDenominations.sum()

        if (totalBalance < paymentAmount + fee.fee) {
            throw Exception("Not enough tokens to pay for the payment request.")
        }

        val isSplitRequired: SplitRequired = isSplitRequired(
            availableDenominations = availableDenominations,
            targetAmount = paymentAmount + fee.fee
        )

        val finalListOfDenominations = when (isSplitRequired) {
            is SplitRequired.No  -> isSplitRequired.finalList
            is SplitRequired.Yes -> {
                // If a split is required, we handle it here before moving on
                val missingDenominations = split(
                    denominationToSplit = isSplitRequired.splitDenomination,
                    requiredAmount = isSplitRequired.requiredAmount
                )

                isSplitRequired.almostFinishedList + missingDenominations
            }
        }

        require(finalListOfDenominations.sum() == paymentAmount + fee.fee) {
            "The sum of tokens to spend must be equal to the sum of the required tokens."
        }

        val preMeltBundle: PreMeltBundle = PreMeltBundle.create(finalListOfDenominations, paymentRequest)
        val meltRequest: MeltRequest = preMeltBundle.buildMeltRequest()

        val response = async {
            client.post("$mintUrl/melt") {
                method = HttpMethod.Post
                contentType(ContentType.Application.Json)
                setBody(meltRequest)
            }
        }.await()
        client.close()

        val responseString: String = response.body<String>()
        println("Response from mint: $responseString")
        val meltResponse: MeltResponse = response.body<MeltResponse>()
        println("Melt response: $meltResponse")

        if (meltResponse.paid) {
            processMeltResponse(preMeltBundle)
        } else {
            throw Exception("The payment request was not paid.")
        }
    }

    private fun processMeltResponse(preMeltBundle: PreMeltBundle) {
        // TODO: Should we simply mark them as archived instead of deleting them? We could have a separate method for
        //       collecting the proofs that are archived and deleting them upon user request.
        // TODO: Should we add the preimage to the database?
        DBSettings.db
        transaction {
            SchemaUtils.create(DBProof)
            val secretsToDelete = preMeltBundle.proofs.map { it.secret }
            DBProof.deleteWhere { secret inList secretsToDelete }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // SPLIT
    // ---------------------------------------------------------------------------------------------

    private fun split(denominationToSplit: ULong, requiredAmount: ULong): NewAvailableDenominations = runBlocking {
        val client = createClient()

        val preSplitRequestBundle = PreSplitBundle.create(denominationToSplit, requiredAmount)
        val splitRequest = preSplitRequestBundle.buildSplitRequest()

        val response = async {
            client.post("$mintUrl/melt") {
                method = HttpMethod.Post
                contentType(ContentType.Application.Json)
                setBody(splitRequest)
            }
        }.await()
        client.close()

        val splitResponse: SplitResponse = response.body()

        // TODO: Process the mint response
        processBlindedSignaturesResponse(preSplitRequestBundle, splitResponse)

        val newAvailableDenominations = decomposeAmount(requiredAmount)
        newAvailableDenominations
    }

    // ---------------------------------------------------------------------------------------------
    // Process BlindedSignatures
    // ---------------------------------------------------------------------------------------------

    /**
     * The wallet processes the mint's response by unblinding the signatures and adding the [Proof]s to its database. If
     * this processing is for a split request, the wallet also deletes the proof that was spent to create the split.
     */
    @OptIn(ExperimentalEncodingApi::class)
    private fun processBlindedSignaturesResponse(requestBundle: PreRequestBundle, mintResponse: BlindedSignaturesResponse): Unit {
        require(requestBundle.blindingDataItems.size == mintResponse.promises.size) {
            "The number of outputs in the request and promises in the response must be the same."
        }
        val scopedActiveKeyset = this.activeKeyset ?: throw Exception("The wallet must have an active keyset for the mint.")

        (requestBundle.blindingDataItems zip mintResponse.promises).forEach { (blindingData, promise) ->
            // Unblinding is done like so: C = C_ - rK
            val r: PrivateKey = blindingData.blindingFactor
            val K: PublicKey = scopedActiveKeyset.getKey(blindingData.amount)
            val rK: PublicKey = K.times(r)
            val unblindedKey: PublicKey = PublicKey.fromHex(promise.blindedKey).minus(rK)

            // Note: The secret is initially a ByteArray and is converted to a Base64 string for storage in the database,
            //       and the mints currently use the utf-8 bytes out of this string instead of the actual ByteArray.
            val proof: Proof = Proof(
                amount = promise.amount,
                secret = Base64.encode(blindingData.secret.value),
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

        if (requestBundle is PreSplitBundle) {
            DBSettings.db
            transaction {
                SchemaUtils.create(DBProof)
                val secretToDelete = requestBundle.proofToSplit.secret
                DBProof.deleteWhere { secret eq secretToDelete }
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Create tokens for peer-to-peer payments
    // ---------------------------------------------------------------------------------------------

    // public fun buildPaymentToken(totalValue: ULong): TokenV3 {
    //     // 1. Do I have the enough in the wallet to build this token?
    //     // 2. Do I have the correct denominations to build it?
    //     // 3. If not, call a split first to acquire the correct denominations
    // }
}
