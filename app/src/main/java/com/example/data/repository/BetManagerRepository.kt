package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AccountHolder
import com.example.data.model.AdminUser
import com.example.data.model.AlertReminder
import com.example.data.model.AttachmentRecord
import com.example.data.model.BalanceReconciliation
import com.example.data.model.BettingAccount
import com.example.data.model.Bookmaker
import com.example.data.model.BookmakerCatalogSeeder
import com.example.data.model.BookmakerRelation
import com.example.data.model.CommissionSettlement
import com.example.data.model.ExchangeRateRecord
import com.example.data.model.FinancialTransaction
import com.example.data.model.StatusHistoryRecord
import com.example.data.model.VerificationRequirement
import com.example.data.model.Wallet
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BetManagerRepository(private val db: FirebaseFirestore) {

    // CRITICAL: Always initialize with R.string.firestore_database_id
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("El administrador debe haber iniciado sesión con Google para operar en Firestore.")
    }

    private fun currentDateStr(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    // --- 1. Admin Profile & Initial Catalog Seed ---
    suspend fun ensureAdminProfileAndSeedCatalog() {
        val uid = requireUserId()
        val email = auth.currentUser?.email ?: ""
        val displayName = auth.currentUser?.displayName ?: "Administrador Principal"
        val profileRef = userDoc(uid)
        try {
            val snap = profileRef.get().await()
            if (!snap.exists()) {
                val adminUser = AdminUser(
                    id = uid,
                    userId = uid,
                    email = email,
                    displayName = displayName,
                    role = "OWNER",
                    baseCurrency = "PEN",
                    createdAt = null,
                    updatedAt = null
                )
                profileRef.set(adminUser).await()
            }
            val bmSnap = profileRef.collection("bookmakers").limit(1).get().await()
            if (bmSnap.isEmpty) {
                seedInitialCatalog(uid)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, profileRef.path)
            throw e
        }
    }

    suspend fun seedInitialCatalog(uid: String = requireUserId()) {
        val colRef = userDoc(uid).collection("bookmakers")
        try {
            val initialList = BookmakerCatalogSeeder.initialBookmakers(uid)
            for (bm in initialList) {
                val docRef = colRef.document(bm.id)
                docRef.set(bm.copy(userId = uid, createdAt = null, updatedAt = null)).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colRef.path)
            throw e
        }
    }

    // --- 2. Realtime Observation Flows ---

    fun observeHolders(): Flow<List<AccountHolder>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/holders"
        emitAll(
            userDoc(uid).collection("holders")
                .snapshots()
                .map { it.toObjects(AccountHolder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { e ->
                    if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                    throw e
                }
        )
    }

    fun observeBookmakers(): Flow<List<Bookmaker>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/bookmakers"
        emitAll(
            userDoc(uid).collection("bookmakers")
                .snapshots()
                .map { it.toObjects(Bookmaker::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { e ->
                    if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                    throw e
                }
        )
    }

    fun observeRelations(): Flow<List<BookmakerRelation>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/relations"
        emitAll(
            userDoc(uid).collection("relations")
                .snapshots()
                .map { it.toObjects(BookmakerRelation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { e ->
                    if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                    throw e
                }
        )
    }

    fun observeAccounts(): Flow<List<BettingAccount>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/accounts"
        emitAll(
            userDoc(uid).collection("accounts")
                .snapshots()
                .map { it.toObjects(BettingAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { e ->
                    if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                    throw e
                }
        )
    }

    fun observeWallets(): Flow<List<Wallet>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/wallets"
        emitAll(
            userDoc(uid).collection("wallets")
                .snapshots()
                .map { it.toObjects(Wallet::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { e ->
                    if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                    throw e
                }
        )
    }

    fun observeTransactions(): Flow<List<FinancialTransaction>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/transactions"
        emitAll(
            userDoc(uid).collection("transactions")
                .snapshots()
                .map { it.toObjects(FinancialTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { e ->
                    if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                    throw e
                }
        )
    }

    fun observeCommissions(): Flow<List<CommissionSettlement>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/commissions"
        emitAll(
            userDoc(uid).collection("commissions")
                .snapshots()
                .map { it.toObjects(CommissionSettlement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { e ->
                    if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                    throw e
                }
        )
    }

    fun observeRequirements(): Flow<List<VerificationRequirement>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/requirements"
        emitAll(
            userDoc(uid).collection("requirements")
                .snapshots()
                .map { it.toObjects(VerificationRequirement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { e ->
                    if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                    throw e
                }
        )
    }

    fun observeHistory(): Flow<List<StatusHistoryRecord>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/history"
        emitAll(
            userDoc(uid).collection("history")
                .snapshots()
                .map { it.toObjects(StatusHistoryRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { e ->
                    if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                    throw e
                }
        )
    }

    fun observeAttachments(): Flow<List<AttachmentRecord>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/attachments"
        emitAll(
            userDoc(uid).collection("attachments")
                .snapshots()
                .map { it.toObjects(AttachmentRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { e ->
                    if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                    throw e
                }
        )
    }

    fun observeAlerts(): Flow<List<AlertReminder>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/alerts"
        emitAll(
            userDoc(uid).collection("alerts")
                .snapshots()
                .map { it.toObjects(AlertReminder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { e ->
                    if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                    throw e
                }
        )
    }

    fun observeReconciliations(): Flow<List<BalanceReconciliation>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/reconciliations"
        emitAll(
            userDoc(uid).collection("reconciliations")
                .snapshots()
                .map { it.toObjects(BalanceReconciliation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { e ->
                    if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                    throw e
                }
        )
    }

    fun observeExchangeRates(): Flow<List<ExchangeRateRecord>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/exchange_rates"
        emitAll(
            userDoc(uid).collection("exchange_rates")
                .snapshots()
                .map { it.toObjects(ExchangeRateRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { e ->
                    if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                    throw e
                }
        )
    }

    // --- 3. CRUD & Business Operations ---

    suspend fun saveHolder(holder: AccountHolder): String {
        val uid = requireUserId()
        val col = userDoc(uid).collection("holders")
        val docRef = if (holder.id.isBlank()) col.document() else col.document(holder.id)
        try {
            if (holder.id.isBlank() || holder.createdAt == null) {
                docRef.set(holder.copy(id = docRef.id, userId = uid, createdAt = null, updatedAt = null)).await()
            } else {
                docRef.set(holder.copy(userId = uid, updatedAt = null)).await()
            }
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun getHolderById(ownerUid: String, holderId: String): AccountHolder? {
        val docRef = userDoc(ownerUid).collection("holders").document(holderId)
        return try {
            docRef.get().await().toObject(AccountHolder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun saveBookmaker(bookmaker: Bookmaker): String {
        val uid = requireUserId()
        val col = userDoc(uid).collection("bookmakers")
        val docRef = if (bookmaker.id.isBlank()) col.document() else col.document(bookmaker.id)
        try {
            if (bookmaker.id.isBlank() || bookmaker.createdAt == null) {
                docRef.set(bookmaker.copy(id = docRef.id, userId = uid, createdAt = null, updatedAt = null)).await()
            } else {
                docRef.set(bookmaker.copy(userId = uid, updatedAt = null)).await()
            }
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun saveRelation(relation: BookmakerRelation): String {
        val uid = requireUserId()
        val col = userDoc(uid).collection("relations")
        val docRef = if (relation.id.isBlank()) col.document() else col.document(relation.id)
        try {
            if (relation.id.isBlank() || relation.createdAt == null) {
                docRef.set(relation.copy(id = docRef.id, userId = uid, createdAt = null, updatedAt = null)).await()
            } else {
                docRef.set(relation.copy(userId = uid, updatedAt = null)).await()
            }
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun deleteRelation(relationId: String) {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("relations").document(relationId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun saveWallet(wallet: Wallet): String {
        val uid = requireUserId()
        val col = userDoc(uid).collection("wallets")
        val docRef = if (wallet.id.isBlank()) col.document() else col.document(wallet.id)
        try {
            if (wallet.id.isBlank() || wallet.createdAt == null) {
                docRef.set(
                    wallet.copy(
                        id = docRef.id,
                        userId = uid,
                        currentBalanceCents = wallet.initialBalanceCents,
                        createdAt = null,
                        updatedAt = null
                    )
                ).await()
            } else {
                docRef.set(wallet.copy(userId = uid, updatedAt = null)).await()
            }
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun saveAccount(account: BettingAccount): String {
        val uid = requireUserId()
        val col = userDoc(uid).collection("accounts")
        val isNew = account.id.isBlank() || account.createdAt == null
        val docRef = if (account.id.isBlank()) col.document() else col.document(account.id)
        try {
            if (isNew) {
                val createdAccount = account.copy(
                    id = docRef.id,
                    userId = uid,
                    calculatedBalanceCents = account.initialBalanceCents,
                    lastVerifiedBalanceCents = account.initialBalanceCents,
                    lastActivityDate = currentDateStr(),
                    createdAt = null,
                    updatedAt = null
                )
                docRef.set(createdAccount).await()
                logStatusChange(
                    entityType = "CUENTA",
                    entityId = docRef.id,
                    entityLabel = "${account.bookmakerName} (${account.username})",
                    previousState = "NUEVA",
                    newState = account.accountStatus,
                    reason = "Apertura de cuenta individual"
                )
            } else {
                docRef.set(account.copy(userId = uid, updatedAt = null)).await()
            }
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    /**
     * Cambia el estado de una cuenta (ej. a LIMITADA o CERRADA), conservando todo su historial
     * de movimientos financieros y registrando trazabilidad en StatusHistoryRecord.
     */
    suspend fun updateAccountStatus(
        account: BettingAccount,
        newStatus: String,
        reason: String,
        limitationCategory: String = "",
        limitationDescription: String = "",
        closureReason: String = "",
        finalWithdrawalStatus: String = "",
        screenshotPath: String = ""
    ) {
        val uid = requireUserId()
        require(account.id.isNotBlank()) { "Account ID must not be blank" }
        val docRef = userDoc(uid).collection("accounts").document(account.id)
        val nowStr = currentDateStr()
        try {
            val existingSnap = docRef.get().await()
            val existingModel = existingSnap.toObject(BettingAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: account
            val updated = existingModel.copy(
                userId = uid,
                accountStatus = newStatus,
                limitationDate = if (newStatus == "LIMITADA") nowStr else existingModel.limitationDate,
                limitationCategory = if (limitationCategory.isNotBlank()) limitationCategory else existingModel.limitationCategory,
                limitationDescription = if (limitationDescription.isNotBlank()) limitationDescription else existingModel.limitationDescription,
                closureDate = if (newStatus == "CERRADA") nowStr else existingModel.closureDate,
                closureReason = if (closureReason.isNotBlank()) closureReason else existingModel.closureReason,
                finalWithdrawalStatus = if (finalWithdrawalStatus.isNotBlank()) finalWithdrawalStatus else existingModel.finalWithdrawalStatus,
                screenshotPath = if (screenshotPath.isNotBlank()) screenshotPath else existingModel.screenshotPath,
                lastActivityDate = nowStr,
                updatedAt = null
            )
            docRef.set(updated).await()
            logStatusChange(
                entityType = "CUENTA",
                entityId = account.id,
                entityLabel = "${account.bookmakerName} (${account.username})",
                previousState = existingModel.accountStatus,
                newState = newStatus,
                reason = reason.ifBlank { "Actualización de estado operativo" }
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    /**
     * Modifica únicamente la disponibilidad particular del mercado ITF en una cuenta individual,
     * sin alterar automáticamente las demás cuentas ni el catálogo general.
     */
    suspend fun updateAccountItfOverride(accountId: String, itfOverride: String) {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("accounts").document(accountId)
        try {
            val snap = docRef.get().await()
            val current = snap.toObject(BettingAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: throw IllegalStateException("Cuenta no encontrada")
            docRef.set(current.copy(userId = uid, itfOverride = itfOverride, updatedAt = null)).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    /**
     * Registra un movimiento financiero y recalcula de forma auditable el saldo de la cuenta
     * y/o billetera afectada.
     */
    suspend fun recordTransaction(transaction: FinancialTransaction): String {
        val uid = requireUserId()
        require(transaction.amountCents >= 0) { "El importe no puede ser negativo" }
        val txCol = userDoc(uid).collection("transactions")
        val docRef = if (transaction.id.isBlank()) txCol.document() else txCol.document(transaction.id)
        try {
            val txToSave = transaction.copy(
                id = docRef.id,
                userId = uid,
                transactionDate = transaction.transactionDate.ifBlank { currentDateStr() },
                createdAt = null,
                updatedAt = null
            )
            docRef.set(txToSave).await()

            if (txToSave.receiptImagePath.isNotBlank()) {
                saveAttachment(
                    AttachmentRecord(
                        userId = uid,
                        ownerType = "MOVIMIENTO",
                        ownerId = docRef.id,
                        title = "Comprobante ${txToSave.type} (${txToSave.accountUsername.ifBlank { txToSave.walletName }})",
                        localUriPath = txToSave.receiptImagePath,
                        notes = txToSave.referenceCode
                    )
                )
            }

            recalculateBalancesForAccountAndWallets(
                uid = uid,
                accountId = txToSave.accountId,
                walletId = txToSave.walletId,
                destinationWalletId = txToSave.destinationWalletId
            )
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    /**
     * Actualiza el estado de un retiro (ej. de PENDIENTE a COMPLETADO o RECHAZADO)
     * sin duplicar capital ni crear ganancias falsas.
     */
    suspend fun updateTransactionStatus(transactionId: String, newStatus: String, newType: String? = null) {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("transactions").document(transactionId)
        try {
            val snap = docRef.get().await()
            val existingTx = snap.toObject(FinancialTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: throw IllegalStateException("Movimiento no encontrado")
            val updatedTx = existingTx.copy(
                userId = uid,
                status = newStatus,
                type = newType ?: existingTx.type,
                updatedAt = null
            )
            docRef.set(updatedTx).await()
            recalculateBalancesForAccountAndWallets(
                uid = uid,
                accountId = updatedTx.accountId,
                walletId = updatedTx.walletId,
                destinationWalletId = updatedTx.destinationWalletId
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    private suspend fun recalculateBalancesForAccountAndWallets(
        uid: String,
        accountId: String,
        walletId: String,
        destinationWalletId: String
    ) {
        val allTxsSnap = userDoc(uid).collection("transactions").get().await()
        val allTxs = allTxsSnap.toObjects(FinancialTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)

        if (accountId.isNotBlank()) {
            val accRef = userDoc(uid).collection("accounts").document(accountId)
            val accSnap = accRef.get().await()
            val acc = accSnap.toObject(BettingAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            if (acc != null) {
                val accTxs = allTxs.filter { it.accountId == accountId }
                val newCalcBalance = FinancialCalculator.calculateAccountBalanceCents(acc.initialBalanceCents, accTxs)
                accRef.set(
                    acc.copy(
                        userId = uid,
                        calculatedBalanceCents = newCalcBalance,
                        lastActivityDate = currentDateStr(),
                        updatedAt = null
                    )
                ).await()
            }
        }

        val walletIdsToUpdate = listOf(walletId, destinationWalletId).filter { it.isNotBlank() }.distinct()
        for (wId in walletIdsToUpdate) {
            val wRef = userDoc(uid).collection("wallets").document(wId)
            val wSnap = wRef.get().await()
            val wallet = wSnap.toObject(Wallet::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            if (wallet != null) {
                val newWalletBalance = FinancialCalculator.calculateWalletBalanceCents(wallet, allTxs)
                wRef.set(
                    wallet.copy(
                        userId = uid,
                        currentBalanceCents = newWalletBalance,
                        updatedAt = null
                    )
                ).await()
            }
        }
    }

    /**
     * Sistema de conciliación de saldos: compara el saldo calculado con el saldo observado en la casa,
     * conserva trazabilidad en BalanceReconciliation y crea un movimiento AJUSTE_CONCILIACION auditable si hay diferencia.
     */
    suspend fun reconcileAccountBalance(
        account: BettingAccount,
        observedBalanceCents: Long,
        reason: String
    ): String {
        val uid = requireUserId()
        require(reason.isNotBlank()) { "El motivo de conciliación es obligatorio para mantener trazabilidad" }
        val diffCents = observedBalanceCents - account.calculatedBalanceCents
        var adjustmentTxId = ""

        if (diffCents != 0L) {
            val isNegative = diffCents < 0
            val absDiff = kotlin.math.abs(diffCents)
            val notePrefix = if (isNegative) "NEG: " else "POS: "
            adjustmentTxId = recordTransaction(
                FinancialTransaction(
                    userId = uid,
                    type = "AJUSTE_CONCILIACION",
                    holderId = account.holderId,
                    holderName = account.holderName,
                    bookmakerId = account.bookmakerId,
                    bookmakerName = account.bookmakerName,
                    accountId = account.id,
                    accountUsername = account.username,
                    transactionDate = currentDateStr(),
                    currency = account.currency,
                    amountCents = absDiff,
                    paymentMethod = "Conciliación de Saldo",
                    status = "COMPLETADO",
                    notes = "$notePrefix$reason"
                )
            )
        }

        val accRef = userDoc(uid).collection("accounts").document(account.id)
        val freshAcc = accRef.get().await()
            .toObject(BettingAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: account
        accRef.set(
            freshAcc.copy(
                userId = uid,
                lastVerifiedBalanceCents = observedBalanceCents,
                lastCheckDate = currentDateStr(),
                updatedAt = null
            )
        ).await()

        val recCol = userDoc(uid).collection("reconciliations")
        val recDoc = recCol.document()
        val rec = BalanceReconciliation(
            id = recDoc.id,
            userId = uid,
            accountId = account.id,
            accountLabel = "${account.bookmakerName} (${account.username})",
            currency = account.currency,
            calculatedBalanceCents = account.calculatedBalanceCents,
            observedBalanceCents = observedBalanceCents,
            differenceCents = diffCents,
            reason = reason,
            reconciliationDate = currentDateStr(),
            adjustmentTransactionId = adjustmentTxId,
            createdAt = null,
            updatedAt = null
        )
        recDoc.set(rec).await()
        return recDoc.id
    }

    /**
     * Crea o actualiza una liquidación de comisión para un titular sobre el beneficio neto elegible
     * (evitando duplicar liquidaciones del mismo periodo y titular).
     */
    suspend fun createOrUpdateCommissionSettlement(
        holder: AccountHolder,
        periodLabel: String,
        currency: String,
        eligibleProfitCents: Long,
        includedAccounts: String,
        manualAdjustmentCents: Long = 0L,
        adjustmentReason: String = ""
    ): String {
        val uid = requireUserId()
        val col = userDoc(uid).collection("commissions")
        val existingSnap = col.get().await()
        val existingList = existingSnap.toObjects(CommissionSettlement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        val duplicate = existingList.find {
            it.holderId == holder.id && it.periodLabel.equals(periodLabel.trim(), ignoreCase = true) && it.currency == currency
        }
        if (duplicate != null && duplicate.status == "PAGADA") {
            throw IllegalStateException("El periodo '$periodLabel' ya fue cerrado y liquidado para este titular.")
        }

        val generatedCents = FinancialCalculator.calculateCommissionForHolder(
            holder = holder,
            eligibleProfitCents = eligibleProfitCents,
            manualAdjustmentCents = manualAdjustmentCents
        )
        val docRef = if (duplicate != null) col.document(duplicate.id) else col.document()
        val paidSoFar = duplicate?.paidAmountCents ?: 0L
        val pendingCents = (generatedCents - paidSoFar).coerceAtLeast(0L)
        val settlement = CommissionSettlement(
            id = docRef.id,
            userId = uid,
            holderId = holder.id,
            holderName = holder.name,
            periodLabel = periodLabel.trim(),
            includedAccounts = includedAccounts,
            currency = currency,
            eligibleProfitCents = eligibleProfitCents,
            manualAdjustmentCents = manualAdjustmentCents,
            adjustmentReason = adjustmentReason,
            appliedPct = holder.commissionPct,
            generatedCommissionCents = generatedCents,
            paidAmountCents = paidSoFar,
            pendingAmountCents = pendingCents,
            status = if (pendingCents == 0L && generatedCents > 0L) "PAGADA" else "PENDIENTE",
            paymentDate = duplicate?.paymentDate ?: "",
            paymentMethod = duplicate?.paymentMethod ?: "",
            receiptPath = duplicate?.receiptPath ?: "",
            createdAt = duplicate?.createdAt,
            updatedAt = null
        )
        docRef.set(settlement).await()
        updateHolderPendingCommission(uid, holder.id)
        return docRef.id
    }

    /**
     * Marca una comisión como pagada, registra el pago en la billetera si se indica y actualiza el saldo pendiente del titular.
     */
    suspend fun payCommissionSettlement(
        settlement: CommissionSettlement,
        amountPaidCents: Long,
        paymentMethod: String,
        walletId: String = "",
        walletName: String = "",
        receiptPath: String = ""
    ) {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("commissions").document(settlement.id)
        val snap = docRef.get().await()
        val current = snap.toObject(CommissionSettlement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: settlement
        val totalPaid = current.paidAmountCents + amountPaidCents
        val remaining = (current.generatedCommissionCents - totalPaid).coerceAtLeast(0L)
        val newStatus = if (remaining == 0L) "PAGADA" else "PARCIAL"
        val nowStr = currentDateStr()

        docRef.set(
            current.copy(
                userId = uid,
                paidAmountCents = totalPaid,
                pendingAmountCents = remaining,
                status = newStatus,
                paymentDate = nowStr,
                paymentMethod = paymentMethod,
                receiptPath = receiptPath.ifBlank { current.receiptPath },
                updatedAt = null
            )
        ).await()

        recordTransaction(
            FinancialTransaction(
                userId = uid,
                type = "PAGO_COMISION",
                holderId = current.holderId,
                holderName = current.holderName,
                walletId = walletId,
                walletName = walletName,
                transactionDate = nowStr,
                currency = current.currency,
                amountCents = amountPaidCents,
                paymentMethod = paymentMethod,
                status = "COMPLETADO",
                notes = "Pago de comisión (${current.periodLabel})",
                receiptImagePath = receiptPath
            )
        )

        updateHolderPendingCommission(uid, current.holderId, lastPaidDate = nowStr)
    }

    private suspend fun updateHolderPendingCommission(uid: String, holderId: String, lastPaidDate: String? = null) {
        val holderRef = userDoc(uid).collection("holders").document(holderId)
        val holderSnap = holderRef.get().await()
        val holder = holderSnap.toObject(AccountHolder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: return
        val settlements = userDoc(uid).collection("commissions").get().await()
            .toObjects(CommissionSettlement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .filter { it.holderId == holderId }
        val totalPending = settlements.sumOf { it.pendingAmountCents }
        holderRef.set(
            holder.copy(
                userId = uid,
                pendingCommissionCents = totalPending,
                lastCommissionPaidDate = lastPaidDate ?: holder.lastCommissionPaidDate,
                updatedAt = null
            )
        ).await()
    }

    suspend fun saveRequirement(requirement: VerificationRequirement): String {
        val uid = requireUserId()
        val col = userDoc(uid).collection("requirements")
        val docRef = if (requirement.id.isBlank()) col.document() else col.document(requirement.id)
        if (requirement.id.isBlank() || requirement.createdAt == null) {
            docRef.set(
                requirement.copy(
                    id = docRef.id,
                    userId = uid,
                    requestDate = requirement.requestDate.ifBlank { currentDateStr() },
                    createdAt = null,
                    updatedAt = null
                )
            ).await()
        } else {
            docRef.set(requirement.copy(userId = uid, updatedAt = null)).await()
        }
        return docRef.id
    }

    suspend fun saveAttachment(attachment: AttachmentRecord): String {
        val uid = requireUserId()
        val col = userDoc(uid).collection("attachments")
        val docRef = if (attachment.id.isBlank()) col.document() else col.document(attachment.id)
        docRef.set(
            attachment.copy(
                id = docRef.id,
                userId = uid,
                createdAt = null,
                updatedAt = null
            )
        ).await()
        return docRef.id
    }

    suspend fun saveAlert(alert: AlertReminder): String {
        val uid = requireUserId()
        val col = userDoc(uid).collection("alerts")
        val docRef = if (alert.id.isBlank()) col.document() else col.document(alert.id)
        docRef.set(
            alert.copy(
                id = docRef.id,
                userId = uid,
                createdAt = null,
                updatedAt = null
            )
        ).await()
        return docRef.id
    }

    suspend fun dismissAlert(alert: AlertReminder) {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("alerts").document(alert.id)
        val snap = docRef.get().await()
        val existing = snap.toObject(AlertReminder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: alert
        docRef.set(existing.copy(userId = uid, isDismissed = true, updatedAt = null)).await()
    }

    suspend fun saveExchangeRate(rateRecord: ExchangeRateRecord): String {
        val uid = requireUserId()
        val col = userDoc(uid).collection("exchange_rates")
        val docRef = if (rateRecord.id.isBlank()) col.document() else col.document(rateRecord.id)
        docRef.set(
            rateRecord.copy(
                id = docRef.id,
                userId = uid,
                rateDate = rateRecord.rateDate.ifBlank { currentDateStr() },
                createdAt = null,
                updatedAt = null
            )
        ).await()
        return docRef.id
    }

    private suspend fun logStatusChange(
        entityType: String,
        entityId: String,
        entityLabel: String,
        previousState: String,
        newState: String,
        reason: String
    ) {
        val uid = requireUserId()
        val col = userDoc(uid).collection("history")
        val docRef = col.document()
        val record = StatusHistoryRecord(
            id = docRef.id,
            userId = uid,
            entityType = entityType,
            entityId = entityId,
            entityLabel = entityLabel,
            previousState = previousState,
            newState = newState,
            reason = reason,
            changeDate = currentDateStr(),
            createdAt = null,
            updatedAt = null
        )
        docRef.set(record).await()
    }
}
