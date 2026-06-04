package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Account
import com.example.data.model.Category
import com.example.data.model.Transaction
import com.example.data.model.Loan
import com.example.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ExpenseRepository

    val accounts: StateFlow<List<Account>>
    val categories: StateFlow<List<Category>>
    val transactions: StateFlow<List<Transaction>>
    val loans: StateFlow<List<Loan>>

    private val prefs = application.getSharedPreferences("ivy_expense_prefs", Context.MODE_PRIVATE)

    // Currency Settings State
    private val _currencySymbol = MutableStateFlow(prefs.getString("currency_symbol", "$") ?: "$")
    val currencySymbol: StateFlow<String> = _currencySymbol.asStateFlow()

    fun setCurrencySymbol(sym: String) {
        prefs.edit().putString("currency_symbol", sym).apply()
        _currencySymbol.value = sym
    }

    // Custom Transaction/Payment records Settings (Cash, Bank Card, Online transfer, etc.)
    private val defaultPaymentMethods = "Cash,Bank Card,Online,Credit Card,UPI"
    private val _paymentMethods = MutableStateFlow(
        (prefs.getString("payment_methods", defaultPaymentMethods) ?: defaultPaymentMethods)
            .split(",")
            .filter { it.isNotBlank() }
    )
    val paymentMethods: StateFlow<List<String>> = _paymentMethods.asStateFlow()

    fun addPaymentMethod(method: String) {
        val current = _paymentMethods.value.toMutableList()
        if (method.isNotBlank() && !current.contains(method)) {
            current.add(method)
            prefs.edit().putString("payment_methods", current.joinToString(",")).apply()
            _paymentMethods.value = current
        }
    }

    fun removePaymentMethod(method: String) {
        val current = _paymentMethods.value.toMutableList()
        if (current.contains(method)) {
            current.remove(method)
            prefs.edit().putString("payment_methods", current.joinToString(",")).apply()
            _paymentMethods.value = current
        }
    }

    // Password Lock settings (Offline PIN)
    private val _appPinCode = MutableStateFlow(prefs.getString("app_pin_code", "") ?: "")
    val appPinCode: StateFlow<String> = _appPinCode.asStateFlow()

    val isPinRequired: StateFlow<Boolean> = _appPinCode
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun updatePinCode(pin: String) {
        prefs.edit().putString("app_pin_code", pin).apply()
        _appPinCode.value = pin
    }

    fun disablePinCode() {
        prefs.edit().putString("app_pin_code", "").apply()
        _appPinCode.value = ""
        setBiometricEnabled(false)
    }

    // Biometric Unlock configuration settings
    private val _isBiometricEnabled = MutableStateFlow(prefs.getBoolean("is_biometric_enabled", false))
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("is_biometric_enabled", enabled).apply()
        _isBiometricEnabled.value = enabled
    }

    // Onboarding and User Info Settings State
    private val _userName = MutableStateFlow(prefs.getString("user_name", "") ?: "")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _isOnboardingCompleted = MutableStateFlow(prefs.getBoolean("onboarding_completed", false))
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

    fun completeOnboarding(name: String, currency: String) {
        prefs.edit()
            .putString("user_name", name)
            .putString("currency_symbol", currency)
            .putBoolean("onboarding_completed", true)
            .apply()
        _userName.value = name
        _currencySymbol.value = currency
        _isOnboardingCompleted.value = true
    }

    fun updateUserName(name: String) {
        prefs.edit().putString("user_name", name).apply()
        _userName.value = name
    }

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ExpenseRepository(
            database.accountDao(),
            database.categoryDao(),
            database.transactionDao(),
            database.loanDao()
        )

        accounts = repository.allAccounts
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        categories = repository.allCategories
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        transactions = repository.allTransactions
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        loans = repository.allLoans
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Ensure defaults are populated on launch
        viewModelScope.launch {
            repository.checkAndPrepopulateDefaults()
        }
    }

    // Reactive calculations
    val totalBalance: StateFlow<Double> = accounts
        .map { list -> list.sumOf { it.balance } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val monthlyExpense: StateFlow<Double> = transactions
        .map { list ->
            val startOfMonth = getStartOfMonthTimestamp()
            list.filter { it.type == "EXPENSE" && it.dateTime >= startOfMonth }
                .sumOf { it.amount }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val monthlyIncome: StateFlow<Double> = transactions
        .map { list ->
            val startOfMonth = getStartOfMonthTimestamp()
            list.filter { it.type == "INCOME" && it.dateTime >= startOfMonth }
                .sumOf { it.amount }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Category percentage for charts
    data class CategorySpent(
        val category: Category,
        val amount: Double,
        val percentage: Float
    )

    val categorySpending: StateFlow<List<CategorySpent>> = combine(
        transactions,
        categories
    ) { transList, catList ->
        val startOfMonth = getStartOfMonthTimestamp()
        val expenses = transList.filter { it.type == "EXPENSE" && it.dateTime >= startOfMonth }
        val totalSpent = expenses.sumOf { it.amount }

        if (totalSpent == 0.0) {
            emptyList()
        } else {
            val groupMap = expenses.groupBy { it.categoryId }
            catList.mapNotNull { category ->
                val spentInCat = groupMap[category.id]?.sumOf { it.amount } ?: 0.0
                if (spentInCat > 0.0) {
                    CategorySpent(
                        category = category,
                        amount = spentInCat,
                        percentage = (spentInCat / totalSpent).toFloat()
                    )
                } else {
                    null
                }
            }.sortedByDescending { it.amount }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Actions
    fun addAccount(name: String, initBalance: Double, colorHex: String, iconName: String) {
        viewModelScope.launch {
            repository.insertAccount(
                Account(name = name, balance = initBalance, colorHex = colorHex, iconName = iconName)
            )
        }
    }

    fun updateAccount(account: Account) {
        viewModelScope.launch {
            repository.updateAccount(account)
        }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            repository.deleteAccount(account)
        }
    }

    fun addCategory(name: String, type: String, iconName: String, colorHex: String) {
        viewModelScope.launch {
            repository.insertCategory(
                Category(name = name, type = type, iconName = iconName, colorHex = colorHex)
            )
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    fun addTransaction(
        accountId: Int,
        categoryId: Int,
        amount: Double,
        type: String,
        note: String,
        paymentMethod: String = "Cash",
        dateTime: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            repository.insertTransaction(
                Transaction(
                    accountId = accountId,
                    categoryId = categoryId,
                    amount = amount,
                    type = type,
                    dateTime = dateTime,
                    note = note,
                    paymentMethod = paymentMethod
                )
            )
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun updateTransaction(oldTrans: Transaction, newTrans: Transaction) {
        viewModelScope.launch {
            repository.updateTransaction(oldTrans, newTrans)
        }
    }

    // Loans CRUD Actions
    fun addLoan(
        personName: String,
        amount: Double,
        type: String,
        notes: String = "",
        date: Long = System.currentTimeMillis(),
        dueDate: Long = System.currentTimeMillis() + (86400000L * 30),
        paymentFrequency: String = "MONTHLY",
        paymentIntervalDays: Int = 30
    ) {
        viewModelScope.launch {
            repository.insertLoan(
                Loan(
                    personName = personName,
                    amount = amount,
                    type = type,
                    isSettled = false,
                    date = date,
                    dueDate = dueDate,
                    notes = notes,
                    paidAmount = 0.0,
                    paymentFrequency = paymentFrequency,
                    paymentIntervalDays = paymentIntervalDays
                )
            )
        }
    }

    fun updateLoan(loan: Loan) {
        viewModelScope.launch {
            repository.updateLoan(loan)
        }
    }

    fun deleteLoan(loan: Loan) {
        viewModelScope.launch {
            repository.deleteLoan(loan)
        }
    }

    fun settleLoan(loan: Loan) {
        viewModelScope.launch {
            repository.updateLoan(loan.copy(paidAmount = loan.amount, isSettled = true))
        }
    }

    fun payLoanInstallment(loan: Loan, paymentAmount: Double) {
        viewModelScope.launch {
            if (paymentAmount <= 0.0) return@launch
            
            val newPaidAmount = loan.paidAmount + paymentAmount
            val isNowSettled = newPaidAmount >= loan.amount
            repository.updateLoan(
                loan.copy(
                    paidAmount = minOf(loan.amount, newPaidAmount),
                    isSettled = isNowSettled
                )
            )
        }
    }

    // 100% Offline Backup Export to JSON format
    fun exportBackup(): String {
        return try {
            val root = org.json.JSONObject()

            val accsArray = org.json.JSONArray()
            accounts.value.forEach {
                val obj = org.json.JSONObject()
                obj.put("id", it.id)
                obj.put("name", it.name)
                obj.put("balance", it.balance)
                obj.put("colorHex", it.colorHex)
                obj.put("iconName", it.iconName)
                accsArray.put(obj)
            }
            root.put("accounts", accsArray)

            val catsArray = org.json.JSONArray()
            categories.value.forEach {
                val obj = org.json.JSONObject()
                obj.put("id", it.id)
                obj.put("name", it.name)
                obj.put("type", it.type)
                obj.put("colorHex", it.colorHex)
                obj.put("iconName", it.iconName)
                catsArray.put(obj)
            }
            root.put("categories", catsArray)

            val transArray = org.json.JSONArray()
            transactions.value.forEach {
                val obj = org.json.JSONObject()
                obj.put("id", it.id)
                obj.put("accountId", it.accountId)
                obj.put("categoryId", it.categoryId)
                obj.put("amount", it.amount)
                obj.put("type", it.type)
                obj.put("dateTime", it.dateTime)
                obj.put("note", it.note)
                obj.put("paymentMethod", it.paymentMethod)
                transArray.put(obj)
            }
            root.put("transactions", transArray)

            val loansArray = org.json.JSONArray()
            loans.value.forEach {
                val obj = org.json.JSONObject()
                obj.put("id", it.id)
                obj.put("personName", it.personName)
                obj.put("amount", it.amount)
                obj.put("type", it.type)
                obj.put("isSettled", it.isSettled)
                obj.put("date", it.date)
                obj.put("dueDate", it.dueDate)
                obj.put("notes", it.notes)
                obj.put("paidAmount", it.paidAmount)
                obj.put("paymentFrequency", it.paymentFrequency)
                obj.put("paymentIntervalDays", it.paymentIntervalDays)
                loansArray.put(obj)
            }
            root.put("loans", loansArray)

            root.toString(4)
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    // 100% Offline Secure JSON Restore
    fun importBackup(jsonString: String): Boolean {
        return try {
            val root = org.json.JSONObject(jsonString)
            viewModelScope.launch {
                // Restore accounts
                val accs = root.optJSONArray("accounts")
                if (accs != null) {
                    for (i in 0 until accs.length()) {
                        val obj = accs.getJSONObject(i)
                        repository.insertAccount(
                            Account(
                                id = obj.optInt("id", 0),
                                name = obj.getString("name"),
                                balance = obj.optDouble("balance", 0.0),
                                colorHex = obj.optString("colorHex", "#4CAF50"),
                                iconName = obj.optString("iconName", "payments")
                            )
                        )
                    }
                }

                // Restore categories
                val cats = root.optJSONArray("categories")
                if (cats != null) {
                    for (i in 0 until cats.length()) {
                        val obj = cats.getJSONObject(i)
                        repository.insertCategory(
                            Category(
                                id = obj.optInt("id", 0),
                                name = obj.getString("name"),
                                type = obj.getString("type"),
                                iconName = obj.optString("iconName", "shopping_cart"),
                                colorHex = obj.optString("colorHex", "#4CAF50")
                            )
                        )
                    }
                }

                // Restore transactions
                val trans = root.optJSONArray("transactions")
                if (trans != null) {
                    for (i in 0 until trans.length()) {
                        val obj = trans.getJSONObject(i)
                        repository.insertTransaction(
                            Transaction(
                                id = obj.optInt("id", 0),
                                accountId = obj.getInt("accountId"),
                                categoryId = obj.getInt("categoryId"),
                                amount = obj.getDouble("amount"),
                                type = obj.getString("type"),
                                dateTime = obj.getLong("dateTime"),
                                note = obj.getString("note"),
                                paymentMethod = obj.optString("paymentMethod", "Cash")
                            )
                        )
                    }
                }

                // Restore loans
                val lns = root.optJSONArray("loans")
                if (lns != null) {
                    for (i in 0 until lns.length()) {
                        val obj = lns.getJSONObject(i)
                        repository.insertLoan(
                            Loan(
                                id = obj.optInt("id", 0),
                                personName = obj.getString("personName"),
                                amount = obj.getDouble("amount"),
                                type = obj.getString("type"),
                                isSettled = obj.optBoolean("isSettled", false),
                                date = obj.optLong("date", System.currentTimeMillis()),
                                dueDate = obj.optLong("dueDate", System.currentTimeMillis()),
                                notes = obj.optString("notes", ""),
                                paidAmount = obj.optDouble("paidAmount", 0.0),
                                paymentFrequency = obj.optString("paymentFrequency", "MONTHLY"),
                                paymentIntervalDays = obj.optInt("paymentIntervalDays", 30)
                            )
                        )
                    }
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun getStartOfMonthTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
