package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.BettingAccount
import com.example.data.model.FinancialTransaction
import com.example.data.model.MoneyUtils
import com.example.data.model.SupportedCurrency
import com.example.data.model.Wallet
import com.example.ui.BetManagerUiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SimpleDropdownSelector
import com.example.ui.components.StatusBadge
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.JetBrainsMonoFontFamily

val allTransactionTypes = listOf(
    "DEPOSITO",
    "RETIRO_PENDIENTE",
    "RETIRO_COMPLETADO",
    "RESULTADO_POSITIVO",
    "RESULTADO_NEGATIVO",
    "BONO",
    "COMISION_CARGO",
    "TRANSFERENCIA_INTERNA",
    "AJUSTE_CONCILIACION",
    "PAGO_COMISION"
)

@Composable
fun TransactionsScreen(
    uiState: BetManagerUiState,
    onRecordTransaction: (FinancialTransaction) -> Unit,
    onUpdateTransactionStatus: (String, String, String?) -> Unit,
    onSaveWallet: (Wallet) -> Unit,
    onCopyReceiptUri: (Uri) -> String
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf("TODOS") }
    var filterStatus by remember { mutableStateOf("TODOS") }
    var showTxDialog by remember { mutableStateOf(false) }
    var defaultTxType by remember { mutableStateOf("DEPOSITO") }
    var showWalletDialog by remember { mutableStateOf(false) }
    var viewingReceiptPath by remember { mutableStateOf<String?>(null) }

    val filteredTransactions = uiState.transactions.filter { tx ->
        val matchesSearch = searchQuery.isBlank() ||
            tx.holderName.contains(searchQuery, ignoreCase = true) ||
            tx.bookmakerName.contains(searchQuery, ignoreCase = true) ||
            tx.accountUsername.contains(searchQuery, ignoreCase = true) ||
            tx.walletName.contains(searchQuery, ignoreCase = true) ||
            tx.referenceCode.contains(searchQuery, ignoreCase = true) ||
            tx.notes.contains(searchQuery, ignoreCase = true)
        val matchesType = filterType == "TODOS" || tx.type == filterType
        val matchesStatus = filterStatus == "TODOS" || tx.status == filterStatus
        matchesSearch && matchesType && matchesStatus
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
                text = "Movimientos (${filteredTransactions.size})",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showWalletDialog = true },
                    modifier = Modifier.testTag("add_wallet_button")
                ) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Billetera")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Caja")
                }
                Button(
                    onClick = {
                        defaultTxType = "DEPOSITO"
                        showTxDialog = true
                    },
                    modifier = Modifier.testTag("add_transaction_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nuevo Movimiento")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Registrar")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tira de Billeteras / Cajas Centrales fuera de las casas de apuestas
        if (uiState.wallets.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                uiState.wallets.forEach { w ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Text(
                                text = "${w.name} (${w.walletType})",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = MoneyUtils.formatCents(w.currentBalanceCents, w.currency, uiState.hideAmounts),
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Buscar por titular, casa, cuenta, billetera o referencia") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("transaction_search_input"),
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "TODOS",
                "DEPOSITO",
                "RETIRO_PENDIENTE",
                "RETIRO_COMPLETADO",
                "RESULTADO_POSITIVO",
                "RESULTADO_NEGATIVO",
                "BONO",
                "AJUSTE_CONCILIACION"
            ).forEach { t ->
                FilterChip(
                    selected = filterType == t,
                    onClick = { filterType = t },
                    label = { Text(t.replace("_", " ")) }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("TODOS", "PENDIENTE", "COMPLETADO", "RECHAZADO", "CANCELADO").forEach { st ->
                FilterChip(
                    selected = filterStatus == st,
                    onClick = { filterStatus = st },
                    label = { Text(st) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredTransactions.isEmpty()) {
            EmptyStateCard(
                title = "Sin movimientos financieros registrados",
                description = "Registra depósitos, retiros pendientes/completados o resultados netos de apuestas por periodo. El flujo de caja y el beneficio neto se contabilizan por separado.",
                icon = Icons.Default.ReceiptLong
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredTransactions, key = { it.id }) { tx ->
                    TransactionItemCard(
                        tx = tx,
                        hideAmounts = uiState.hideAmounts,
                        onMarkCompleted = {
                            onUpdateTransactionStatus(tx.id, "COMPLETADO", "RETIRO_COMPLETADO")
                        },
                        onMarkRejected = {
                            onUpdateTransactionStatus(tx.id, "RECHAZADO", "RETIRO_RECHAZADO")
                        },
                        onViewReceipt = { path ->
                            viewingReceiptPath = path
                        }
                    )
                }
            }
        }
    }

    if (showTxDialog) {
        TransactionFormDialog(
            initialType = defaultTxType,
            accounts = uiState.accounts,
            wallets = uiState.wallets,
            onCopyReceiptUri = onCopyReceiptUri,
            onDismiss = { showTxDialog = false },
            onConfirm = { newTx ->
                onRecordTransaction(newTx)
                showTxDialog = false
            }
        )
    }

    if (showWalletDialog) {
        WalletFormDialog(
            onDismiss = { showWalletDialog = false },
            onConfirm = { newWallet ->
                onSaveWallet(newWallet)
                showWalletDialog = false
            }
        )
    }

    if (viewingReceiptPath != null) {
        AlertDialog(
            onDismissRequest = { viewingReceiptPath = null },
            title = { Text("Comprobante / Captura Adjunta") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AsyncImage(
                        model = viewingReceiptPath,
                        contentDescription = "Comprobante de operación",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = viewingReceiptPath ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewingReceiptPath = null }) {
                    Text("Cerrar")
                }
            }
        )
    }
}

@Composable
private fun TransactionItemCard(
    tx: FinancialTransaction,
    hideAmounts: Boolean,
    onMarkCompleted: () -> Unit,
    onMarkRejected: () -> Unit,
    onViewReceipt: (String) -> Unit
) {
    val amountColor = when (tx.type) {
        "RESULTADO_POSITIVO", "BONO", "DEPOSITO" -> EmeraldGreen
        "RESULTADO_NEGATIVO", "COMISION_CARGO", "PAGO_COMISION" -> CrimsonRed
        else -> CyanAccent
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tx_card_${tx.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tx.type.replace("_", " "),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    val subtitleParts = listOfNotNull(
                        tx.bookmakerName.takeIf { it.isNotBlank() },
                        tx.holderName.takeIf { it.isNotBlank() },
                        tx.walletName.takeIf { it.isNotBlank() }?.let { "Caja: $it" }
                    )
                    Text(
                        text = subtitleParts.joinToString(" • ").ifBlank { tx.paymentMethod },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    StatusBadge(tx.status)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = MoneyUtils.formatCents(tx.amountCents, tx.currency, hideAmounts),
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = amountColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Fecha: ${tx.transactionDate} • Medio: ${tx.paymentMethod}" +
                    if (tx.referenceCode.isNotBlank()) " • Ref: ${tx.referenceCode}" else "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (tx.notes.isNotBlank()) {
                Text(
                    text = tx.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (tx.receiptImagePath.isNotBlank()) {
                    OutlinedButton(
                        onClick = { onViewReceipt(tx.receiptImagePath) },
                        modifier = Modifier.testTag("view_receipt_${tx.id}")
                    ) {
                        Icon(Icons.Default.Image, contentDescription = "Ver comprobante")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ver Comprobante")
                    }
                }

                if (tx.status == "PENDIENTE" && (tx.type == "RETIRO_PENDIENTE" || tx.type == "RETIRO")) {
                    Button(
                        onClick = onMarkCompleted,
                        modifier = Modifier.testTag("complete_withdrawal_${tx.id}")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Completar retiro")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Confirmar Llegada")
                    }
                    OutlinedButton(
                        onClick = onMarkRejected,
                        modifier = Modifier.testTag("reject_withdrawal_${tx.id}")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Rechazar retiro", tint = CrimsonRed)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Rechazado")
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionFormDialog(
    initialType: String,
    accounts: List<BettingAccount>,
    wallets: List<Wallet>,
    onCopyReceiptUri: (Uri) -> String,
    onDismiss: () -> Unit,
    onConfirm: (FinancialTransaction) -> Unit
) {
    var txType by remember { mutableStateOf(initialType) }
    var selectedAccount by remember { mutableStateOf(accounts.firstOrNull()) }
    var selectedWallet by remember { mutableStateOf(wallets.firstOrNull()) }
    var amountStr by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(selectedAccount?.currency ?: selectedWallet?.currency ?: "PEN") }
    var paymentMethod by remember { mutableStateOf("Transferencia Bancaria") }
    var status by remember {
        mutableStateOf(if (initialType == "RETIRO_PENDIENTE") "PENDIENTE" else "COMPLETADO")
    }
    var referenceCode by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var receiptPath by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            receiptPath = onCopyReceiptUri(uri)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Movimiento Financiero") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (errorMsg != null) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }

                SimpleDropdownSelector(
                    label = "Tipo de Movimiento *",
                    selectedValue = txType,
                    options = allTransactionTypes,
                    onValueSelected = {
                        txType = it
                        status = if (it == "RETIRO_PENDIENTE") "PENDIENTE" else "COMPLETADO"
                    }
                )

                if (accounts.isNotEmpty()) {
                    SimpleDropdownSelector(
                        label = "Cuenta de Casa de Apuestas",
                        selectedValue = selectedAccount?.let { "${it.bookmakerName} - ${it.username} (${it.holderName})" } ?: "Sin cuenta específica",
                        options = accounts.map { "${it.bookmakerName} - ${it.username} (${it.holderName})" },
                        onValueSelected = { label ->
                            val acc = accounts.find { "${it.bookmakerName} - ${it.username} (${it.holderName})" == label }
                            selectedAccount = acc
                            if (acc != null) currency = acc.currency
                        }
                    )
                }

                if (wallets.isNotEmpty()) {
                    SimpleDropdownSelector(
                        label = "Billetera / Caja Central (Origen o Destino)",
                        selectedValue = selectedWallet?.let { "${it.name} (${it.currency})" } ?: "Sin billetera",
                        options = wallets.map { "${it.name} (${it.currency})" },
                        onValueSelected = { label ->
                            selectedWallet = wallets.find { "${it.name} (${it.currency})" == label }
                        }
                    )
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Importe (${currency}) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("tx_amount_input")
                )

                SimpleDropdownSelector(
                    label = "Moneda",
                    selectedValue = currency,
                    options = SupportedCurrency.entries.map { it.code },
                    onValueSelected = { currency = it }
                )

                SimpleDropdownSelector(
                    label = "Medio de Pago",
                    selectedValue = paymentMethod,
                    options = listOf(
                        "Transferencia Bancaria",
                        "Yape / Plin",
                        "PagoEfectivo",
                        "Tarjeta Débito",
                        "AstroPay / Skrill",
                        "Resultado de Apuestas",
                        "Efectivo"
                    ),
                    onValueSelected = { paymentMethod = it }
                )

                SimpleDropdownSelector(
                    label = "Estado de la Operación",
                    selectedValue = status,
                    options = listOf("COMPLETADO", "PENDIENTE", "RECHAZADO", "CANCELADO"),
                    onValueSelected = { status = it }
                )

                OutlinedTextField(
                    value = referenceCode,
                    onValueChange = { referenceCode = it },
                    label = { Text("Referencia de operación (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observaciones / Periodo de resultado") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Adjuntar comprobante")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (receiptPath.isBlank()) "Adjuntar Captura / Comprobante" else "Comprobante Adjuntado")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cents = MoneyUtils.parseToCents(amountStr)
                    if (cents == null || cents <= 0L) {
                        errorMsg = "Ingresa un importe mayor a 0 con precisión decimal válida."
                        return@Button
                    }
                    if (txType == "AJUSTE_CONCILIACION" && notes.isBlank()) {
                        errorMsg = "Los ajustes de conciliación requieren motivo obligatorio en Observaciones."
                        return@Button
                    }
                    onConfirm(
                        FinancialTransaction(
                            type = txType,
                            holderId = selectedAccount?.holderId ?: "",
                            holderName = selectedAccount?.holderName ?: "",
                            bookmakerId = selectedAccount?.bookmakerId ?: "",
                            bookmakerName = selectedAccount?.bookmakerName ?: "",
                            accountId = selectedAccount?.id ?: "",
                            accountUsername = selectedAccount?.username ?: "",
                            walletId = selectedWallet?.id ?: "",
                            walletName = selectedWallet?.name ?: "",
                            currency = currency,
                            amountCents = cents,
                            paymentMethod = paymentMethod,
                            status = status,
                            referenceCode = referenceCode.trim(),
                            notes = notes.trim(),
                            receiptImagePath = receiptPath
                        )
                    )
                },
                modifier = Modifier.testTag("save_tx_button")
            ) {
                Text("Registrar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun WalletFormDialog(
    onDismiss: () -> Unit,
    onConfirm: (Wallet) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var walletType by remember { mutableStateOf("BANCO") }
    var currency by remember { mutableStateOf("PEN") }
    var initialBalanceStr by remember { mutableStateOf("0.00") }
    var notes by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva Billetera / Caja Central") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (errorMsg != null) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error)
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre de la billetera o banco *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("wallet_name_input")
                )
                SimpleDropdownSelector(
                    label = "Tipo de Caja",
                    selectedValue = walletType,
                    options = listOf("BANCO", "BILLETERA_DIGITAL", "EFECTIVO", "CRIPTO", "OTRO"),
                    onValueSelected = { walletType = it }
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
                    label = { Text("Saldo inicial en caja (${currency})") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("wallet_balance_input")
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
                    val cents = MoneyUtils.parseToCents(initialBalanceStr)
                    if (name.isBlank()) {
                        errorMsg = "Ingresa el nombre de la billetera o caja central."
                        return@Button
                    }
                    if (cents == null || cents < 0) {
                        errorMsg = "Ingresa un saldo inicial válido."
                        return@Button
                    }
                    onConfirm(
                        Wallet(
                            name = name.trim(),
                            walletType = walletType,
                            currency = currency,
                            initialBalanceCents = cents,
                            currentBalanceCents = cents,
                            notes = notes.trim()
                        )
                    )
                },
                modifier = Modifier.testTag("save_wallet_button")
            ) {
                Text("Guardar Caja")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
