package com.example.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AccountHolder
import com.example.data.model.AlertReminder
import com.example.data.model.AttachmentRecord
import com.example.data.model.BalanceReconciliation
import com.example.data.model.BettingAccount
import com.example.data.model.Bookmaker
import com.example.data.model.BookmakerRelation
import com.example.data.model.CommissionSettlement
import com.example.data.model.ExchangeRateRecord
import com.example.data.model.FinancialTransaction
import com.example.data.model.MoneyUtils
import com.example.data.model.StatusHistoryRecord
import com.example.data.model.VerificationRequirement
import com.example.data.model.Wallet
import com.example.data.repository.BetManagerRepository
import com.example.data.repository.CurrencySummary
import com.example.data.repository.FinancialCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

enum class MainTab {
    INICIO, CUENTAS, MOVIMIENTOS, TITULARES, MAS
}

enum class MoreSection {
    MENU,
    BILLETERAS,
    CATALOGO_CASAS,
    CASAS_RELACIONADAS,
    COMISIONES,
    CONCILIACIONES,
    REQUISITOS,
    ALERTAS,
    ESTADISTICAS,
    MULTIMONEDA,
    EXPORTACIONES,
    HISTORIAL
}

data class BetManagerUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val statusMessage: String? = null,
    val hideAmounts: Boolean = false,
    val darkThemeOverride: Boolean? = null,
    val baseCurrency: String = "PEN",
    val selectedPeriodDays: Int = 30, // 7, 30, 90, 365, 0 (Todo)
    val holders: List<AccountHolder> = emptyList(),
    val bookmakers: List<Bookmaker> = emptyList(),
    val relations: List<BookmakerRelation> = emptyList(),
    val accounts: List<BettingAccount> = emptyList(),
    val wallets: List<Wallet> = emptyList(),
    val transactions: List<FinancialTransaction> = emptyList(),
    val commissions: List<CommissionSettlement> = emptyList(),
    val requirements: List<VerificationRequirement> = emptyList(),
    val history: List<StatusHistoryRecord> = emptyList(),
    val attachments: List<AttachmentRecord> = emptyList(),
    val alerts: List<AlertReminder> = emptyList(),
    val reconciliations: List<BalanceReconciliation> = emptyList(),
    val exchangeRates: List<ExchangeRateRecord> = emptyList(),
    val currencySummaries: List<CurrencySummary> = emptyList()
)

class BetManagerViewModel(
    private val repository: BetManagerRepository,
    val currentUserId: String
) : ViewModel() {

    private val _hideAmounts = MutableStateFlow(false)
    private val _darkThemeOverride = MutableStateFlow<Boolean?>(null)
    private val _baseCurrency = MutableStateFlow("PEN")
    private val _selectedPeriodDays = MutableStateFlow(30)
    private val _statusMessage = MutableStateFlow<String?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private val _activeTab = MutableStateFlow(MainTab.INICIO)
    val activeTab: StateFlow<MainTab> = _activeTab.asStateFlow()

    private val _activeMoreSection = MutableStateFlow(MoreSection.MENU)
    val activeMoreSection: StateFlow<MoreSection> = _activeMoreSection.asStateFlow()

    private val _selectedHolderId = MutableStateFlow<String?>(null)
    val selectedHolderId: StateFlow<String?> = _selectedHolderId.asStateFlow()

    private val _selectedAccountId = MutableStateFlow<String?>(null)
    val selectedAccountId: StateFlow<String?> = _selectedAccountId.asStateFlow()

    private val _selectedBookmakerId = MutableStateFlow<String?>(null)
    val selectedBookmakerId: StateFlow<String?> = _selectedBookmakerId.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                repository.ensureAdminProfileAndSeedCatalog()
            } catch (e: Exception) {
                Log.w("BetManagerVM", "Initial catalog check failed", e)
            }
        }
    }

    private val coreDataFlow = combine(
        repository.observeHolders().catch { emit(emptyList()) },
        repository.observeBookmakers().catch { emit(emptyList()) },
        repository.observeAccounts().catch { emit(emptyList()) },
        repository.observeWallets().catch { emit(emptyList()) },
        repository.observeTransactions().catch { emit(emptyList()) }
    ) { holders, bookmakers, accounts, wallets, transactions ->
        CoreData(holders, bookmakers, accounts, wallets, transactions)
    }

    private val secondaryDataFlow = combine(
        repository.observeRelations().catch { emit(emptyList()) },
        repository.observeCommissions().catch { emit(emptyList()) },
        repository.observeRequirements().catch { emit(emptyList()) },
        repository.observeHistory().catch { emit(emptyList()) },
        repository.observeAttachments().catch { emit(emptyList()) }
    ) { relations, commissions, requirements, history, attachments ->
        SecondaryData(relations, commissions, requirements, history, attachments)
    }

    private val extraDataFlow = combine(
        repository.observeAlerts().catch { emit(emptyList()) },
        repository.observeReconciliations().catch { emit(emptyList()) },
        repository.observeExchangeRates().catch { emit(emptyList()) }
    ) { alerts, reconciliations, rates ->
        Triple(alerts, reconciliations, rates)
    }

    private val prefsFlow = combine(
        _hideAmounts,
        _darkThemeOverride,
        _baseCurrency,
        _selectedPeriodDays,
        _statusMessage
    ) { hide, dark, baseCurr, period, status ->
        PrefsState(hide, dark, baseCurr, period, status)
    }

    val uiState: StateFlow<BetManagerUiState> = combine(
        coreDataFlow,
        secondaryDataFlow,
        extraDataFlow,
        prefsFlow,
        _errorMessage
    ) { core, sec, extra, prefs, err ->
        val summaries = FinancialCalculator.buildSummariesByCurrency(
            accounts = core.accounts,
            wallets = core.wallets,
            transactions = core.transactions
        )
        BetManagerUiState(
            isLoading = false,
            errorMessage = err,
            statusMessage = prefs.statusMessage,
            hideAmounts = prefs.hideAmounts,
            darkThemeOverride = prefs.darkThemeOverride,
            baseCurrency = prefs.baseCurrency,
            selectedPeriodDays = prefs.selectedPeriodDays,
            holders = core.holders.sortedBy { it.name },
            bookmakers = core.bookmakers.sortedWith(compareBy({ it.country }, { it.brandName })),
            relations = sec.relations,
            accounts = core.accounts.sortedByDescending { it.calculatedBalanceCents },
            wallets = core.wallets.sortedBy { it.name },
            transactions = core.transactions.sortedByDescending { it.transactionDate },
            commissions = sec.commissions.sortedByDescending { it.periodLabel },
            requirements = sec.requirements.sortedBy { it.dueDate },
            history = sec.history.sortedByDescending { it.changeDate },
            attachments = sec.attachments,
            alerts = extra.first.filter { !it.isDismissed },
            reconciliations = extra.second.sortedByDescending { it.reconciliationDate },
            exchangeRates = extra.third.sortedByDescending { it.rateDate },
            currencySummaries = summaries
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = BetManagerUiState(isLoading = true)
    )

    fun selectTab(tab: MainTab) {
        _activeTab.value = tab
        if (tab != MainTab.MAS) {
            _activeMoreSection.value = MoreSection.MENU
        }
        _selectedHolderId.value = null
        _selectedAccountId.value = null
        _selectedBookmakerId.value = null
    }

    fun selectMoreSection(section: MoreSection) {
        _activeTab.value = MainTab.MAS
        _activeMoreSection.value = section
    }

    fun selectHolderDetail(holderId: String?) {
        _selectedHolderId.value = holderId
    }

    fun selectAccountDetail(accountId: String?) {
        _selectedAccountId.value = accountId
    }

    fun selectBookmakerDetail(bookmakerId: String?) {
        _selectedBookmakerId.value = bookmakerId
    }

    fun toggleHideAmounts() {
        _hideAmounts.value = !_hideAmounts.value
    }

    fun setDarkThemeOverride(dark: Boolean?) {
        _darkThemeOverride.value = dark
    }

    fun setBaseCurrency(currency: String) {
        _baseCurrency.value = currency
    }

    fun setSelectedPeriodDays(days: Int) {
        _selectedPeriodDays.value = days
    }

    fun clearMessages() {
        _statusMessage.value = null
        _errorMessage.value = null
    }

    fun saveHolder(holder: AccountHolder) {
        viewModelScope.launch {
            try {
                repository.saveHolder(holder)
                _statusMessage.value = "Titular '${holder.name}' guardado correctamente."
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Error al guardar titular"
            }
        }
    }

    fun saveBookmaker(bookmaker: Bookmaker) {
        viewModelScope.launch {
            try {
                repository.saveBookmaker(bookmaker)
                _statusMessage.value = "Casa '${bookmaker.brandName} (${bookmaker.country})' actualizada."
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Error al guardar casa de apuestas"
            }
        }
    }

    fun saveRelation(relation: BookmakerRelation) {
        viewModelScope.launch {
            try {
                repository.saveRelation(relation)
                _statusMessage.value = "Relación entre casas guardada."
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Error al guardar relación"
            }
        }
    }

    fun deleteRelation(relationId: String) {
        viewModelScope.launch {
            try {
                repository.deleteRelation(relationId)
                _statusMessage.value = "Relación eliminada."
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Error al eliminar relación"
            }
        }
    }

    fun saveWallet(wallet: Wallet) {
        viewModelScope.launch {
            try {
                repository.saveWallet(wallet)
                _statusMessage.value = "Billetera '${wallet.name}' registrada."
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Error al guardar billetera"
            }
        }
    }

    fun saveAccount(account: BettingAccount) {
        viewModelScope.launch {
            try {
                repository.saveAccount(account)
                _statusMessage.value = "Cuenta '${account.bookmakerName} - ${account.username}' guardada."
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Error al guardar cuenta"
            }
        }
    }

    fun updateAccountStatus(
        account: BettingAccount,
        newStatus: String,
        reason: String,
        limitationCategory: String = "",
        limitationDescription: String = "",
        closureReason: String = "",
        finalWithdrawalStatus: String = "",
        screenshotPath: String = ""
    ) {
        viewModelScope.launch {
            try {
                repository.updateAccountStatus(
                    account = account,
                    newStatus = newStatus,
                    reason = reason,
                    limitationCategory = limitationCategory,
                    limitationDescription = limitationDescription,
                    closureReason = closureReason,
                    finalWithdrawalStatus = finalWithdrawalStatus,
                    screenshotPath = screenshotPath
                )
                _statusMessage.value = "Estado de cuenta actualizado a $newStatus conservando historial."
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Error al actualizar estado de cuenta"
            }
        }
    }

    fun updateAccountItf(accountId: String, itfValue: String) {
        viewModelScope.launch {
            try {
                repository.updateAccountItfOverride(accountId, itfValue)
                _statusMessage.value = "Disponibilidad ITF de la cuenta actualizada a $itfValue."
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Error al actualizar ITF"
            }
        }
    }

    fun recordTransaction(transaction: FinancialTransaction) {
        viewModelScope.launch {
            try {
                repository.recordTransaction(transaction)
                _statusMessage.value = "Movimiento ${transaction.type} registrado y saldos actualizados."
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Error al registrar movimiento"
            }
        }
    }

    fun updateTransactionStatus(transactionId: String, newStatus: String, newType: String? = null) {
        viewModelScope.launch {
            try {
                repository.updateTransactionStatus(transactionId, newStatus, newType)
                _statusMessage.value = "Estado de movimiento actualizado a $newStatus."
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Error al actualizar movimiento"
            }
        }
    }

    fun reconcileAccount(account: BettingAccount, observedCents: Long, reason: String) {
        viewModelScope.launch {
            try {
                repository.reconcileAccountBalance(account, observedCents, reason)
                _statusMessage.value = "Conciliación registrada con trazabilidad completa."
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Error en conciliación"
            }
        }
    }

    fun generateCommissionSettlement(
        holder: AccountHolder,
        periodLabel: String,
        currency: String,
        manualAdjustmentCents: Long = 0L,
        adjustmentReason: String = ""
    ) {
        viewModelScope.launch {
            try {
                val state = uiState.value
                val holderTxs = state.transactions.filter { it.holderId == holder.id && it.currency == currency }
                val eligibleProfit = FinancialCalculator.calculateNetBettingProfitCents(holderTxs)
                val holderAccounts = state.accounts
                    .filter { it.holderId == holder.id && it.currency == currency }
                    .joinToString(", ") { "${it.bookmakerName} (${it.username})" }
                repository.createOrUpdateCommissionSettlement(
                    holder = holder,
                    periodLabel = periodLabel,
                    currency = currency,
                    eligibleProfitCents = eligibleProfit,
                    includedAccounts = holderAccounts.ifBlank { "Cuentas en $currency" },
                    manualAdjustmentCents = manualAdjustmentCents,
                    adjustmentReason = adjustmentReason
                )
                _statusMessage.value = "Liquidación de comisión ($periodLabel) generada para ${holder.name}."
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Error al calcular liquidación"
            }
        }
    }

    fun payCommission(
        settlement: CommissionSettlement,
        amountPaidCents: Long,
        paymentMethod: String,
        walletId: String = "",
        walletName: String = "",
        receiptPath: String = ""
    ) {
        viewModelScope.launch {
            try {
                repository.payCommissionSettlement(
                    settlement = settlement,
                    amountPaidCents = amountPaidCents,
                    paymentMethod = paymentMethod,
                    walletId = walletId,
                    walletName = walletName,
                    receiptPath = receiptPath
                )
                _statusMessage.value = "Pago de comisión registrado correctamente."
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Error al pagar comisión"
            }
        }
    }

    fun saveRequirement(requirement: VerificationRequirement) {
        viewModelScope.launch {
            try {
                repository.saveRequirement(requirement)
                _statusMessage.value = "Requisito de verificación guardado."
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Error al guardar requisito"
            }
        }
    }

    fun saveExchangeRate(rate: ExchangeRateRecord) {
        viewModelScope.launch {
            try {
                repository.saveExchangeRate(rate)
                _statusMessage.value = "Tipo de cambio ${rate.fromCurrency}/${rate.toCurrency} registrado."
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Error al guardar tipo de cambio"
            }
        }
    }

    fun dismissAlert(alert: AlertReminder) {
        viewModelScope.launch {
            try {
                repository.dismissAlert(alert)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
            }
        }
    }

    /**
     * Copia una imagen seleccionada con Android Photo Picker al almacenamiento privado local de la app
     * para garantizar persistencia y privacidad.
     */
    fun copyUriToInternalStorage(context: Context, uri: Uri): String {
        return try {
            val dir = File(context.filesDir, "receipts").apply { mkdirs() }
            val file = File(dir, "comprobante_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            uri.toString()
        }
    }

    /**
     * Exporta datos reales almacenados en formato CSV o JSON estructurado.
     */
    fun generateExportContent(exportType: String): String {
        val s = uiState.value
        return when (exportType) {
            "TITULARES_CSV" -> buildString {
                appendLine("ID,Nombre,Pais,Tipo,ComisionPct,Modalidad,Periodicidad,ComisionPendiente,Estado")
                s.holders.forEach {
                    appendLine("${it.id},\"${it.name}\",${it.country},${it.holderType},${it.commissionPct},${it.commissionMode},${it.settlementPeriod},${MoneyUtils.centsToDecimalString(it.pendingCommissionCents)},${it.status}")
                }
            }
            "CUENTAS_CSV" -> buildString {
                appendLine("ID,Titular,Casa,Pais,Usuario,Moneda,SaldoInicial,SaldoCalculado,ITF,Estado")
                s.accounts.forEach {
                    appendLine("${it.id},\"${it.holderName}\",\"${it.bookmakerName}\",${it.country},${it.username},${it.currency},${MoneyUtils.centsToDecimalString(it.initialBalanceCents)},${MoneyUtils.centsToDecimalString(it.calculatedBalanceCents)},${it.itfOverride},${it.accountStatus}")
                }
            }
            "MOVIMIENTOS_CSV" -> buildString {
                appendLine("ID,Fecha,Tipo,Titular,Casa,Cuenta,Billetera,Moneda,Importe,MedioPago,Estado,Referencia")
                s.transactions.forEach {
                    appendLine("${it.id},${it.transactionDate},${it.type},\"${it.holderName}\",\"${it.bookmakerName}\",${it.accountUsername},\"${it.walletName}\",${it.currency},${MoneyUtils.centsToDecimalString(it.amountCents)},\"${it.paymentMethod}\",${it.status},\"${it.referenceCode}\"")
                }
            }
            else -> {
                val root = JSONObject().apply {
                    put("app", "BetManager Pro")
                    put("version", 1)
                    put("holdersCount", s.holders.size)
                    put("accountsCount", s.accounts.size)
                    put("transactionsCount", s.transactions.size)
                    put("walletsCount", s.wallets.size)
                    val holdersArr = JSONArray()
                    s.holders.forEach { h ->
                        holdersArr.put(JSONObject().apply {
                            put("id", h.id)
                            put("name", h.name)
                            put("country", h.country)
                            put("commissionPct", h.commissionPct)
                        })
                    }
                    put("holders", holdersArr)
                }
                root.toString(2)
            }
        }
    }
}

private data class CoreData(
    val holders: List<AccountHolder>,
    val bookmakers: List<Bookmaker>,
    val accounts: List<BettingAccount>,
    val wallets: List<Wallet>,
    val transactions: List<FinancialTransaction>
)

private data class SecondaryData(
    val relations: List<BookmakerRelation>,
    val commissions: List<CommissionSettlement>,
    val requirements: List<VerificationRequirement>,
    val history: List<StatusHistoryRecord>,
    val attachments: List<AttachmentRecord>
)

private data class PrefsState(
    val hideAmounts: Boolean,
    val darkThemeOverride: Boolean?,
    val baseCurrency: String,
    val selectedPeriodDays: Int,
    val statusMessage: String?
)
