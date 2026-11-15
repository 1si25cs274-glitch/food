package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Donation
import com.example.data.model.FoodRequest
import com.example.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class FoodBridgeRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveUserProfile_andObserveActiveDonations_succeedsForSignedInUsers() = runBlocking {
        val donorUid = signInTestUser("donor_${UUID.randomUUID()}@test.in")
        val userRepo = UserRepository(firestore)
        val donationRepo = DonationRepository(firestore, userRepo)

        val saveRes = withTimeout(TIMEOUT_MS) {
            userRepo.saveUserProfile(
                UserProfile(
                    userId = donorUid,
                    displayName = "Ananya Rao",
                    email = "ananya@test.in",
                    phone = "+919876543210",
                    accountType = "donor",
                    organizationName = "Annapurna Kitchen",
                    address = "MG Road",
                    district = "Bengaluru Urban",
                    state = "Karnataka",
                    pinCode = "560038"
                )
            )
        }
        assertTrue("Profile save should succeed: ${saveRes.exceptionOrNull()}", saveRes.isSuccess)

        val now = System.currentTimeMillis()
        val createRes = withTimeout(TIMEOUT_MS) {
            donationRepo.createDonation(
                donation = Donation(
                    donorOrgName = "Annapurna Kitchen",
                    donorDisplayName = "Ananya Rao",
                    foodName = "Hot Veg Biryani",
                    category = "Cooked Meals",
                    quantityKg = 10.0,
                    servings = 40L,
                    dietType = "veg",
                    preparedAtMillis = now - 1_800_000L,
                    expiryAtMillis = now + 10_800_000L,
                    storageType = "Hot Box",
                    allergens = listOf("Dairy"),
                    isSafeConfirmed = true,
                    areaLabel = "Indiranagar",
                    district = "Bengaluru Urban",
                    state = "Karnataka",
                    pinCode = "560038",
                    approxLat = 12.9784,
                    approxLng = 77.6408
                ),
                exactAddress = "12 MG Road, Indiranagar",
                exactLat = 12.9784,
                exactLng = 77.6408,
                donorPhone = "+919876543210",
                contactConsent = true
            )
        }
        assertTrue("Donation create should succeed: ${createRes.exceptionOrNull()}", createRes.isSuccess)
        val donationId = createRes.getOrThrow()

        val activeList = withTimeout(FLOW_TIMEOUT_MS) {
            donationRepo.observeActiveDonations().first { list -> list.any { it.id == donationId } }
        }
        assertTrue(activeList.any { it.id == donationId && it.foodName == "Hot Veg Biryani" })
    }

    @Test
    fun privateDonationDetails_crossUserStrangerRejected_volunteerClaimAllowed() = runBlocking {
        signInTestUser("donor2_${UUID.randomUUID()}@test.in")
        val userRepo = UserRepository(firestore)
        val donationRepo = DonationRepository(firestore, userRepo)
        val now = System.currentTimeMillis()

        val donationId = withTimeout(TIMEOUT_MS) {
            donationRepo.createDonation(
                donation = Donation(
                    donorOrgName = "Sitara Mess",
                    donorDisplayName = "Kiran",
                    foodName = "Ragi Mudde & Sambar",
                    category = "Cooked Meals",
                    quantityKg = 8.0,
                    servings = 30L,
                    dietType = "veg",
                    preparedAtMillis = now - 1_000_000L,
                    expiryAtMillis = now + 9_000_000L,
                    storageType = "Hot Box",
                    allergens = listOf("None"),
                    isSafeConfirmed = true,
                    areaLabel = "BH Road",
                    district = "Tumakuru",
                    state = "Karnataka",
                    pinCode = "572103"
                ),
                exactAddress = "45 BH Road, Tumakuru",
                exactLat = 13.3392,
                exactLng = 77.1140,
                donorPhone = "+919988776655",
                contactConsent = true
            ).getOrThrow()
        }

        // Stranger signs in and tries to read private PII details -> must fail PERMISSION_DENIED
        signInTestUser("stranger_${UUID.randomUUID()}@test.in")
        val strangerRepo = DonationRepository(firestore, userRepo)
        val privateResult = withTimeout(TIMEOUT_MS) {
            strangerRepo.getPrivateDetails(donationId)
        }
        assertTrue("Stranger should be denied private details", privateResult.isFailure)
        val fsEx = privateResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)

        // Volunteer claims the task -> now volunteer can read private details & complete with OTP
        val createdDonation = withTimeout(FLOW_TIMEOUT_MS) {
            strangerRepo.observeActiveDonations().first { list -> list.any { it.id == donationId } }
                .first { it.id == donationId }
        }
        val claimRes = withTimeout(TIMEOUT_MS) {
            strangerRepo.claimVolunteerTask(createdDonation, "Vikram Volunteer", "+919123456789")
        }
        assertTrue("Volunteer claim should succeed: ${claimRes.exceptionOrNull()}", claimRes.isSuccess)

        val volunteerPrivate = withTimeout(TIMEOUT_MS) {
            strangerRepo.getPrivateDetails(donationId)
        }
        assertTrue("Assigned volunteer should read private details", volunteerPrivate.isSuccess)
        val otp = volunteerPrivate.getOrThrow()?.deliveryOtp
        assertNotNull(otp)
    }

    @Test
    fun observeActiveDonations_unauthenticatedCaller_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val userRepo = UserRepository(firestore)
        val donationRepo = DonationRepository(firestore, userRepo)

        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                donationRepo.observeActiveDonations().first()
            }
        } catch (e: Exception) {
            caught = e
        }
        assertNotNull("Expected exception when unauthenticated", caught)
        val fsEx = caught?.unwrapFirestoreException()
        if (fsEx != null) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx.code)
        } else {
            assertTrue(caught is IllegalStateException)
        }
    }

    @Test
    fun receiverRequestAndDonorAccept_updatesDonationStatus() = runBlocking {
        val donorUid = signInTestUser("donor3_${UUID.randomUUID()}@test.in")
        val userRepo = UserRepository(firestore)
        val donationRepo = DonationRepository(firestore, userRepo)
        val now = System.currentTimeMillis()

        val donationId = withTimeout(TIMEOUT_MS) {
            donationRepo.createDonation(
                donation = Donation(
                    donorOrgName = "Annadanam Trust",
                    donorDisplayName = "Meera",
                    foodName = "Dry Rice & Lentil Kits",
                    category = "Packaged Ration",
                    quantityKg = 20.0,
                    servings = 60L,
                    dietType = "vegan",
                    preparedAtMillis = now - 3_600_000L,
                    expiryAtMillis = now + 86_400_000L,
                    storageType = "Ambient Dry",
                    allergens = listOf("None"),
                    isSafeConfirmed = true,
                    areaLabel = "Jayanagar",
                    district = "Bengaluru Urban",
                    state = "Karnataka",
                    pinCode = "560041"
                ),
                exactAddress = "4th Block, Jayanagar",
                exactLat = 12.9299,
                exactLng = 77.5824,
                donorPhone = "+919811223344",
                contactConsent = true
            ).getOrThrow()
        }
        val createdDonation = withTimeout(FLOW_TIMEOUT_MS) {
            donationRepo.observeDonorDonations(donorUid).first { it.any { d -> d.id == donationId } }
                .first { it.id == donationId }
        }

        // Receiver requests food
        signInTestUser("receiver_${UUID.randomUUID()}@test.in")
        val reqRes = withTimeout(TIMEOUT_MS) {
            donationRepo.submitFoodRequest(
                donation = createdDonation,
                receiverName = "Hope Shelter",
                receiverOrgType = "Shelter",
                requestedServings = 30L,
                receiverPhone = "+919700112233",
                notes = "Needed for tonight",
                urgency = "normal"
            )
        }
        assertTrue("Food request should succeed: ${reqRes.exceptionOrNull()}", reqRes.isSuccess)
    }

    private companion object {
        const val TIMEOUT_MS = 6000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
