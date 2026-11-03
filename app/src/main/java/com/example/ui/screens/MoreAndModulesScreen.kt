package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AccountHolder
import com.example.data.model.AlertReminder
import com.example.data.model.Bookmaker
import com.example.data.model.BookmakerRelation
import com.example.data.model.CommissionSettlement
import com.example.data.model.ExchangeRateRecord
import com.example.data.model.MoneyUtils
import com.example.data.model.SupportedCurrency
import com.example.data.model.VerificationRequirement
import com.example.data.model.Wallet
import com.example.ui.BetManagerUiState
import com.example.ui.MoreSection
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SimpleDropdownSelector
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun MoreHubScreen(
    uiState: BetManagerUiState,
    activeSection: MoreSection,
    onSelectSection: (MoreSection) -> Unit,
    onSaveBookmaker: (Bookmaker) -> Unit,
    onSaveRelation: (BookmakerRelation) -> Unit,
    onDeleteRelation: (String) -> Unit,
    onSaveWallet: (Wallet) -> Unit,
    onGenerateCommission: (AccountHolder, String, String, Long, String) -> Unit,
    onPayCommission: (CommissionSettlement, Long, String, String, String) -> Unit,
    onSaveRequirement: (VerificationRequirement) -> Unit,
    onSaveExchangeRate: (ExchangeRateRecord) -> Unit,
    onDismissAlert: (AlertReminder) -> Unit,
    onGenerateExport: (String) -> String,
    onCopyReceiptUri: (Uri) -> String,
    onSignOut: () -> Unit
) {
    when (activeSection) {
        MoreSection.MENU -> MoreMenuOverview(
            uiState = uiState,
            onSelectSection = onSelectSection,
            onSignOut = onSignOut
        )
        MoreSection.CATALOGO_CASAS -> BookmakersCatalogScreen(
            uiState = uiState,
            onBack = { onSelectSection(MoreSection.MENU) },
            onSaveBookmaker = onSaveBookmaker
        )
        MoreSection.CASAS_RELACIONADAS -> BookmakerRelationsScreen(
            uiState = uiState,
            onBack = { onSelectSection(MoreSection.MENU) },
            onSaveRelation = onSaveRelation,
            onDeleteRelation = onDeleteRelation
        )
        MoreSection.BILLETERAS -> WalletsManagementSection(
            uiState = uiState,
            onBack = { onSelectSection(MoreSection.MENU) },
            onSaveWallet = onSaveWallet
        )
        MoreSection.COMISIONES -> CommissionsSection(
            uiState = uiState,
            onBack = { onSelectSection(MoreSection.MENU) },
            onGenerateCommission = onGenerateCommission,
            onPayCommission = onPayCommission
        )
        MoreSection.CONCILIACIONES -> ReconciliationsSection(
            uiState = uiState,
            onBack = { onSelectSection(MoreSection.MENU) }
        )
        MoreSection.REQUISITOS -> RequirementsSection(
            uiState = uiState,
            onBack = { onSelectSection(MoreSection.MENU) },
            onSaveRequirement = onSaveRequirement
        )
        MoreSection.ALERTAS -> AlertsSection(
            uiState = uiState,
            onBack = { onSelectSection(MoreSection.MENU) },
            onDismissAlert = onDismissAlert
        )
        MoreSection.MULTIMONEDA -> MultiCurrencySection(
            uiState = uiState,
            onBack = { onSelectSection(MoreSection.MENU) },
            onSaveExchangeRate = onSaveExchangeRate
        )
        MoreSection.EXPORTACIONES -> ExportsAndBackupSection(
            onBack = { onSelectSection(MoreSection.MENU) },
            onGenerateExport = onGenerateExport
        )
        MoreSection.HISTORIAL, MoreSection.ESTADISTICAS -> AuditHistorySection(
            uiState = uiState,
            onBack = { onSelectSection(MoreSection.MENU) }
        )
    }
}

@Composable
private fun MoreMenuOverview(
    uiState: BetManagerUiState,
    onSelectSection: (MoreSection) -> Unit,
    onSignOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Módulos Avanzados y Administración",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        MoreNavCard(
            title = "Catálogo Maestro de Casas de Apuestas",
            subtitle = "${uiState.bookmakers.size} marcas y dominios por país • Disponibilidad Tenis ITF",
            icon = Icons.Default.Storefront,
            tag = "more_catalog_card",
            onClick = { onSelectSection(MoreSection.CATALOGO_CASAS) }
        )
        MoreNavCard(
            title = "Casas Gemelas y Proveedores",
            subtitle = "${uiState.relations.size} vínculos documentados • Agrupación tecnológica",
            icon = Icons.Default.Hub,
            tag = "more_relations_card",
            onClick = { onSelectSection(MoreSection.CASAS_RELACIONADAS) }
        )
        MoreNavCard(
            title = "Billeteras y Cajas Centrales",
            subtitle = "${uiState.wallets.size} cajas fuera de casas de apuestas",
            icon = Icons.Default.AccountBalanceWallet,
            tag = "more_wallets_card",
            onClick = { onSelectSection(MoreSection.BILLETERAS) }
        )
        MoreNavCard(
            title = "Comisiones y Liquidaciones",
            subtitle = "${uiState.commissions.size} liquidaciones • Cálculo sobre beneficio neto elegible",
            icon = Icons.Default.Payments,
            tag = "more_commissions_card",
            onClick = { onSelectSection(MoreSection.COMISIONES) }
        )
        MoreNavCard(
            title = "Conciliaciones de Saldo",
            subtitle = "${uiState.reconciliations.size} registros de conciliación auditable",
            icon = Icons.Default.Rule,
            tag = "more_reconciliations_card",
            onClick = { onSelectSection(MoreSection.CONCILIACIONES) }
        )
        MoreNavCard(
            title = "Requisitos y Verificaciones KYC",
            subtitle = "${uiState.requirements.count { it.status == "PENDIENTE" }} pendientes de resolver",
            icon = Icons.Default.VerifiedUser,
            tag = "more_requirements_card",
            onClick = { onSelectSection(MoreSection.REQUISITOS) }
        )
        MoreNavCard(
            title = "Bandeja de Alertas y Recordatorios",
            subtitle = "Retiros demorados, cuentas sin conciliar y comisiones",
            icon = Icons.Default.NotificationsActive,
            tag = "more_alerts_card",
            onClick = { onSelectSection(MoreSection.ALERTAS) }
        )
        MoreNavCard(
            title = "Multimoneda y Tipos de Cambio",
            subtitle = "${uiState.exchangeRates.size} tasas registradas (PEN, CLP, COP, ARS, MXN, USD, BOB, EUR)",
            icon = Icons.Default.CurrencyExchange,
            tag = "more_multicurrency_card",
            onClick = { onSelectSection(MoreSection.MULTIMONEDA) }
        )
        MoreNavCard(
            title = "Exportaciones CSV y Respaldo Estructurado",
            subtitle = "Exportar titulares, cuentas, movimientos y respaldo JSON",
            icon = Icons.Default.Download,
            tag = "more_exports_card",
            onClick = { onSelectSection(MoreSection.EXPORTACIONES) }
        )
        MoreNavCard(
            title = "Historial de Auditoría y Cambios de Estado",
            subtitle = "${uiState.history.size} eventos registrados con trazabilidad",
            icon = Icons.Default.History,
            tag = "more_history_card",
            onClick = { onSelectSection(MoreSection.HISTORIAL) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("sign_out_button")
        ) {
            Icon(Icons.Default.Logout, contentDescription = "Cerrar sesión", tint = CrimsonRed)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Cerrar Sesión del Administrador", color = CrimsonRed)
        }
    }
}

@Composable
private fun MoreNavCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(tag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Abrir",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WalletsManagementSection(
    uiState: BetManagerUiState,
    onBack: () -> Unit,
    onSaveWallet: (Wallet) -> Unit
) {
    BackHandler { onBack() }
    var showAdd by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
                Text("Billeteras / Cajas Centrales", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Button(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = "Nueva Caja")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Nueva Caja")
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        if (uiState.wallets.isEmpty()) {
            EmptyStateCard(
                title = "Sin billeteras centrales",
                description = "Crea una billetera o caja central para representar el capital fuera de las casas de apuestas.",
                icon = Icons.Default.AccountBalanceWallet
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(uiState.wallets, key = { it.id }) { w ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("${w.name} (${w.walletType})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Saldo inicial: ${MoneyUtils.formatCents(w.initialBalanceCents, w.currency, uiState.hideAmounts)}", style = MaterialTheme.typography.bodyMedium)
                                if (w.notes.isNotBlank()) Text(w.notes, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = MoneyUtils.formatCents(w.currentBalanceCents, w.currency, uiState.hideAmounts),
                                style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        WalletFormDialog(
            onDismiss = { showAdd = false },
            onConfirm = {
                onSaveWallet(it)
                showAdd = false
            }
        )
    }
}

@Composable
private fun CommissionsSection(
    uiState: BetManagerUiState,
    onBack: () -> Unit,
    onGenerateCommission: (AccountHolder, String, String, Long, String) -> Unit,
    onPayCommission: (CommissionSettlement, Long, String, String, String) -> Unit
) {
    BackHandler { onBack() }
    var showGenerateDialog by remember { mutableStateOf(false) }
    var payingSettlement by remember { mutableStateOf<CommissionSettlement?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
                Text("Comisiones y Liquidaciones", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { showGenerateDialog = true },
                modifier = Modifier.testTag("generate_commission_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Liquidar Periodo")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Liquidar")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "La comisión porcentual se calcula exclusivamente sobre el beneficio neto elegible de apuestas (nunca sobre depósitos o retiros).",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (uiState.commissions.isEmpty()) {
            EmptyStateCard(
                title = "Sin liquidaciones registradas",
                description = "Genera una liquidación por titular y periodo para calcular automáticamente la comisión sobre beneficios netos elegibles.",
                icon = Icons.Default.Payments
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(uiState.commissions, key = { it.id }) { comm ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("${comm.holderName} • Periodo ${comm.periodLabel}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Cuentas: ${comm.includedAccounts}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                StatusBadge(comm.status)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Beneficio Elegible: ${MoneyUtils.formatCents(comm.eligibleProfitCents, comm.currency, uiState.hideAmounts)} • Acuerdo: ${comm.appliedPct}%",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Comisión Generada: ${MoneyUtils.formatCents(comm.generatedCommissionCents, comm.currency, uiState.hideAmounts)}",
                                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Pagado: ${MoneyUtils.formatCents(comm.paidAmountCents, comm.currency, uiState.hideAmounts)} • Pendiente: ${MoneyUtils.formatCents(comm.pendingAmountCents, comm.currency, uiState.hideAmounts)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (comm.pendingAmountCents > 0) AmberWarning else EmeraldGreen
                                    )
                                }
                                if (comm.pendingAmountCents > 0L) {
                                    Button(onClick = { payingSettlement = comm }) {
                                        Icon(Icons.Default.Check, contentDescription = "Pagar")
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Registrar Pago")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showGenerateDialog && uiState.holders.isNotEmpty()) {
        var selectedHolder by remember { mutableStateOf(uiState.holders.first()) }
        var periodLabel by remember { mutableStateOf("2026-11") }
        var currency by remember { mutableStateOf("PEN") }
        var adjStr by remember { mutableStateOf("0.00") }
        var adjReason by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showGenerateDialog = false },
            title = { Text("Generar Liquidación de Comisión") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SimpleDropdownSelector(
                        label = "Titular",
                        selectedValue = selectedHolder.name,
                        options = uiState.holders.map { it.name },
                        onValueSelected = { name ->
                            uiState.holders.find { it.name == name }?.let { selectedHolder = it }
                        }
                    )
                    OutlinedTextField(
                        value = periodLabel,
                        onValueChange = { periodLabel = it },
                        label = { Text("Periodo (ej. 2026-11 o Sem-44)") },
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
                        value = adjStr,
                        onValueChange = { adjStr = it },
                        label = { Text("Ajuste manual opcional (${currency})") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = adjReason,
                        onValueChange = { adjReason = it },
                        label = { Text("Motivo del ajuste manual") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val adjCents = MoneyUtils.parseToCents(adjStr) ?: 0L
                        onGenerateCommission(selectedHolder, periodLabel, currency, adjCents, adjReason)
                        showGenerateDialog = false
                    }
                ) {
                    Text("Calcular y Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGenerateDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (payingSettlement != null) {
        val target = payingSettlement!!
        var amountStr by remember { mutableStateOf(MoneyUtils.centsToDecimalString(target.pendingAmountCents)) }
        var method by remember { mutableStateOf("Yape / Plin") }
        var selectedWallet by remember { mutableStateOf(uiState.wallets.firstOrNull()) }

        AlertDialog(
            onDismissRequest = { payingSettlement = null },
            title = { Text("Registrar Pago de Comisión a ${target.holderName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Importe pagado (${target.currency})") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    SimpleDropdownSelector(
                        label = "Medio de pago",
                        selectedValue = method,
                        options = listOf("Yape / Plin", "Transferencia Bancaria", "Efectivo"),
                        onValueSelected = { method = it }
                    )
                    if (uiState.wallets.isNotEmpty()) {
                        SimpleDropdownSelector(
                            label = "Descontar de Billetera Central",
                            selectedValue = selectedWallet?.name ?: "",
                            options = uiState.wallets.map { it.name },
                            onValueSelected = { wName ->
                                selectedWallet = uiState.wallets.find { it.name == wName }
                            }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val paidCents = MoneyUtils.parseToCents(amountStr) ?: target.pendingAmountCents
                        onPayCommission(
                            target,
                            paidCents,
                            method,
                            selectedWallet?.id ?: "",
                            selectedWallet?.name ?: ""
                        )
                        payingSettlement = null
                    }
                ) {
                    Text("Confirmar Pago")
                }
            },
            dismissButton = {
                TextButton(onClick = { payingSettlement = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun ReconciliationsSection(
    uiState: BetManagerUiState,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
            Text("Registros de Conciliación de Saldos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(10.dp))
        if (uiState.reconciliations.isEmpty()) {
            EmptyStateCard(
                title = "Sin conciliaciones registradas",
                description = "Desde el detalle de cualquier cuenta puedes conciliar el saldo calculado frente al saldo real observado en la casa.",
                icon = Icons.Default.Rule
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(uiState.reconciliations, key = { it.id }) { rec ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("${rec.accountLabel} • ${rec.reconciliationDate}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Calculado: ${MoneyUtils.formatCents(rec.calculatedBalanceCents, rec.currency, uiState.hideAmounts)} | Observado: ${MoneyUtils.formatCents(rec.observedBalanceCents, rec.currency, uiState.hideAmounts)} | Diferencia: ${MoneyUtils.formatCents(rec.differenceCents, rec.currency, uiState.hideAmounts)}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFontFamily)
                            )
                            Text("Motivo: ${rec.reason}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RequirementsSection(
    uiState: BetManagerUiState,
    onBack: () -> Unit,
    onSaveRequirement: (VerificationRequirement) -> Unit
) {
    BackHandler { onBack() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
            Text("Requisitos y Verificaciones KYC", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(10.dp))
        if (uiState.requirements.isEmpty()) {
            EmptyStateCard(
                title = "Sin requisitos pendientes",
                description = "Las tareas de verificación de identidad, domicilio o medios de pago de cada cuenta aparecerán aquí.",
                icon = Icons.Default.VerifiedUser
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(uiState.requirements, key = { it.id }) { req ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${req.requirementType} • ${req.accountLabel}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Límite: ${req.dueDate} • Prioridad: ${req.priority}", style = MaterialTheme.typography.bodyMedium)
                                if (req.notes.isNotBlank()) Text(req.notes, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (req.status != "RESUELTO") {
                                Button(
                                    onClick = {
                                        onSaveRequirement(req.copy(status = "RESUELTO", resolutionDate = "2026-11-03"))
                                    }
                                ) {
                                    Text("Resolver")
                                }
                            } else {
                                StatusBadge("RESUELTO")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertsSection(
    uiState: BetManagerUiState,
    onBack: () -> Unit,
    onDismissAlert: (AlertReminder) -> Unit
) {
    BackHandler { onBack() }

    // Alertas automáticas derivadas en tiempo real del estado real del portafolio
    val pendingWithdrawals = uiState.transactions.filter {
        (it.type == "RETIRO_PENDIENTE" || it.type == "RETIRO") && it.status == "PENDIENTE"
    }
    val unresolvedReqs = uiState.requirements.filter { it.status == "PENDIENTE" || it.status == "VENCIDO" }
    val closingAccountsWithBalance = uiState.accounts.filter {
        (it.accountStatus == "RETIRO_FINAL" || it.accountStatus == "LIMITADA") && it.calculatedBalanceCents > 0
    }
    val pendingCommissions = uiState.commissions.filter { it.pendingAmountCents > 0 }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
            Text("Bandeja de Alertas y Recordatorios", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        if (pendingWithdrawals.isEmpty() && unresolvedReqs.isEmpty() && closingAccountsWithBalance.isEmpty() && pendingCommissions.isEmpty() && uiState.alerts.isEmpty()) {
            EmptyStateCard(
                title = "Bandeja de alertas limpia",
                description = "No se detectan retiros pendientes en tránsito, requisitos vencidos ni cuentas limitadas con saldo pendiente de retiro.",
                icon = Icons.Default.NotificationsActive
            )
        }

        pendingWithdrawals.forEach { tx ->
            AlertItemCard(
                title = "Retiro en Tránsito Pendiente",
                message = "${tx.bookmakerName} (${tx.holderName}): ${MoneyUtils.formatCents(tx.amountCents, tx.currency, uiState.hideAmounts)} desde ${tx.transactionDate}",
                severity = "ALTA"
            )
        }

        unresolvedReqs.forEach { req ->
            AlertItemCard(
                title = "Requisito KYC Pendiente: ${req.requirementType}",
                message = "Cuenta ${req.accountLabel} • Fecha límite: ${req.dueDate}",
                severity = req.priority
            )
        }

        closingAccountsWithBalance.forEach { acc ->
            AlertItemCard(
                title = "Cuenta ${acc.accountStatus} con Saldo por Retirar",
                message = "${acc.bookmakerName} (${acc.username}) mantiene ${MoneyUtils.formatCents(acc.calculatedBalanceCents, acc.currency, uiState.hideAmounts)}",
                severity = "MEDIA"
            )
        }

        pendingCommissions.forEach { comm ->
            AlertItemCard(
                title = "Comisión Próxima a Liquidar",
                message = "Titular ${comm.holderName} (${comm.periodLabel}): Pendiente ${MoneyUtils.formatCents(comm.pendingAmountCents, comm.currency, uiState.hideAmounts)}",
                severity = "INFO"
            )
        }
    }
}

@Composable
private fun AlertItemCard(title: String, message: String, severity: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StatusBadge(severity)
        }
    }
}

@Composable
private fun MultiCurrencySection(
    uiState: BetManagerUiState,
    onBack: () -> Unit,
    onSaveExchangeRate: (ExchangeRateRecord) -> Unit
) {
    BackHandler { onBack() }
    var showAddRate by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
                Text("Multimoneda y Tasas de Cambio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Button(onClick = { showAddRate = true }) {
                Icon(Icons.Default.Add, contentDescription = "Nueva Tasa")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Registrar Tasa")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Monedas soportadas: PEN, CLP, COP, ARS, MXN, USD, BOB, EUR. No se suman monedas distintas sin conversión registrada.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(uiState.exchangeRates, key = { it.id }) { rate ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                            Text("1 ${rate.fromCurrency} = ${rate.rate} ${rate.toCurrency}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Fuente: ${rate.source} • Fecha: ${rate.rateDate}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        StatusBadge("ACTIVA")
                    }
                }
            }
        }
    }

    if (showAddRate) {
        var fromCurr by remember { mutableStateOf("USD") }
        var toCurr by remember { mutableStateOf("PEN") }
        var rateStr by remember { mutableStateOf("3.75") }
        var source by remember { mutableStateOf("SBS / Tipo de cambio bancario") }

        AlertDialog(
            onDismissRequest = { showAddRate = false },
            title = { Text("Registrar Tipo de Cambio") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SimpleDropdownSelector(
                        label = "Moneda Origen",
                        selectedValue = fromCurr,
                        options = SupportedCurrency.entries.map { it.code },
                        onValueSelected = { fromCurr = it }
                    )
                    SimpleDropdownSelector(
                        label = "Moneda Destino",
                        selectedValue = toCurr,
                        options = SupportedCurrency.entries.map { it.code },
                        onValueSelected = { toCurr = it }
                    )
                    OutlinedTextField(
                        value = rateStr,
                        onValueChange = { rateStr = it },
                        label = { Text("Valor de conversión") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = source,
                        onValueChange = { source = it },
                        label = { Text("Fuente de referencia") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val r = rateStr.toDoubleOrNull() ?: return@Button
                        if (r <= 0.0) return@Button
                        onSaveExchangeRate(
                            ExchangeRateRecord(
                                fromCurrency = fromCurr,
                                toCurrency = toCurr,
                                rate = r,
                                source = source.trim()
                            )
                        )
                        showAddRate = false
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddRate = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun ExportsAndBackupSection(
    onBack: () -> Unit,
    onGenerateExport: (String) -> String
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    var previewContent by remember { mutableStateOf("") }
    var copiedBanner by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
            Text("Exportaciones CSV y Respaldo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { previewContent = onGenerateExport("TITULARES_CSV"); copiedBanner = false }) {
                Text("CSV Titulares")
            }
            OutlinedButton(onClick = { previewContent = onGenerateExport("CUENTAS_CSV"); copiedBanner = false }) {
                Text("CSV Cuentas")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { previewContent = onGenerateExport("MOVIMIENTOS_CSV"); copiedBanner = false }) {
                Text("CSV Movimientos")
            }
            Button(onClick = { previewContent = onGenerateExport("BACKUP_JSON"); copiedBanner = false }) {
                Text("Respaldo JSON")
            }
        }

        if (previewContent.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Vista Previa de Exportación", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Button(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("BetManagerExport", previewContent))
                                copiedBanner = true
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copiar")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (copiedBanner) "Copiado" else "Copiar")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = previewContent,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFontFamily)
                    )
                }
            }
        }
    }
}

@Composable
private fun AuditHistorySection(
    uiState: BetManagerUiState,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
            Text("Historial de Cambios de Estado y Auditoría", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(10.dp))
        if (uiState.history.isEmpty()) {
            EmptyStateCard(
                title = "Sin eventos en el historial",
                description = "Cada cambio de estado de cuenta o limitación queda registrado automáticamente aquí.",
                icon = Icons.Default.Assessment
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(uiState.history, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("${item.entityLabel} • ${item.changeDate}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${item.previousState} → ${item.newState}", style = MaterialTheme.typography.labelLarge, color = CyanAccent)
                            Text(item.reason, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
