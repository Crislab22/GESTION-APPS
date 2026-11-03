package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AccountHolder
import com.example.data.model.BookmakerCatalogSeeder
import com.example.data.model.MoneyUtils
import com.example.data.repository.FinancialCalculator
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

@Composable
fun HoldersScreen(
    uiState: BetManagerUiState,
    selectedHolderId: String?,
    onSelectHolder: (String?) -> Unit,
    onSaveHolder: (AccountHolder) -> Unit,
    onOpenAccountDetail: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf("TODOS") }
    var showHolderDialog by remember { mutableStateOf(false) }
    var editingHolder by remember { mutableStateOf<AccountHolder?>(null) }

    val selectedHolder = uiState.holders.find { it.id == selectedHolderId }

    if (selectedHolder != null) {
        BackHandler { onSelectHolder(null) }
        HolderDetailView(
            holder = selectedHolder,
            uiState = uiState,
            onBack = { onSelectHolder(null) },
            onEditHolder = {
                editingHolder = selectedHolder
                showHolderDialog = true
            },
            onOpenAccountDetail = onOpenAccountDetail
        )
    } else {
        val filteredHolders = uiState.holders.filter { h ->
            val matchesQuery = searchQuery.isBlank() ||
                h.name.contains(searchQuery, ignoreCase = true) ||
                h.country.contains(searchQuery, ignoreCase = true) ||
                h.contactInfo.contains(searchQuery, ignoreCase = true)
            val matchesStatus = filterStatus == "TODOS" || h.status == filterStatus
            matchesQuery && matchesStatus
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
                    text = "Titulares (${filteredHolders.size})",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = {
                        editingHolder = null
                        showHolderDialog = true
                    },
                    modifier = Modifier.testTag("add_holder_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nuevo Titular")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Nuevo Titular")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Buscar por nombre, alias o país") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("holder_search_input"),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("TODOS", "ACTIVO", "INACTIVO", "ARCHIVADO").forEach { st ->
                    FilterChip(
                        selected = filterStatus == st,
                        onClick = { filterStatus = st },
                        label = { Text(st) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (filteredHolders.isEmpty()) {
                EmptyStateCard(
                    title = "No hay titulares registrados",
                    description = "Agrega titulares propios o terceros autorizados para vincular cuentas individuales y gestionar comisiones.",
                    icon = Icons.Default.People
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredHolders, key = { it.id }) { holder ->
                        val holderAccounts = uiState.accounts.filter { it.holderId == holder.id }
                        val totalBalanceCents = holderAccounts.sumOf { it.calculatedBalanceCents }
                        val primaryCurr = holderAccounts.firstOrNull()?.currency ?: uiState.baseCurrency
                        HolderListItemCard(
                            holder = holder,
                            accountsCount = holderAccounts.size,
                            totalBalanceCents = totalBalanceCents,
                            currency = primaryCurr,
                            hideAmounts = uiState.hideAmounts,
                            onClick = { onSelectHolder(holder.id) }
                        )
                    }
                }
            }
        }
    }

    if (showHolderDialog) {
        HolderFormDialog(
            initial = editingHolder,
            onDismiss = { showHolderDialog = false },
            onConfirm = { newHolder ->
                onSaveHolder(newHolder)
                showHolderDialog = false
            }
        )
    }
}

@Composable
private fun HolderListItemCard(
    holder: AccountHolder,
    accountsCount: Int,
    totalBalanceCents: Long,
    currency: String,
    hideAmounts: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("holder_card_${holder.id}"),
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
                        text = holder.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${holder.country} • ${holder.holderType} • Comisión ${holder.commissionPct}% (${holder.settlementPeriod})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusBadge(holder.status)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$accountsCount cuentas asociadas",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Saldo: ${MoneyUtils.formatCents(totalBalanceCents, currency, hideAmounts)}",
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HolderDetailView(
    holder: AccountHolder,
    uiState: BetManagerUiState,
    onBack: () -> Unit,
    onEditHolder: () -> Unit,
    onOpenAccountDetail: (String) -> Unit
) {
    val holderAccounts = uiState.accounts.filter { it.holderId == holder.id }
    val holderTransactions = uiState.transactions.filter { it.holderId == holder.id }
    val holderCommissions = uiState.commissions.filter { it.holderId == holder.id }

    val activeCount = holderAccounts.count { it.accountStatus == "ACTIVA" }
    val limitedCount = holderAccounts.count { it.accountStatus == "LIMITADA" || it.accountStatus == "RESTRICCION_PARCIAL" }
    val closedCount = holderAccounts.count { it.accountStatus == "CERRADA" }

    val totalDepositedCents = holderTransactions
        .filter { it.type == "DEPOSITO" && it.status == "COMPLETADO" }
        .sumOf { it.amountCents }
    val totalWithdrawnCents = holderTransactions
        .filter { (it.type == "RETIRO" || it.type == "RETIRO_COMPLETADO") && it.status == "COMPLETADO" }
        .sumOf { it.amountCents }
    val netProfitCents = FinancialCalculator.calculateNetBettingProfitCents(holderTransactions)
    val generatedCommCents = holderCommissions.sumOf { it.generatedCommissionCents }
    val paidCommCents = holderCommissions.sumOf { it.paidAmountCents }
    val pendingCommCents = holderCommissions.sumOf { it.pendingAmountCents }
    val currency = holderAccounts.firstOrNull()?.currency ?: uiState.baseCurrency

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
                        text = holder.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${holder.country} • ${holder.holderType} • Incorporación: ${holder.joinDate}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            OutlinedButton(onClick = onEditHolder) {
                Icon(Icons.Default.Edit, contentDescription = "Editar Titular")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Editar")
            }
        }

        // Saldos por moneda del titular
        val balancesByCurr = holderAccounts.groupBy { it.currency }
            .mapValues { entry -> entry.value.sumOf { it.calculatedBalanceCents } }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            maxItemsInEachRow = 2
        ) {
            MetricSummaryCard(
                title = "Saldo en Casas",
                valueText = if (balancesByCurr.isEmpty()) MoneyUtils.formatCents(0, currency, uiState.hideAmounts)
                else balancesByCurr.entries.joinToString(" | ") { (c, v) -> MoneyUtils.formatCents(v, c, uiState.hideAmounts) },
                subtitle = "Activas: $activeCount • Limitadas: $limitedCount • Cerradas: $closedCount",
                accentColor = EmeraldGreen,
                modifier = Modifier.weight(1f)
            )
            MetricSummaryCard(
                title = "Beneficio / Pérdida Acum.",
                valueText = MoneyUtils.formatCents(netProfitCents, currency, uiState.hideAmounts),
                subtitle = "Dep: ${MoneyUtils.formatCents(totalDepositedCents, currency, uiState.hideAmounts)} | Ret: ${MoneyUtils.formatCents(totalWithdrawnCents, currency, uiState.hideAmounts)}",
                accentColor = if (netProfitCents >= 0) EmeraldGreen else CrimsonRed,
                modifier = Modifier.weight(1f)
            )
            MetricSummaryCard(
                title = "Acuerdo de Comisión",
                valueText = "${holder.commissionPct}% (${holder.settlementPeriod})",
                subtitle = "Modalidad: ${holder.commissionMode}",
                accentColor = CyanAccent,
                modifier = Modifier.weight(1f)
            )
            MetricSummaryCard(
                title = "Comisiones Pendientes",
                valueText = MoneyUtils.formatCents(pendingCommCents, currency, uiState.hideAmounts),
                subtitle = "Generadas: ${MoneyUtils.formatCents(generatedCommCents, currency, uiState.hideAmounts)} | Pagadas: ${MoneyUtils.formatCents(paidCommCents, currency, uiState.hideAmounts)}",
                accentColor = AmberWarning,
                modifier = Modifier.weight(1f)
            )
        }

        if (holder.authorizationRef.isNotBlank() || holder.notes.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    if (holder.authorizationRef.isNotBlank()) {
                        Text("Constancia de Autorización: ${holder.authorizationRef}", style = MaterialTheme.typography.labelLarge)
                    }
                    if (holder.notes.isNotBlank()) {
                        Text("Observaciones: ${holder.notes}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        Text(
            text = "Cuentas de Apuestas del Titular (${holderAccounts.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        if (holderAccounts.isEmpty()) {
            Text("Este titular aún no tiene cuentas registradas.", style = MaterialTheme.typography.bodyMedium)
        } else {
            holderAccounts.forEach { acc ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenAccountDetail(acc.id) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${acc.bookmakerName} (${acc.country})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Usuario: ${acc.username} • ITF: ${acc.itfOverride}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            StatusBadge(acc.accountStatus)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = MoneyUtils.formatCents(acc.calculatedBalanceCents, acc.currency, uiState.hideAmounts),
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = "Historial Financiero del Titular (${holderTransactions.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        holderTransactions.take(25).forEach { tx ->
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
                    Column {
                        Text("${tx.type} • ${tx.bookmakerName}", style = MaterialTheme.typography.labelLarge)
                        Text("${tx.transactionDate} • ${tx.paymentMethod}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
}

@Composable
fun HolderFormDialog(
    initial: AccountHolder?,
    onDismiss: () -> Unit,
    onConfirm: (AccountHolder) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var country by remember { mutableStateOf(initial?.country ?: "Perú") }
    var contactInfo by remember { mutableStateOf(initial?.contactInfo ?: "") }
    var joinDate by remember { mutableStateOf(initial?.joinDate ?: "2026-11-03") }
    var holderType by remember { mutableStateOf(initial?.holderType ?: "TERCERO") }
    var commissionPctStr by remember { mutableStateOf((initial?.commissionPct ?: 20.0).toString()) }
    var commissionMode by remember { mutableStateOf(initial?.commissionMode ?: "PORCENTAJE_BENEFICIO") }
    var settlementPeriod by remember { mutableStateOf(initial?.settlementPeriod ?: "MENSUAL") }
    var compensateLosses by remember { mutableStateOf(initial?.compensatePreviousLosses ?: true) }
    var status by remember { mutableStateOf(initial?.status ?: "ACTIVO") }
    var authorizationRef by remember { mutableStateOf(initial?.authorizationRef ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Registrar Titular" else "Editar Titular") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (validationError != null) {
                    Text(validationError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre o alias *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("holder_name_input")
                )
                SimpleDropdownSelector(
                    label = "País",
                    selectedValue = country,
                    options = BookmakerCatalogSeeder.supportedCountries,
                    onValueSelected = { country = it }
                )
                SimpleDropdownSelector(
                    label = "Tipo de Titular",
                    selectedValue = holderType,
                    options = listOf("PROPIA", "TERCERO"),
                    onValueSelected = { holderType = it }
                )
                OutlinedTextField(
                    value = contactInfo,
                    onValueChange = { contactInfo = it },
                    label = { Text("Datos de contacto (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = joinDate,
                    onValueChange = { joinDate = it },
                    label = { Text("Fecha de incorporación (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = commissionPctStr,
                    onValueChange = { commissionPctStr = it },
                    label = { Text("Porcentaje de comisión acordado (%)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                SimpleDropdownSelector(
                    label = "Modalidad de comisión",
                    selectedValue = commissionMode,
                    options = listOf("PORCENTAJE_BENEFICIO", "FIJO_PERIODO", "PERSONALIZADO"),
                    onValueSelected = { commissionMode = it }
                )
                SimpleDropdownSelector(
                    label = "Periodicidad de liquidación",
                    selectedValue = settlementPeriod,
                    options = listOf("SEMANAL", "QUINCENAL", "MENSUAL", "MANUAL"),
                    onValueSelected = { settlementPeriod = it }
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = compensateLosses, onCheckedChange = { compensateLosses = it })
                    Text("Compensar pérdidas previas antes de generar comisión", style = MaterialTheme.typography.bodyMedium)
                }
                SimpleDropdownSelector(
                    label = "Estado",
                    selectedValue = status,
                    options = listOf("ACTIVO", "INACTIVO", "ARCHIVADO"),
                    onValueSelected = { status = it }
                )
                OutlinedTextField(
                    value = authorizationRef,
                    onValueChange = { authorizationRef = it },
                    label = { Text("Constancia opcional de autorización") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observaciones") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedName = name.trim()
                    val pct = commissionPctStr.toDoubleOrNull()
                    if (trimmedName.isEmpty()) {
                        validationError = "Ingresa el nombre o alias del titular."
                        return@Button
                    }
                    if (pct == null || pct < 0.0 || pct > 100.0) {
                        validationError = "Ingresa un porcentaje de comisión válido (0 a 100)."
                        return@Button
                    }
                    onConfirm(
                        (initial ?: AccountHolder()).copy(
                            name = trimmedName,
                            country = country,
                            contactInfo = contactInfo.trim(),
                            joinDate = joinDate.trim().ifBlank { "2026-11-03" },
                            holderType = holderType,
                            commissionPct = pct,
                            commissionMode = commissionMode,
                            settlementPeriod = settlementPeriod,
                            compensatePreviousLosses = compensateLosses,
                            status = status,
                            authorizationRef = authorizationRef.trim(),
                            notes = notes.trim()
                        )
                    )
                },
                modifier = Modifier.testTag("save_holder_button")
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
