/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE.txt file.
 */
 
 package me.tb.cashuclient.types

public sealed class SwapRequired {
    public data class No(
        val finalList: List<ULong>
    ): SwapRequired()

    public data class Yes(
        val requiredAmount: ULong,
        val almostFinishedList: List<ULong>,
        val availableForSwap: List<ULong>,
    ): SwapRequired()
}
