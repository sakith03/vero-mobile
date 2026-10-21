package com.example.veromobile.domain.model

enum class PaymentChannel(val label: String) {
    CARD("Card"), CASH("Cash"), SECONDARY_ACCOUNT("Secondary"),
    BANK_TRANSFER("Bank transfer"), OTHER("Other")
}