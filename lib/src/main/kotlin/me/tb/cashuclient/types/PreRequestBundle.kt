package me.tb.cashuclient.types

import me.tb.cashuclient.KeysetId

public interface PreRequestBundle {
    public val blindingDataItems: List<BlindingData>
    public val keysetId: KeysetId
}
