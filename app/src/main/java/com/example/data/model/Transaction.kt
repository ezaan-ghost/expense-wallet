package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val accountId: Int,
    val categoryId: Int,
    val amount: Double,
    val type: String, // "EXPENSE" or "INCOME"
    val dateTime: Long, // timestamp
    val note: String,
    val paymentMethod: String = "Cash"
)
