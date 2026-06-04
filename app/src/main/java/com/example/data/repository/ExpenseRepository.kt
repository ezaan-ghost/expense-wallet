package com.example.data.repository

import com.example.data.local.AccountDao
import com.example.data.local.CategoryDao
import com.example.data.local.TransactionDao
import com.example.data.local.LoanDao
import com.example.data.model.Account
import com.example.data.model.Category
import com.example.data.model.Transaction
import com.example.data.model.Loan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ExpenseRepository(
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val loanDao: LoanDao
) {
    val allAccounts: Flow<List<Account>> = accountDao.getAllAccounts()
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    val allLoans: Flow<List<Loan>> = loanDao.getAllLoans()

    suspend fun getAccountById(id: Int): Account? = accountDao.getAccountById(id)
    suspend fun getCategoryById(id: Int): Category? = categoryDao.getCategoryById(id)

    suspend fun checkAndPrepopulateDefaults() {
        val categories = allCategories.first()
        if (categories.isEmpty()) {
            val defaultCategories = listOf(
                Category(name = "Groceries", type = "EXPENSE", iconName = "shopping_cart", colorHex = "#4CAF50"),
                Category(name = "Food & Dining", type = "EXPENSE", iconName = "restaurant", colorHex = "#FF9800"),
                Category(name = "Transport", type = "EXPENSE", iconName = "directions_car", colorHex = "#2196F3"),
                Category(name = "Entertainment", type = "EXPENSE", iconName = "sports_esports", colorHex = "#9C27B0"),
                Category(name = "Utilities", type = "EXPENSE", iconName = "bolt", colorHex = "#FFD600"),
                Category(name = "Health & Care", type = "EXPENSE", iconName = "medical_services", colorHex = "#E91E63"),
                Category(name = "Salary", type = "INCOME", iconName = "payments", colorHex = "#009688"),
                Category(name = "Investments", type = "INCOME", iconName = "trending_up", colorHex = "#00E676"),
                Category(name = "Gifts", type = "INCOME", iconName = "redeem", colorHex = "#FF4081")
            )
            for (category in defaultCategories) {
                categoryDao.insertCategory(category)
            }
        }

        val accounts = allAccounts.first()
        if (accounts.isEmpty()) {
            val defaultAccounts = listOf(
                Account(name = "Cash", balance = 0.00, colorHex = "#4CAF50", iconName = "payments"),
                Account(name = "Bank Card", balance = 0.00, colorHex = "#2196F3", iconName = "credit_card"),
                Account(name = "Savings", balance = 0.00, colorHex = "#FFD700", iconName = "savings")
            )
            for (account in defaultAccounts) {
                accountDao.insertAccount(account)
            }
        }
    }

    suspend fun insertAccount(account: Account) {
        accountDao.insertAccount(account)
    }

    suspend fun updateAccount(account: Account) {
        accountDao.updateAccount(account)
    }

    suspend fun deleteAccount(account: Account) {
        accountDao.deleteAccount(account)
    }

    suspend fun insertCategory(category: Category) {
        categoryDao.insertCategory(category)
    }

    suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category)
    }

    // Modern transaction builder: automatically adjustments balance of the linked Account.
    suspend fun insertTransaction(transaction: Transaction) {
        val account = accountDao.getAccountById(transaction.accountId)
        if (account != null) {
            val balanceAdjustment = if (transaction.type == "INCOME") {
                transaction.amount
            } else {
                -transaction.amount
            }
            val updatedAccount = account.copy(balance = account.balance + balanceAdjustment)
            accountDao.updateAccount(updatedAccount)
        }
        transactionDao.insertTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        // Reverse the balance adjustment
        val account = accountDao.getAccountById(transaction.accountId)
        if (account != null) {
            val balanceAdjustment = if (transaction.type == "INCOME") {
                -transaction.amount
            } else {
                transaction.amount
            }
            val updatedAccount = account.copy(balance = account.balance + balanceAdjustment)
            accountDao.updateAccount(updatedAccount)
        }
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun updateTransaction(oldTransaction: Transaction, newTransaction: Transaction) {
        // Reverse old transaction balance adjustment
        val oldAccount = accountDao.getAccountById(oldTransaction.accountId)
        if (oldAccount != null) {
            val oldAdjustment = if (oldTransaction.type == "INCOME") {
                -oldTransaction.amount
            } else {
                oldTransaction.amount
            }
            accountDao.updateAccount(oldAccount.copy(balance = oldAccount.balance + oldAdjustment))
        }

        // Apply new transaction balance adjustment
        val newAccount = accountDao.getAccountById(newTransaction.accountId)
        if (newAccount != null) {
            val newAdjustment = if (newTransaction.type == "INCOME") {
                newTransaction.amount
            } else {
                -newTransaction.amount
            }
            accountDao.updateAccount(newAccount.copy(balance = newAccount.balance + newAdjustment))
        }

        transactionDao.updateTransaction(newTransaction)
    }

    // Modern Loans Management Integrations
    suspend fun insertLoan(loan: Loan) {
        loanDao.insertLoan(loan)
    }

    suspend fun updateLoan(loan: Loan) {
        loanDao.updateLoan(loan)
    }

    suspend fun deleteLoan(loan: Loan) {
        loanDao.deleteLoan(loan)
    }
}
