/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE.txt file.
 */

package me.tb.cashuclient.db

import me.tb.cashuclient.types.Proof

/**
 * Interface for interacting with the database layer of the wallet.
 *
 * The default implementation of this interface is a [SQLiteDB] database. Android applications
 * might want build their own implementation to use Room.
 */
public interface CashuDB {
    /**
     * Inserts a proof into the database.
     *
     * This method is responsible for persisting the given proof object into the database,
     * ensuring that it can be referenced in future transactions or queries.
     *
     * @param proof The [Proof] object to be inserted into the database.
     */
    public fun insertProof(proof: Proof): Unit

    /**
     * Deletes a proof from the database.
     *
     * This method removes the specified proof object from the database. Use with caution, as this
     * means that the associated ecash is not recoverable.
     *
     * @param proof The [Proof] object to be deleted from the database.
     */
    public fun deleteProof(proof: Proof): Unit

    /**
     * Retrieves a list of spendable note sizes available in the database.
     *
     * This method is used to query the database for the sizes of notes that are currently
     * spendable. This is useful for determining what sizes are available for creating new
     * transactions.
     *
     * @return A list of [ULong] representing the sizes of spendable notes.
     */
    public fun spendableNoteSizes(): List<ULong>
}
