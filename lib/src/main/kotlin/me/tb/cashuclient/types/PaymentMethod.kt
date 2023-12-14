package me.tb.cashuclient.types

public enum class PaymentMethod {
    BOLT11;

    override fun toString(): String {
        return this.name.lowercase()
    }
}
