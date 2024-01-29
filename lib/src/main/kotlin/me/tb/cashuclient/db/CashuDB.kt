package me.tb.cashuclient.db

import me.tb.cashuclient.types.Proof

public interface CashuDB {
    public fun insertProof(proof: Proof): Unit

    public fun deleteProof(proof: Proof): Unit

    public fun spendableNoteSizes(): List<ULong>
}
