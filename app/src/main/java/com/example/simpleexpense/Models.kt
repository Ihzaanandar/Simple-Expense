package com.example.simpleexpense

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey
    val id: String,
    val amount: Double,
    val paymentMethod: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    // Foreign key to link to a MonthYear
    val monthYearKey: String
)

@Entity(tableName = "month_years")
data class MonthYear(
    @PrimaryKey
    val key: String, // Example: "2025-12"
    val month: Int,
    val year: Int
) {
    fun getDisplayName(): String {
        val months = arrayOf(
            "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        )
        return "${months[month - 1]} $year"
    }
}

// This is no longer a database entity, just a helper class for summaries.
data class ExpenseSummary(
    val totalExpense: Double,
    val expenseCount: Int,
    val byPaymentMethod: Map<String, Double>
)
