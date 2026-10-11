package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TransactionType {
    YOU_GAVE,   // Credit given / Payment made -> increases what party owes you (or reduces what you owe)
    YOU_GOT,    // Payment received / Credit taken -> decreases what party owes you
    CASH_IN,    // Cashbook inflow
    CASH_OUT    // Cashbook outflow
}

enum class PaymentMode {
    CASH,
    UPI,
    BANK_TRANSFER,
    CARD,
    OTHER
}

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Party::class,
            parentColumns = ["id"],
            childColumns = ["partyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["partyId"])]
)
data class TransactionEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val partyId: Long? = null, // null for standalone Cashbook entries
    val partyNameSnapshot: String = "",
    val amount: Double,
    val type: TransactionType,
    val paymentMode: PaymentMode = PaymentMode.CASH,
    val description: String = "",
    val billNumber: String = "",
    val itemsSummary: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
