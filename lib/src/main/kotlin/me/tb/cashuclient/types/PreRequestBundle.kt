package me.tb.cashuclient.types

public interface PreRequestBundle {
    public val blindingDataItems: List<BlindingData>
    public val keysetId: KeysetId
}
