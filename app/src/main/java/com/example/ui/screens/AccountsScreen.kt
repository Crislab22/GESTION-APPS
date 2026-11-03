package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.AccountHolder
import com.example.data.model.BettingAccount
import com.example.data.model.Bookmaker
import com.example.data.model.BookmakerCatalogSeeder
import com.example.data.model.MoneyUtils
import com.example.data.model.SupportedCurrency
import com.example.data.model.VerificationRequirement
import com.example.ui.BetManagerUiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MetricSummaryCard
import com.example.ui.components.SimpleDropdownSelector
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.JetBrainsMonoFontFamily

val allAccountStatuses = listOf(
    "PENDIENTE_APERTURA",
    "PENDIENTE_VERIFICACION",
    "ACTIVA",
    "RESTRICCION_PARCIAL",
    "LIMITADA",
    "SUSPENDIDA",
    "RETIRO_FINAL",
    "CERRADA"
)

@Composable
fun AccountsScreen(
    uiState: BetManagerUiState,
    selectedAccountId: String?,
    onSelectAccount: (String?) -> Unit,
    onSaveAccount: (BettingAccount) -> Unit,
    onUpdateAccountStatus: (
        account: BettingAccount,
        newStatus: String,
        reason: String,
        limitationCategory: String,
        limitationDescription: String,
        closureReason: String,
        finalWithdrawalStatus: String,
        screenshotPath: String
    ) -> Unit,
    onUpdateAccountItf: (String, String) -> Unit,
    onReconcileAccount: (BettingAccount, Long, String) -> Unit,
    onSaveRequirement: (VerificationRequirement) -> Unit,
    onCopyReceiptUri: (Uri) -> String
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf("TODOS") }
    var filterItf by remember { mutableStateOf("TODOS") }
    var sortBy by remember { mutableStateOf("SALDO") } // SALDO, NOMBRE, ESTADO, ACTIVIDAD
    var showCreateDialog by remember { mutableStateOf(false) }

    val selectedAccount = uiState.accounts.find { it.id == selectedAccountId }

    if (selectedAccount != null) {
        BackHandler { onSelectAccount(null) }
        AccountDetailView(
            account = selectedAccount,
            uiState = uiState,
            onBack = { onSelectAccount(null) },
            onUpdateStatus = onUpdateAccountStatus,
            onUpdateItf = onUpdateAccountItf,
            onReconcile = onReconcileAccount,
            onSaveRequirement = onSaveRequirement,
            onCopyReceiptUri = onCopyReceiptUri
        )
    } else {
        val filteredAccounts = uiState.accounts
            .filter { acc ->
                val matchesSearch = searchQuery.isBlank() ||
                    acc.holderName.contains(searchQuery, ignoreCase = true) ||
                    acc.bookmakerName.contains(searchQuery, ignoreCase = true) ||
                    acc.username.contains(searchQuery, ignoreCase = true) ||
                    acc.country.contains(searchQuery, ignoreCase = true)
                val matchesStatus = filterStatus == "TODOS" || acc.accountStatus == filterStatus
                val effectiveItf = if (acc.itfOverride != "HEREDAR") acc.itfOverride else {
                    uiState.bookmakers.find { it.id == acc.bookmakerId }?.itfAvailability ?: "POR_VERIFICAR"
                }
                val matchesItf = filterItf == "TODOS" || effectiveItf == filterItf
                matchesSearch && matchesStatus && matchesItf
            }
            .let { list ->
                when (sortBy) {
                    "SALDO" -> list.sortedByDescending { it.calculatedBalanceCents }
                    "NOMBRE" -> list.sortedBy { "${it.bookmakerName} ${it.holderName}" }
                    "ESTADO" -> list.sortedBy { it.accountStatus }
                    else -> list.sortedByDescending { it.lastActivityDate }
                }
            }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cuentas (${filteredAccounts.size})",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.testTag("add_account_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nueva Cuenta")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Nueva Cuenta")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Buscar por titular, casa, país o usuario") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("account_search_input"),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("TODOS", "ACTIVA", "LIMITADA", "RESTRICCION_PARCIAL", "PENDIENTE_VERIFICACION", "CERRADA").forEach { st ->
                    FilterChip(
                        selected = filterStatus == st,
                        onClick = { filterStatus = st },
                        label = { Text(st.replace("_", " ")) }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Tenis ITF:", style = MaterialTheme.typography.labelLarge)
                listOf("TODOS", "SI", "PARCIAL", "NO", "POR_VERIFICAR").forEach { itf ->
                    FilterChip(
                        selected = filterItf == itf,
                        onClick = { filterItf = itf },
                        label = { Text(itf.replace("_", " ")) }
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("Orden:", style = MaterialTheme.typography.labelLarge)
                listOf("SALDO", "NOMBRE", "ESTADO").forEach { s ->
                    FilterChip(
                        selected = sortBy == s,
                        onClick = { sortBy = s },
                        label = { Text(s) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredAccounts.isEmpty()) {
                EmptyStateCard(
                    title = "No se encontraron cuentas individuales",
                    description = "Vincula una cuenta seleccionando un titular y una casa del catálogo maestro.",
                    icon = Icons.Default.AccountBalance
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredAccounts, key = { it.id }) { account ->
                        val bm = uiState.bookmakers.find { it.id == account.bookmakerId }
                        val effectiveItf = if (account.itfOverride != "HEREDAR") account.itfOverride else (bm?.itfAvailability ?: "POR_VERIFICAR")
                        AccountItemCard(
                            account = account,
                            effectiveItf = effectiveItf,
                            hideAmounts = uiState.hideAmounts,
                            onClick = { onSelectAccount(account.id) }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AccountFormDialog(
            holders = uiState.holders,
            bookmakers = uiState.bookmakers,
            onDismiss = { showCreateDialog = false },
            onConfirm = { newAcc ->
                onSaveAccount(newAcc)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun AccountItemCard(
    account: BettingAccount,
    effectiveItf: String,
    hideAmounts: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("account_card_${account.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${account.bookmakerName} • ${account.holderName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Usuario: ${account.username} (${account.country})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusBadge(account.accountStatus)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Tenis ITF: $effectiveItf",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (effectiveItf == "SI") EmeraldGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (account.limitationCategory.isNotBlank()) {
                        Text(
                            text = "• Límite: ${account.limitationCategory}",
                            style = MaterialTheme.typography.labelSmall,
                            color = CrimsonRed
                        )
                    }
                }
                Text(
                    text = MoneyUtils.formatCents(account.calculatedBalanceCents, account.currency, hideAmounts),
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccountDetailView(
    account: BettingAccount,
    uiState: BetManagerUiState,
    onBack: () -> Unit,
    onUpdateStatus: (
        account: BettingAccount,
        newStatus: String,
        reason: String,
        limitationCategory: String,
        limitationDescription: String,
        closureReason: String,
        finalWithdrawalStatus: String,
        screenshotPath: String
    ) -> Unit,
    onUpdateItf: (String, String) -> Unit,
    onReconcile: (BettingAccount, Long, String) -> Unit,
    onSaveRequirement: (VerificationRequirement) -> Unit,
    onCopyReceiptUri: (Uri) -> String
) {
    var showLimitDialog by remember { mutableStateOf(false) }
    var showCloseDialog by remember { mutableStateOf(false) }
    var showReconcileDialog by remember { mutableStateOf(false) }
    var showRequirementDialog by remember { mutableStateOf(false) }

    val accountTxs = uiState.transactions.filter { it.accountId == account.id }
    val accountHistory = uiState.history.filter { it.entityId == account.id }
    val accountReqs = uiState.requirements.filter { it.accountId == account.id }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
                Column {
                    Text(
                        text = "${account.bookmakerName} (${account.username})",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Titular: ${account.holderName} • ${account.country} • Moneda: ${account.currency}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            StatusBadge(account.accountStatus)
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            maxItemsInEachRow = 2
        ) {
            MetricSummaryCard(
                title = "Saldo Actual Calculado",
                valueText = MoneyUtils.formatCents(account.calculatedBalanceCents, account.currency, uiState.hideAmounts),
                subtitle = "Saldo inicial: ${MoneyUtils.formatCents(account.initialBalanceCents, account.currency, uiState.hideAmounts)}",
                accentColor = EmeraldGreen,
                modifier = Modifier.weight(1f)
            )
            MetricSummaryCard(
                title = "Último Saldo Comprobado",
                valueText = MoneyUtils.formatCents(account.lastVerifiedBalanceCents, account.currency, uiState.hideAmounts),
                subtitle = "Fecha revisión: ${account.lastCheckDate.ifBlank { "Sin conciliar" }}",
                accentColor = CyanAccent,
                modifier = Modifier.weight(1f)
            )
        }

        // Acciones operativas de cuenta: Conciliar, Limitar, Cerrar, ITF Override
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { showReconcileDialog = true },
                modifier = Modifier.testTag("reconcile_account_button")
            ) {
                Icon(Icons.Default.SyncAlt, contentDescription = "Conciliar")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Conciliar Saldo")
            }
            OutlinedButton(
                onClick = { showLimitDialog = true },
                modifier = Modifier.testTag("limit_account_button")
            ) {
                Icon(Icons.Default.Block, contentDescription = "Marcar Limitada", tint = CrimsonRed)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Registrar Limitación / Estado")
            }
            OutlinedButton(
                onClick = { showCloseDialog = true },
                modifier = Modifier.testTag("close_account_button")
            ) {
                Icon(Icons.Default.Lock, contentDescription = "Marcar Cerrada")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Cerrar Cuenta")
            }
            OutlinedButton(
                onClick = { showRequirementDialog = true }
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Requisito KYC")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Añadir Requisito KYC")
            }
        }

        // Sobrescritura individual de Tenis ITF (sin alterar otras cuentas)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Disponibilidad Particular de Tenis ITF en esta Cuenta",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Sobrescribe el valor general de la casa sin alterar las demás cuentas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    listOf("HEREDAR", "SI", "PARCIAL", "NO", "POR_VERIFICAR").forEach { opt ->
                        FilterChip(
                            selected = account.itfOverride == opt,
                            onClick = { onUpdateItf(account.id, opt) },
                            label = { Text(opt.replace("_", " ")) }
                        )
                    }
                }
            }
        }

        if (account.limitationDescription.isNotBlank() || account.closureReason.isNotBlank() || account.screenshotPath.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Documentación de Restricción / Cierre", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (account.limitationDate.isNotBlank()) {
                        Text("Fecha Limitación: ${account.limitationDate} (${account.limitationCategory})")
                    }
                    if (account.limitationDescription.isNotBlank()) {
                        Text("Detalle Restricción: ${account.limitationDescription}")
                    }
                    if (account.closureDate.isNotBlank()) {
                        Text("Fecha Cierre: ${account.closureDate} • Motivo: ${account.closureReason} • Fondos: ${account.finalWithdrawalStatus}")
                    }
                    if (account.screenshotPath.isNotBlank()) {
                        Text("Captura de evidencia adjunta:", style = MaterialTheme.typography.labelLarge)
                        AsyncImage(
                            model = account.screenshotPath,
                            contentDescription = "Evidencia de limitación",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }

        // Requisitos y verificaciones de la cuenta
        Text(
            text = "Requisitos y Verificación (${accountReqs.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        accountReqs.forEach { req ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(req.requirementType, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text("Solicitud: ${req.requestDate} • Límite: ${req.dueDate.ifBlank { "N/A" }} • Prioridad: ${req.priority}", style = MaterialTheme.typography.bodyMedium)
                        if (req.notes.isNotBlank()) Text(req.notes, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    StatusBadge(req.status)
                }
            }
        }

        // Historial de cambios de estado
        Text(
            text = "Historial de Cambios de Estado (${accountHistory.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        accountHistory.forEach { h ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("${h.previousState} → ${h.newState} (${h.changeDate})", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text(h.reason, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Libro de movimientos de la cuenta
        Text(
            text = "Libro de Movimientos de la Cuenta (${accountTxs.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        accountTxs.forEach { tx ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(tx.type, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text("${tx.transactionDate} • ${tx.paymentMethod}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (tx.notes.isNotBlank()) {
                            Text(tx.notes, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        StatusBadge(tx.status)
                        Text(
                            text = MoneyUtils.formatCents(tx.amountCents, tx.currency, uiState.hideAmounts),
                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showLimitDialog) {
        LimitOrStateChangeDialog(
            account = account,
            onCopyReceiptUri = onCopyReceiptUri,
            onDismiss = { showLimitDialog = false },
            onConfirm = { st, reason, cat, desc, imgPath ->
                onUpdateStatus(account, st, reason, cat, desc, "", "", imgPath)
                showLimitDialog = false
            }
        )
    }

    if (showCloseDialog) {
        CloseAccountDialog(
            account = account,
            onDismiss = { showCloseDialog = false },
            onConfirm = { reason, withdrawalStatus ->
                onUpdateStatus(account, "CERRADA", reason, "", "", reason, withdrawalStatus, "")
                showCloseDialog = false
            }
        )
    }

    if (showReconcileDialog) {
        ReconcileAccountDialog(
            account = account,
            onDismiss = { showReconcileDialog = false },
            onConfirm = { observedCents, reason ->
                onReconcile(account, observedCents, reason)
                showReconcileDialog = false
            }
        )
    }

    if (showRequirementDialog) {
        AddRequirementDialog(
            account = account,
            onDismiss = { showRequirementDialog = false },
            onConfirm = { req ->
                onSaveRequirement(req)
                showRequirementDialog = false
            }
        )
    }
}

@Composable
fun AccountFormDialog(
    holders: List<AccountHolder>,
    bookmakers: List<Bookmaker>,
    onDismiss: () -> Unit,
    onConfirm: (BettingAccount) -> Unit
) {
    var selectedHolder by remember { mutableStateOf(holders.firstOrNull()) }
    var selectedBookmaker by remember { mutableStateOf(bookmakers.firstOrNull()) }
    var username by remember { mutableStateOf("") }
    var internalAlias by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(selectedBookmaker?.primaryCurrency ?: "PEN") }
    var openDate by remember { mutableStateOf("2026-11-03") }
    var initialBalanceStr by remember { mutableStateOf("0.00") }
    var accountStatus by remember { mutableStateOf("ACTIVA") }
    var itfOverride by remember { mutableStateOf("HEREDAR") }
    var notes by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva Cuenta Individual") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (errorText != null) {
                    Text(errorText!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
                if (holders.isEmpty()) {
                    Text("Primero debes registrar al menos un Titular en la pestaña Titulares.", color = CrimsonRed)
                } else {
                    SimpleDropdownSelector(
                        label = "Titular *",
                        selectedValue = selectedHolder?.name ?: "",
                        options = holders.map { it.name },
                        onValueSelected = { name ->
                            selectedHolder = holders.find { it.name == name }
                        }
                    )
                }
                if (bookmakers.isNotEmpty()) {
                    SimpleDropdownSelector(
                        label = "Casa de Apuestas *",
                        selectedValue = selectedBookmaker?.let { "${it.brandName} (${it.country})" } ?: "",
                        options = bookmakers.map { "${it.brandName} (${it.country})" },
                        onValueSelected = { label ->
                            val bm = bookmakers.find { "${it.brandName} (${it.country})" == label }
                            selectedBookmaker = bm
                            if (bm != null) currency = bm.primaryCurrency
                        }
                    )
                }
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Nombre de usuario en la casa *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("account_username_input")
                )
                OutlinedTextField(
                    value = internalAlias,
                    onValueChange = { internalAlias = it },
                    label = { Text("Alias interno (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                SimpleDropdownSelector(
                    label = "Moneda",
                    selectedValue = currency,
                    options = SupportedCurrency.entries.map { it.code },
                    onValueSelected = { currency = it }
                )
                OutlinedTextField(
                    value = initialBalanceStr,
                    onValueChange = { initialBalanceStr = it },
                    label = { Text("Saldo inicial (ej. 0.00 o 100.00)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                SimpleDropdownSelector(
                    label = "Estado inicial",
                    selectedValue = accountStatus,
                    options = allAccountStatuses,
                    onValueSelected = { accountStatus = it }
                )
                SimpleDropdownSelector(
                    label = "Disponibilidad Tenis ITF",
                    selectedValue = itfOverride,
                    options = listOf("HEREDAR", "SI", "PARCIAL", "NO", "POR_VERIFICAR"),
                    onValueSelected = { itfOverride = it }
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observaciones personales") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val holder = selectedHolder
                    val bm = selectedBookmaker
                    val initCents = MoneyUtils.parseToCents(initialBalanceStr)
                    if (holder == null) {
                        errorText = "Selecciona un titular válido."
                        return@Button
                    }
                    if (bm == null) {
                        errorText = "Selecciona una casa de apuestas del catálogo."
                        return@Button
                    }
                    if (username.isBlank()) {
                        errorText = "Ingresa el nombre de usuario de la cuenta."
                        return@Button
                    }
                    if (initCents == null || initCents < 0) {
                        errorText = "Ingresa un saldo inicial válido."
                        return@Button
                    }
                    onConfirm(
                        BettingAccount(
                            holderId = holder.id,
                            holderName = holder.name,
                            bookmakerId = bm.id,
                            bookmakerName = bm.brandName,
                            country = bm.country,
                            username = username.trim(),
                            internalAlias = internalAlias.trim(),
                            currency = currency,
                            openDate = openDate,
                            initialBalanceCents = initCents,
                            calculatedBalanceCents = initCents,
                            lastVerifiedBalanceCents = initCents,
                            verificationStatus = "VERIFICADA",
                            itfOverride = itfOverride,
                            accountStatus = accountStatus,
                            notes = notes.trim()
                        )
                    )
                },
                modifier = Modifier.testTag("save_account_button")
            ) {
                Text("Crear Cuenta")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun LimitOrStateChangeDialog(
    account: BettingAccount,
    onCopyReceiptUri: (Uri) -> String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    var newStatus by remember { mutableStateOf("LIMITADA") }
    var category by remember { mutableStateOf("MERCADO") }
    var description by remember { mutableStateOf(account.limitationDescription) }
    var reason by remember { mutableStateOf("") }
    var screenshotPath by remember { mutableStateOf(account.screenshotPath) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            screenshotPath = onCopyReceiptUri(uri)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Actualizar Estado / Registrar Limitación") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SimpleDropdownSelector(
                    label = "Nuevo Estado de Cuenta",
                    selectedValue = newStatus,
                    options = allAccountStatuses,
                    onValueSelected = { newStatus = it }
                )
                SimpleDropdownSelector(
                    label = "Categoría de Restricción",
                    selectedValue = category,
                    options = listOf("MERCADO", "IMPORTE", "CUOTA", "RETIRO", "OTRA"),
                    onValueSelected = { category = it }
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción de la restricción / límite") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Notas / Motivo para historial auditable") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Adjuntar captura")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (screenshotPath.isBlank()) "Adjuntar Captura de Limitación" else "Captura Adjuntada (Cambiar)")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        newStatus,
                        reason.ifBlank { "Cambio a $newStatus ($category)" },
                        category,
                        description,
                        screenshotPath
                    )
                }
            ) {
                Text("Guardar Cambio")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun CloseAccountDialog(
    account: BettingAccount,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    var finalWithdrawalStatus by remember { mutableStateOf("Fondos retirados en su totalidad") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Marcar Cuenta como Cerrada") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Las cuentas cerradas conservan todo su historial y movimientos financieros.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Motivo del cierre *") },
                    modifier = Modifier.fillMaxWidth()
                )
                SimpleDropdownSelector(
                    label = "Estado de retiro de fondos finales",
                    selectedValue = finalWithdrawalStatus,
                    options = listOf(
                        "Fondos retirados en su totalidad",
                        "Retiro final en tránsito",
                        "Saldo retenido en disputa",
                        "Sin saldo al cierre"
                    ),
                    onValueSelected = { finalWithdrawalStatus = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(reason.ifBlank { "Cierre administrativo" }, finalWithdrawalStatus)
                }
            ) {
                Text("Confirmar Cierre")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun ReconcileAccountDialog(
    account: BettingAccount,
    onDismiss: () -> Unit,
    onConfirm: (Long, String) -> Unit
) {
    var observedStr by remember { mutableStateOf(MoneyUtils.centsToDecimalString(account.calculatedBalanceCents)) }
    var reason by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Conciliación de Saldo en Casa") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Saldo Calculado en Libro: ${MoneyUtils.formatCents(account.calculatedBalanceCents, account.currency)}",
                    style = MaterialTheme.typography.labelLarge
                )
                if (errorMsg != null) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error)
                }
                OutlinedTextField(
                    value = observedStr,
                    onValueChange = { observedStr = it },
                    label = { Text("Saldo real observado en la casa (${account.currency})") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Motivo / Observación de conciliación *") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val obs = MoneyUtils.parseToCents(observedStr)
                    if (obs == null || obs < 0) {
                        errorMsg = "Ingresa un saldo observado válido."
                        return@Button
                    }
                    if (reason.isBlank()) {
                        errorMsg = "El motivo es obligatorio para conservar trazabilidad."
                        return@Button
                    }
                    onConfirm(obs, reason.trim())
                }
            ) {
                Text("Conciliar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun AddRequirementDialog(
    account: BettingAccount,
    onDismiss: () -> Unit,
    onConfirm: (VerificationRequirement) -> Unit
) {
    var reqType by remember { mutableStateOf("Verificación de identidad") }
    var priority by remember { mutableStateOf("ALTA") }
    var dueDate by remember { mutableStateOf("2026-11-10") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo Requisito de Verificación") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SimpleDropdownSelector(
                    label = "Tipo de Requisito",
                    selectedValue = reqType,
                    options = listOf(
                        "Verificación de identidad",
                        "Confirmación de dirección",
                        "Verificación de medio de pago",
                        "Revisión de cuenta",
                        "Requisito adicional del operador",
                        "Documentación pendiente",
                        "Consulta de retiro"
                    ),
                    onValueSelected = { reqType = it }
                )
                SimpleDropdownSelector(
                    label = "Prioridad",
                    selectedValue = priority,
                    options = listOf("ALTA", "MEDIA", "BAJA"),
                    onValueSelected = { priority = it }
                )
                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Fecha límite (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas (sin almacenar documentos sensibles)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        VerificationRequirement(
                            accountId = account.id,
                            accountLabel = "${account.bookmakerName} (${account.username})",
                            holderName = account.holderName,
                            bookmakerName = account.bookmakerName,
                            requirementType = reqType,
                            status = "PENDIENTE",
                            requestDate = "2026-11-03",
                            dueDate = dueDate,
                            priority = priority,
                            notes = notes.trim()
                        )
                    )
                }
            ) {
                Text("Guardar Requisito")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
