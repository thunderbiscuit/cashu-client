/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE.txt file.
 */

package me.tb.cashuclient.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class InfoResponse(
    val name: String,
    val pubkey: String,
    val version: String,
    val description: String? = null,
    @SerialName("description_long") val longDescription: String? = null,
    val contact: List<List<String>>,
    @SerialName("motd") val messageOfTheDay: String? = null,
    val nuts: Map<String, Nut>,
)

@Serializable
public data class Method(
    val method: String,
    val unit: String
)

@Serializable
public data class Nut(
    val methods: List<Method>? = null,
    val disabled: Boolean? = null,
    val supported: Boolean? = null
)
