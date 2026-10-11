package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Party
import com.example.data.model.PaymentMode
import com.example.data.model.TransactionType
import com.example.ui.components.AppLockScreen
import com.example.ui.dialogs.AddPartyDialog
import com.example.ui.dialogs.BillGeneratorDialog
import com.example.ui.dialogs.BusinessCardDialog
import com.example.ui.dialogs.CloudSyncDialog
import com.example.ui.dialogs.KhatabookImportDialog
import com.example.ui.dialogs.LanguageSelectionDialog
import com.example.ui.dialogs.MonthlyReportDialog
import com.example.ui.dialogs.QrStandeeDialog
import com.example.ui.dialogs.QuickDashboardTransactionDialog
import com.example.ui.dialogs.ReminderDialog
import com.example.ui.dialogs.StoreOwnerSetupDialog
import com.example.ui.screens.CashbookScreen
import com.example.ui.screens.ChatbotScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PartyDetailScreen
import com.example.ui.screens.ProfileSettingsScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.KhataViewModel
import com.example.util.BiometricAuthManager
import com.example.util.Localization
import kotlinx.coroutines.launch

enum class KirioTab {
    KHATA,
    CASHBOOK,
    REPORTS,
    AI_MUNSHI,
    PROFILE
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                KirioMainApp()
            }
        }
    }
}

@Composable
fun KirioMainApp(viewModel: KhataViewModel = viewModel()) {
    val profile by viewModel.businessProfile.collectAsState()
    val allParties by viewModel.allParties.collectAsState()
    val filteredParties by viewModel.filteredParties.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val cashbookTransactions by viewModel.cashbookTransactions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.partyFilter.collectAsState()
    val isAppUnlocked by viewModel.isAppUnlocked.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isChatLoading by viewModel.isChatLoading.collectAsState()
    val syncStatusMessage by viewModel.syncStatusMessage.collectAsState()

    val strings = remember(profile.languageCode) {
        Localization.getStrings(profile.languageCode)
    }

    var currentTab by remember { mutableStateOf(KirioTab.KHATA) }
    var selectedPartyId by remember { mutableStateOf<Long?>(null) }

    // Manual override to view Onboarding / App Guide anytime
    var showManualOnboardingGuide by remember { mutableStateOf(false) }

    // Dialog states
    var showAddPartyDialog by remember { mutableStateOf(false) }
    var showQuickEntryDialog by remember { mutableStateOf(false) }
    var showBillGeneratorDialog by remember { mutableStateOf(false) }
    var showQrStandeeDialog by remember { mutableStateOf(false) }
    var showPdfReportDialog by remember { mutableStateOf(false) }
    var showBusinessCardDialog by remember { mutableStateOf(false) }
    var showCloudSyncDialog by remember { mutableStateOf(false) }
    var showImportCsvDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showStoreOwnerDialog by remember { mutableStateOf(false) }
    var reminderTargetParty by remember { mutableStateOf<Party?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // App Lock Check
    if (BiometricAuthManager.isPinConfigured(profile.appPin) && !isAppUnlocked) {
        AppLockScreen(
            businessName = profile.businessName.ifBlank { "My Kirio Store" },
            savedPin = profile.appPin,
            biometricEnabled = profile.biometricEnabled,
            onUnlocked = { viewModel.unlockApp() }
        )
        return
    }

    // First-Time Startup Intro, Instructions & Store Owner Setup Screen
    if (!profile.hasCompletedOnboarding || showManualOnboardingGuide) {
        BackHandler(enabled = showManualOnboardingGuide) {
            showManualOnboardingGuide = false
        }
        OnboardingScreen(
            initialProfile = profile,
            onCompleteSetup = { updatedProfile ->
                viewModel.saveBusinessProfile(updatedProfile)
                showManualOnboardingGuide = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Store owner profile saved!")
                }
            },
            onSkip = {
                viewModel.skipOnboarding()
                showManualOnboardingGuide = false
            }
        )
        return
    }

    // Active Party Detail Screen
    val activeParty = remember(allParties, selectedPartyId) {
        allParties.find { it.id == selectedPartyId }
    }

    if (activeParty != null) {
        val partyTxs by viewModel.getTransactionsForParty(activeParty.id)
            .collectAsState(initial = emptyList())

        PartyDetailScreen(
            party = activeParty,
            transactions = partyTxs,
            profile = profile,
            onBack = { selectedPartyId = null },
            onAddTransaction = { amt, type, mode, desc, bill ->
                viewModel.addPartyTransaction(
                    partyId = activeParty.id,
                    amount = amt,
                    type = type,
                    paymentMode = mode,
                    description = desc,
                    billNumber = bill
                )
            },
            onDeleteTransaction = { tx -> viewModel.deleteTransaction(tx) },
            onDeleteParty = { party ->
                viewModel.deleteParty(party) {
                    selectedPartyId = null
                }
            },
            onSendReminder = { party -> reminderTargetParty = party }
        )
    } else {
        BackHandler(enabled = currentTab != KirioTab.KHATA) {
            currentTab = KirioTab.KHATA
        }

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentTab == KirioTab.KHATA,
                        onClick = { currentTab = KirioTab.KHATA },
                        icon = { Icon(Icons.Default.Book, contentDescription = strings.homeTitle) },
                        label = { Text(strings.homeTitle) },
                        modifier = Modifier.testTag("nav_tab_khata")
                    )
                    NavigationBarItem(
                        selected = currentTab == KirioTab.CASHBOOK,
                        onClick = { currentTab = KirioTab.CASHBOOK },
                        icon = {
                            Icon(
                                Icons.Default.AccountBalanceWallet,
                                contentDescription = strings.cashbookTitle
                            )
                        },
                        label = { Text(strings.cashbookTitle) },
                        modifier = Modifier.testTag("nav_tab_cashbook")
                    )
                    NavigationBarItem(
                        selected = currentTab == KirioTab.REPORTS,
                        onClick = { currentTab = KirioTab.REPORTS },
                        icon = { Icon(Icons.Default.Assessment, contentDescription = strings.reportsTitle) },
                        label = { Text(strings.reportsTitle) },
                        modifier = Modifier.testTag("nav_tab_reports")
                    )
                    NavigationBarItem(
                        selected = currentTab == KirioTab.AI_MUNSHI,
                        onClick = { currentTab = KirioTab.AI_MUNSHI },
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = strings.aiMunshiTitle) },
                        label = { Text(strings.aiMunshiTitle) },
                        modifier = Modifier.testTag("nav_tab_ai")
                    )
                    NavigationBarItem(
                        selected = currentTab == KirioTab.PROFILE,
                        onClick = { currentTab = KirioTab.PROFILE },
                        icon = { Icon(Icons.Default.Person, contentDescription = strings.profileTitle) },
                        label = { Text("More") },
                        modifier = Modifier.testTag("nav_tab_profile")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    KirioTab.KHATA -> HomeScreen(
                        profile = profile,
                        allParties = allParties,
                        filteredParties = filteredParties,
                        searchQuery = searchQuery,
                        selectedFilter = selectedFilter,
                        strings = strings,
                        onSearchQueryChange = viewModel::setSearchQuery,
                        onFilterChange = viewModel::setPartyFilter,
                        onPartyClick = { party -> selectedPartyId = party.id },
                        onRemindParty = { party -> reminderTargetParty = party },
                        onAddPartyClick = { showAddPartyDialog = true },
                        onQuickEntryClick = { showQuickEntryDialog = true },
                        onGenerateBillClick = { showBillGeneratorDialog = true },
                        onQrStandeeClick = { showQrStandeeDialog = true },
                        onPdfReportClick = { showPdfReportDialog = true },
                        onBusinessCardClick = { showBusinessCardDialog = true },
                        onCloudSyncClick = { showCloudSyncDialog = true },
                        onImportCsvClick = { showImportCsvDialog = true },
                        onLanguageClick = { showLanguageDialog = true },
                        onSetupStoreDetailsClick = { showStoreOwnerDialog = true },
                        onShowIntroGuideClick = { showManualOnboardingGuide = true }
                    )

                    KirioTab.CASHBOOK -> CashbookScreen(
                        cashbookEntries = cashbookTransactions,
                        currencySymbol = profile.currencySymbol.ifBlank { "₹" },
                        onAddCashbookEntry = { amt, isIn, mode, desc, partyName, bill ->
                            viewModel.addCashbookEntry(amt, isIn, mode, desc, partyName, bill)
                        },
                        onDeleteEntry = viewModel::deleteTransaction
                    )

                    KirioTab.REPORTS -> ReportsScreen(
                        profile = profile,
                        parties = allParties,
                        transactions = allTransactions,
                        onDownloadPdfClick = { showPdfReportDialog = true },
                        onDeleteTransaction = viewModel::deleteTransaction
                    )

                    KirioTab.AI_MUNSHI -> ChatbotScreen(
                        messages = chatMessages,
                        isLoading = isChatLoading,
                        onSendMessage = viewModel::sendChatMessage,
                        onClearChat = viewModel::clearChatHistory
                    )

                    KirioTab.PROFILE -> ProfileSettingsScreen(
                        profile = profile,
                        onSaveProfile = { updated ->
                            viewModel.saveBusinessProfile(updated)
                            if (BiometricAuthManager.isPinConfigured(updated.appPin)) {
                                viewModel.unlockApp()
                            }
                        },
                        onOpenBusinessCard = { showBusinessCardDialog = true },
                        onOpenQrStandee = { showQrStandeeDialog = true },
                        onOpenCloudSync = { showCloudSyncDialog = true },
                        onOpenImportCsv = { showImportCsvDialog = true },
                        onOpenLanguageSelector = { showLanguageDialog = true },
                        onLockAppNow = { viewModel.lockApp() },
                        onOpenAppGuide = { showManualOnboardingGuide = true }
                    )
                }
            }
        }
    }

    // Dialogs
    if (showStoreOwnerDialog) {
        StoreOwnerSetupDialog(
            profile = profile,
            onDismiss = { showStoreOwnerDialog = false },
            onSave = { updated ->
                viewModel.saveBusinessProfile(updated)
                showStoreOwnerDialog = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Store owner details updated!")
                }
            }
        )
    }

    if (showAddPartyDialog) {
        AddPartyDialog(
            onDismiss = { showAddPartyDialog = false },
            onSave = { name, phone, type, addr, gst, notes, openBal, isGave ->
                viewModel.addParty(name, phone, type, addr, gst, notes, openBal, isGave) {
                    showAddPartyDialog = false
                }
            }
        )
    }

    if (showQuickEntryDialog) {
        QuickDashboardTransactionDialog(
            parties = allParties,
            currencySymbol = profile.currencySymbol.ifBlank { "₹" },
            onDismiss = { showQuickEntryDialog = false },
            onSave = { partyId, amt, type, mode, desc ->
                viewModel.addPartyTransaction(partyId, amt, type, mode, desc)
                showQuickEntryDialog = false
            }
        )
    }

    if (showBillGeneratorDialog) {
        BillGeneratorDialog(
            profile = profile,
            parties = allParties,
            onDismiss = { showBillGeneratorDialog = false },
            onRecordBillToKhata = { partyId, amt, billNo, summary, isCredit ->
                if (isCredit && partyId != null) {
                    viewModel.addPartyTransaction(
                        partyId = partyId,
                        amount = amt,
                        type = TransactionType.YOU_GAVE,
                        paymentMode = PaymentMode.CASH,
                        description = "GST Invoice $billNo",
                        billNumber = billNo,
                        itemsSummary = summary
                    )
                } else {
                    viewModel.addCashbookEntry(
                        amount = amt,
                        isCashIn = true,
                        paymentMode = PaymentMode.CASH,
                        description = "Paid Invoice $billNo ($summary)",
                        partyName = allParties.find { it.id == partyId }?.name ?: "Walk-in Sale",
                        billNumber = billNo
                    )
                }
                showBillGeneratorDialog = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Invoice $billNo recorded to ledger!")
                }
            }
        )
    }

    if (showQrStandeeDialog) {
        QrStandeeDialog(
            profile = profile,
            onDismiss = { showQrStandeeDialog = false },
            onSaveUpiId = { newUpi ->
                viewModel.saveBusinessProfile(profile.copy(upiId = newUpi))
            }
        )
    }

    if (showPdfReportDialog) {
        MonthlyReportDialog(
            profile = profile,
            parties = allParties,
            transactions = allTransactions,
            onDismiss = { showPdfReportDialog = false }
        )
    }

    if (showBusinessCardDialog) {
        BusinessCardDialog(
            profile = profile,
            onDismiss = { showBusinessCardDialog = false }
        )
    }

    if (showCloudSyncDialog) {
        CloudSyncDialog(
            profile = profile,
            partiesCount = allParties.size,
            transactionsCount = allTransactions.size,
            syncStatusMessage = syncStatusMessage,
            onBackupNow = { viewModel.performCloudBackup() },
            onRestoreNow = { viewModel.performCloudRestore() },
            onDismiss = { showCloudSyncDialog = false }
        )
    }

    if (showImportCsvDialog) {
        KhatabookImportDialog(
            onDismiss = { showImportCsvDialog = false },
            onImportRecords = { records ->
                viewModel.importKhatabookRecords(records) { count ->
                    showImportCsvDialog = false
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Imported $count entries into Kirio!")
                    }
                }
            }
        )
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguageCode = profile.languageCode,
            onSelectLanguage = { code -> viewModel.updateLanguage(code) },
            onDismiss = { showLanguageDialog = false }
        )
    }

    if (reminderTargetParty != null) {
        ReminderDialog(
            party = reminderTargetParty!!,
            profile = profile,
            onDismiss = { reminderTargetParty = null }
        )
    }
}
