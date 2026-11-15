const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const DONOR_UID = "donor_123";
const RECEIVER_UID = "receiver_456";
const VOLUNTEER_UID = "volunteer_789";
const STRANGER_UID = "stranger_999";
const ADMIN_EMAIL = "1si25cs274@sit.ac.in";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function nowTimestamp() {
  return new Date();
}

function validUserPayload(uid, accountType = "donor") {
  const ts = nowTimestamp();
  return {
    userId: uid,
    displayName: "Aarav Sharma",
    email: "aarav@example.in",
    phone: "+919876543210",
    accountType,
    organizationName: "Annapurna Kitchen",
    address: "12 MG Road, Indiranagar",
    district: "Bengaluru Urban",
    state: "Karnataka",
    pinCode: "560038",
    fssaiOrNgoId: "FSSAI-11223344",
    preferredLanguage: "en",
    sharePhoneWithMatchedParties: true,
    shareExactLocationOnAccept: true,
    whatsappAlertsEnabled: true,
    ngoVerified: false,
    suspended: false,
    points: 0,
    mealsShared: 0,
    deliveriesCompleted: 0,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validDonationPayload(donorId) {
  const ts = nowTimestamp();
  const prep = Date.now() - 3600000;
  const exp = Date.now() + 14400000;
  return {
    donorId,
    donorOrgName: "Annapurna Kitchen",
    donorDisplayName: "Aarav Sharma",
    foodName: "Fresh Veg Thali & Rice",
    category: "Cooked Meals",
    quantityKg: 12.5,
    servings: 45,
    dietType: "veg",
    preparedAtMillis: prep,
    expiryAtMillis: exp,
    storageType: "Hot Box",
    allergens: ["Dairy"],
    isSafeConfirmed: true,
    areaLabel: "Indiranagar",
    district: "Bengaluru Urban",
    state: "Karnataka",
    pinCode: "560038",
    approxLat: 12.9784,
    approxLng: 77.6408,
    imageKey: "preset_meal",
    trackingId: "FBI-2026-1001",
    status: "Donation Posted",
    isActiveListing: true,
    acceptedReceiverId: "",
    acceptedReceiverName: "",
    assignedVolunteerId: "",
    assignedVolunteerName: "",
    volunteerLat: 12.9784,
    volunteerLng: 77.6408,
    locationSharingEnabled: false,
    qrPayload: "foodbridge://track/FBI-2026-1001",
    isSample: false,
    estimatedValueInr: 2250,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validPrivatePayload(donorId) {
  return {
    donorId,
    acceptedReceiverId: "",
    assignedVolunteerId: "",
    donorPhone: "+919876543210",
    receiverPhone: "",
    volunteerPhone: "",
    exactAddress: "12 MG Road, 2nd Cross, Indiranagar",
    exactLat: 12.9784,
    exactLng: 77.6408,
    deliveryOtp: "4829",
    contactConsent: true,
    updatedAt: nowTimestamp(),
  };
}

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

test("Unauthenticated user cannot read donations or users", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("donations").where("isActiveListing", "==", true).get());
  await assertFails(unauthDb.collection("users").doc(DONOR_UID).get());
});

test("User can create own profile but cannot self-assign ngoVerified or admin", async () => {
  const donorDb = testEnv.authenticatedContext(DONOR_UID).firestore();
  await assertSucceeds(donorDb.collection("users").doc(DONOR_UID).set(validUserPayload(DONOR_UID, "donor")));

  const forged = validUserPayload(STRANGER_UID, "donor");
  forged.ngoVerified = true;
  const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
  await assertFails(strangerDb.collection("users").doc(STRANGER_UID).set(forged));

  const adminRoleAttempt = validUserPayload(STRANGER_UID, "admin");
  await assertFails(strangerDb.collection("users").doc(STRANGER_UID).set(adminRoleAttempt));
});

test("PII Isolation: Stranger cannot read another user's private profile", async () => {
  const donorDb = testEnv.authenticatedContext(DONOR_UID).firestore();
  await assertSucceeds(donorDb.collection("users").doc(DONOR_UID).set(validUserPayload(DONOR_UID)));

  const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
  await assertFails(strangerDb.collection("users").doc(DONOR_UID).get());
});

test("Donation creation, active listing query, and private PII isolation", async () => {
  const donorDb = testEnv.authenticatedContext(DONOR_UID).firestore();
  await assertSucceeds(donorDb.collection("donations").doc("don_1").set(validDonationPayload(DONOR_UID)));
  await assertSucceeds(
    donorDb.collection("donations").doc("don_1").collection("private").doc("details").set(validPrivatePayload(DONOR_UID))
  );

  // Receiver can list active safe donations
  const receiverDb = testEnv.authenticatedContext(RECEIVER_UID).firestore();
  await assertSucceeds(
    receiverDb.collection("donations").where("isActiveListing", "==", true).where("isSafeConfirmed", "==", true).get()
  );

  // Receiver cannot list all donations without filter
  await assertFails(receiverDb.collection("donations").get());

  // Stranger cannot read private PII details of donation
  const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
  await assertFails(
    strangerDb.collection("donations").doc("don_1").collection("private").doc("details").get()
  );
});

test("Shadow update & unsafe donation rejection", async () => {
  const donorDb = testEnv.authenticatedContext(DONOR_UID).firestore();
  const unsafe = validDonationPayload(DONOR_UID);
  unsafe.isSafeConfirmed = false;
  await assertFails(donorDb.collection("donations").doc("don_unsafe").set(unsafe));

  const ghost = validDonationPayload(DONOR_UID);
  ghost.hackedField = "evil";
  await assertFails(donorDb.collection("donations").doc("don_ghost").set(ghost));
});

test("Volunteer can claim task and matched volunteer can read private details", async () => {
  const donorDb = testEnv.authenticatedContext(DONOR_UID).firestore();
  await assertSucceeds(donorDb.collection("donations").doc("don_2").set(validDonationPayload(DONOR_UID)));
  await assertSucceeds(
    donorDb.collection("donations").doc("don_2").collection("private").doc("details").set(validPrivatePayload(DONOR_UID))
  );

  const volunteerDb = testEnv.authenticatedContext(VOLUNTEER_UID).firestore();
  await assertSucceeds(
    volunteerDb.collection("donations").doc("don_2").update({
      assignedVolunteerId: VOLUNTEER_UID,
      assignedVolunteerName: "Vikram Volunteer",
      status: "Volunteer Assigned",
      updatedAt: nowTimestamp(),
    })
  );

  await assertSucceeds(
    volunteerDb.collection("donations").doc("don_2").collection("private").doc("details").get()
  );
});

test("Email spoofing check: unverified admin email is rejected for admin list", async () => {
  const spoofAdminDb = testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();
  await assertFails(spoofAdminDb.collection("users").get());

  const realAdminDb = testEnv.authenticatedContext("real_admin_uid", {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  await assertSucceeds(realAdminDb.collection("users").get());
});
