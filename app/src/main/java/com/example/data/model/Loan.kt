package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "loans")
data class Loan(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val personName: String,
    val amount: Double,
    val type: String, // "LENT" or "BORROWED"
    val isSettled: Boolean = false,
    val date: Long = System.currentTimeMillis(),
    val dueDate: Long = System.currentTimeMillis() + (86400000L * 30), // defaulting to 30 days
    val notes: String = "",
    val paidAmount: Double = 0.0,
    val paymentFrequency: String = "MONTHLY", // "MONTHLY" or "CUSTOM_DAYS"
    val paymentIntervalDays: Int = 30
)
