package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.AccountHolder
import com.example.data.model.BettingAccount
import com.example.data.model.FinancialTransaction
import com.example.data.model.Wallet
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class BetManagerRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun mandatoryBusinessFlows_and_securityRules_succeedAndIsolateProperly() = runBlocking {
        val uniqueSuffix = UUID.randomUUID().toString().take(8)
        val adminUid = signInTestUser("admin_$uniqueSuffix@betmanager.test")
        val repo = BetManagerRepository(firestore)

        // 1. Crear billetera central PEN con S/ 1,000.00 (100,000 céntimos)
        val walletId = withTimeout(5000L) {
            repo.saveWallet(
                Wallet(
                    name = "Caja Central PEN",
                    walletType = "BANCO",
                    currency = "PEN",
                    initialBalanceCents = 100000L,
                    currentBalanceCents = 100000L,
                    notes = "Cuenta principal soles"
                )
            )
        }
        assertTrue(walletId.isNotBlank())

        // 2. Crear dos titulares con cuentas en una misma casa (Betsson) y comprobar saldos independientes
        val holder1Id = withTimeout(5000L) {
            repo.saveHolder(
                AccountHolder(
                    name = "Titular Uno",
                    country = "Perú",
                    joinDate = "2026-11-01",
                    holderType = "TERCERO",
                    commissionPct = 25.0,
                    commissionMode = "PORCENTAJE_BENEFICIO",
                    settlementPeriod = "MENSUAL",
                    compensatePreviousLosses = true
                )
            )
        }
        val holder2Id = withTimeout(5000L) {
            repo.saveHolder(
                AccountHolder(
                    name = "Titular Dos",
                    country = "Perú",
                    joinDate = "2026-11-02",
                    holderType = "TERCERO",
                    commissionPct = 20.0,
                    commissionMode = "PORCENTAJE_BENEFICIO",
                    settlementPeriod = "SEMANAL",
                    compensatePreviousLosses = true
                )
            )
        }

        val acc1Id = withTimeout(5000L) {
            repo.saveAccount(
                BettingAccount(
                    holderId = holder1Id,
                    holderName = "Titular Uno",
                    bookmakerId = "bm_betsson_pe",
                    bookmakerName = "Betsson",
                    country = "Perú",
                    username = "titular1_betsson",
                    currency = "PEN",
                    openDate = "2026-11-01",
                    initialBalanceCents = 0L,
                    calculatedBalanceCents = 0L,
                    itfOverride = "HEREDAR",
                    accountStatus = "ACTIVA"
                )
            )
        }
        val acc2Id = withTimeout(5000L) {
            repo.saveAccount(
                BettingAccount(
                    holderId = holder2Id,
                    holderName = "Titular Dos",
                    bookmakerId = "bm_betsson_pe",
                    bookmakerName = "Betsson",
                    country = "Perú",
                    username = "titular2_betsson",
                    currency = "PEN",
                    openDate = "2026-11-02",
                    initialBalanceCents = 5000L, // S/ 50.00 inicial
                    calculatedBalanceCents = 5000L,
                    itfOverride = "HEREDAR",
                    accountStatus = "ACTIVA"
                )
            )
        }

        // 3. Registrar un depósito de S/ 100 (10,000 céntimos) en acc1 y verificar billetera de origen
        withTimeout(5000L) {
            repo.recordTransaction(
                FinancialTransaction(
                    type = "DEPOSITO",
                    holderId = holder1Id,
                    holderName = "Titular Uno",
                    bookmakerId = "bm_betsson_pe",
                    bookmakerName = "Betsson",
                    accountId = acc1Id,
                    accountUsername = "titular1_betsson",
                    walletId = walletId,
                    walletName = "Caja Central PEN",
                    transactionDate = "2026-11-03 10:00",
                    currency = "PEN",
                    amountCents = 10000L,
                    paymentMethod = "Transferencia",
                    status = "COMPLETADO",
                    receiptImagePath = "file:///storage/emulated/0/comprobante_dep_100.jpg"
                )
            )
        }

        val walletsAfterDeposit = withTimeout(3000L) { repo.observeWallets().first { it.isNotEmpty() } }
        val updatedWallet = walletsAfterDeposit.first { it.id == walletId }
        assertEquals(90000L, updatedWallet.currentBalanceCents) // S/ 1000 - S/ 100 = S/ 900

        // Verificar que el comprobante adjunto se haya registrado
        val attachments = withTimeout(3000L) { repo.observeAttachments().first { it.isNotEmpty() } }
        assertTrue(attachments.any { it.localUriPath.contains("comprobante_dep_100.jpg") })

        // 4. Registrar un beneficio neto de S/ 20 (2,000 céntimos) en acc1 y comprobar que no se confunde con depósito
        withTimeout(5000L) {
            repo.recordTransaction(
                FinancialTransaction(
                    type = "RESULTADO_POSITIVO",
                    holderId = holder1Id,
                    holderName = "Titular Uno",
                    bookmakerId = "bm_betsson_pe",
                    bookmakerName = "Betsson",
                    accountId = acc1Id,
                    accountUsername = "titular1_betsson",
                    transactionDate = "2026-11-03 12:00",
                    currency = "PEN",
                    amountCents = 2000L,
                    paymentMethod = "Resultado de Apuestas",
                    status = "COMPLETADO"
                )
            )
        }

        // 5. Marcar un retiro de S/ 50 (5,000 céntimos) como PENDIENTE y luego COMPLETADO sin duplicar capital ni ganancia
        val withdrawalTxId = withTimeout(5000L) {
            repo.recordTransaction(
                FinancialTransaction(
                    type = "RETIRO_PENDIENTE",
                    holderId = holder1Id,
                    holderName = "Titular Uno",
                    bookmakerId = "bm_betsson_pe",
                    bookmakerName = "Betsson",
                    accountId = acc1Id,
                    accountUsername = "titular1_betsson",
                    walletId = walletId,
                    walletName = "Caja Central PEN",
                    transactionDate = "2026-11-03 14:00",
                    currency = "PEN",
                    amountCents = 5000L,
                    paymentMethod = "Transferencia Bancaria",
                    status = "PENDIENTE"
                )
            )
        }

        val allTxsPending = withTimeout(3000L) { repo.observeTransactions().first { it.size >= 3 } }
        assertEquals(5000L, FinancialCalculator.calculatePendingWithdrawalsCents(allTxsPending))
        assertEquals(2000L, FinancialCalculator.calculateNetBettingProfitCents(allTxsPending)) // Beneficio sigue siendo S/ 20

        // Completar el retiro pendiente
        withTimeout(5000L) {
            repo.updateTransactionStatus(withdrawalTxId, newStatus = "COMPLETADO", newType = "RETIRO_COMPLETADO")
        }

        val accountsAfterWithdrawal = withTimeout(3000L) { repo.observeAccounts().first { it.size >= 2 } }
        val acc1 = accountsAfterWithdrawal.first { it.id == acc1Id }
        val acc2 = accountsAfterWithdrawal.first { it.id == acc2Id }
        assertEquals(7000L, acc1.calculatedBalanceCents) // S/ 100 dep + S/ 20 beneficio - S/ 50 retiro = S/ 70
        assertEquals(5000L, acc2.calculatedBalanceCents) // S/ 50 intactos e independientes en Titular Dos

        val walletsAfterWithdrawal = withTimeout(3000L) { repo.observeWallets().first() }
        assertEquals(95000L, walletsAfterWithdrawal.first { it.id == walletId }.currentBalanceCents) // S/ 900 + S/ 50 = S/ 950

        // 6. Cambiar estado de acc1 a LIMITADA y conservar todos sus movimientos; modificar ITF en acc1 sin alterar acc2
        withTimeout(5000L) {
            repo.updateAccountStatus(
                account = acc1,
                newStatus = "LIMITADA",
                reason = "Stake máximo reducido en tenis ITF",
                limitationCategory = "MERCADO",
                limitationDescription = "Límite S/ 15 en circuitos ITF"
            )
            repo.updateAccountItfOverride(acc1Id, "NO")
        }

        val accountsAfterLimit = withTimeout(3000L) { repo.observeAccounts().first() }
        val acc1Limited = accountsAfterLimit.first { it.id == acc1Id }
        val acc2Unchanged = accountsAfterLimit.first { it.id == acc2Id }
        assertEquals("LIMITADA", acc1Limited.accountStatus)
        assertEquals("NO", acc1Limited.itfOverride)
        assertEquals("HEREDAR", acc2Unchanged.itfOverride)
        assertEquals(7000L, acc1Limited.calculatedBalanceCents)

        // 7. Calcular comisión sobre beneficio elegible (25% de S/ 20.00 = S/ 5.00 = 500 céntimos) y registrar pago
        val holder1Model = withTimeout(3000L) { repo.getHolderById(adminUid, holder1Id) }!!
        val settlementId = withTimeout(5000L) {
            repo.createOrUpdateCommissionSettlement(
                holder = holder1Model,
                periodLabel = "2026-11",
                currency = "PEN",
                eligibleProfitCents = 2000L,
                includedAccounts = "Betsson (titular1_betsson)"
            )
        }
        val settlements = withTimeout(3000L) { repo.observeCommissions().first { it.isNotEmpty() } }
        val settlement = settlements.first { it.id == settlementId }
        assertEquals(500L, settlement.generatedCommissionCents)

        withTimeout(5000L) {
            repo.payCommissionSettlement(
                settlement = settlement,
                amountPaidCents = 500L,
                paymentMethod = "Yape",
                walletId = walletId,
                walletName = "Caja Central PEN"
            )
        }
        val paidSettlement = withTimeout(3000L) { repo.observeCommissions().first() }.first { it.id == settlementId }
        assertEquals("PAGADA", paidSettlement.status)
        assertEquals(0L, paidSettlement.pendingAmountCents)
    }

    @Test
    fun crossUserAccess_and_unauthenticatedCaller_failWithPermissionDenied() = runBlocking {
        val uniqueSuffix = UUID.randomUUID().toString().take(8)
        val aliceUid = signInTestUser("alice_$uniqueSuffix@betmanager.test")
        val aliceRepo = BetManagerRepository(firestore)
        val holderId = withTimeout(5000L) {
            aliceRepo.saveHolder(
                AccountHolder(
                    name = "Titular Privado de Alice",
                    country = "Perú",
                    joinDate = "2026-11-03"
                )
            )
        }

        // Otro usuario autenticado intenta leer el titular de Alice -> PERMISSION_DENIED
        signInTestUser("bob_$uniqueSuffix@betmanager.test")
        val bobRepo = BetManagerRepository(firestore)
        var crossUserException: Exception? = null
        try {
            withTimeout(5000L) {
                bobRepo.getHolderById(aliceUid, holderId)
            }
        } catch (e: Exception) {
            crossUserException = e
        }
        val fsEx = crossUserException?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException on cross-user read", fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)

        // Usuario no autenticado intenta leer en Firestore -> falla
        auth.signOut()
        var unauthError: Exception? = null
        try {
            withTimeout(5000L) {
                bobRepo.getHolderById(aliceUid, holderId)
            }
        } catch (e: Exception) {
            unauthError = e
        }
        val unauthFsEx = unauthError?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException on unauthenticated read", unauthFsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, unauthFsEx?.code)
    }
}
