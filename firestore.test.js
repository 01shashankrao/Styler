const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_styler_1";
const BOB_UID = "bob_styler_2";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

test("Unauthenticated user cannot read or write profiles, wardrobe_items, or outfits", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      name: "Alice",
      heightCm: 170,
      weightKg: 62,
      temperatureC: 24,
      weatherCondition: "Pleasant",
      lastMissingPieceTip: "Tip: Add a trench coat.",
      profileImageBase64: "",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("wardrobe_items").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("outfits").get()
  );
});

test("Authenticated owner can create and partially update UserProfile, WardrobeItem, and OutfitCombo", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);
  const updated = new Date(Date.now() - 1000);

  // 1. UserProfile create & partial update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      name: "Alice Style",
      heightCm: 172,
      weightKg: 64,
      temperatureC: 25,
      weatherCondition: "Pleasant",
      lastMissingPieceTip: "",
      profileImageBase64: "",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      heightCm: 173,
      weightKg: 65,
      updatedAt: updated,
    })
  );

  // Reject ghost field on UserProfile update
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isVerified: true,
      updatedAt: updated,
    })
  );

  // 2. WardrobeItem create across 'Jacket', 'Hoodie', 'Pullover', 'Sweatshirt', and 'Perfume' categories & list query
  for (const [idx, cat] of ["Jacket", "Hoodie", "Pullover", "Sweatshirt", "Perfume"].entries()) {
    await assertSucceeds(
      aliceDb
        .collection("users")
        .doc(ALICE_UID)
        .collection("wardrobe_items")
        .doc(`item_cat_${idx}`)
        .set({
          userId: ALICE_UID,
          name: `Curated ${cat}`,
          category: cat,
          dominantColor: "Forest Green",
          colorHex: "#264A31",
          undertone: "Cool",
          styleNotes: "Relaxed silhouette, pairs with warm earth tones",
          imageBase64: "base64_jpeg_payload",
          createdAt: created,
          updatedAt: created,
        })
    );
  }

  // Reject unsupported category string not in the wardrobe schema
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("wardrobe_items")
      .doc("item_invalid_cat")
      .set({
        userId: ALICE_UID,
        name: "Invalid Category Item",
        category: "Spacesuit",
        dominantColor: "Silver",
        colorHex: "#CCCCCC",
        undertone: "Cool",
        styleNotes: "Unsupported category",
        imageBase64: "base64_jpeg_payload",
        createdAt: created,
        updatedAt: created,
      })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("wardrobe_items").get()
  );

  // Reject @DocumentId 'id' field in create payload
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("wardrobe_items")
      .doc("item_bad_id")
      .set({
        id: "item_bad_id",
        userId: ALICE_UID,
        name: "Bad Item",
        category: "Perfume / Fragrance",
        dominantColor: "Amber",
        colorHex: "#F27D16",
        undertone: "Warm",
        styleNotes: "Woody amber notes",
        imageBase64: "base64",
        createdAt: created,
        updatedAt: created,
      })
  );

  // 3. OutfitCombo create & partial update (pin / mark worn today)
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("outfits")
      .doc("outfit_1")
      .set({
        userId: ALICE_UID,
        title: "Autumn Street Layering",
        occasion: "Casual Date",
        weatherVibe: "Pleasant",
        temperatureC: 22,
        itemIdsCsv: "item_hoodie_1",
        stylingNote: "Layered hoodie balances a 173cm athletic frame.",
        colorTheoryNote: "Complementary Forest Green (#264A31) and Terracotta (#F27D16) with Woody Amber Perfume.",
        colorHarmonyScore: 95,
        pinnedToDashboard: true,
        lastWornDate: "",
        missingPieceSuggestion: "Tip: Add a structured leather jacket for Night Out.",
        createdAt: created,
        updatedAt: created,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("outfits")
      .doc("outfit_1")
      .update({
        lastWornDate: "2026-11-18",
        pinnedToDashboard: false,
        updatedAt: updated,
      })
  );
});

test("Cross-user isolation: Bob cannot read, list, update, or delete Alice's data", async () => {
  const now = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const adminDb = context.firestore();
    await adminDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      name: "Alice",
      heightCm: 170,
      weightKg: 60,
      temperatureC: 24,
      weatherCondition: "Pleasant",
      lastMissingPieceTip: "",
      profileImageBase64: "",
      createdAt: now,
      updatedAt: now,
    });
    await adminDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("wardrobe_items")
      .doc("item_1")
      .set({
        userId: ALICE_UID,
        name: "Oud Wood Perfume",
        category: "Perfume / Fragrance",
        dominantColor: "Amber Gold",
        colorHex: "#C9963B",
        undertone: "Warm",
        styleNotes: "Warm spicy evening scent",
        imageBase64: "abc",
        createdAt: now,
        updatedAt: now,
      });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("wardrobe_items").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("wardrobe_items")
      .doc("item_1")
      .delete()
  );
});
