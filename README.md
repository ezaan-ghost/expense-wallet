# 🌿 Ivy Expense

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg?logo=android&logoColor=white)]()
[![Language](https://img.shields.io/badge/Language-Kotlin-purple.svg?logo=kotlin&logoColor=white)]()
[![Toolkit](https://img.shields.io/badge/UI--Toolkit-Jetpack%20Compose-blue.svg?logo=jetpackcompose&logoColor=white)]()
[![Design](https://img.shields.io/badge/Design-Material%203-emerald.svg)]()
[![License](https://img.shields.io/badge/Security-100%25%20On--Device%20Local-blue.svg)]()

**Ivy Expense** is a premium personal finance, budget, and transaction manager inspired by the elegant layout of Ivy Wallet. Built entirely with **Kotlin** and **Jetpack Compose (Material Design 3)**, Ivy Expense is designed with an elegant dark "Slate" aesthetic that pairs responsive animations with localized storage, putting privacy and performance first.

Unlike traditional cloud-tethered money managers, Ivy Expense keeps 100% of your data **strictly on-device**. Your financial records belong only to you.

---

## ✨ Features Breakdown

### 📊 1. Wallet & Account Management (Home Hub)
* **Real-time Balance Summary**: A beautiful gradient display card displaying your **Total Balance**, **Income**, and **Expenses** with standard Material symbols.
* **Privacy Controls**: Insta-hide secure toggling for balances (`••••••`) on the home deck to shield your information from prying eyes.
* **Flexible Sub-Accounts**: Set up, customize, and edit multiple wallets (e.g., Cash, Credit, Online bank accounts, Savings) with personal color codings.
* **Transaction Ledger**: Scroll through clean transaction list views detailing exactly when, where, and why funds were updated.

### 🎯 2. Budget and Category Tracking
* **Smart Budgets**: Group expenses into custom categories (e.g., Food, Travel, Streaming, Rent) equipped with illustrative icons.
* **Spending Boundaries**: Configure custom allocation caps per budget to visualize how much threshold room remains.
* **Dynamic Indicators**: Modern custom progress indicators warning you as categories approach their maximum limit.

### 📈 3. Deep Statistical Insights
* **Interactive Breakdown**: Track your transaction trends across custom date periods or categories.
* **Aesthetic Visualizers**: Modern, high-contrast, clean visual representations of category spending ratios and weekly analytics.

### 🤝 4. Integrated Debt & Loan Ledger
* **Borrow & Lend Tracker**: Organize active outstanding liabilities or items you've lent to other parties.
* **Flexible Offsets**: Settle outstanding loans partially with payment installments or trigger full-settlement offsets.

### 🛡️ 5. Privacy & Security Architecture
* **Passcode-Guarded Boot**: Protect your entire budget database using a secure 4-digit PIN lock screen that secures application visibility on startup.
* **Biometric Integrity**: Local validation logic keeping unauthorized local users out.

### 🗄️ 6. Seamless Backup & Offline-First Integrity
* **100% Local Backup (JSON)**: Export your entire SQLite database structure into a compact, encrypted JSON string.
* **Immediate System Recovery**: Copy your backup string into any device and restore transactions instantly within seconds. No external servers or API hooks needed.
* **Statement Generator**: Compile fully formatted transaction sheets and export them as HTML tables or reports.

---

## 🎨 Design Highlights & Aesthetic Pairings

* **Physical Navigation Springs**: Tab-switching indicators leveraging Material 3's modern physical spring weights for adaptive sizing, creating a pleasant and dynamic navigation experience.
* **True Slate Midnight Theme**: A gorgeous, eye-safe midnight black visual atmosphere designed to minimize screen glare and power consumption.
* **Touch Target Compliance**: Fully accessible components built according to Material Design 3 guidelines (minimum of 48.dp interactive dimensions, helpful content accessibility tags).
* **Material U Companion Icon**: A fully optimized adaptive launcher icon featuring a clean vector monochrome design for modern Pixel and Android launchers.

---

## 🛠️ Architecture and Stack

Ivy Expense uses industry-standard Android architecture patterns to guarantee safety and performance:

* **Jetpack Compose**: Declarative, responsive UI layer using dynamic compositions.
* **Room Database**: Offline persistence engine leveraging local SQLite tables with indexing.
* **Kotlin Coroutines & Flow**: Performance-first, asynchronous background database streams.
* **Model-View-ViewModel (MVVM)**: Strict clean separation of architectural layers (Ui-State tracking, background ViewModels, dynamic repository abstraction).
* **StateFlow**: Safe asynchronous propagation of data changes to prevent composition lag.

---

## 🚀 Getting Started

### Prerequisites
* **Android SDK**: minimum targeting **API 26 (Android 8.0)**, recommended **API 34 (Android 14)**
* **Java Development Kit**: JDK 17
* **Android Studio**: Ladybug or newer recommended

### Build & Run
First, clone the repository locally:
```bash
git clone https://github.com/yourusername/ivy-expense.git
cd ivy-expense
```

Verify or run build checks directly from the command line:
```bash
# Build the application
gradle assembleDebug

# Compile checks and test suite run
gradle test
```

---

## 💾 Local Safe-Backup Structure Example
Your backups are stored as raw JSON keys. A standard backup string resembles this structured syntax format:
```json
{
  "accounts": [
    { "id": 1, "name": "Main Pocket", "balance": 1500.0, "colorHex": "#3A86FF", "iconName": "AccountBalanceWallet" }
  ],
  "categories": [
    { "id": 1, "name": "Daily Groceries", "limit": 400.0, "colorHex": "#FF006E", "iconName": "ShoppingBag" }
  ],
  "transactions": [ ... ],
  "loans": [ ... ]
}
```

---

## 📜 Copyright License

Copyright © **Ezaan Ahmed**. All Rights Reserved.

*Licensed under standard personal copyrights. Unauthorized duplication, modification, or distribution of application resources compiled here is prohibited.*
