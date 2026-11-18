const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_789";

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

test("Unauthenticated user: denied across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("products").get());
  await assertFails(unauthDb.collection("categories").get());
  await assertFails(unauthDb.collection("orders").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("User profile: owner can create/update own profile; cross-user & role escalation fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Santos",
      email: "alice@example.com",
      role: "customer",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Cannot escalate own role to admin on create
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      email: "bob@example.com",
      role: "admin",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Owner partial update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Alice M. Santos",
      phone: "09171234567",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Owner cannot elevate role on update
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "admin",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Cross-user read/write blocked
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ displayName: "Hacked", updatedAt: new Date(Date.now() - 500) }));
});

test("Cart subcollection: owner-isolated create, update, list, and delete", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("cart").doc("cart_1").set({
      productId: "antonio_latte",
      productName: "Antonio Latte",
      categoryName: "Coffee",
      imageKey: "sample_coffee_latte",
      basePricePhp: 155.0,
      unitPricePhp: 185.0,
      quantity: 2,
      temperature: "Iced",
      size: "Large (16oz)",
      sugarLevel: "75%",
      milkPreference: "Oat Milk",
      extraShots: 1,
      addOns: ["Vanilla Syrup"],
      customizationSummary: "Iced • Large (16oz) • 75% Sugar • Oat Milk • +1 Shot • Vanilla Syrup",
      customizableDrink: true,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("cart").get());
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("cart").doc("cart_1").update({
      quantity: 3,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Bob cannot list, read, or mutate Alice's cart
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("cart").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("cart").doc("cart_1").delete());
});

test("Catalog (categories & products): admin writes succeed, customer writes rejected", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "jomellemoral55@gmail.com",
    email_verified: true,
  }).firestore();
  const spoofedAdminDb = testEnv.authenticatedContext("spoof", {
    email: "jomellemoral55@gmail.com",
    email_verified: false,
  }).firestore();
  const customerDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    adminDb.collection("categories").doc("cat_coffee").set({
      name: "Coffee",
      sortOrder: 1,
      active: true,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    adminDb.collection("products").doc("antonio_latte").set({
      name: "Antonio Latte",
      description: "Signature espresso latte (Sample Menu)",
      categoryId: "cat_coffee",
      categoryName: "Coffee",
      pricePhp: 155.0,
      imageKey: "sample_coffee_latte",
      featured: true,
      bestSeller: true,
      available: true,
      sampleData: true,
      customizableDrink: true,
      allowedTemps: ["Hot", "Iced"],
      createdAt: past,
      updatedAt: past,
    })
  );

  // Spoofed unverified email rejected
  await assertFails(
    spoofedAdminDb.collection("products").doc("hacked").set({
      name: "Hacked",
      description: "",
      categoryId: "cat_coffee",
      categoryName: "Coffee",
      pricePhp: 1.0,
      imageKey: "sample_coffee_latte",
      featured: false,
      bestSeller: false,
      available: true,
      sampleData: true,
      customizableDrink: true,
      allowedTemps: ["Hot"],
      createdAt: past,
      updatedAt: past,
    })
  );

  // Customer can read catalog but cannot create or update products
  await assertSucceeds(customerDb.collection("products").get());
  await assertFails(
    customerDb.collection("products").doc("antonio_latte").update({
      pricePhp: 1.0,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Orders: customer scoped query/create, admin status updates, terminal state lock", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "jomellemoral55@gmail.com",
    email_verified: true,
  }).firestore();
  const past = new Date(Date.now() - 5000);

  const validOrder = {
    orderNumber: "CAL-1001",
    userId: ALICE_UID,
    customerName: "Alice Santos",
    contactNumber: "09171234567",
    orderType: "Pickup",
    paymentMethod: "Cash",
    status: "Order Received",
    items: [
      {
        productId: "antonio_latte",
        productName: "Antonio Latte",
        unitPrice: 155.0,
        quantity: 1,
        lineTotal: 155.0,
        customizationSummary: "Hot • Regular (12oz)",
        imageKey: "sample_coffee_latte",
      },
    ],
    subtotalPhp: 155.0,
    serviceChargePhp: 0.0,
    totalPhp: 155.0,
    createdAt: past,
    updatedAt: past,
  };

  await assertSucceeds(aliceDb.collection("orders").doc("order_1").set(validOrder));

  // Customer cannot create order already marked Completed
  await assertFails(
    aliceDb.collection("orders").doc("order_cheat").set({
      ...validOrder,
      orderNumber: "CAL-1002",
      status: "Completed",
    })
  );

  // Customer scoped query succeeds; unfiltered query rejected for customer
  await assertSucceeds(aliceDb.collection("orders").where("userId", "==", ALICE_UID).get());
  await assertFails(aliceDb.collection("orders").get());
  await assertFails(bobDb.collection("orders").doc("order_1").get());

  // Customer cannot self-advance order to Ready for Pickup
  await assertFails(
    aliceDb.collection("orders").doc("order_1").update({
      status: "Ready for Pickup",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Admin can list all orders and advance status to Completed
  await assertSucceeds(adminDb.collection("orders").get());
  await assertSucceeds(
    adminDb.collection("orders").doc("order_1").update({
      status: "Preparing Your Order",
      updatedAt: new Date(Date.now() - 900),
    })
  );
  await assertSucceeds(
    adminDb.collection("orders").doc("order_1").update({
      status: "Completed",
      updatedAt: new Date(Date.now() - 800),
    })
  );

  // Once Completed, customer cannot cancel or modify
  await assertFails(
    aliceDb.collection("orders").doc("order_1").update({
      status: "Cancelled",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});
