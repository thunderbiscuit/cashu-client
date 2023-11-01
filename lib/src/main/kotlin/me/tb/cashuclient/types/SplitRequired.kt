package me.tb.cashuclient.types

public sealed class SplitRequired {
    public data class No(
        val finalList: List<ULong>
    ): SplitRequired()

    public data class Yes(
        val almostFinishedList: List<ULong>,
        val splitDenomination: ULong,
        val requiredAmount: ULong,
    ): SplitRequired()
}
