package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.repository.BetManagerRepository
import com.example.ui.BetManagerViewModel
import com.example.ui.MainTab
import com.example.ui.MoreSection
import com.example.ui.screens.AccountFormDialog
import com.example.ui.screens.AccountsScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HolderFormDialog
import com.example.ui.screens.HoldersScreen
import com.example.ui.screens.MoreHubScreen
import com.example.ui.screens.TransactionFormDialog
import com.example.ui.screens.TransactionsScreen
import com.example.ui.screens.WalletFormDialog
import com.example.ui.screens.signOutAdmin
import com.example.ui.theme.BetManagerTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BetManagerTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser }
        )
    } else {
        AuthenticatedBetManagerApp(
            currentUserId = user.uid,
            onSignedOut = { currentUser = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticatedBetManagerApp(
    currentUserId: String,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val viewModel: BetManagerViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                BetManagerViewModel(BetManagerRepository(db), currentUserId)
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val activeMoreSection by viewModel.activeMoreSection.collectAsStateWithLifecycle()
    val selectedHolderId by viewModel.selectedHolderId.collectAsStateWithLifecycle()
    val selectedAccountId by viewModel.selectedAccountId.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // Quick Action Dialog triggers from Dashboard
    var quickTxDialogType by remember { mutableStateOf<String?>(null) }
    var showQuickHolderDialog by remember { mutableStateOf(false) }
    var showQuickAccountDialog by remember { mutableStateOf(false) }
    var showQuickWalletDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.statusMessage, uiState.errorMessage) {
        val msg = uiState.errorMessage ?: uiState.statusMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    if (activeTab != MainTab.INICIO) {
        BackHandler {
            viewModel.selectTab(MainTab.INICIO)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "BetManager Pro",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    Row {
                        IconButton(
                            onClick = { viewModel.toggleHideAmounts() },
                            modifier = Modifier.testTag("toggle_hide_amounts_button")
                        ) {
                            Icon(
                                imageVector = if (uiState.hideAmounts) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (uiState.hideAmounts) "Mostrar importes" else "Ocultar importes"
                            )
                        }
                        IconButton(
                            onClick = { viewModel.selectMoreSection(MoreSection.ALERTAS) },
                            modifier = Modifier.testTag("top_alerts_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Alertas operativas"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = activeTab == MainTab.INICIO,
                    onClick = { viewModel.selectTab(MainTab.INICIO) },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Inicio") },
                    label = { Text("Inicio") },
                    modifier = Modifier.testTag("nav_inicio")
                )
                NavigationBarItem(
                    selected = activeTab == MainTab.CUENTAS,
                    onClick = { viewModel.selectTab(MainTab.CUENTAS) },
                    icon = { Icon(Icons.Default.AccountBalance, contentDescription = "Cuentas") },
                    label = { Text("Cuentas") },
                    modifier = Modifier.testTag("nav_cuentas")
                )
                NavigationBarItem(
                    selected = activeTab == MainTab.MOVIMIENTOS,
                    onClick = { viewModel.selectTab(MainTab.MOVIMIENTOS) },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Movimientos") },
                    label = { Text("Movimientos") },
                    modifier = Modifier.testTag("nav_movimientos")
                )
                NavigationBarItem(
                    selected = activeTab == MainTab.TITULARES,
                    onClick = { viewModel.selectTab(MainTab.TITULARES) },
                    icon = { Icon(Icons.Default.People, contentDescription = "Titulares") },
                    label = { Text("Titulares") },
                    modifier = Modifier.testTag("nav_titulares")
                )
                NavigationBarItem(
                    selected = activeTab == MainTab.MAS,
                    onClick = { viewModel.selectTab(MainTab.MAS) },
                    icon = { Icon(Icons.Default.Menu, contentDescription = "Más") },
                    label = { Text("Más") },
                    modifier = Modifier.testTag("nav_mas")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                MainTab.INICIO -> DashboardScreen(
                    uiState = uiState,
                    onQuickDeposit = { quickTxDialogType = "DEPOSITO" },
                    onQuickWithdrawal = { quickTxDialogType = "RETIRO_PENDIENTE" },
                    onQuickNewAccount = { showQuickAccountDialog = true },
                    onQuickReconcile = { viewModel.selectTab(MainTab.CUENTAS) },
                    onQuickRecordProfit = { quickTxDialogType = "RESULTADO_POSITIVO" },
                    onQuickNewHolder = { showQuickHolderDialog = true },
                    onQuickNewWallet = { showQuickWalletDialog = true },
                    onPeriodSelected = { viewModel.setSelectedPeriodDays(it) }
                )
                MainTab.CUENTAS -> AccountsScreen(
                    uiState = uiState,
                    selectedAccountId = selectedAccountId,
                    onSelectAccount = { viewModel.selectAccountDetail(it) },
                    onSaveAccount = { viewModel.saveAccount(it) },
                    onUpdateAccountStatus = { acc, st, reason, cat, desc, closeReason, finalStatus, img ->
                        viewModel.updateAccountStatus(acc, st, reason, cat, desc, closeReason, finalStatus, img)
                    },
                    onUpdateAccountItf = { id, itf -> viewModel.updateAccountItf(id, itf) },
                    onReconcileAccount = { acc, obs, reason -> viewModel.reconcileAccount(acc, obs, reason) },
                    onSaveRequirement = { viewModel.saveRequirement(it) },
                    onCopyReceiptUri = { uri -> viewModel.copyUriToInternalStorage(context, uri) }
                )
                MainTab.MOVIMIENTOS -> TransactionsScreen(
                    uiState = uiState,
                    onRecordTransaction = { viewModel.recordTransaction(it) },
                    onUpdateTransactionStatus = { id, st, type -> viewModel.updateTransactionStatus(id, st, type) },
                    onSaveWallet = { viewModel.saveWallet(it) },
                    onCopyReceiptUri = { uri -> viewModel.copyUriToInternalStorage(context, uri) }
                )
                MainTab.TITULARES -> HoldersScreen(
                    uiState = uiState,
                    selectedHolderId = selectedHolderId,
                    onSelectHolder = { viewModel.selectHolderDetail(it) },
                    onSaveHolder = { viewModel.saveHolder(it) },
                    onOpenAccountDetail = { accId ->
                        viewModel.selectTab(MainTab.CUENTAS)
                        viewModel.selectAccountDetail(accId)
                    }
                )
                MainTab.MAS -> MoreHubScreen(
                    uiState = uiState,
                    activeSection = activeMoreSection,
                    onSelectSection = { viewModel.selectMoreSection(it) },
                    onSaveBookmaker = { viewModel.saveBookmaker(it) },
                    onSaveRelation = { viewModel.saveRelation(it) },
                    onDeleteRelation = { viewModel.deleteRelation(it) },
                    onSaveWallet = { viewModel.saveWallet(it) },
                    onGenerateCommission = { h, p, c, adj, r ->
                        viewModel.generateCommissionSettlement(h, p, c, adj, r)
                    },
                    onPayCommission = { s, paid, m, wId, wName ->
                        viewModel.payCommission(s, paid, m, wId, wName)
                    },
                    onSaveRequirement = { viewModel.saveRequirement(it) },
                    onSaveExchangeRate = { viewModel.saveExchangeRate(it) },
                    onDismissAlert = { viewModel.dismissAlert(it) },
                    onGenerateExport = { viewModel.generateExportContent(it) },
                    onCopyReceiptUri = { uri -> viewModel.copyUriToInternalStorage(context, uri) },
                    onSignOut = {
                        signOutAdmin(context, onSignOutComplete = onSignedOut, scope = scope)
                    }
                )
            }
        }
    }

    if (quickTxDialogType != null) {
        TransactionFormDialog(
            initialType = quickTxDialogType!!,
            accounts = uiState.accounts,
            wallets = uiState.wallets,
            onCopyReceiptUri = { uri -> viewModel.copyUriToInternalStorage(context, uri) },
            onDismiss = { quickTxDialogType = null },
            onConfirm = { tx ->
                viewModel.recordTransaction(tx)
                quickTxDialogType = null
            }
        )
    }

    if (showQuickHolderDialog) {
        HolderFormDialog(
            initial = null,
            onDismiss = { showQuickHolderDialog = false },
            onConfirm = { h ->
                viewModel.saveHolder(h)
                showQuickHolderDialog = false
            }
        )
    }

    if (showQuickAccountDialog) {
        AccountFormDialog(
            holders = uiState.holders,
            bookmakers = uiState.bookmakers,
            onDismiss = { showQuickAccountDialog = false },
            onConfirm = { acc ->
                viewModel.saveAccount(acc)
                showQuickAccountDialog = false
            }
        )
    }

    if (showQuickWalletDialog) {
        WalletFormDialog(
            onDismiss = { showQuickWalletDialog = false },
            onConfirm = { w ->
                viewModel.saveWallet(w)
                showQuickWalletDialog = false
            }
        )
    }
}
