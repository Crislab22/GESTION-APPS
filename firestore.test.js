const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_user_123";
const OTHER_UID = "intruder_user_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read holders, accounts, or transactions", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ADMIN_UID}/holders`).get());
  await assertFails(unauthDb.collection(`users/${ADMIN_UID}/accounts`).get());
  await assertFails(unauthDb.collection(`users/${ADMIN_UID}/transactions`).get());
});

test("Cross-user isolation: another authenticated user cannot read or write admin's data", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const now = new Date();
    await context.firestore().doc(`users/${ADMIN_UID}/holders/holder_1`).set({
      userId: ADMIN_UID,
      name: "Carlos Mendoza",
      country: "Perú",
      joinDate: "2026-11-01",
      holderType: "TERCERO",
      commissionPct: 20,
      commissionMode: "PORCENTAJE_BENEFICIO",
      settlementPeriod: "MENSUAL",
      compensatePreviousLosses: true,
      pendingCommissionCents: 0,
      status: "ACTIVO",
      createdAt: now,
      updatedAt: now,
    });
  });

  const intruderDb = testEnv.authenticatedContext(OTHER_UID).firestore();
  await assertFails(intruderDb.doc(`users/${ADMIN_UID}/holders/holder_1`).get());
  await assertFails(intruderDb.collection(`users/${ADMIN_UID}/holders`).get());
});

test("Authenticated admin: can create and read holders, wallets, accounts, and transactions", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  const now = new Date();

  await assertSucceeds(
    adminDb.doc(`users/${ADMIN_UID}/holders/holder_1`).set({
      userId: ADMIN_UID,
      name: "Carlos Mendoza",
      country: "Perú",
      contactInfo: "+51 999888777",
      joinDate: "2026-11-01",
      holderType: "TERCERO",
      commissionPct: 20,
      commissionMode: "PORCENTAJE_BENEFICIO",
      settlementPeriod: "MENSUAL",
      compensatePreviousLosses: true,
      lastCommissionPaidDate: "",
      pendingCommissionCents: 0,
      notes: "Titular verificado",
      status: "ACTIVO",
      authorizationRef: "Constancia firmada",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    adminDb.doc(`users/${ADMIN_UID}/wallets/wallet_1`).set({
      userId: ADMIN_UID,
      name: "Caja Central PEN",
      walletType: "BANCO",
      currency: "PEN",
      initialBalanceCents: 500000,
      currentBalanceCents: 500000,
      notes: "BCP Soles",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    adminDb.doc(`users/${ADMIN_UID}/transactions/tx_1`).set({
      userId: ADMIN_UID,
      type: "DEPOSITO",
      holderId: "holder_1",
      holderName: "Carlos Mendoza",
      bookmakerId: "bm_betsson_pe",
      bookmakerName: "Betsson",
      accountId: "acc_1",
      accountUsername: "cmendoza_pe",
      walletId: "wallet_1",
      walletName: "Caja Central PEN",
      destinationWalletId: "",
      transactionDate: "2026-11-03 10:00",
      currency: "PEN",
      amountCents: 10000,
      paymentMethod: "Transferencia Bancaria",
      status: "COMPLETADO",
      referenceCode: "OP-1001",
      notes: "Depósito inicial",
      receiptImagePath: "",
      exchangeRateApplied: 1.0,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(adminDb.collection(`users/${ADMIN_UID}/holders`).get());
  await assertSucceeds(adminDb.collection(`users/${ADMIN_UID}/transactions`).get());
});

test("Schema validation: rejects negative transaction amount or mismatched userId", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  const now = new Date();

  await assertFails(
    adminDb.doc(`users/${ADMIN_UID}/transactions/tx_bad`).set({
      userId: ADMIN_UID,
      type: "DEPOSITO",
      transactionDate: "2026-11-03",
      currency: "PEN",
      amountCents: -5000, // negative not allowed
      paymentMethod: "Yape",
      status: "COMPLETADO",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertFails(
    adminDb.doc(`users/${ADMIN_UID}/holders/holder_spoof`).set({
      userId: OTHER_UID, // spoofed userId
      name: "Spoof",
      country: "Perú",
      joinDate: "2026-11-01",
      holderType: "PROPIA",
      commissionPct: 0,
      commissionMode: "PORCENTAJE_BENEFICIO",
      settlementPeriod: "MANUAL",
      compensatePreviousLosses: false,
      pendingCommissionCents: 0,
      status: "ACTIVO",
      createdAt: now,
      updatedAt: now,
    })
  );
});
