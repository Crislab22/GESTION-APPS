package com.example.data.repository

import com.example.data.model.AccountHolder
import com.example.data.model.BettingAccount
import com.example.data.model.FinancialTransaction
import com.example.data.model.Wallet
import kotlin.math.roundToLong

data class CurrencySummary(
    val currency: String,
    val totalInBookmakersCents: Long,
    val totalInWalletsCents: Long,
    val pendingWithdrawalsCents: Long,
    val totalCapitalCents: Long,
    val totalDepositsCents: Long,
    val totalCompletedWithdrawalsCents: Long,
    val netBettingProfitCents: Long
)

object FinancialCalculator {

    /**
     * Calcula el saldo actual de una cuenta individual a partir de su saldo inicial
     * y los movimientos confirmados / en tránsito.
     *
     * Regla indispensable:
     * - SALDO_INICIAL: ya está reflejado en initialBalanceCents (o si se registra como movimiento adicional, suma).
     * - DEPOSITO (COMPLETADO): aumenta el saldo de la casa.
     * - RETIRO / RETIRO_COMPLETADO (COMPLETADO): reduce el saldo de la casa (transfiere a billetera, NO es ganancia).
     * - RETIRO_PENDIENTE (o RETIRO con estado PENDIENTE): reduce el saldo disponible en la casa y pasa a "Dinero en tránsito".
     * - RETIRO_RECHAZADO (o estado RECHAZADO / CANCELADO): no reduce el saldo de la casa (fondos devueltos a la cuenta).
     * - RESULTADO_POSITIVO (COMPLETADO): aumenta el saldo de la casa y suma al beneficio neto de apuestas.
     * - RESULTADO_NEGATIVO (COMPLETADO): reduce el saldo de la casa y resta al beneficio neto de apuestas.
     * - BONO (COMPLETADO): aumenta el saldo de la casa y suma al beneficio elegible.
     * - COMISION_CARGO (COMPLETADO): reduce el saldo de la casa.
     * - AJUSTE_CONCILIACION (COMPLETADO): suma o resta según sign (o nota), sin confundirse con ganancia de apuestas.
     */
    fun calculateAccountBalanceCents(
        initialBalanceCents: Long,
        accountTransactions: List<FinancialTransaction>
    ): Long {
        var balance = initialBalanceCents
        for (tx in accountTransactions) {
            if (tx.status == "CANCELADO" || tx.status == "RECHAZADO" || tx.type == "RETIRO_RECHAZADO") {
                continue
            }
            when (tx.type) {
                "DEPOSITO" -> if (tx.status == "COMPLETADO") balance += tx.amountCents
                "RETIRO", "RETIRO_COMPLETADO" -> {
                    // Tanto si está COMPLETADO como PENDIENTE, ya salió del saldo disponible de la casa
                    if (tx.status == "COMPLETADO" || tx.status == "PENDIENTE") {
                        balance -= tx.amountCents
                    }
                }
                "RETIRO_PENDIENTE" -> {
                    if (tx.status == "PENDIENTE" || tx.status == "COMPLETADO") {
                        balance -= tx.amountCents
                    }
                }
                "RESULTADO_POSITIVO", "BONO" -> if (tx.status == "COMPLETADO") balance += tx.amountCents
                "RESULTADO_NEGATIVO", "COMISION_CARGO" -> if (tx.status == "COMPLETADO") balance -= tx.amountCents
                "AJUSTE_CONCILIACION" -> if (tx.status == "COMPLETADO") {
                    if (tx.notes.startsWith("NEG:")) {
                        balance -= tx.amountCents
                    } else {
                        balance += tx.amountCents
                    }
                }
            }
        }
        return balance
    }

    /**
     * Calcula el saldo actual de una billetera o caja central a partir de su saldo inicial
     * y los movimientos confirmados vinculados a ella.
     */
    fun calculateWalletBalanceCents(
        wallet: Wallet,
        allTransactions: List<FinancialTransaction>
    ): Long {
        var balance = wallet.initialBalanceCents
        for (tx in allTransactions) {
            if (tx.status != "COMPLETADO") continue
            when (tx.type) {
                "DEPOSITO" -> {
                    if (tx.walletId == wallet.id) {
                        balance -= tx.amountCents
                    }
                }
                "RETIRO", "RETIRO_COMPLETADO" -> {
                    if (tx.walletId == wallet.id || tx.destinationWalletId == wallet.id) {
                        balance += tx.amountCents
                    }
                }
                "TRANSFERENCIA_INTERNA" -> {
                    if (tx.walletId == wallet.id) balance -= tx.amountCents
                    if (tx.destinationWalletId == wallet.id) balance += tx.amountCents
                }
                "PAGO_COMISION", "COMISION_CARGO" -> {
                    if (tx.walletId == wallet.id) balance -= tx.amountCents
                }
            }
        }
        return balance
    }

    /**
     * Calcula el beneficio o pérdida neta de apuestas (separado estrictamente de depósitos, retiros y ajustes).
     */
    fun calculateNetBettingProfitCents(transactions: List<FinancialTransaction>): Long {
        var netProfit = 0L
        for (tx in transactions) {
            if (tx.status != "COMPLETADO") continue
            when (tx.type) {
                "RESULTADO_POSITIVO", "BONO" -> netProfit += tx.amountCents
                "RESULTADO_NEGATIVO" -> netProfit -= tx.amountCents
            }
        }
        return netProfit
    }

    /**
     * Calcula el dinero en tránsito (retiros pendientes que aún no han llegado a la billetera).
     */
    fun calculatePendingWithdrawalsCents(transactions: List<FinancialTransaction>): Long {
        return transactions
            .filter {
                (it.type == "RETIRO_PENDIENTE" && it.status == "PENDIENTE") ||
                    (it.type == "RETIRO" && it.status == "PENDIENTE")
            }
            .sumOf { it.amountCents }
    }

    /**
     * Calcula la comisión generada para un titular sobre el beneficio neto elegible.
     * Cuando no existe beneficio elegible (<= 0), no genera comisión positiva basada en beneficios.
     */
    fun calculateCommissionForHolder(
        holder: AccountHolder,
        eligibleProfitCents: Long,
        manualAdjustmentCents: Long = 0L
    ): Long {
        val baseCommission = if (eligibleProfitCents > 0L && holder.commissionPct > 0.0) {
            ((eligibleProfitCents.toDouble() * holder.commissionPct) / 100.0).roundToLong()
        } else {
            0L
        }
        return (baseCommission + manualAdjustmentCents).coerceAtLeast(0L)
    }

    /**
     * Agrupa los indicadores financieros por moneda original (sin mezclar monedas distintas sin conversión).
     */
    fun buildSummariesByCurrency(
        accounts: List<BettingAccount>,
        wallets: List<Wallet>,
        transactions: List<FinancialTransaction>
    ): List<CurrencySummary> {
        val currencies = (accounts.map { it.currency } +
            wallets.map { it.currency } +
            transactions.map { it.currency })
            .filter { it.isNotBlank() }
            .distinct()
            .ifEmpty { listOf("PEN") }

        return currencies.map { currency ->
            val currencyAccounts = accounts.filter { it.currency == currency }
            val currencyWallets = wallets.filter { it.currency == currency }
            val currencyTxs = transactions.filter { it.currency == currency }

            val totalInBookmakers = currencyAccounts.sumOf { it.calculatedBalanceCents }
            val totalInWallets = currencyWallets.sumOf { it.currentBalanceCents }
            val pendingWithdrawals = calculatePendingWithdrawalsCents(currencyTxs)
            val totalDeposits = currencyTxs
                .filter { it.type == "DEPOSITO" && it.status == "COMPLETADO" }
                .sumOf { it.amountCents }
            val totalWithdrawals = currencyTxs
                .filter { (it.type == "RETIRO" || it.type == "RETIRO_COMPLETADO") && it.status == "COMPLETADO" }
                .sumOf { it.amountCents }
            val netProfit = calculateNetBettingProfitCents(currencyTxs)

            CurrencySummary(
                currency = currency,
                totalInBookmakersCents = totalInBookmakers,
                totalInWalletsCents = totalInWallets,
                pendingWithdrawalsCents = pendingWithdrawals,
                totalCapitalCents = totalInBookmakers + totalInWallets + pendingWithdrawals,
                totalDepositsCents = totalDeposits,
                totalCompletedWithdrawalsCents = totalWithdrawals,
                netBettingProfitCents = netProfit
            )
        }
    }
}
