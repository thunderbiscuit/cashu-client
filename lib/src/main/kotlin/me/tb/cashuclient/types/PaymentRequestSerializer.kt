/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE.txt file.
 */

package me.tb.cashuclient.types

import fr.acinq.lightning.payment.PaymentRequest
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

public object PaymentRequestSerializer : KSerializer<PaymentRequest> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("PaymentRequest", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: PaymentRequest) {
        encoder.encodeString(value.write())
    }

    // TODO: Handle the error case here
    override fun deserialize(decoder: Decoder): PaymentRequest {
        return PaymentRequest.read(decoder.decodeString()).get()
    }
}
