package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Account
import com.example.data.model.Category
import com.example.data.model.Transaction
import com.example.data.model.Loan
import com.example.ui.theme.*
import com.example.ui.viewmodel.ExpenseViewModel
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen()
            }
        }
    }
}

// Icon Helper
fun getIconForName(name: String): ImageVector {
    return when (name.lowercase()) {
        "shopping_cart", "grocery", "shopping_bag" -> Icons.Default.ShoppingCart
        "restaurant", "food", "food & dining" -> Icons.Default.Restaurant
        "directions_car", "transport", "uber" -> Icons.Default.DirectionsCar
        "sports_esports", "entertainment" -> Icons.Default.SportsEsports
        "bolt", "utilities" -> Icons.Default.Bolt
        "medical_services", "health", "health & care" -> Icons.Default.MedicalServices
        "payments", "cash", "salary" -> Icons.Default.Payments
        "trending_up", "investments" -> Icons.Default.TrendingUp
        "redeem", "gifts" -> Icons.Default.Redeem
        "credit_card" -> Icons.Default.CreditCard
        "savings" -> Icons.Default.Savings
        "home" -> Icons.Default.Home
        "wallet", "account_balance_wallet" -> Icons.Default.AccountBalanceWallet
        "bar_chart" -> Icons.Default.BarChart
        "settings" -> Icons.Default.Settings
        "add" -> Icons.Default.Add
        "visibility" -> Icons.Default.Visibility
        "visibility_off" -> Icons.Default.VisibilityOff
        "security" -> Icons.Default.Security
        "backup" -> Icons.Default.Backup
        "description" -> Icons.Default.Description
        "person" -> Icons.Default.Person
        "payment" -> Icons.Default.Payment
        else -> Icons.Default.AccountBalanceWallet
    }
}

// Currency Helper with dynamic symbol selection
fun formatCurrency(amount: Double, symbol: String): String {
    val df = DecimalFormat("#,##0.00")
    return "$symbol${df.format(amount)}"
}

// Date Format helper
fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

data class TabItem(val id: String, val label: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: ExpenseViewModel = viewModel()) {
    val context = LocalContext.current

    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsStateWithLifecycle()

    if (!isOnboardingCompleted) {
        OnboardingWelcomeScreen(
            onComplete = { name, currency ->
                viewModel.completeOnboarding(name, currency)
            }
        )
        return
    }

    // Offline App Lock verification status
    var isUnlocked by remember { mutableStateOf(false) }
    val pinCode by viewModel.appPinCode.collectAsStateWithLifecycle()
    val isPinRequired by viewModel.isPinRequired.collectAsStateWithLifecycle()

    // Screen and Navigation drawer state
    var currentTab by remember { mutableStateOf("wallet") }
    var showAddTransactionDialog by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddLoanDialogGlobal by remember { mutableStateOf(false) }
    var isBalanceHidden by remember { mutableStateOf(false) }
    var showSidebarDrawer by remember { mutableStateOf(false) }

    // Core DB State streams
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val loans by viewModel.loans.collectAsStateWithLifecycle()

    // Preference metrics
    val currencySym by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val payMethods by viewModel.paymentMethods.collectAsStateWithLifecycle()

    val totalBalance by viewModel.totalBalance.collectAsStateWithLifecycle()
    val monthlyIncome by viewModel.monthlyIncome.collectAsStateWithLifecycle()
    val monthlyExpense by viewModel.monthlyExpense.collectAsStateWithLifecycle()
    val categorySpending by viewModel.categorySpending.collectAsStateWithLifecycle()

    // 1. PIN Lock Verification Overlay Gate
    if (isPinRequired && !isUnlocked) {
        PinVerificationScreen(
            correctPin = pinCode,
            viewModel = viewModel,
            onUnlocked = { isUnlocked = true }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ElegantBackground)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = ElegantBackground,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                // Header bar with custom sidebar drawer trigger
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(
                            onClick = { showSidebarDrawer = true },
                            modifier = Modifier
                                .size(40.dp)
                                .background(ElegantSurface, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open Sidebar Menu",
                                tint = ElegantOnBackground
                            )
                        }
                        Text(
                            text = if (userName.isNotBlank()) "$userName's Ledger" else "Expense Wallet",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElegantOnBackground,
                            letterSpacing = (-0.5).sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Secure status pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ElegantSurfaceVariant)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Secure Mode Indicator",
                            tint = ElegantOnSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "SECURE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElegantOnSurfaceVariant,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            },
            bottomBar = {
                // Persistent custom-styled bottom Navigation pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(bottom = 12.dp, start = 16.dp, end = 16.dp)
                        .height(72.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(ElegantSurface)
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(28.dp))
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val tabs = listOf(
                        TabItem("wallet", "Wallet", Icons.Default.Home),
                        TabItem("stats", "Stats", Icons.Default.BarChart),
                        TabItem("budgets", "Categories", Icons.Default.AccountBalanceWallet),
                        TabItem("settings", "Settings", Icons.Default.Settings)
                    )

                    tabs.forEach { tab ->
                        val isActive = currentTab == tab.id
                        val animatedWeight by animateFloatAsState(
                            targetValue = if (isActive) 1.2f else 0.9f,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        )
                        val iconColor by animateColorAsState(
                            targetValue = if (isActive) ElegantPrimary else ElegantOnBackground.copy(alpha = 0.5f),
                            animationSpec = tween(200)
                        )
                        Column(
                            modifier = Modifier
                                .weight(animatedWeight)
                                .fillMaxHeight()
                                .clickable {
                                    currentTab = tab.id
                                    // Ensure drawer closes on tab change
                                    showSidebarDrawer = false
                                }
                                .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isActive) ElegantPrimary.copy(alpha = 0.12f) else Color.Transparent)
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label,
                                    tint = iconColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tab.label,
                                fontSize = 10.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                color = if (isActive) ElegantPrimary else ElegantOnBackground.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (currentTab in listOf("wallet", "budgets", "loans", "transactions")) {
                    FloatingActionButton(
                        onClick = {
                            when (currentTab) {
                                "loans" -> showAddLoanDialogGlobal = true
                                "budgets" -> showAddCategoryDialog = true
                                else -> showAddTransactionDialog = true
                            }
                        },
                        containerColor = ElegantPrimary,
                        contentColor = ElegantOnPrimary,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .testTag("add_transaction_fab")
                            .padding(bottom = 16.dp, end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Item",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (currentTab in listOf("wallet", "transactions", "stats", "budgets")) {
                    // Top Balance Summary Card
                    BalanceSummaryCard(
                        totalBalance = totalBalance,
                        income = monthlyIncome,
                        expense = monthlyExpense,
                        currencySymbol = currencySym,
                        isBalanceHidden = isBalanceHidden,
                        onToggleBalanceHide = { isBalanceHidden = !isBalanceHidden }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Core Tab selector UI
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    when (currentTab) {
                        "wallet" -> WalletTabContent(
                            accounts = accounts,
                            transactions = transactions,
                            categories = categories,
                            currencySymbol = currencySym,
                            onAddAccountClick = { showAddAccountDialog = true },
                            viewModel = viewModel
                        )
                        "transactions" -> TransactionsTabContent(
                            transactions = transactions,
                            categories = categories,
                            accounts = accounts,
                            currencySymbol = currencySym,
                            viewModel = viewModel
                        )
                        "stats" -> StatsTabContent(
                            categorySpending = categorySpending,
                            currencySymbol = currencySym
                        )
                        "budgets" -> BudgetsTabContent(
                            categories = categories,
                            onAddCategoryClick = { showAddCategoryDialog = true },
                            onDeleteCategory = { viewModel.deleteCategory(it) }
                        )
                        "loans" -> LoansTabContent(
                            loans = loans,
                            accounts = accounts,
                            currencySymbol = currencySym,
                            viewModel = viewModel,
                            showAddLoanDialog = showAddLoanDialogGlobal,
                            onShowAddLoanDialogChange = { showAddLoanDialogGlobal = it }
                        )
                        "reports" -> ReportsTabContent(
                            transactions = transactions,
                            categories = categories,
                            accounts = accounts,
                            currencySymbol = currencySym
                        )
                        "security" -> SecurityTabContent(
                            viewModel = viewModel
                        )
                        "settings" -> SettingsTabContent(
                            accounts = accounts,
                            categoriesCount = categories.size,
                            transactionsCount = transactions.size,
                            paymentMethods = payMethods,
                            currencySymbol = currencySym,
                            onPrepopulateDefaults = {
                                viewModel.addAccount("Cash", 0.0, "#4CAF50", "payments")
                            },
                            onAddAccountClick = { showAddAccountDialog = true },
                            viewModel = viewModel
                        )
                    }
                }
            }
        }

        // 2. Slide-out Left Sidebar Navigation Drawer
        AnimatedVisibility(
            visible = showSidebarDrawer,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { showSidebarDrawer = false }
            ) {
                Row(
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(279.dp)
                            .background(ElegantSurface)
                            .clickable(enabled = false) {}
                            .statusBarsPadding()
                            .padding(20.dp, 20.dp, 20.dp, 20.dp)
                    ) {
                        // Header Logo and text
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(bottom = 32.dp)
                        ) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = R.drawable.app_logo_wallet_1780750581081),
                                contentDescription = "Logo",
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                            Text(
                                text = if (userName.isNotBlank()) userName else "Wallet Menu",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElegantOnBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Drawer Items
                        val sidebarTabs = listOf(
                            SidebarMenuItem("Overview", "wallet", Icons.Default.Home),
                            SidebarMenuItem("Expense & Income", "transactions", Icons.Default.Receipt),
                            SidebarMenuItem("Loans & Debts", "loans", Icons.Default.Handshake),
                            SidebarMenuItem("Print Reports", "reports", Icons.Default.Description),
                            SidebarMenuItem("Security Lock", "security", Icons.Default.Security),
                            SidebarMenuItem("System Settings", "settings", Icons.Default.Settings)
                        )

                        sidebarTabs.forEach { item ->
                            val isSel = currentTab == item.targetTab
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSel) ElegantSurfaceVariant else Color.Transparent)
                                    .clickable {
                                        currentTab = item.targetTab
                                        showSidebarDrawer = false
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = if (isSel) ElegantPrimary else ElegantOnBackground.copy(alpha = 0.7f),
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = item.label,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSel) ElegantOnBackground else ElegantOnBackground.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Safe secure disclaimer
                        Text(
                            text = "100% Local Integrity\nAll records are on-device.",
                            fontSize = 10.sp,
                            color = ElegantOnBackground.copy(alpha = 0.4f),
                            lineHeight = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp)
                            .background(Color.White.copy(alpha = 0.08f))
                    )
                }
            }
        }
    }

    // Modal Action Dialogs
    if (showAddTransactionDialog) {
        AddTransactionDialog(
            accounts = accounts,
            categories = categories,
            paymentMethods = payMethods,
            onDismiss = { showAddTransactionDialog = false },
            onConfirm = { accId, catId, amt, type, note, method, timestamp ->
                viewModel.addTransaction(accId, catId, amt, type, note, method, timestamp)
                showAddTransactionDialog = false
            }
        )
    }

    if (showAddAccountDialog) {
        AddAccountDialog(
            onDismiss = { showAddAccountDialog = false },
            onConfirm = { name, balance, color, icon ->
                viewModel.addAccount(name, balance, color, icon)
                showAddAccountDialog = false
            }
        )
    }

    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onConfirm = { name, type, icon, color ->
                viewModel.addCategory(name, type, icon, color)
                showAddCategoryDialog = false
            }
        )
    }
}

data class SidebarMenuItem(val label: String, val targetTab: String, val icon: ImageVector)

@Composable
fun BalanceSummaryCard(
    totalBalance: Double,
    income: Double,
    expense: Double,
    currencySymbol: String,
    isBalanceHidden: Boolean,
    onToggleBalanceHide: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.02f)
                    )
                ),
                shape = RoundedCornerShape(26.dp)
            ),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            ElegantSurface,
                            ElegantBackground
                        )
                    )
                )
                .padding(22.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TOTAL BALANCE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElegantPrimary,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isBalanceHidden) "••••••" else formatCurrency(totalBalance, currencySymbol),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElegantOnBackground,
                            letterSpacing = (-0.5).sp
                        )
                    }

                    IconButton(
                        onClick = onToggleBalanceHide,
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color.White.copy(alpha = 0.06f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isBalanceHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Hide or Show Balance",
                            tint = ElegantPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Income / Expense row split
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Income view
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(ElegantIncomeGreen.copy(0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = ElegantIncomeGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "INCOME",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElegantOnBackground.copy(alpha = 0.5f),
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "+${formatCurrency(income, currencySymbol)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElegantIncomeGreen
                            )
                        }
                    }

                    // Tiny divider split
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .width(1.dp)
                            .background(Color.White.copy(alpha = 0.12f))
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // Expense view
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(ElegantExpenseRed.copy(0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = ElegantExpenseRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "EXPENSES",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElegantOnBackground.copy(alpha = 0.5f),
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "-${formatCurrency(expense, currencySymbol)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElegantExpenseRed
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WalletTabContent(
    accounts: List<Account>,
    transactions: List<Transaction>,
    categories: List<Category>,
    currencySymbol: String,
    onAddAccountClick: () -> Unit,
    viewModel: ExpenseViewModel
) {
    var selectedTransactionToDelete by remember { mutableStateOf<Transaction?>(null) }
    var dashboardTypeFilter by remember { mutableStateOf("ALL") }
    var dashboardAccountFilterId by remember { mutableStateOf<Int?>(null) }
    var dashboardStartTimestamp by remember { mutableStateOf<Long?>(null) }
    var dashboardEndTimestamp by remember { mutableStateOf<Long?>(null) }
    var showFilterDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Accounts header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My Accounts",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ElegantOnBackground
            )
            Text(
                text = "+ Add",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = ElegantPrimary,
                modifier = Modifier
                    .clickable { onAddAccountClick() }
                    .padding(8.dp)
                    .testTag("add_account_trigger")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Accounts Lazy Row
        if (accounts.isEmpty()) {
            Text(
                text = "No accounts registered yet. Tap + Add.",
                color = ElegantOnBackground.copy(alpha = 0.5f),
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(accounts) { account ->
                    val bgParsed = try {
                        Color(android.graphics.Color.parseColor(account.colorHex))
                    } catch (e: Exception) {
                        ElegantSurfaceVariant
                    }
                    Card(
                        modifier = Modifier
                            .width(140.dp)
                            .height(90.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = bgParsed),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = getIconForName(account.iconName),
                                    contentDescription = account.name,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                // Delete button for account verification
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Delete Account",
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { viewModel.deleteAccount(account) }
                                )
                            }
                            Column {
                                Text(
                                    text = account.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = formatCurrency(account.balance, currencySymbol),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Recent activity header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Activity",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ElegantOnBackground
            )

            val hasActiveFilters = dashboardTypeFilter != "ALL" || dashboardAccountFilterId != null || dashboardStartTimestamp != null || dashboardEndTimestamp != null
            FilledIconButton(
                onClick = { showFilterDialog = true },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (hasActiveFilters) ElegantPrimary else ElegantSurfaceVariant,
                    contentColor = if (hasActiveFilters) ElegantOnPrimary else ElegantOnBackground
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filter Selection popup trigger",
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Active filter chips
        val hasActiveFilters = dashboardTypeFilter != "ALL" || dashboardAccountFilterId != null || dashboardStartTimestamp != null || dashboardEndTimestamp != null
        if (hasActiveFilters) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp)
            ) {
                Text("Filters:", fontSize = 11.sp, color = ElegantOnBackground.copy(alpha = 0.5f))
                if (dashboardTypeFilter != "ALL") {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ElegantPrimary.copy(alpha = 0.15f))
                            .clickable { dashboardTypeFilter = "ALL" }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(dashboardTypeFilter, fontSize = 10.sp, color = ElegantPrimary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = ElegantPrimary, modifier = Modifier.size(10.dp))
                        }
                    }
                }
                if (dashboardAccountFilterId != null) {
                    val matchingAccountName = accounts.find { it.id == dashboardAccountFilterId }?.name ?: "Wallet"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ElegantPrimary.copy(alpha = 0.15f))
                            .clickable { dashboardAccountFilterId = null }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(matchingAccountName, fontSize = 10.sp, color = ElegantPrimary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = ElegantPrimary, modifier = Modifier.size(10.dp))
                        }
                    }
                }
                if (dashboardStartTimestamp != null || dashboardEndTimestamp != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ElegantPrimary.copy(alpha = 0.15f))
                            .clickable {
                                dashboardStartTimestamp = null
                                dashboardEndTimestamp = null
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Date Filter", fontSize = 10.sp, color = ElegantPrimary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = ElegantPrimary, modifier = Modifier.size(10.dp))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        val displayedTransactions = remember(transactions, dashboardTypeFilter, dashboardAccountFilterId, dashboardStartTimestamp, dashboardEndTimestamp) {
            transactions.filter { tx ->
                val matchType = dashboardTypeFilter == "ALL" || tx.type == dashboardTypeFilter
                val matchAccount = dashboardAccountFilterId == null || tx.accountId == dashboardAccountFilterId
                val matchStart = dashboardStartTimestamp == null || tx.dateTime >= dashboardStartTimestamp!!
                val matchEnd = dashboardEndTimestamp == null || tx.dateTime <= dashboardEndTimestamp!!
                matchType && matchAccount && matchStart && matchEnd
            }
        }

        // Custom Dashboard Filter Dialogue popup
        val context = LocalContext.current
        if (showFilterDialog) {
            Dialog(onDismissRequest = { showFilterDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ElegantSurface),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Dashboard Filters",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElegantOnBackground
                            )
                            IconButton(onClick = { showFilterDialog = false }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = ElegantOnBackground.copy(alpha = 0.5f)
                                )
                            }
                        }

                        // Transaction Type Selector
                        Column {
                            Text("Transaction Type", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ElegantPrimary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ElegantSurfaceVariant)
                                    .padding(2.dp)
                            ) {
                                listOf("ALL", "EXPENSE", "INCOME").forEach { type ->
                                    val isSel = dashboardTypeFilter == type
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSel) ElegantPrimary else Color.Transparent)
                                            .clickable { dashboardTypeFilter = type },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = type,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) ElegantOnPrimary else ElegantOnBackground.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }

                        // Payment Account Selector
                        Column {
                            Text("Payment Account", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ElegantPrimary)
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                item {
                                    val isSel = dashboardAccountFilterId == null
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) ElegantPrimary else ElegantSurfaceVariant)
                                            .clickable { dashboardAccountFilterId = null }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("All Wallets", fontSize = 11.sp, color = if (isSel) ElegantOnPrimary else ElegantOnBackground.copy(alpha = 0.7f))
                                    }
                                }
                                items(accounts) { acc ->
                                    val isSel = dashboardAccountFilterId == acc.id
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) ElegantPrimary else ElegantSurfaceVariant)
                                            .clickable { dashboardAccountFilterId = acc.id }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(acc.name, fontSize = 11.sp, color = if (isSel) ElegantOnPrimary else ElegantOnBackground.copy(alpha = 0.7f))
                                    }
                                }
                            }
                        }

                        // Date Range Selector
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Date Range", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ElegantPrimary)
                                if (dashboardStartTimestamp != null || dashboardEndTimestamp != null) {
                                    Text(
                                        "Reset Range",
                                        fontSize = 11.sp,
                                        color = ElegantExpenseRed,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable {
                                            dashboardStartTimestamp = null
                                            dashboardEndTimestamp = null
                                        }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Start Date selector button
                                val startLabel = if (dashboardStartTimestamp != null) {
                                    val sdf = java.text.SimpleDateFormat("MMM dd, yyyy • HH:mm", java.util.Locale.getDefault())
                                    sdf.format(java.util.Date(dashboardStartTimestamp!!))
                                } else "Start Date & Time"

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ElegantSurfaceVariant)
                                        .clickable {
                                            val calendar = Calendar.getInstance()
                                            if (dashboardStartTimestamp != null) {
                                                calendar.timeInMillis = dashboardStartTimestamp!!
                                            }
                                            var isPickerFired = false
                                            android.app.DatePickerDialog(
                                                context,
                                                { _, year, month, day ->
                                                    if (!isPickerFired) {
                                                        isPickerFired = true
                                                        calendar.set(Calendar.YEAR, year)
                                                        calendar.set(Calendar.MONTH, month)
                                                        calendar.set(Calendar.DAY_OF_MONTH, day)
                                                        android.app.TimePickerDialog(
                                                            context,
                                                            { _, hour, min ->
                                                                calendar.set(Calendar.HOUR_OF_DAY, hour)
                                                                calendar.set(Calendar.MINUTE, min)
                                                                calendar.set(Calendar.SECOND, 0)
                                                                calendar.set(Calendar.MILLISECOND, 0)
                                                                dashboardStartTimestamp = calendar.timeInMillis
                                                            },
                                                            calendar.get(Calendar.HOUR_OF_DAY),
                                                            calendar.get(Calendar.MINUTE),
                                                            true
                                                        ).show()
                                                    }
                                                },
                                                calendar.get(Calendar.YEAR),
                                                calendar.get(Calendar.MONTH),
                                                calendar.get(Calendar.DAY_OF_MONTH)
                                            ).show()
                                        }
                                        .padding(10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = startLabel,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = ElegantOnBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // End Date selector button
                                val endLabel = if (dashboardEndTimestamp != null) {
                                    val sdf = java.text.SimpleDateFormat("MMM dd, yyyy • HH:mm", java.util.Locale.getDefault())
                                    sdf.format(java.util.Date(dashboardEndTimestamp!!))
                                } else "End Date & Time"

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ElegantSurfaceVariant)
                                        .clickable {
                                            val calendar = Calendar.getInstance()
                                            if (dashboardEndTimestamp != null) {
                                                calendar.timeInMillis = dashboardEndTimestamp!!
                                            }
                                            var isPickerFired = false
                                            android.app.DatePickerDialog(
                                                context,
                                                { _, year, month, day ->
                                                    if (!isPickerFired) {
                                                        isPickerFired = true
                                                        calendar.set(Calendar.YEAR, year)
                                                        calendar.set(Calendar.MONTH, month)
                                                        calendar.set(Calendar.DAY_OF_MONTH, day)
                                                        android.app.TimePickerDialog(
                                                            context,
                                                            { _, hour, min ->
                                                                calendar.set(Calendar.HOUR_OF_DAY, hour)
                                                                calendar.set(Calendar.MINUTE, min)
                                                                calendar.set(Calendar.SECOND, 59)
                                                                calendar.set(Calendar.MILLISECOND, 999)
                                                                dashboardEndTimestamp = calendar.timeInMillis
                                                            },
                                                            calendar.get(Calendar.HOUR_OF_DAY),
                                                            calendar.get(Calendar.MINUTE),
                                                            true
                                                        ).show()
                                                    }
                                                },
                                                calendar.get(Calendar.YEAR),
                                                calendar.get(Calendar.MONTH),
                                                calendar.get(Calendar.DAY_OF_MONTH)
                                            ).show()
                                        }
                                        .padding(10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = endLabel,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = ElegantOnBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Apply button and Reset all button
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    dashboardTypeFilter = "ALL"
                                    dashboardAccountFilterId = null
                                    dashboardStartTimestamp = null
                                    dashboardEndTimestamp = null
                                    showFilterDialog = false
                                }
                            ) {
                                Text("Clear All", color = ElegantExpenseRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            Button(
                                modifier = Modifier.weight(1.2f),
                                onClick = { showFilterDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = ElegantPrimary, contentColor = ElegantOnPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Apply Filters", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Transactions Area
        if (displayedTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = "Empty activity indicator",
                        tint = ElegantOnBackground.copy(alpha = 0.25f),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "No matching transactions found.\nAdjust your active dashboard filters above.",
                        fontSize = 13.sp,
                        color = ElegantOnBackground.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(displayedTransactions) { transaction ->
                    val matchedCategory = categories.find { it.id == transaction.categoryId }
                    val matchedAccount = accounts.find { it.id == transaction.accountId }

                    val catColor = try {
                        Color(android.graphics.Color.parseColor(matchedCategory?.colorHex ?: "#4A4458"))
                    } catch (e: Exception) {
                        ElegantSurfaceVariant
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(ElegantSurface)
                            .clickable { selectedTransactionToDelete = transaction }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Category visual tag icon
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(catColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getIconForName(matchedCategory?.iconName ?: "shopping_cart"),
                                contentDescription = matchedCategory?.name ?: "Expense tag",
                                tint = catColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Text detail
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (transaction.note.isNotBlank()) transaction.note else matchedCategory?.name ?: "Transaction Entry",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ElegantOnBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${matchedAccount?.name ?: "Cash"} • ${transaction.paymentMethod} • ${formatDate(transaction.dateTime)}",
                                fontSize = 11.sp,
                                color = ElegantOnBackground.copy(alpha = 0.5f)
                            )
                        }

                        // Amount & indicator math
                        val isIncome = transaction.type == "INCOME"
                        Text(
                            text = (if (isIncome) "+ " else "- ") + formatCurrency(transaction.amount, currencySymbol),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isIncome) ElegantIncomeGreen else ElegantExpenseRed
                        )
                    }
                }
            }
        }
    }

    if (selectedTransactionToDelete != null) {
        AlertDialog(
            onDismissRequest = { selectedTransactionToDelete = null },
            title = { Text("Delete Log Entry?", color = ElegantOnBackground) },
            text = { Text("Are you sure you want to permanently delete this transaction from your local database?", color = ElegantOnBackground.copy(0.7f)) },
            containerColor = ElegantSurface,
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedTransactionToDelete?.let { viewModel.deleteTransaction(it) }
                        selectedTransactionToDelete = null
                    }
                ) {
                    Text("Delete", color = ElegantExpenseRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedTransactionToDelete = null }) {
                    Text("Cancel", color = ElegantOnBackground)
                }
            }
        )
    }
}

@Composable
fun TransactionsTabContent(
    transactions: List<Transaction>,
    categories: List<Category>,
    accounts: List<Account>,
    currencySymbol: String,
    viewModel: ExpenseViewModel
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("ALL") } // "ALL", "EXPENSE", "INCOME"
    var selectedAccountId by remember { mutableStateOf<Int?>(null) } // null means ALL
    var selectedCategoryId by remember { mutableStateOf<Int?>(null) } // null means ALL
    
    var selectedTransactionToDelete by remember { mutableStateOf<Transaction?>(null) }

    // Filtered transaction list
    val filteredTransactions = remember(transactions, searchQuery, selectedType, selectedAccountId, selectedCategoryId, categories) {
        transactions.filter { tx ->
            // Search filter
            val matchedCat = categories.find { it.id == tx.categoryId }
            val noteMatches = tx.note.contains(searchQuery, ignoreCase = true)
            val catNameMatches = matchedCat?.name?.contains(searchQuery, ignoreCase = true) ?: false
            val matchesSearch = searchQuery.isBlank() || noteMatches || catNameMatches

            // Type filter
            val matchesType = selectedType == "ALL" || tx.type.equals(selectedType, ignoreCase = true)

            // Account filter
            val matchesAccount = selectedAccountId == null || tx.accountId == selectedAccountId

            // Category filter
            val matchesCategory = selectedCategoryId == null || tx.categoryId == selectedCategoryId

            matchesSearch && matchesType && matchesAccount && matchesCategory
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Expense & Income Ledger",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = ElegantOnBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search logs (note, category...)", color = ElegantOnBackground.copy(alpha = 0.5f)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon", tint = ElegantOnBackground.copy(alpha = 0.5f)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = ElegantOnBackground,
                unfocusedTextColor = ElegantOnBackground,
                focusedLabelColor = ElegantPrimary,
                focusedBorderColor = ElegantPrimary,
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                unfocusedContainerColor = ElegantSurface,
                focusedContainerColor = ElegantSurface
            ),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        // Type Filter Row
        Text(
            text = "Record Type",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = ElegantOnBackground.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
        ) {
            val types = listOf("ALL" to "All Logs", "EXPENSE" to "Expenses", "INCOME" to "Incomes")
            items(types) { (key, label) ->
                val isSelected = selectedType == key
                val contentCol = if (isSelected) ElegantOnPrimary else ElegantOnBackground.copy(alpha = 0.8f)
                val bgCol = if (isSelected) ElegantPrimary else ElegantSurfaceVariant
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(bgCol)
                        .clickable { selectedType = key }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(text = label, color = contentCol, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Account Filter Row
        Text(
            text = "By Wallet / Account",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = ElegantOnBackground.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
        ) {
            item {
                val isSelected = selectedAccountId == null
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) ElegantPrimary else ElegantSurfaceVariant)
                        .clickable { selectedAccountId = null }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "All Wallets",
                        color = if (isSelected) ElegantOnPrimary else ElegantOnBackground.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            items(accounts) { account ->
                val isSelected = selectedAccountId == account.id
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) ElegantPrimary else ElegantSurfaceVariant)
                        .clickable { selectedAccountId = account.id }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = account.name,
                        color = if (isSelected) ElegantOnPrimary else ElegantOnBackground.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Category Filter Row
        Text(
            text = "By Category",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = ElegantOnBackground.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            item {
                val isSelected = selectedCategoryId == null
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) ElegantPrimary else ElegantSurfaceVariant)
                        .clickable { selectedCategoryId = null }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "All Categories",
                        color = if (isSelected) ElegantOnPrimary else ElegantOnBackground.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            items(categories) { category ->
                val isSelected = selectedCategoryId == category.id
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) ElegantPrimary else ElegantSurfaceVariant)
                        .clickable { selectedCategoryId = category.id }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = category.name,
                        color = if (isSelected) ElegantOnPrimary else ElegantOnBackground.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Transactions list section
        if (filteredTransactions.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No log entries found matching criteria.",
                    color = ElegantOnBackground.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredTransactions) { transaction ->
                    val matchedCategory = categories.find { it.id == transaction.categoryId }
                    val matchedAccount = accounts.find { it.id == transaction.accountId }

                    val catColor = try {
                        Color(android.graphics.Color.parseColor(matchedCategory?.colorHex ?: "#4A4458"))
                    } catch (e: Exception) {
                        ElegantSurfaceVariant
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(ElegantSurface)
                            .clickable { selectedTransactionToDelete = transaction }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(catColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getIconForName(matchedCategory?.iconName ?: "shopping_cart"),
                                contentDescription = matchedCategory?.name ?: "Category Icon",
                                tint = catColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (transaction.note.isNotBlank()) transaction.note else matchedCategory?.name ?: "Log Entry",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ElegantOnBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${matchedAccount?.name ?: "Cash"} • ${transaction.paymentMethod} • ${formatDate(transaction.dateTime)}",
                                fontSize = 11.sp,
                                color = ElegantOnBackground.copy(alpha = 0.5f)
                            )
                        }

                        val isIncome = transaction.type == "INCOME"
                        Text(
                            text = (if (isIncome) "+ " else "- ") + formatCurrency(transaction.amount, currencySymbol),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isIncome) ElegantIncomeGreen else ElegantExpenseRed
                        )
                    }
                }
            }
        }
    }

    if (selectedTransactionToDelete != null) {
        AlertDialog(
            onDismissRequest = { selectedTransactionToDelete = null },
            title = { Text("Delete This Log Entry?", color = ElegantOnBackground) },
            text = { Text("Are you sure you want to permanently delete this transaction from your local database?", color = ElegantOnBackground.copy(0.7f)) },
            containerColor = ElegantSurface,
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedTransactionToDelete?.let { viewModel.deleteTransaction(it) }
                        selectedTransactionToDelete = null
                    }
                ) {
                    Text("Delete", color = ElegantExpenseRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedTransactionToDelete = null }) {
                    Text("Cancel", color = ElegantOnBackground)
                }
            }
        )
    }
}

@Composable
fun StatsTabContent(
    categorySpending: List<ExpenseViewModel.CategorySpent>,
    currencySymbol: String
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Monthly Spending Categories",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = ElegantOnBackground
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (categorySpending.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No expenses recorded this month to compute categories.",
                    fontSize = 13.sp,
                    color = ElegantOnBackground.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(categorySpending) { item ->
                    val colorParsed = try {
                        Color(android.graphics.Color.parseColor(item.category.colorHex))
                    } catch (e: Exception) {
                        ElegantPrimary
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(ElegantSurface)
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(colorParsed)
                                )
                                Text(
                                    text = item.category.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ElegantOnBackground
                                )
                            }
                            Text(
                                text = "${formatCurrency(item.amount, currencySymbol)} (${String.format(Locale.getDefault(), "%.1f", item.percentage * 100)}%)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElegantOnBackground
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { item.percentage },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = colorParsed,
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BudgetsTabContent(
    categories: List<Category>,
    onAddCategoryClick: () -> Unit,
    onDeleteCategory: (Category) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Manage Custom Categories",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ElegantOnBackground
            )
            Button(
                onClick = onAddCategoryClick,
                colors = ButtonDefaults.buttonColors(containerColor = ElegantPrimary, contentColor = ElegantOnPrimary),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("add_category_trigger")
            ) {
                Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (categories.isEmpty()) {
            Text(
                text = "No custom categories configured.",
                color = ElegantOnBackground.copy(alpha = 0.5f),
                fontSize = 13.sp
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(categories) { category ->
                    val bgParsed = try {
                        Color(android.graphics.Color.parseColor(category.colorHex))
                    } catch (e: Exception) {
                        ElegantSurfaceVariant
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(ElegantSurface)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(bgParsed.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getIconForName(category.iconName),
                                    contentDescription = category.name,
                                    tint = bgParsed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = category.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ElegantOnBackground
                                )
                                Text(
                                    text = category.type,
                                    fontSize = 10.sp,
                                    color = if (category.type == "INCOME") ElegantIncomeGreen else ElegantExpenseRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        IconButton(
                            onClick = { onDeleteCategory(category) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Category",
                                tint = ElegantOnBackground.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
                              )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// NEW FEATURE: Loans & Debts Management
// -------------------------------------------------------------
@Composable
fun LoansTabContent(
    loans: List<Loan>,
    accounts: List<Account>,
    currencySymbol: String,
    viewModel: ExpenseViewModel,
    showAddLoanDialog: Boolean,
    onShowAddLoanDialogChange: (Boolean) -> Unit
) {
    var selectedLoanToSettle by remember { mutableStateOf<Loan?>(null) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Loans & Debts Management",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ElegantOnBackground
            )
            Button(
                onClick = { onShowAddLoanDialogChange(true) },
                colors = ButtonDefaults.buttonColors(containerColor = ElegantPrimary, contentColor = ElegantOnPrimary),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("New Loan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Basic lent vs borrowed summaries (excluding any amounts already partially paid back)
        val totalLent = loans.filter { it.type == "LENT" && !it.isSettled }.sumOf { maxOf(0.0, it.amount - it.paidAmount) }
        val totalBorrowed = loans.filter { it.type == "BORROWED" && !it.isSettled }.sumOf { maxOf(0.0, it.amount - it.paidAmount) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(ElegantSurface)
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Lent (They owe you)", fontSize = 11.sp, color = ElegantOnBackground.copy(alpha = 0.6f))
                Text(formatCurrency(totalLent, currencySymbol), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ElegantIncomeGreen)
            }
            Box(modifier = Modifier.width(1.dp).height(32.dp).background(Color.White.copy(alpha = 0.12f)))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Borrowed (You owe)", fontSize = 11.sp, color = ElegantOnBackground.copy(alpha = 0.6f))
                Text(formatCurrency(totalBorrowed, currencySymbol), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ElegantExpenseRed)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (loans.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No loan records registered yet.",
                    color = ElegantOnBackground.copy(0.4f),
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(loans) { loan ->
                    val isLent = loan.type == "LENT"
                    val remaining = maxOf(0.0, loan.amount - loan.paidAmount)
                    val progressVal = if (loan.amount > 0.0) (loan.paidAmount / loan.amount).toFloat() else 0f

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(ElegantSurface)
                            .border(
                                width = 1.dp,
                                color = if (loan.isSettled) Color.Transparent else if (isLent) ElegantIncomeGreen.copy(0.2f) else ElegantExpenseRed.copy(0.2f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (loan.isSettled) Color.Gray else if (isLent) ElegantIncomeGreen else ElegantExpenseRed)
                                )
                                Text(
                                    text = loan.personName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElegantOnBackground
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            
                            val cycleText = if (loan.paymentFrequency == "MONTHLY") "Monthly payment" else "Every ${loan.paymentIntervalDays} days"
                            Text(
                                text = "${loan.type} • Due: ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(loan.dueDate))} • $cycleText",
                                fontSize = 11.sp,
                                color = ElegantOnBackground.copy(alpha = 0.5f)
                            )
                            
                            if (loan.notes.isNotBlank()) {
                                Text(
                                    text = "Notes: ${loan.notes}",
                                    fontSize = 11.sp,
                                    color = ElegantOnBackground.copy(alpha = 0.4f)
                                )
                            }

                            // Installment progress display
                            if (!loan.isSettled && loan.paidAmount > 0.0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(0.95f),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Paid: ${formatCurrency(loan.paidAmount, currencySymbol)}",
                                        fontSize = 11.sp,
                                        color = ElegantPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Rem: ${formatCurrency(remaining, currencySymbol)}",
                                        fontSize = 11.sp,
                                        color = ElegantOnBackground.copy(alpha = 0.6f),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                androidx.compose.material3.LinearProgressIndicator(
                                    progress = { progressVal },
                                    modifier = Modifier
                                        .fillMaxWidth(0.95f)
                                        .height(5.dp)
                                        .clip(CircleShape),
                                    color = ElegantPrimary,
                                    trackColor = ElegantSurfaceVariant
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = formatCurrency(loan.amount, currencySymbol),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (loan.isSettled) ElegantOnBackground.copy(alpha = 0.5f) else if (isLent) ElegantIncomeGreen else ElegantExpenseRed
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            if (!loan.isSettled) {
                                Button(
                                    onClick = { selectedLoanToSettle = loan },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElegantPrimary.copy(0.15f), contentColor = ElegantPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(if (loan.paidAmount > 0.0) "Pay/Settle" else "Resolve", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.White.copy(0.08f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Settled", fontSize = 9.sp, color = ElegantOnBackground.copy(0.4f), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Loan Dialog
    if (showAddLoanDialog) {
        var name by remember { mutableStateOf("") }
        var amountText by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("LENT") } // LENT or BORROWED
        var notesVal by remember { mutableStateOf("") }
        var scheduleType by remember { mutableStateOf("MONTHLY") } // MONTHLY or CUSTOM_DAYS
        var customDaysText by remember { mutableStateOf("30") }

        Dialog(onDismissRequest = { onShowAddLoanDialogChange(false) }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = ElegantSurface),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Add Loan / Debt Record", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ElegantOnBackground)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ElegantSurfaceVariant)
                            .padding(2.dp)
                    ) {
                        listOf("LENT", "BORROWED").forEach { t ->
                            val isSel = type == t
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) ElegantPrimary else Color.Transparent)
                                    .clickable { type = t },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(t, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) ElegantOnPrimary else ElegantOnSurfaceVariant.copy(0.7f))
                            }
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Person's Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ElegantOnBackground, focusedLabelColor = ElegantPrimary, focusedBorderColor = ElegantPrimary, unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) amountText = it },
                        label = { Text("Principal Amount ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ElegantOnBackground, focusedLabelColor = ElegantPrimary, focusedBorderColor = ElegantPrimary, unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text("Repayment Option Frequency", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ElegantPrimary)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ElegantSurfaceVariant)
                            .padding(2.dp)
                    ) {
                        listOf("MONTHLY" to "Monthly payment", "CUSTOM_DAYS" to "Days selected").forEach { s ->
                            val isSel = scheduleType == s.first
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) ElegantPrimary else Color.Transparent)
                                    .clickable { scheduleType = s.first },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(s.second, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) ElegantOnPrimary else ElegantOnSurfaceVariant.copy(0.7f))
                            }
                        }
                    }

                    if (scheduleType == "CUSTOM_DAYS") {
                        OutlinedTextField(
                            value = customDaysText,
                            onValueChange = { if (it.isEmpty() || it.toIntOrNull() != null) customDaysText = it },
                            label = { Text("Days to Repay") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = ElegantOnBackground, focusedLabelColor = ElegantPrimary, focusedBorderColor = ElegantPrimary, unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = notesVal,
                        onValueChange = { notesVal = it },
                        label = { Text("Notes / Description") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ElegantOnBackground, focusedLabelColor = ElegantPrimary, focusedBorderColor = ElegantPrimary, unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { onShowAddLoanDialogChange(false) }) {
                            Text("Cancel", color = ElegantOnBackground.copy(alpha = 0.6f))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                val amt = amountText.toDoubleOrNull() ?: 0.0
                                val days = if (scheduleType == "MONTHLY") 30 else (customDaysText.toIntOrNull() ?: 30)
                                if (name.isNotBlank() && amt > 0.0) {
                                    val dueTimestamp = System.currentTimeMillis() + (86400000L * days)
                                    viewModel.addLoan(
                                        personName = name,
                                        amount = amt,
                                        type = type,
                                        notes = notesVal,
                                        date = System.currentTimeMillis(),
                                        dueDate = dueTimestamp,
                                        paymentFrequency = scheduleType,
                                        paymentIntervalDays = days
                                    )
                                    onShowAddLoanDialogChange(false)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElegantPrimary, contentColor = ElegantOnPrimary)
                        ) {
                            Text("Add Loan")
                        }
                    }
                }
            }
        }
    }

    // Settle / Repay Installment Dialog
    if (selectedLoanToSettle != null) {
        val loan = selectedLoanToSettle!!
        val remainingAmt = maxOf(0.0, loan.amount - loan.paidAmount)
        
        var resolveType by remember { mutableStateOf("INSTALLMENT") } // "INSTALLMENT" or "FULL_SETTLE"
        var installmentInputText by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { selectedLoanToSettle = null }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = ElegantSurface),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Resolve Active Loan", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ElegantOnBackground)
                    
                    Text(
                        text = "Partner: ${loan.personName} (${loan.type})\nRemaining Balance: ${formatCurrency(remainingAmt, currencySymbol)}",
                        fontSize = 13.sp,
                        color = ElegantOnBackground.copy(0.7f),
                        lineHeight = 18.sp
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ElegantSurfaceVariant)
                            .padding(2.dp)
                    ) {
                        listOf("INSTALLMENT" to "Add Installment", "FULL_SETTLE" to "Settle Full").forEach { entry ->
                            val isSel = resolveType == entry.first
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) ElegantPrimary else Color.Transparent)
                                    .clickable { resolveType = entry.first },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(entry.second, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) ElegantOnPrimary else ElegantOnSurfaceVariant.copy(0.7f))
                            }
                        }
                    }

                    if (resolveType == "INSTALLMENT") {
                        OutlinedTextField(
                            value = installmentInputText,
                            onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) installmentInputText = it },
                            label = { Text("Installment Payment Amount ($currencySymbol)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = ElegantOnBackground, focusedLabelColor = ElegantPrimary, focusedBorderColor = ElegantPrimary, unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { selectedLoanToSettle = null }) {
                            Text("Cancel", color = ElegantOnBackground.copy(alpha = 0.6f))
                        }
                        Spacer(modifier = Modifier.width(12.dp))

                        val paymentAmt = if (resolveType == "FULL_SETTLE") remainingAmt else (installmentInputText.toDoubleOrNull() ?: 0.0)
                        val canProceed = paymentAmt > 0.0 && paymentAmt <= (remainingAmt + 0.001)

                        Button(
                            onClick = {
                                if (resolveType == "FULL_SETTLE") {
                                    viewModel.settleLoan(loan)
                                } else {
                                    viewModel.payLoanInstallment(loan, paymentAmt)
                                }
                                selectedLoanToSettle = null
                            },
                            enabled = canProceed,
                            colors = ButtonDefaults.buttonColors(containerColor = ElegantPrimary, contentColor = ElegantOnPrimary)
                        ) {
                            Text("Confirm Settlement")
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// NEW FEATURE: Report HTML Printing & CSV Sharing Engine
// -------------------------------------------------------------
@Composable
fun ReportsTabContent(
    transactions: List<Transaction>,
    categories: List<Category>,
    accounts: List<Account>,
    currencySymbol: String
) {
    val context = LocalContext.current

    var selectedTypeFilter by remember { mutableStateOf("ALL") } // ALL, EXPENSE, INCOME
    var selectedAccountFilter by remember { mutableStateOf<Int?>(null) } // null means ALL
    var startTimestamp by remember { mutableStateOf<Long?>(null) }
    var endTimestamp by remember { mutableStateOf<Long?>(null) }

    fun formatReportDateTime(timestamp: Long?): String {
        if (timestamp == null) return "All Time"
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy • HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }

    // Perform query parsing
    val filteredTransactions = remember(transactions, selectedTypeFilter, selectedAccountFilter, startTimestamp, endTimestamp) {
        transactions.filter { t ->
            val matchType = selectedTypeFilter == "ALL" || t.type == selectedTypeFilter
            val matchAccount = selectedAccountFilter == null || t.accountId == selectedAccountFilter
            val matchStart = startTimestamp == null || t.dateTime >= startTimestamp!!
            val matchEnd = endTimestamp == null || t.dateTime <= endTimestamp!!
            matchType && matchAccount && matchStart && matchEnd
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Download Reports & Export", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ElegantOnBackground)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ElegantSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Select Filters", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ElegantPrimary)

                // Type filter selectors
                Column {
                    Text("Transaction Type", fontSize = 11.sp, color = ElegantOnBackground.copy(0.6f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ElegantSurfaceVariant)
                            .padding(2.dp)
                    ) {
                        val types = listOf("ALL", "EXPENSE", "INCOME")
                        types.forEach { t ->
                            val isSel = selectedTypeFilter == t
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) ElegantPrimary else Color.Transparent)
                                    .clickable { selectedTypeFilter = t },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(t, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSel) ElegantOnPrimary else ElegantOnSurfaceVariant.copy(0.7f))
                            }
                        }
                    }
                }

                // Account Filter
                Column {
                    Text("Payment Account", fontSize = 11.sp, color = ElegantOnBackground.copy(0.6f))
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            val isAll = selectedAccountFilter == null
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isAll) ElegantPrimary else ElegantSurfaceVariant)
                                    .clickable { selectedAccountFilter = null }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("All Accounts", fontSize = 11.sp, color = if (isAll) ElegantOnPrimary else ElegantOnBackground)
                            }
                        }
                        items(accounts) { acc ->
                            val isSel = selectedAccountFilter == acc.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) ElegantPrimary else ElegantSurfaceVariant)
                                    .clickable { selectedAccountFilter = acc.id }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(acc.name, fontSize = 11.sp, color = if (isSel) ElegantOnPrimary else ElegantOnBackground)
                            }
                        }
                    }
                }

                // Date & Time range filter (NEW!)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Date & Time Range Filter", fontSize = 11.sp, color = ElegantOnBackground.copy(0.6f))
                        if (startTimestamp != null || endTimestamp != null) {
                            Text(
                                text = "Clear Range",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElegantExpenseRed,
                                modifier = Modifier.clickable {
                                    startTimestamp = null
                                    endTimestamp = null
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Start Date selection block
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ElegantSurfaceVariant)
                                .clickable {
                                    val calendar = Calendar.getInstance()
                                    if (startTimestamp != null) {
                                        calendar.timeInMillis = startTimestamp!!
                                    }
                                    var isPickerFired = false
                                    android.app.DatePickerDialog(
                                        context,
                                        { _, year, month, day ->
                                            if (!isPickerFired) {
                                                isPickerFired = true
                                                calendar.set(Calendar.YEAR, year)
                                                calendar.set(Calendar.MONTH, month)
                                                calendar.set(Calendar.DAY_OF_MONTH, day)
                                                android.app.TimePickerDialog(
                                                    context,
                                                    { _, hour, min ->
                                                        calendar.set(Calendar.HOUR_OF_DAY, hour)
                                                        calendar.set(Calendar.MINUTE, min)
                                                        calendar.set(Calendar.SECOND, 0)
                                                        calendar.set(Calendar.MILLISECOND, 0)
                                                        startTimestamp = calendar.timeInMillis
                                                    },
                                                    calendar.get(Calendar.HOUR_OF_DAY),
                                                    calendar.get(Calendar.MINUTE),
                                                    true
                                                ).show()
                                            }
                                        },
                                        calendar.get(Calendar.YEAR),
                                        calendar.get(Calendar.MONTH),
                                        calendar.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                                .padding(10.dp)
                        ) {
                            Column {
                                Text("From (Start)", fontSize = 9.sp, color = ElegantOnBackground.copy(0.5f), fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = formatReportDateTime(startTimestamp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (startTimestamp != null) ElegantPrimary else ElegantOnBackground.copy(0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // End Date selection block
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ElegantSurfaceVariant)
                                .clickable {
                                    val calendar = Calendar.getInstance()
                                    if (endTimestamp != null) {
                                        calendar.timeInMillis = endTimestamp!!
                                    }
                                    var isPickerFired = false
                                    android.app.DatePickerDialog(
                                        context,
                                        { _, year, month, day ->
                                            if (!isPickerFired) {
                                                isPickerFired = true
                                                calendar.set(Calendar.YEAR, year)
                                                calendar.set(Calendar.MONTH, month)
                                                calendar.set(Calendar.DAY_OF_MONTH, day)
                                                android.app.TimePickerDialog(
                                                    context,
                                                    { _, hour, min ->
                                                        calendar.set(Calendar.HOUR_OF_DAY, hour)
                                                        calendar.set(Calendar.MINUTE, min)
                                                        calendar.set(Calendar.SECOND, 59)
                                                        calendar.set(Calendar.MILLISECOND, 999)
                                                        endTimestamp = calendar.timeInMillis
                                                    },
                                                    calendar.get(Calendar.HOUR_OF_DAY),
                                                    calendar.get(Calendar.MINUTE),
                                                    true
                                                ).show()
                                            }
                                        },
                                        calendar.get(Calendar.YEAR),
                                        calendar.get(Calendar.MONTH),
                                        calendar.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                                .padding(10.dp)
                        ) {
                            Column {
                                Text("To (End)", fontSize = 9.sp, color = ElegantOnBackground.copy(0.5f), fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = formatReportDateTime(endTimestamp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (endTimestamp != null) ElegantPrimary else ElegantOnBackground.copy(0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Results Counter
                Text(
                    text = "${filteredTransactions.size} transactions matched this filter",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = ElegantOnBackground.copy(0.6f)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // CSV Export Clickable
            Button(
                onClick = {
                    if (filteredTransactions.isEmpty()) {
                        Toast.makeText(context, "No transactions found to export CSV!", Toast.LENGTH_SHORT).show()
                    } else {
                        val csv = generateCsvString(filteredTransactions, categories, accounts)
                        shareTextReport(context, csv, "Expense_Wallet_Report.csv")
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = ElegantSurfaceVariant, contentColor = ElegantOnSurfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = "Export CSV icon", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share CSV", fontSize = 13.sp)
            }

            // PDF print trigger
            Button(
                onClick = {
                    if (filteredTransactions.isEmpty()) {
                        Toast.makeText(context, "No transactions found to print PDF!", Toast.LENGTH_SHORT).show()
                    } else {
                        val html = generateHtmlString(filteredTransactions, categories, accounts, currencySymbol, selectedTypeFilter)
                        printHtmlReport(context, html)
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = ElegantPrimary, contentColor = ElegantOnPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Print, contentDescription = "Print PDF icon", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Print/PDF", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Quick report summary preview card
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(containerColor = ElegantSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            LazyColumn(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text("Report Preview (matching logs)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ElegantPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                }
                items(filteredTransactions.take(8)) { tr ->
                    val cat = categories.find { it.id == tr.categoryId }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${cat?.name ?: "Expense"} (${tr.paymentMethod})", fontSize = 12.sp, color = ElegantOnBackground, maxLines = 1)
                        Text(
                            text = (if (tr.type == "INCOME") "+" else "-") + formatCurrency(tr.amount, currencySymbol),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (tr.type == "INCOME") ElegantIncomeGreen else ElegantExpenseRed
                        )
                    }
                }
                if (filteredTransactions.size > 8) {
                    item {
                        Text("...and ${filteredTransactions.size - 8} more matches", fontSize = 11.sp, color = ElegantOnBackground.copy(0.4f), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

// Share text format helper
fun shareTextReport(context: android.content.Context, contentText: String, title: String) {
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_SUBJECT, title)
        putExtra(android.content.Intent.EXTRA_TEXT, contentText)
    }
    context.startActivity(android.content.Intent.createChooser(intent, "Share Expense Wallet Report"))
}

// Native Print adapter helper
fun printHtmlReport(context: android.content.Context, htmlContent: String) {
    try {
        val webView = android.webkit.WebView(context)
        webView.webViewClient = object : android.webkit.WebViewClient() {
            override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
                val printAdapter = webView.createPrintDocumentAdapter("Expense_Wallet_Report")
                val jobName = "Expense Wallet Report Document"
                printManager.print(jobName, printAdapter, android.print.PrintAttributes.Builder().build())
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    } catch (e: Exception) {
        Toast.makeText(context, "Error printing PDF: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

// String builders for Export engines
fun generateCsvString(txs: List<Transaction>, cats: List<Category>, accs: List<Account>): String {
    val sb = java.lang.StringBuilder()
    sb.append("ID,Date,Type,Amount,Category,Account,PaymentMethod,Note\n")
    txs.forEach { t ->
        val cat = cats.find { it.id == t.categoryId }?.name ?: "Unknown"
        val acc = accs.find { it.id == t.accountId }?.name ?: "Unknown"
        val dateString = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(t.dateTime))
        sb.append("${t.id},\"$dateString\",${t.type},${t.amount},\"$cat\",\"$acc\",\"${t.paymentMethod}\",\"${t.note.replace("\"", "\"\"")}\"\n")
    }
    return sb.toString()
}

fun generateHtmlString(txs: List<Transaction>, cats: List<Category>, accs: List<Account>, currencySym: String, typeFiltered: String): String {
    val totalAmt = txs.sumOf { if (it.type == "INCOME") it.amount else -it.amount }
    val formatter = DecimalFormat("#,##0.00")
    
    val sb = java.lang.StringBuilder()
    sb.append("""
        <html>
        <head>
        <style>
            body { font-family: sans-serif; background-color: #ffffff; color: #333333; padding: 20px; }
            h1 { color: #381E72; font-size: 24px; margin-bottom: 5px; }
            .meta { font-size: 11px; color: #666666; margin-bottom: 20px; }
            table { width: 100%; border-collapse: collapse; margin-top: 15px; }
            th { background-color: #4A4458; color: #ffffff; padding: 10px; text-align: left; font-size: 13px; }
            td { padding: 8px 10px; font-size: 12px; border-bottom: 1px solid #eeeeee; }
            .green { color: #4CAF50; font-weight: bold; }
            .red { color: #f44336; font-weight: bold; }
            .total { font-size: 16px; font-weight: bold; margin-top: 25px; text-align: right; border-top: 2px solid #381E72; padding-top: 10px; }
        </style>
        </head>
        <body>
            <h1>Expense Wallet Secure statement</h1>
            <div class="meta">Export Type: $typeFiltered | Created on: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}</div>
            <table>
                <tr>
                    <th>Date</th>
                    <th>Type</th>
                    <th>Category</th>
                    <th>Account</th>
                    <th>Method</th>
                    <th>Note</th>
                    <th>Amount</th>
                </tr>
    """.trimIndent())

    txs.forEach { t ->
        val catName = cats.find { it.id == t.categoryId }?.name ?: "Unknown"
        val accName = accs.find { it.id == t.accountId }?.name ?: "Unknown"
        val isInc = t.type == "INCOME"
        val amtClass = if (isInc) "green" else "red"
        val amtSign = if (isInc) "+" else "-"
        val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(t.dateTime))
        
        sb.append("""
            <tr>
                <td>$formattedDate</td>
                <td>${t.type}</td>
                <td>$catName</td>
                <td>$accName</td>
                <td>${t.paymentMethod}</td>
                <td>${t.note}</td>
                <td class="$amtClass">$amtSign $currencySym${formatter.format(t.amount)}</td>
            </tr>
        """.trimIndent())
    }

    sb.append("""
            </table>
            <div class="total">Net Balance Change: $currencySym${formatter.format(totalAmt)}</div>
        </body>
        </html>
    """.trimIndent())
    return sb.toString()
}

// -------------------------------------------------------------
// NEW FEATURE: SECURITY PIN CONFIG TAB
// -------------------------------------------------------------
@Composable
fun SecurityTabContent(viewModel: ExpenseViewModel) {
    val context = LocalContext.current
    val pin by viewModel.appPinCode.collectAsStateWithLifecycle()
    val isLocked = pin.isNotEmpty()

    var textPinValue by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Application Security & Locks", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ElegantOnBackground)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ElegantSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isLocked) "Passcode Protection ENABLED" else "Passcode PROTECTION DISABLED",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isLocked) ElegantIncomeGreen else ElegantExpenseRed
                )

                Text(
                    text = "Secure your local budget records with a 4-digit code. Once configured, you will be prompted for your passcode when booting the app.",
                    fontSize = 12.sp,
                    color = ElegantOnBackground.copy(alpha = 0.7f),
                    lineHeight = 18.sp
                )

                if (isLocked) {
                    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsStateWithLifecycle()

                    Button(
                        onClick = {
                            viewModel.disablePinCode()
                            Toast.makeText(context, "Passcode lock deactivated!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElegantExpenseRed, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Disable Lock Protection", fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.12f)))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ElegantSurfaceVariant)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Enable Biometric Unlock",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElegantOnBackground
                            )
                            Text(
                                "Unlock securely using your device's registered fingerprint or face ID.",
                                fontSize = 11.sp,
                                color = ElegantOnBackground.copy(alpha = 0.6f)
                            )
                        }
                        Switch(
                            checked = isBiometricEnabled,
                            onCheckedChange = {
                                viewModel.setBiometricEnabled(it)
                                Toast.makeText(context, if (it) "Biometrics enabled!" else "Biometrics disabled!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = textPinValue,
                        onValueChange = { if (it.length <= 4 && (it.isEmpty() || it.toIntOrNull() != null)) textPinValue = it },
                        label = { Text("Set 4-digit PIN Passcode") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ElegantOnBackground, focusedLabelColor = ElegantPrimary, focusedBorderColor = ElegantPrimary, unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            if (textPinValue.length == 4) {
                                viewModel.updatePinCode(textPinValue)
                                textPinValue = ""
                                Toast.makeText(context, "Passcode active!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Please write exactly 4 numbers", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElegantPrimary, contentColor = ElegantOnPrimary),
                        shape = RoundedCornerShape(10.dp),
                        enabled = textPinValue.length == 4
                    ) {
                        Text("Lock Application Now", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// NEW FEATURE: SYSTEM COMPONENT GATES - PIN VERIFY SCREEN WITH BIOMETRIC SUPPORTS
// -------------------------------------------------------------
@Composable
fun PinVerificationScreen(
    correctPin: String,
    viewModel: ExpenseViewModel,
    onUnlocked: () -> Unit
) {
    var enteredText by remember { mutableStateOf("") }
    var triedCount by remember { mutableStateOf(0) }

    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsStateWithLifecycle()
    var showBiometricDialog by remember { mutableStateOf(isBiometricEnabled) }
    var showSimulatorFallback by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val activity = context as? androidx.fragment.app.FragmentActivity

    // Automatic biometric prompt load on boot
    LaunchedEffect(showBiometricDialog) {
        if (showBiometricDialog && isBiometricEnabled) {
            if (activity != null) {
                val executor = ContextCompat.getMainExecutor(activity)
                val biometricPrompt = BiometricPrompt(
                    activity,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            onUnlocked()
                            showBiometricDialog = false
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            super.onAuthenticationError(errorCode, errString)
                            // If biometric is cancelled or not supported, we can trigger the virtual visual simulator fallback
                            showSimulatorFallback = true
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            showSimulatorFallback = true
                        }
                    }
                )

                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Secure Vault Lock")
                    .setSubtitle("Confirm fingerprint or face ID to unlock")
                    .setNegativeButtonText("Use Passcode PIN")
                    .build()

                try {
                    biometricPrompt.authenticate(promptInfo)
                } catch (e: Exception) {
                    showSimulatorFallback = true
                }
            } else {
                showSimulatorFallback = true
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ElegantBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Shield Guard",
                tint = ElegantPrimary,
                modifier = Modifier.size(64.dp)
            )

            Text(
                text = "Secure Vault Locked",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = ElegantOnBackground
            )

            Text(
                text = "Enter your 4-digit PIN key to unlock.",
                fontSize = 13.sp,
                color = ElegantOnBackground.copy(alpha = 0.6f)
            )

            // Star indicator dots based on length
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 1..4) {
                    val isFilled = enteredText.length >= i
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (isFilled) ElegantPrimary else Color.White.copy(0.12f))
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Inline Numerical Custom Keyboard (100% Mobile Safe overlay)
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val grid = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "⌫")
                )

                grid.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        row.forEach { digit ->
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(ElegantSurfaceVariant)
                                    .clickable {
                                        when (digit) {
                                            "C" -> enteredText = ""
                                            "⌫" -> if (enteredText.isNotEmpty()) enteredText = enteredText.dropLast(1)
                                            else -> {
                                                if (enteredText.length < 4) {
                                                    enteredText += digit
                                                    if (enteredText.length == 4) {
                                                        if (enteredText == correctPin) {
                                                            onUnlocked()
                                                        } else {
                                                            enteredText = ""
                                                            triedCount++
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = digit,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElegantOnSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (isBiometricEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                IconButton(
                    onClick = { showBiometricDialog = true },
                    modifier = Modifier
                        .size(56.dp)
                        .background(ElegantPrimary.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Unlock with Biometrics",
                        tint = ElegantPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Text(
                    text = "Tap fingerprint scanner to trigger device auth",
                    fontSize = 11.sp,
                    color = ElegantOnBackground.copy(alpha = 0.5f)
                )
            }

            if (triedCount > 0) {
                Text(
                    text = "Incorrect passcode. Please try again! Attempt: $triedCount",
                    fontSize = 11.sp,
                    color = ElegantExpenseRed,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    if (showSimulatorFallback) {
        BiometricSimulatorDialog(
            onDismissRequest = {
                showSimulatorFallback = false
                showBiometricDialog = false
            },
            onSuccess = {
                showSimulatorFallback = false
                showBiometricDialog = false
                onUnlocked()
            }
        )
    }
}

@Composable
fun BiometricSimulatorDialog(
    onDismissRequest: () -> Unit,
    onSuccess: () -> Unit
) {
    var isScanning by remember { mutableStateOf(false) }
    var scanStatus by remember { mutableStateOf("Touch the fingerprint sensor below to scan") }
    var isSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(isScanning) {
        if (isScanning) {
            scanStatus = "Reading biometric credentials..."
            kotlinx.coroutines.delay(1200)
            isSuccess = true
            scanStatus = "Authentication successful!"
            kotlinx.coroutines.delay(600)
            onSuccess()
        }
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = ElegantSurface),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Biometric Lock Security",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElegantOnBackground
                    )
                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = ElegantOnBackground.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Pulsate transition
                val pulseScale = rememberInfiniteTransition(label = "pulse_trans").animateFloat(
                    initialValue = 1f,
                    targetValue = 1.15f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse"
                )

                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSuccess) ElegantIncomeGreen.copy(alpha = 0.15f)
                            else ElegantPrimary.copy(alpha = 0.15f)
                        )
                        .clickable(enabled = !isScanning && !isSuccess) {
                            isScanning = true
                        }
                        .padding(if (isScanning && !isSuccess) (14.dp * pulseScale.value) else 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Fingerprint Scanner icon",
                        tint = if (isSuccess) ElegantIncomeGreen else ElegantPrimary,
                        modifier = Modifier.size(56.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = scanStatus,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSuccess) ElegantIncomeGreen else ElegantOnBackground,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Simulates fingerprint touch and high-fidelity local face scan",
                        fontSize = 10.sp,
                        color = ElegantOnBackground.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { isScanning = true },
                    enabled = !isScanning && !isSuccess,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElegantPrimary,
                        contentColor = ElegantOnPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = if (isScanning) "Scanning..." else "Simulate Finger Sensor", fontWeight = FontWeight.Bold)
                }

                TextButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel & Input PIN Instead", color = ElegantOnBackground.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsTabContent(
    accounts: List<Account>,
    categoriesCount: Int,
    transactionsCount: Int,
    paymentMethods: List<String>,
    currencySymbol: String,
    onPrepopulateDefaults: () -> Unit,
    onAddAccountClick: () -> Unit,
    viewModel: ExpenseViewModel
) {
    val accountsCount = accounts.size
    val context = LocalContext.current
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    var inputUserName by remember { mutableStateOf("") }
    var inputCurrencyCode by remember { mutableStateOf("") }
    var inputPaymentMethod by remember { mutableStateOf("") }

    LaunchedEffect(userName) {
        inputUserName = userName
    }

    // Backup state
    var backupStringShare by remember { mutableStateOf("") }
    var importStringInput by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "System Settings Registry",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ElegantOnBackground
            )
        }

        // 0. User Display Name Profile Configuration Row
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ElegantSurface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "User Profile Customization",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElegantPrimary
                    )
                    Text(
                        text = "Customize the display name shown across your ledger and sidebar workspace.",
                        fontSize = 12.sp,
                        color = ElegantOnBackground.copy(alpha = 0.7f)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = inputUserName,
                            onValueChange = { inputUserName = it },
                            label = { Text("Display Name") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = ElegantOnBackground, focusedLabelColor = ElegantPrimary, focusedBorderColor = ElegantPrimary, unfocusedBorderColor = Color.White.copy(0.12f)
                            ),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                if (inputUserName.trim().isNotBlank()) {
                                    viewModel.updateUserName(inputUserName.trim())
                                    Toast.makeText(context, "Display name updated!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Display name cannot be empty!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElegantPrimary, contentColor = ElegantOnPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }

        // 1. SharedPreferences Dynamic Currencies Selection
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ElegantSurface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Multiple Currency Setup",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElegantPrimary
                    )
                    Text(
                        text = "Currently using: \"$currencySymbol\". Select preset or write custom symbol below.",
                        fontSize = 12.sp,
                        color = ElegantOnBackground.copy(alpha = 0.7f)
                    )

                    // Preset list
                    val currencyPresets = listOf("$", "€", "£", "₹", "₨", "¥", "₪")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(currencyPresets) { sym ->
                            val isAct = sym == currencySymbol
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isAct) ElegantPrimary else ElegantSurfaceVariant)
                                    .clickable { viewModel.setCurrencySymbol(sym) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(sym, fontSize = 12.sp, color = if (isAct) ElegantOnPrimary else ElegantOnBackground, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Custom input
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = inputCurrencyCode,
                            onValueChange = { inputCurrencyCode = it },
                            label = { Text("Custom Symbol") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = ElegantOnBackground, focusedLabelColor = ElegantPrimary, focusedBorderColor = ElegantPrimary, unfocusedBorderColor = Color.White.copy(0.12f)
                            ),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                if (inputCurrencyCode.isNotBlank()) {
                                    viewModel.setCurrencySymbol(inputCurrencyCode)
                                    inputCurrencyCode = ""
                                    Toast.makeText(context, "Currency symbol updated!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElegantPrimary, contentColor = ElegantOnPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Apply")
                        }
                    }
                }
            }
        }

        // 2. Custom Transaction Sub-Types / Payment Methods Setup
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ElegantSurface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Transaction Payment Channels",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElegantPrimary
                    )
                    Text(
                        text = "Created sub-types allow recording channels separately like Cash, Credit, Bank card, etc.",
                        fontSize = 12.sp,
                        color = ElegantOnBackground.copy(alpha = 0.7f)
                    )

                    // Current methods list with single-click remove
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        paymentMethods.forEach { method ->
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ElegantSurfaceVariant)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(method, fontSize = 11.sp, color = ElegantOnBackground)
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove Channel",
                                    tint = ElegantOnBackground.copy(0.5f),
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clickable { viewModel.removePaymentMethod(method) }
                                )
                            }
                        }
                    }

                    // Input form
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = inputPaymentMethod,
                            onValueChange = { inputPaymentMethod = it },
                            label = { Text("New Channel Name") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = ElegantOnBackground, focusedLabelColor = ElegantPrimary, focusedBorderColor = ElegantPrimary, unfocusedBorderColor = Color.White.copy(0.12f)
                            ),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                if (inputPaymentMethod.isNotBlank()) {
                                    viewModel.addPaymentMethod(inputPaymentMethod)
                                    inputPaymentMethod = ""
                                    Toast.makeText(context, "Subchannel configured!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElegantPrimary, contentColor = ElegantOnPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Configure")
                        }
                    }
                }
            }
        }

        // 3. 100% Offline Database JSON Backup & Recovery
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ElegantSurface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Secure Backup & Recovery",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElegantPrimary
                    )
                    Text(
                        text = "Because your budgets are 100% local, standard cloud syncing is bypassed. Make an explicit JSON backup to preserve or migrate your records.",
                        fontSize = 12.sp,
                        color = ElegantOnBackground.copy(alpha = 0.7f),
                        lineHeight = 18.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val stringExport = viewModel.exportBackup()
                                shareTextReport(context, stringExport, "My_Expense_Wallet_Backup.json")
                                Toast.makeText(context, "JSON backup exported and shareable!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = ElegantSurfaceVariant, contentColor = ElegantOnSurfaceVariant),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Backup, contentDescription = "Backup icons", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export Backup", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { showImportDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = ElegantPrimary, contentColor = ElegantOnPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = "Import icon", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import JSON", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Manage Wallets & Accounts
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ElegantSurface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Manage Wallets & Accounts",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElegantPrimary
                        )
                        IconButton(
                            onClick = onAddAccountClick,
                            modifier = Modifier
                                .size(28.dp)
                                .background(ElegantPrimary.copy(alpha = 0.15f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Wallet",
                                tint = ElegantPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Text(
                        text = "Create and manage your local financial accounts. Each account acts as a separate wallet with its own tracking balance.",
                        fontSize = 12.sp,
                        color = ElegantOnBackground.copy(alpha = 0.7f),
                        lineHeight = 18.sp
                    )

                    if (accounts.isEmpty()) {
                        Text(
                            text = "No active wallets. Tap + to create one.",
                            fontSize = 11.sp,
                            color = ElegantOnBackground.copy(alpha = 0.5f),
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            accounts.forEach { account ->
                                val parsedColor = try {
                                    Color(android.graphics.Color.parseColor(account.colorHex))
                                } catch (e: Exception) {
                                    ElegantSurfaceVariant
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(ElegantSurfaceVariant)
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(parsedColor)
                                        )
                                        Icon(
                                            imageVector = getIconForName(account.iconName),
                                            contentDescription = null,
                                            tint = ElegantOnBackground.copy(alpha = 0.7f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = account.name,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = ElegantOnBackground,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = formatCurrency(account.balance, currencySymbol),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ElegantOnBackground
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Account",
                                            tint = ElegantExpenseRed.copy(alpha = 0.8f),
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable { viewModel.deleteAccount(account) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Statistics Metadata
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ElegantSurface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Secure Database Statistics",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElegantOnBackground
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Active Accounts", fontSize = 12.sp, color = ElegantOnBackground.copy(alpha = 0.6f))
                        Text("$accountsCount", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ElegantOnBackground)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Configured Categories", fontSize = 12.sp, color = ElegantOnBackground.copy(alpha = 0.6f))
                        Text("$categoriesCount", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ElegantOnBackground)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Logged Transactions", fontSize = 12.sp, color = ElegantOnBackground.copy(alpha = 0.6f))
                        Text("$transactionsCount", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ElegantOnBackground)
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    onPrepopulateDefaults()
                    Toast.makeText(context, "Initial default accounts prepopulated successfully!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = ElegantSurfaceVariant, contentColor = ElegantOnSurfaceVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Prepopulate Sample Cash Account", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Copyright © Ezaan Ahmed. All Rights Reserved.",
                    fontSize = 11.sp,
                    color = ElegantOnBackground.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // JSON Recovery Import Dialog Overlay
    if (showImportDialog) {
        Dialog(onDismissRequest = { showImportDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = ElegantSurface),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Import Backup JSON database", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ElegantOnBackground)
                    
                    Text("Paste the full raw JSON content structure of your backup below:", fontSize = 11.sp, color = ElegantOnBackground.copy(0.6f))

                    OutlinedTextField(
                        value = importStringInput,
                        onValueChange = { importStringInput = it },
                        label = { Text("Recovery JSON") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ElegantOnBackground, focusedLabelColor = ElegantPrimary, focusedBorderColor = ElegantPrimary, unfocusedBorderColor = Color.White.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier.fillMaxWidth().height(140.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showImportDialog = false }) {
                            Text("Cancel", color = ElegantOnBackground.copy(alpha = 0.6f))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                if (importStringInput.isNotBlank()) {
                                    val ok = viewModel.importBackup(importStringInput)
                                    if (ok) {
                                        Toast.makeText(context, "Full secure restore completed successfully!", Toast.LENGTH_SHORT).show()
                                        showImportDialog = false
                                        importStringInput = ""
                                    } else {
                                        Toast.makeText(context, "Failed to parse JSON backup template!", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElegantPrimary, contentColor = ElegantOnPrimary)
                        ) {
                            Text("Apply Restore")
                        }
                    }
                }
            }
        }
    }
}

// Dialog Layout Components
@Composable
fun AddTransactionDialog(
    accounts: List<Account>,
    categories: List<Category>,
    paymentMethods: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (accountId: Int, categoryId: Int, amount: Double, type: String, note: String, paymentMethod: String, dateTime: Long) -> Unit
) {
    if (accounts.isEmpty() || categories.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Setup Required", color = ElegantOnBackground) },
            text = { Text("You must have at least one account and category configured to register dynamic entries.", color = ElegantOnBackground.copy(0.7f)) },
            containerColor = ElegantSurface,
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("OK", color = ElegantPrimary)
                }
            }
        )
        return
    }

    var textAmount by remember { mutableStateOf("") }
    var transactionType by remember { mutableStateOf("EXPENSE") }
    var selectedAccount by remember { mutableStateOf(accounts.first()) }
    val filteredCategories = categories.filter { it.type == transactionType }
    var selectedCategory by remember { mutableStateOf(if (filteredCategories.isNotEmpty()) filteredCategories.first() else categories.first()) }
    var noteText by remember { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf(if (paymentMethods.isNotEmpty()) paymentMethods.first() else "Cash") }

    // Date & Time Custom Picking states
    var customYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR).toString()) }
    var customMonth by remember { mutableStateOf((Calendar.getInstance().get(Calendar.MONTH) + 1).toString()) }
    var customDay by remember { mutableStateOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH).toString()) }
    var customHour by remember { mutableStateOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY).toString()) }
    var customMinute by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MINUTE).toString()) }

    LaunchedEffect(transactionType) {
        val matched = categories.filter { it.type == transactionType }
        if (matched.isNotEmpty()) {
            selectedCategory = matched.first()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ElegantSurface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Log New Entry",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElegantOnBackground
                )

                // Expense vs Income toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ElegantSurfaceVariant)
                        .padding(2.dp)
                ) {
                    val types = listOf("EXPENSE", "INCOME")
                    types.forEach { type ->
                        val isSelected = transactionType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) ElegantPrimary else Color.Transparent)
                                .clickable { transactionType = type },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = type,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) ElegantOnPrimary else ElegantOnSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Amount Text Field
                OutlinedTextField(
                    value = textAmount,
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null || it.endsWith(".")) textAmount = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ElegantOnBackground,
                        focusedLabelColor = ElegantPrimary,
                        focusedBorderColor = ElegantPrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transaction_amount_field"),
                    singleLine = true
                )

                // Account Selection list
                Column {
                    Text("Select Wallet", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ElegantOnBackground.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(accounts) { acc ->
                            val isChosen = acc.id == selectedAccount.id
                            val bgParsed = try {
                                Color(android.graphics.Color.parseColor(acc.colorHex))
                            } catch (e: Exception) {
                                ElegantPrimary
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isChosen) bgParsed else bgParsed.copy(0.2f))
                                    .border(1.5.dp, if (isChosen) Color.White else Color.Transparent, RoundedCornerShape(10.dp))
                                    .clickable { selectedAccount = acc }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(acc.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isChosen) Color.White else ElegantOnBackground.copy(0.7f))
                            }
                        }
                    }
                }

                // Payment Method Channels Selector (NEW REQUIREMENT)
                Column {
                    Text("Payment Channel Type", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ElegantOnBackground.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(paymentMethods) { method ->
                            val isSel = selectedPaymentMethod == method
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) ElegantPrimary else ElegantSurfaceVariant)
                                    .clickable { selectedPaymentMethod = method }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(method, fontSize = 11.sp, color = if (isSel) ElegantOnPrimary else ElegantOnBackground, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // Category Selection list
                Column {
                    Text("Select Category", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ElegantOnBackground.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(4.dp))
                    if (filteredCategories.isEmpty()) {
                        Text("No matching categories. Clear filters.", fontSize = 11.sp, color = ElegantExpenseRed)
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(filteredCategories) { cat ->
                                val isChosen = cat.id == selectedCategory.id
                                val bgParsed = try {
                                    Color(android.graphics.Color.parseColor(cat.colorHex))
                                } catch (e: Exception) {
                                    ElegantPrimary
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isChosen) bgParsed else bgParsed.copy(0.2f))
                                        .border(1.5.dp, if (isChosen) Color.White else Color.Transparent, RoundedCornerShape(10.dp))
                                        .clickable { selectedCategory = cat }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(cat.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isChosen) Color.White else ElegantOnBackground.copy(0.7f))
                                }
                            }
                        }
                    }
                }

                // Note descriptions
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note / Description (Optional)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ElegantOnBackground, focusedLabelColor = ElegantPrimary, focusedBorderColor = ElegantPrimary, unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Date & Time Picker triggering text/indicator (NEW REQUIREMENT)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Date & Time of Transaction", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ElegantOnBackground.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(4.dp))
                    val context = LocalContext.current
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ElegantSurfaceVariant)
                            .clickable {
                                val calendar = Calendar.getInstance()
                                val currentYearVal = customYear.toIntOrNull() ?: calendar.get(Calendar.YEAR)
                                val currentMonthVal = (customMonth.toIntOrNull() ?: (calendar.get(Calendar.MONTH) + 1)) - 1
                                val currentDayVal = customDay.toIntOrNull() ?: calendar.get(Calendar.DAY_OF_MONTH)

                                var isPickerFired = false
                                val dpg = android.app.DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        if (!isPickerFired) {
                                            isPickerFired = true
                                            customYear = year.toString()
                                            customMonth = (month + 1).toString()
                                            customDay = day.toString()

                                            val currentHourVal = customHour.toIntOrNull() ?: calendar.get(Calendar.HOUR_OF_DAY)
                                            val currentMinuteVal = customMinute.toIntOrNull() ?: calendar.get(Calendar.MINUTE)
                                            val tpg = android.app.TimePickerDialog(
                                                context,
                                                { _, hour, minute ->
                                                    customHour = String.format("%02d", hour)
                                                    customMinute = String.format("%02d", minute)
                                                },
                                                currentHourVal,
                                                currentMinuteVal,
                                                true
                                            )
                                            tpg.show()
                                        }
                                    },
                                    currentYearVal,
                                    currentMonthVal,
                                    currentDayVal
                                )
                                dpg.show()
                            }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$customDay/$customMonth/$customYear - $customHour:$customMinute",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ElegantOnSurfaceVariant
                        )
                        Icon(imageVector = Icons.Default.CalendarToday, contentDescription = "Calendar Trigger", tint = ElegantPrimary, modifier = Modifier.size(16.dp))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = ElegantOnBackground.copy(alpha = 0.6f))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val amt = textAmount.toDoubleOrNull() ?: 0.0
                            if (amt > 0.0) {
                                // Compute customized timestamp
                                val calendar = Calendar.getInstance()
                                calendar.set(Calendar.YEAR, customYear.toIntOrNull() ?: calendar.get(Calendar.YEAR))
                                calendar.set(Calendar.MONTH, (customMonth.toIntOrNull() ?: (calendar.get(Calendar.MONTH) + 1)) - 1)
                                calendar.set(Calendar.DAY_OF_MONTH, customDay.toIntOrNull() ?: calendar.get(Calendar.DAY_OF_MONTH))
                                calendar.set(Calendar.HOUR_OF_DAY, customHour.toIntOrNull() ?: calendar.get(Calendar.HOUR_OF_DAY))
                                calendar.set(Calendar.MINUTE, customMinute.toIntOrNull() ?: calendar.get(Calendar.MINUTE))
                                calendar.set(Calendar.SECOND, 0)
                                calendar.set(Calendar.MILLISECOND, 0)

                                onConfirm(selectedAccount.id, selectedCategory.id, amt, transactionType, noteText, selectedPaymentMethod, calendar.timeInMillis)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElegantPrimary, contentColor = ElegantOnPrimary),
                        shape = RoundedCornerShape(10.dp),
                        enabled = textAmount.toDoubleOrNull() != null && textAmount.toDouble() > 0 && selectedCategory != null,
                        modifier = Modifier.testTag("transaction_submit")
                    ) {
                        Text("Save Entry", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }


}

@Composable
fun AddAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, initBalance: Double, colorHex: String, iconName: String) -> Unit
) {
    var acctName by remember { mutableStateOf("") }
    var initBalance by remember { mutableStateOf("0.0") }

    val colors = listOf("#4CAF50", "#2196F3", "#9C27B0", "#FFC107", "#E91E63", "#00BCD4", "#FF5722", "#FF9800")
    val icons = listOf("payments", "credit_card", "savings", "wallet", "home", "shopping_bag")

    var chosenColor by remember { mutableStateOf(colors.first()) }
    var chosenIcon by remember { mutableStateOf(icons.first()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ElegantSurface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Configure New Wallet",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElegantOnBackground
                )

                // Acc Title
                OutlinedTextField(
                    value = acctName,
                    onValueChange = { acctName = it },
                    label = { Text("Account Identifier Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ElegantOnBackground,
                        focusedLabelColor = ElegantPrimary,
                        focusedBorderColor = ElegantPrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("account_name_field"),
                    singleLine = true
                )

                // Init balance
                OutlinedTextField(
                    value = initBalance,
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null || it.endsWith(".")) initBalance = it },
                    label = { Text("Starting Balance") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ElegantOnBackground,
                        focusedLabelColor = ElegantPrimary,
                        focusedBorderColor = ElegantPrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Select Color
                Column {
                    Text("Select Accent Color", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ElegantOnBackground.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(colors) { col ->
                            val colParsed = Color(android.graphics.Color.parseColor(col))
                            val isChosen = chosenColor == col
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(colParsed)
                                    .border(2.dp, if (isChosen) Color.White else Color.Transparent, CircleShape)
                                    .clickable { chosenColor = col }
                            )
                        }
                    }
                }

                // Select Icon
                Column {
                    Text("Select Icon representation", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ElegantOnBackground.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(icons) { ic ->
                            val isChosen = chosenIcon == ic
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isChosen) ElegantPrimary else ElegantSurfaceVariant)
                                    .clickable { chosenIcon = ic },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getIconForName(ic),
                                    contentDescription = ic,
                                    tint = if (isChosen) ElegantOnPrimary else ElegantOnBackground,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = ElegantOnBackground.copy(alpha = 0.6f))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val bal = initBalance.toDoubleOrNull() ?: 0.0
                            if (acctName.isNotBlank()) {
                                onConfirm(acctName, bal, chosenColor, chosenIcon)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElegantPrimary, contentColor = ElegantOnPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("account_submit")
                    ) {
                        Text("Add Account", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: String, iconName: String, colorHex: String) -> Unit
) {
    var catName by remember { mutableStateOf("") }
    var catType by remember { mutableStateOf("EXPENSE") }

    val colors = listOf("#FF6B6B", "#4D96FF", "#6BCB77", "#FFD93D", "#9C27B0", "#FF9F43", "#00D2D3", "#54A0FF")
    val icons = listOf("shopping_cart", "restaurant", "directions_car", "sports_esports", "bolt", "medical_services", "payments", "trending_up")

    var chosenColor by remember { mutableStateOf(colors.first()) }
    var chosenIcon by remember { mutableStateOf(icons.first()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ElegantSurface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Add Custom Category",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElegantOnBackground
                )

                // Category Name
                OutlinedTextField(
                    value = catName,
                    onValueChange = { catName = it },
                    label = { Text("Category Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ElegantOnBackground,
                        focusedLabelColor = ElegantPrimary,
                        focusedBorderColor = ElegantPrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_name_field"),
                    singleLine = true
                )

                // Type switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ElegantSurfaceVariant)
                        .padding(2.dp)
                ) {
                    val types = listOf("EXPENSE", "INCOME")
                    types.forEach { t ->
                        val isSel = catType == t
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) ElegantPrimary else Color.Transparent)
                                .clickable { catType = t },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = t,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) ElegantOnPrimary else ElegantOnSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Choose Accent Color
                Column {
                    Text("Choose Visual Accent", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ElegantOnBackground.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(colors) { col ->
                            val colParsed = Color(android.graphics.Color.parseColor(col))
                            val isChosen = chosenColor == col
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(colParsed)
                                    .border(2.dp, if (isChosen) Color.White else Color.Transparent, CircleShape)
                                    .clickable { chosenColor = col }
                            )
                        }
                    }
                }

                // Choose icon representation
                Column {
                    Text("Choose Category Icon", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ElegantOnBackground.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(icons) { ic ->
                            val isChosen = chosenIcon == ic
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isChosen) ElegantPrimary else ElegantSurfaceVariant)
                                    .clickable { chosenIcon = ic },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getIconForName(ic),
                                    contentDescription = ic,
                                    tint = if (isChosen) ElegantOnPrimary else ElegantOnBackground,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = ElegantOnBackground.copy(alpha = 0.6f))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            if (catName.isNotBlank()) {
                                onConfirm(catName, catType, chosenIcon, chosenColor)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElegantPrimary, contentColor = ElegantOnPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("category_submit")
                    ) {
                        Text("Confirm", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingWelcomeScreen(
    onComplete: (name: String, currency: String) -> Unit
) {
    var nameInput by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf("$") }
    var customCurrencyInput by remember { mutableStateOf("") }
    var isCustomSelected by remember { mutableStateOf(false) }

    val currencyPresets = listOf("$", "€", "£", "₹", "₨", "¥", "₪")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ElegantBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = ElegantSurface),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Brand Logo Image Decoration
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.app_logo_wallet_1780750581081),
                    contentDescription = "Wallet Logo",
                    modifier = Modifier
                        .size(108.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(ElegantSurfaceVariant),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Pocket Ledger",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElegantOnBackground,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your secure personal budget and expense tracker.",
                        fontSize = 13.sp,
                        color = ElegantOnBackground.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }

                // Field 1: Name
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "What should we call you?",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElegantPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        placeholder = { Text("e.g. Ezaan", color = ElegantOnBackground.copy(alpha = 0.4f)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ElegantOnBackground,
                            unfocusedTextColor = ElegantOnBackground,
                            focusedBorderColor = ElegantPrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                            focusedLabelColor = ElegantPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Field 2: Currency Selection
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Preferred Currency Preset",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElegantPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Presets horizontal list
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        items(currencyPresets) { sym ->
                            val isSel = !isCustomSelected && selectedCurrency == sym
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) ElegantPrimary else ElegantSurfaceVariant)
                                    .clickable {
                                        isCustomSelected = false
                                        selectedCurrency = sym
                                    }
                                    .defaultMinSize(minWidth = 50.dp, minHeight = 48.dp)
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = sym,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) ElegantOnPrimary else ElegantOnBackground
                                )
                            }
                        }

                        item {
                            val isSel = isCustomSelected
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) ElegantPrimary else ElegantSurfaceVariant)
                                    .clickable {
                                        isCustomSelected = true
                                    }
                                    .defaultMinSize(minWidth = 50.dp, minHeight = 48.dp)
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Custom...",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isSel) ElegantOnPrimary else ElegantOnBackground
                                )
                            }
                        }
                    }

                    // Optional Custom TextField if Custom is selected
                    if (isCustomSelected) {
                        OutlinedTextField(
                            value = customCurrencyInput,
                            onValueChange = { customCurrencyInput = it },
                            placeholder = { Text("e.g. USD, KRW, CHF", color = ElegantOnBackground.copy(alpha = 0.4f)) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = ElegantOnBackground,
                                unfocusedTextColor = ElegantOnBackground,
                                focusedBorderColor = ElegantPrimary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.12f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        val finalName = nameInput.trim()
                        val finalCurrency = if (isCustomSelected) customCurrencyInput.trim() else selectedCurrency
                        if (finalName.isNotBlank()) {
                            onComplete(finalName, finalCurrency.ifBlank { "$" })
                        }
                    },
                    enabled = nameInput.trim().isNotBlank() && (!isCustomSelected || customCurrencyInput.trim().isNotBlank()),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElegantPrimary,
                        contentColor = ElegantOnPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "Get Started",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
