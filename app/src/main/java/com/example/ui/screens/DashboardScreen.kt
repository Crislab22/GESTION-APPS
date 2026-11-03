package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.MoneyUtils
import com.example.data.repository.CurrencySummary
import com.example.ui.BetManagerUiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MetricSummaryCard
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SlateNavy

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    uiState: BetManagerUiState,
    onQuickDeposit: () -> Unit,
    onQuickWithdrawal: () -> Unit,
    onQuickNewAccount: () -> Unit,
    onQuickReconcile: () -> Unit,
    onQuickRecordProfit: () -> Unit,
    onQuickNewHolder: () -> Unit,
    onQuickNewWallet: () -> Unit,
    onPeriodSelected: (Int) -> Unit
) {
    val scrollState = rememberScrollState()

    val activeAccountsCount = uiState.accounts.count { it.accountStatus == "ACTIVA" }
    val limitedAccountsCount = uiState.accounts.count { it.accountStatus == "LIMITADA" || it.accountStatus == "RESTRICCION_PARCIAL" }
    val closedAccountsCount = uiState.accounts.count { it.accountStatus == "CERRADA" }
    val itfAvailableAccountsCount = uiState.accounts.count { acc ->
        if (acc.itfOverride != "HEREDAR") {
            acc.itfOverride == "SI" || acc.itfOverride == "PARCIAL"
        } else {
            val bm = uiState.bookmakers.find { it.id == acc.bookmakerId }
            bm?.itfAvailability == "SI" || bm?.itfAvailability == "PARCIAL"
        }
    }
    val pendingCommissionsCents = uiState.commissions.sumOf { it.pendingAmountCents }
    val pendingRequirementsCount = uiState.requirements.count { it.status == "PENDIENTE" || it.status == "VENCIDO" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Executive Portfolio Banner
        ExecutivePortfolioBanner(
            summaries = uiState.currencySummaries,
            hideAmounts = uiState.hideAmounts,
            holdersCount = uiState.holders.size,
            accountsCount = uiState.accounts.size
        )

        // 2. Botones de Acción Rápida (Priorizando velocidad de consulta y registro)
        Text(
            text = "Acciones Rápidas",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionPill(
                label = "Nuevo Depósito",
                icon = Icons.Default.ArrowDownward,
                accentColor = EmeraldGreen,
                tag = "quick_action_deposit",
                onClick = onQuickDeposit
            )
            QuickActionPill(
                label = "Nuevo Retiro",
                icon = Icons.Default.ArrowUpward,
                accentColor = CyanAccent,
                tag = "quick_action_withdrawal",
                onClick = onQuickWithdrawal
            )
            QuickActionPill(
                label = "Registrar Resultado",
                icon = Icons.Default.TrendingUp,
                accentColor = AmberWarning,
                tag = "quick_action_profit",
                onClick = onQuickRecordProfit
            )
            QuickActionPill(
                label = "Nueva Cuenta",
                icon = Icons.Default.AddCard,
                accentColor = EmeraldGreen,
                tag = "quick_action_account",
                onClick = onQuickNewAccount
            )
            QuickActionPill(
                label = "Actualizar Saldo",
                icon = Icons.Default.SyncAlt,
                accentColor = CyanAccent,
                tag = "quick_action_reconcile",
                onClick = onQuickReconcile
            )
            QuickActionPill(
                label = "Nuevo Titular",
                icon = Icons.Default.PersonAdd,
                accentColor = EmeraldGreen,
                tag = "quick_action_holder",
                onClick = onQuickNewHolder
            )
            QuickActionPill(
                label = "Nueva Billetera",
                icon = Icons.Default.AccountBalanceWallet,
                accentColor = CyanAccent,
                tag = "quick_action_wallet",
                onClick = onQuickNewWallet
            )
        }

        // 3. Filtro de periodo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val periods = listOf(7 to "7 días", 30 to "30 días", 90 to "90 días", 365 to "Año actual", 0 to "Histórico")
            periods.forEach { (days, label) ->
                FilterChip(
                    selected = uiState.selectedPeriodDays == days,
                    onClick = { onPeriodSelected(days) },
                    label = { Text(label) }
                )
            }
        }

        // 4. Desglose de Capital por Moneda (Sin mezclar monedas sin conversión)
        Text(
            text = "Capital y Resultados por Moneda",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        uiState.currencySummaries.forEach { summary ->
            CurrencyCapitalCard(summary = summary, hideAmounts = uiState.hideAmounts)
        }

        // 5. Indicadores Operativos de Cuentas, ITF, Comisiones y Requisitos
        Text(
            text = "Estado Operativo de Cuentas y Alertas",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            maxItemsInEachRow = 2
        ) {
            MetricSummaryCard(
                title = "Titulares / Cuentas",
                valueText = "${uiState.holders.size} / ${uiState.accounts.size}",
                subtitle = "$activeAccountsCount activas",
                accentColor = EmeraldGreen,
                modifier = Modifier.weight(1f)
            )
            MetricSummaryCard(
                title = "Limitadas / Cerradas",
                valueText = "$limitedAccountsCount / $closedAccountsCount",
                subtitle = "Monitoreo de restricciones",
                accentColor = CrimsonRed,
                modifier = Modifier.weight(1f)
            )
            MetricSummaryCard(
                title = "Cuentas con Tenis ITF",
                valueText = "$itfAvailableAccountsCount",
                subtitle = "Circuito ITF disponible/parcial",
                accentColor = CyanAccent,
                modifier = Modifier.weight(1f)
            )
            MetricSummaryCard(
                title = "Comisiones Pendientes",
                valueText = MoneyUtils.formatCents(pendingCommissionsCents, uiState.baseCurrency, uiState.hideAmounts),
                subtitle = "$pendingRequirementsCount requisitos KYC pendientes",
                accentColor = AmberWarning,
                modifier = Modifier.weight(1f)
            )
        }

        // 6. Gráficos Visuales Reales (Basados estrictamente en datos almacenados)
        Text(
            text = "Análisis Visual del Portafolio",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        if (uiState.accounts.isEmpty() && uiState.transactions.isEmpty()) {
            EmptyStateCard(
                title = "Sin operaciones registradas aún",
                description = "Registra tu primera billetera, titular, cuenta o movimiento financiero para visualizar gráficos reales de distribución de capital, evolución y rentabilidad.",
                icon = Icons.Default.AccountBalance
            )
        } else {
            PortfolioChartsSection(uiState = uiState)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ExecutivePortfolioBanner(
    summaries: List<CurrencySummary>,
    hideAmounts: Boolean,
    holdersCount: Int,
    accountsCount: Int
) {
    val primarySummary = summaries.firstOrNull() ?: CurrencySummary("PEN", 0, 0, 0, 0, 0, 0, 0)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(DeepNavy, SlateNavy, Color(0xFF0F2942))
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CAPITAL TOTAL ADMINISTRADO (${primarySummary.currency})",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF94A3B8)
                    )
                    Surface(
                        color = EmeraldGreen.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "$holdersCount Titulares • $accountsCount Cuentas",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = MoneyUtils.formatCents(
                        primarySummary.totalCapitalCents,
                        primarySummary.currency,
                        hideAmounts
                    ),
                    style = MaterialTheme.typography.headlineLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BannerSubMetric(
                        label = "En Casas",
                        value = MoneyUtils.formatCents(
                            primarySummary.totalInBookmakersCents,
                            primarySummary.currency,
                            hideAmounts
                        ),
                        color = Color.White
                    )
                    BannerSubMetric(
                        label = "En Billeteras",
                        value = MoneyUtils.formatCents(
                            primarySummary.totalInWalletsCents,
                            primarySummary.currency,
                            hideAmounts
                        ),
                        color = CyanAccent
                    )
                    BannerSubMetric(
                        label = "En Tránsito",
                        value = MoneyUtils.formatCents(
                            primarySummary.pendingWithdrawalsCents,
                            primarySummary.currency,
                            hideAmounts
                        ),
                        color = AmberWarning
                    )
                }
            }
        }
    }
}

@Composable
private fun BannerSubMetric(label: String, value: String, color: Color) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF94A3B8)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun QuickActionPill(
    label: String,
    icon: ImageVector,
    accentColor: Color,
    tag: String,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.testTag(tag)
    )
}

@Composable
private fun CurrencyCapitalCard(
    summary: CurrencySummary,
    hideAmounts: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                Text(
                    text = "Moneda: ${summary.currency}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                val profitColor = if (summary.netBettingProfitCents >= 0) EmeraldGreen else CrimsonRed
                val profitSign = if (summary.netBettingProfitCents > 0) "+" else ""
                Text(
                    text = "Beneficio Neto Apuestas: $profitSign${
                        MoneyUtils.formatCents(
                            summary.netBettingProfitCents,
                            summary.currency,
                            hideAmounts
                        )
                    }",
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = profitColor,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Depósitos", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        MoneyUtils.formatCents(summary.totalDepositsCents, summary.currency, hideAmounts),
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFontFamily)
                    )
                }
                Column {
                    Text("Total Retiros (Flujo Caja)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        MoneyUtils.formatCents(summary.totalCompletedWithdrawalsCents, summary.currency, hideAmounts),
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFontFamily)
                    )
                }
                Column {
                    Text("Retiros Pendientes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        MoneyUtils.formatCents(summary.pendingWithdrawalsCents, summary.currency, hideAmounts),
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = AmberWarning
                    )
                }
            }
        }
    }
}

@Composable
private fun PortfolioChartsSection(uiState: BetManagerUiState) {
    // Distribución de capital por casa de apuestas
    val byBookmaker = uiState.accounts
        .groupBy { it.bookmakerName }
        .mapValues { entry -> entry.value.sumOf { it.calculatedBalanceCents }.coerceAtLeast(0L) }
        .filterValues { it > 0L }
        .entries
        .sortedByDescending { it.value }
        .take(6)

    // Distribución por titular
    val byHolder = uiState.accounts
        .groupBy { it.holderName }
        .mapValues { entry -> entry.value.sumOf { it.calculatedBalanceCents }.coerceAtLeast(0L) }
        .filterValues { it > 0L }
        .entries
        .sortedByDescending { it.value }
        .take(6)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Distribución de Capital por Casa y Titular",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (byBookmaker.isEmpty()) {
                Text(
                    text = "Aún no hay cuentas con saldo positivo para graficar la distribución por casa.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val maxVal = byBookmaker.maxOf { it.value }.coerceAtLeast(1L)
                byBookmaker.forEach { (bmName, cents) ->
                    val ratio = (cents.toFloat() / maxVal.toFloat()).coerceIn(0.05f, 1f)
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(bmName, style = MaterialTheme.typography.labelLarge)
                            Text(
                                MoneyUtils.formatCents(cents, uiState.baseCurrency, uiState.hideAmounts),
                                style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                        ) {
                            drawRoundRect(
                                color = Color(0xFFCBD5E1).copy(alpha = 0.35f),
                                size = Size(size.width, size.height),
                                cornerRadius = CornerRadius(6f, 6f)
                            )
                            drawRoundRect(
                                color = EmeraldGreen,
                                size = Size(size.width * ratio, size.height),
                                cornerRadius = CornerRadius(6f, 6f)
                            )
                        }
                    }
                }
            }

            if (byHolder.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Capital por Titular",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                val maxHolder = byHolder.maxOf { it.value }.coerceAtLeast(1L)
                byHolder.forEach { (holderName, cents) ->
                    val ratio = (cents.toFloat() / maxHolder.toFloat()).coerceIn(0.05f, 1f)
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(holderName, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                MoneyUtils.formatCents(cents, uiState.baseCurrency, uiState.hideAmounts),
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFontFamily)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                        ) {
                            drawRoundRect(
                                color = CyanAccent,
                                topLeft = Offset.Zero,
                                size = Size(size.width * ratio, size.height),
                                cornerRadius = CornerRadius(6f, 6f)
                            )
                        }
                    }
                }
            }
        }
    }
}
