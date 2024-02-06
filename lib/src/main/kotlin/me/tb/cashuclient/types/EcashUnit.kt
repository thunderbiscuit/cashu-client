/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE.txt file.
 */
 
package me.tb.cashuclient.types

/**
 * Represents the possible units of ecash that are represented by the Cashu tokens a wallet holds. This library
 * currently only supports the satoshi as the underlying unit for the ecash token it handles.
 */
public enum class EcashUnit {
    /**
     * The satoshi, equivalent to 0.00000001 bitcoin.
     */
    SAT;

    override fun toString(): String {
        return this.name.lowercase()
    }
}
