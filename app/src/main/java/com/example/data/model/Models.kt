package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

enum class SupportedCurrency(val code: String, val symbol: String, val displayName: String) {
    PEN("PEN", "S/", "Sol peruano"),
    CLP("CLP", "CLP$", "Peso chileno"),
    COP("COP", "COP$", "Peso colombiano"),
    ARS("ARS", "ARS$", "Peso argentino"),
    MXN("MXN", "MX$", "Peso mexicano"),
    USD("USD", "US$", "Dólar estadounidense"),
    BOB("BOB", "Bs.", "Boliviano"),
    EUR("EUR", "€", "Euro");

    companion object {
        fun fromCode(code: String): SupportedCurrency =
            entries.find { it.code.equals(code, ignoreCase = true) } ?: PEN
    }
}

object MoneyUtils {
    private val symbols = DecimalFormatSymbols(Locale.US).apply {
        groupingSeparator = ','
        decimalSeparator = '.'
    }
    private val formatter = DecimalFormat("#,##0.00", symbols)

    fun parseToCents(input: String): Long? {
        val cleaned = input.trim().replace(",", "").replace(" ", "")
        if (cleaned.isEmpty()) return null
        return try {
            val bd = BigDecimal(cleaned).setScale(2, RoundingMode.HALF_UP)
            bd.multiply(BigDecimal(100)).longValueExact()
        } catch (e: Exception) {
            null
        }
    }

    fun formatCents(cents: Long, currencyCode: String = "PEN", hideAmounts: Boolean = false): String {
        val symbol = SupportedCurrency.fromCode(currencyCode).symbol
        if (hideAmounts) return "$symbol ••••"
        val isNegative = cents < 0
        val absCents = kotlin.math.abs(cents)
        val bd = BigDecimal(absCents).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
        val formatted = formatter.format(bd)
        return if (isNegative) "-$symbol $formatted" else "$symbol $formatted"
    }

    fun centsToDecimalString(cents: Long): String {
        val bd = BigDecimal(cents).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
        return bd.toPlainString()
    }
}

@IgnoreExtraProperties
data class AdminUser(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = "OWNER",
    val baseCurrency: String = "PEN",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AccountHolder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val country: String = "Perú",
    val contactInfo: String = "",
    val joinDate: String = "",
    val holderType: String = "TERCERO", // PROPIA | TERCERO
    val commissionPct: Double = 20.0,
    val commissionMode: String = "PORCENTAJE_BENEFICIO", // PORCENTAJE_BENEFICIO | FIJO_PERIODO | PERSONALIZADO
    val settlementPeriod: String = "MENSUAL", // SEMANAL | QUINCENAL | MENSUAL | MANUAL
    val compensatePreviousLosses: Boolean = true,
    val lastCommissionPaidDate: String = "",
    val pendingCommissionCents: Long = 0L,
    val notes: String = "",
    val status: String = "ACTIVO", // ACTIVO | INACTIVO | ARCHIVADO
    val authorizationRef: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Bookmaker(
    @DocumentId val id: String = "",
    val userId: String = "",
    val brandName: String = "",
    val country: String = "Perú",
    val domain: String = "",
    val operatorCompany: String = "Por verificar",
    val platformProvider: String = "Por verificar",
    val oddsProvider: String = "Por verificar",
    val primaryCurrency: String = "PEN",
    val regulatoryStatus: String = "PENDIENTE_VERIFICAR", // VERIFICADO | PENDIENTE_VERIFICAR | NO_DISPONIBLE | DESCONOCIDO
    val verificationSource: String = "",
    val verificationDate: String = "",
    val itfAvailability: String = "POR_VERIFICAR", // SI | NO | PARCIAL | POR_VERIFICAR (Tenis ITF)
    val liveBettingAvailable: String = "POR_VERIFICAR", // SI | NO | POR_VERIFICAR
    val marketNotes: String = "",
    val lastCheckedDate: String = "",
    val verificationEase: String = "SIN_EVALUAR", // FACIL | MEDIA | DIFICIL | SIN_EVALUAR
    val withdrawalEase: String = "SIN_EVALUAR", // FACIL | MEDIA | DIFICIL | SIN_EVALUAR
    val estimatedWithdrawalTime: String = "Por verificar",
    val minDepositCents: Long = 0L,
    val minWithdrawalCents: Long = 0L,
    val paymentMethods: String = "Por verificar",
    val knownFees: String = "Por verificar",
    val knownKycRequirements: String = "Por verificar",
    val operationalNotes: String = "",
    val dataReliability: String = "REFERENCIA_PROVISIONAL", // VERIFICADO | REFERENCIA_PROVISIONAL | USUARIO
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BookmakerRelation(
    @DocumentId val id: String = "",
    val userId: String = "",
    val relationType: String = "MISMA_PLATAFORMA", // MISMO_OPERADOR | MISMA_PLATAFORMA | MISMO_SPORTSBOOK | MISMO_PROVEEDOR_CUOTAS | RELACION_OPERATIVA
    val bookmakerIds: String = "",
    val bookmakerNames: String = "",
    val affectedCountries: String = "",
    val evidenceSource: String = "",
    val confidenceLevel: String = "POR_VERIFICAR", // ALTO | MEDIO | BAJO | POR_VERIFICAR
    val checkDate: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BettingAccount(
    @DocumentId val id: String = "",
    val userId: String = "",
    val holderId: String = "",
    val holderName: String = "",
    val bookmakerId: String = "",
    val bookmakerName: String = "",
    val country: String = "Perú",
    val username: String = "",
    val internalAlias: String = "",
    val currency: String = "PEN",
    val openDate: String = "",
    val initialBalanceCents: Long = 0L,
    val calculatedBalanceCents: Long = 0L,
    val lastVerifiedBalanceCents: Long = 0L,
    val lastCheckDate: String = "",
    val lastActivityDate: String = "",
    val verificationStatus: String = "PENDIENTE", // VERIFICADA | PENDIENTE | EN_REVISION | OBSERVADA
    val itfOverride: String = "HEREDAR", // HEREDAR | SI | NO | PARCIAL | POR_VERIFICAR
    val customCommissionPct: Double = 0.0,
    val hasCustomCommission: Boolean = false,
    val accountStatus: String = "ACTIVA",
    // Estados posibles:
    // PENDIENTE_APERTURA, PENDIENTE_VERIFICACION, ACTIVA, RESTRICCION_PARCIAL,
    // LIMITADA, SUSPENDIDA, RETIRO_FINAL, CERRADA
    val limitationDate: String = "",
    val limitationCategory: String = "", // MERCADO | IMPORTE | CUOTA | OTRA
    val limitationDescription: String = "",
    val closureDate: String = "",
    val closureReason: String = "",
    val finalWithdrawalStatus: String = "",
    val screenshotPath: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Wallet(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val walletType: String = "BANCO", // BANCO | BILLETERA_DIGITAL | EFECTIVO | CRIPTO | OTRO
    val currency: String = "PEN",
    val initialBalanceCents: Long = 0L,
    val currentBalanceCents: Long = 0L,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FinancialTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = "DEPOSITO",
    // SALDO_INICIAL, DEPOSITO, RETIRO, RETIRO_PENDIENTE, RETIRO_COMPLETADO,
    // RETIRO_RECHAZADO, RESULTADO_POSITIVO, RESULTADO_NEGATIVO, BONO,
    // COMISION_CARGO, TRANSFERENCIA_INTERNA, AJUSTE_CONCILIACION, PAGO_COMISION
    val holderId: String = "",
    val holderName: String = "",
    val bookmakerId: String = "",
    val bookmakerName: String = "",
    val accountId: String = "",
    val accountUsername: String = "",
    val walletId: String = "",
    val walletName: String = "",
    val destinationWalletId: String = "",
    val transactionDate: String = "",
    val currency: String = "PEN",
    val amountCents: Long = 0L,
    val paymentMethod: String = "Transferencia Bancaria",
    val status: String = "COMPLETADO", // PENDIENTE | COMPLETADO | RECHAZADO | CANCELADO
    val referenceCode: String = "",
    val notes: String = "",
    val receiptImagePath: String = "",
    val exchangeRateApplied: Double = 1.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CommissionSettlement(
    @DocumentId val id: String = "",
    val userId: String = "",
    val holderId: String = "",
    val holderName: String = "",
    val periodLabel: String = "",
    val includedAccounts: String = "",
    val currency: String = "PEN",
    val eligibleProfitCents: Long = 0L,
    val manualAdjustmentCents: Long = 0L,
    val adjustmentReason: String = "",
    val appliedPct: Double = 20.0,
    val generatedCommissionCents: Long = 0L,
    val paidAmountCents: Long = 0L,
    val pendingAmountCents: Long = 0L,
    val status: String = "PENDIENTE", // PENDIENTE | PARCIAL | PAGADA
    val paymentDate: String = "",
    val paymentMethod: String = "",
    val receiptPath: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class VerificationRequirement(
    @DocumentId val id: String = "",
    val userId: String = "",
    val accountId: String = "",
    val accountLabel: String = "",
    val holderName: String = "",
    val bookmakerName: String = "",
    val requirementType: String = "Verificación de identidad",
    val status: String = "PENDIENTE", // PENDIENTE | EN_REVISION | RESUELTO | VENCIDO
    val requestDate: String = "",
    val dueDate: String = "",
    val priority: String = "ALTA", // ALTA | MEDIA | BAJA
    val resolutionDate: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StatusHistoryRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val entityType: String = "CUENTA",
    val entityId: String = "",
    val entityLabel: String = "",
    val previousState: String = "",
    val newState: String = "",
    val reason: String = "",
    val changeDate: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AttachmentRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val ownerType: String = "MOVIMIENTO",
    val ownerId: String = "",
    val title: String = "",
    val localUriPath: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AlertReminder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val alertType: String = "RETIRO_DEMORADO",
    val title: String = "",
    val message: String = "",
    val severity: String = "ALTA", // ALTA | MEDIA | INFO
    val relatedEntityId: String = "",
    val isDismissed: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BalanceReconciliation(
    @DocumentId val id: String = "",
    val userId: String = "",
    val accountId: String = "",
    val accountLabel: String = "",
    val currency: String = "PEN",
    val calculatedBalanceCents: Long = 0L,
    val observedBalanceCents: Long = 0L,
    val differenceCents: Long = 0L,
    val reason: String = "",
    val reconciliationDate: String = "",
    val adjustmentTransactionId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ExchangeRateRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fromCurrency: String = "USD",
    val toCurrency: String = "PEN",
    val rate: Double = 1.0,
    val source: String = "",
    val rateDate: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
