/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE.txt file.
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
import me.tb.cashuclient.db.DBProof
import me.tb.cashuclient.db.DBSettings
import me.tb.cashuclient.melt.MeltQuoteRequest
import me.tb.cashuclient.melt.MeltQuoteResponse
import me.tb.cashuclient.melt.MeltRequest
import me.tb.cashuclient.melt.MeltResponse
import me.tb.cashuclient.melt.PreMeltBundle
import me.tb.cashuclient.types.MintQuoteData
import me.tb.cashuclient.mint.MintQuoteRequest
import me.tb.cashuclient.mint.MintQuoteResponse
import me.tb.cashuclient.mint.MintRequest
import me.tb.cashuclient.mint.MintResponse
import me.tb.cashuclient.mint.PreMintBundle
import me.tb.cashuclient.swap.PreSwapBundle
import me.tb.cashuclient.swap.SwapResponse
import me.tb.cashuclient.types.ActiveKeysetsResponse
import me.tb.cashuclient.types.BlindedSignaturesResponse
import me.tb.cashuclient.types.EcashUnit
import me.tb.cashuclient.types.InfoResponse
import me.tb.cashuclient.types.Keyset
import me.tb.cashuclient.types.KeysetId
import me.tb.cashuclient.types.PaymentMethod
import me.tb.cashuclient.types.PreRequestBundle
import me.tb.cashuclient.types.Proof
import me.tb.cashuclient.types.SpecificKeysetResponse
import me.tb.cashuclient.types.SwapRequired
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

public typealias NewAvailableDenominations = List<ULong>
public typealias MintInfo = InfoResponse

/**
 * A wallet handles all operations between a client and a mint. A single wallet is associated
 * with a single mint and a single unit.
 *
 * @param activeKeyset The [Keyset] that is currently active for the mint.
 * @param mintUrl The URL of the mint.
 * @param unit The underlying unit used with the ecash tokens for this wallet.
 */
public class Wallet(
    public var activeKeyset: Keyset? = null,
    private val mintUrl: String,
    private val unit: EcashUnit,
) {
    public val inactiveKeysets: MutableList<Keyset> = mutableListOf()
    private val logger = LoggerFactory.getLogger(Wallet::class.java)

    init {
        logger.info("Wallet initialized with mint url $mintUrl and unit '$unit'.")
    }

    // ---------------------------------------------------------------------------------------------
    // Keysets
    // ---------------------------------------------------------------------------------------------

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
     * Query the mint for the active [Keyset] and set it as the active keyset.
     */
    public fun getActiveKeyset(): Unit = runBlocking(Dispatchers.IO) {
        val client = createClient()
        val keyset = async {
            val response = client.get("$mintUrl$ACTIVE_KEYSET_ENDPOINT")
            val activeKeysetsResponse = response.body<ActiveKeysetsResponse>()

            // TODO: I'm not sure why there can be multiple active keysets at the same time. Open issue on specs repo.
            activeKeysetsResponse.keysets.first().toKeyset()
        }.await()
        client.close()
        addKeyset(keyset)
    }

    // TODO: This method doesn't handle mint errors yet.
    /**
     * Query the mint for the [Keyset] associated with a given [KeysetId].
     */
    public fun getSpecificKeyset(keysetId: KeysetId): Keyset = runBlocking(Dispatchers.IO) {
        val keysetIdHex: String = keysetId.value
        val client = createClient()
        val specificKeyset = async {
            // val response = client.get("$mintUrl$SPECIFIC_KEYSET_PATH$keysetIdHex").bodyAsText()
            // val mintResponse = Json.decodeFromString(ActiveKeysetsResponse.serializer(), response)
            val response = client.get("$mintUrl$SPECIFIC_KEYSET_ENDPOINT$keysetIdHex")
            val specificKeysetResponse: SpecificKeysetResponse = response.body<SpecificKeysetResponse>()

            // TODO: There should only be one keyset in the response it feels odd that the spec requires an array
            specificKeysetResponse.keysets.first().toKeyset()
        }.await()
        client.close()
        specificKeyset
    }

    // ---------------------------------------------------------------------------------------------
    // Mint
    // ---------------------------------------------------------------------------------------------

    /**
     * Request newly minted tokens from the mint. Note that this is done in two parts, this method being the second of
     * the two. You must first ask the mint for a quote, pay the invoice it quoted, and then call this method. The mint
     * returns a list of [me.tb.cashuclient.types.BlindedSignature]s, which the client unblinds and adds to its
     * database.
     *
     * Note that the library currently only supports a single payment method, bolt11 lightning invoices, and a single
     * unit, the satoshi.
     *
     * @param amount The total value to mint.
     */
    public fun mint(amount: Satoshi): Unit = runBlocking(Dispatchers.IO) {
        val client = createClient()
        val scopedActiveKeyset = activeKeyset ?: throw Exception("The wallet must have an active keyset for the mint when attempting a mint operation.")
        val quote: MintQuoteData = requestMintQuote(amount, PaymentMethod.BOLT11)

        val preMintBundle: PreMintBundle  = PreMintBundle.create(
            amount.sat.toULong(),
            quote.quote.quoteId,
            scopedActiveKeyset.keysetId
        )
        val mintingRequest: MintRequest = preMintBundle.buildMintRequest()

        val response = async {
            client.post("$mintUrl$MINT_ENDPOINT/bolt11") {
                method = HttpMethod.Post
                contentType(ContentType.Application.Json)
                setBody(mintingRequest)
            }
        }.await()
        client.close()

        val mintResponse: MintResponse = response.body()

        processBlindedSignaturesResponse(preMintBundle, mintResponse)
    }

    // TODO: This method doesn't handle mint errors yet.
    // Note: We don't persist the quote in the database. The client must pay the quote and call the mint again while
    //       keeping the quote in memory, otherwise simply request a new quote. This is not optimal because a client
    //       might pay an invoice and then get wiped out before calling the mint again, but if it doesn't know the quote
    //       id then it will not be able to prove to the mint that it paid the invoice.
    public fun requestMintQuote(amount: Satoshi, paymentMethod: PaymentMethod): MintQuoteData = runBlocking(Dispatchers.IO) {
        val client = createClient()
        val mintQuoteRequest = MintQuoteRequest(amount.sat.toULong(), unit.toString())

        val response = async {
            client.post("$mintUrl$MINT_QUOTE_ENDPOINT$paymentMethod") {
                method = HttpMethod.Post
                contentType(ContentType.Application.Json)
                setBody(mintQuoteRequest)
            }
        }.await()
        client.close()
        println("Response from mint: ${response.bodyAsText()}")
        val mintQuoteResponse: MintQuoteResponse = response.body<MintQuoteResponse>()

        MintQuoteData.fromMintQuoteResponse(amount, mintQuoteResponse)
    }

    public fun checkMintQuoteStatus(quoteId: String): MintQuoteResponse = runBlocking(Dispatchers.IO) {
        val client = createClient()

        val response = async {
            client.get("$mintUrl$MINT_QUOTE_STATUS_ENDPOINT$quoteId")
        }.await()
        client.close()
        println("Response from the mint regarding quote status: ${response.bodyAsText()}")
        val mintQuoteResponse: MintQuoteResponse = response.body<MintQuoteResponse>()
        mintQuoteResponse
    }

    // ---------------------------------------------------------------------------------------------
    // Melt
    // ---------------------------------------------------------------------------------------------

    public fun requestMeltQuote(pr: PaymentRequest): MeltQuoteResponse = runBlocking(Dispatchers.IO) {
        val client = createClient()
        val meltQuoteRequest = MeltQuoteRequest(pr, EcashUnit.SAT)

        val response = async {
            client.post("$mintUrl${MELT_QUOTE_ENDPOINT}bolt11") {
                method = HttpMethod.Post
                contentType(ContentType.Application.Json)
                setBody(meltQuoteRequest)
            }
        }.await()
        client.close()
        println("Response from mint: ${response.bodyAsText()}")
        val meltQuoteResponse: MeltQuoteResponse = response.body<MeltQuoteResponse>()

        meltQuoteResponse
    }

    /**
     * Melting is exchanging tokens for lightning payments. The process is done in two communication rounds:
     * 1. Asking the mint for a quote for the given payment request (the quote will include fees and fee reserve).
     * 2. Sending the payment request and the fees to the mint.
     *
     * @param paymentRequest The lightning payment request.
     */
    public fun melt(paymentRequest: PaymentRequest): Unit = runBlocking(Dispatchers.IO) {
        val client = createClient()

        val quote: MeltQuoteResponse = requestMeltQuote(paymentRequest)
        // TODO: Look into payment requests and make sure they always have an amount in the case of Cashu. I don't think
        //       they do.
        val paymentAmount: ULong = paymentRequest
            .amount
            ?.truncateToSatoshi()
            ?.toULong() ?: throw Exception("Payment request does not have an amount.")

        val availableProofs: List<ULong> = transaction(DBSettings.db) {
            SchemaUtils.create(DBProof)
            DBProof
                .selectAll()
                .map { it[DBProof.amount] }
        }
        val totalBalance = availableProofs.sum()
        val totalCost = quote.amount + quote.feeReserve

        if (totalBalance < totalCost) {
            throw Exception("Not enough proofs to pay for the payment request.")
        }

        val isSwapRequired: SwapRequired = isSwapRequired(
            allDenominations = availableProofs,
            targetAmount = quote.amount + quote.feeReserve
        )

        val finalListOfProofs = when (isSwapRequired) {
            is SwapRequired.No  -> isSwapRequired.finalList
            is SwapRequired.Yes -> {
                // If a swap is required, we handle it here before moving on
                val missingProofs = swap(
                    availableForSwap = isSwapRequired.availableForSwap,
                    requiredAmount = isSwapRequired.requiredAmount
                )

                isSwapRequired.almostFinishedList + missingProofs
            }
        }

        require(finalListOfProofs.sum() == totalCost) {
            "The sum of tokens to spend must be equal to the sum of the required tokens."
        }

        val preMeltBundle: PreMeltBundle = PreMeltBundle.create(finalListOfProofs, quote.quoteId)
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
        // TODO: Does the inList operator delete _all_ proofs that match the condition or just the first one? In this
        //       case secrets should always be unique anyway, but still I'm wondering how the API works.
        transaction(DBSettings.db) {
            SchemaUtils.create(DBProof)
            val secretsToDelete = preMeltBundle.proofs.map { it.secret }
            DBProof.deleteWhere { secret inList secretsToDelete }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Swap
    // ---------------------------------------------------------------------------------------------

    private fun swap(availableForSwap: List<ULong>, requiredAmount: ULong): NewAvailableDenominations = runBlocking {
        val client = createClient()
        val scopedActiveKeyset = activeKeyset ?: throw Exception("The wallet must have an active keyset for the mint when attempting a swap operation.")
        
        val preSwapRequestBundle = PreSwapBundle.create(availableForSwap, requiredAmount, scopedActiveKeyset.keysetId)
        val swapRequest = preSwapRequestBundle.buildSwapRequest()

        val response = async {
            client.post("$mintUrl$SWAP_ENDPOINT") {
                method = HttpMethod.Post
                contentType(ContentType.Application.Json)
                setBody(swapRequest)
            }
        }.await()
        client.close()

        val swapResponse: SwapResponse = response.body()

        // TODO: Process the mint response
        processBlindedSignaturesResponse(preSwapRequestBundle, swapResponse)

        val newAvailableDenominations = decomposeAmount(requiredAmount)
        newAvailableDenominations
    }

    // ---------------------------------------------------------------------------------------------
    // Process BlindedSignatures
    // ---------------------------------------------------------------------------------------------

    /**
     * The wallet processes the mint's response containing [BlindedSignature]s by unblinding the signatures and adding
     * the [Proof]s to its database. If this processing is for a swap request, the wallet also deletes the proof that
     * was spent to create the swap.
     */
    private fun processBlindedSignaturesResponse(requestBundle: PreRequestBundle, mintResponse: BlindedSignaturesResponse): Unit {
        require(requestBundle.blindingDataItems.size == mintResponse.signatures.size) {
            "The number of outputs in the request and promises in the response must be the same."
        }
        val scopedActiveKeyset = activeKeyset ?: throw Exception("The wallet must have an active keyset for the mint.")

        (requestBundle.blindingDataItems zip mintResponse.signatures).forEach { (blindingData, promise) ->
            // Unblinding is done like so: C = C_ - rK
            val r: PrivateKey = blindingData.blindingFactor
            val K: PublicKey = scopedActiveKeyset.getKey(blindingData.amount)
            val rK: PublicKey = K.times(r)
            val unblindedKey: PublicKey = PublicKey.fromHex(promise.blindedKey).minus(rK)

            val proof: Proof = Proof(
                amount = promise.amount,
                id = scopedActiveKeyset.keysetId.value,
                secret = blindingData.secret.toHex(),
                C = unblindedKey.toHex(),
                script = null
            )

            transaction(DBSettings.db) {
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

        if (requestBundle is PreSwapBundle) {
            SchemaUtils.create(DBProof)
            requestBundle.proofsToSwap.forEach { proof ->
                val secretToDelete = proof.secret
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
    //     // 3. If not, call a swap first to acquire the correct denominations
    // }

    // ---------------------------------------------------------------------------------------------
    // Info
    // ---------------------------------------------------------------------------------------------

    public fun getInfo(): MintInfo = runBlocking(Dispatchers.IO) {
        val client = createClient()
        val response = async {
            client.get("$mintUrl$INFO_ENDPOINT")
        }.await()
        client.close()

        val mintInfo: InfoResponse = response.body<InfoResponse>()
        mintInfo
    }

    // ---------------------------------------------------------------------------------------------
    // Utilities
    // ---------------------------------------------------------------------------------------------

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
}
