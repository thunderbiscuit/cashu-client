/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE file.
 */

package me.tb.cashuclient.split

import me.tb.cashuclient.types.BlindedSignature

public data class SplitResponse(
    public val promises: List<BlindedSignature>
)
