/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE.txt file.
 */
 
package me.tb.cashuclient.types

/**
 * This interface represents the response from the mint and swap endpoints, where the mint returns a list of blinded
 * signatures ready to be processed and persisted.
 */
public interface BlindedSignaturesResponse {
    public val signatures: List<BlindedSignature>
}
