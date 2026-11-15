package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.IndiaData
import com.example.data.model.Complaint
import com.example.data.model.Donation
import com.example.data.model.DonationPrivateDetails
import com.example.data.model.EmergencyRequest
import com.example.data.model.FoodRequest
import com.example.data.model.Review
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class DonationRepository(
    private val db: FirebaseFirestore,
    private val userRepository: UserRepository
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        UserRepository(context)
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("User must be signed in.")

    fun observeActiveDonations(): Flow<List<Donation>> = flow {
        val uid = requireUserId()
        val path = "donations"
        emitAll(
            db.collection("donations")
                .whereEqualTo("isActiveListing", true)
                .whereEqualTo("isSafeConfirmed", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Donation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeDonorDonations(donorId: String): Flow<List<Donation>> = flow {
        val path = "donations"
        emitAll(
            db.collection("donations")
                .whereEqualTo("donorId", donorId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Donation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, donorId)
                    throw error
                }
        )
    }

    fun observeReceiverDonations(receiverId: String): Flow<List<Donation>> = flow {
        val path = "donations"
        emitAll(
            db.collection("donations")
                .whereEqualTo("acceptedReceiverId", receiverId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Donation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, receiverId)
                    throw error
                }
        )
    }

    fun observeVolunteerDonations(volunteerId: String): Flow<List<Donation>> = flow {
        val path = "donations"
        emitAll(
            db.collection("donations")
                .whereEqualTo("assignedVolunteerId", volunteerId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Donation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, volunteerId)
                    throw error
                }
        )
    }

    fun observeAllDonationsAdmin(): Flow<List<Donation>> = flow {
        val uid = requireUserId()
        val path = "donations"
        emitAll(
            db.collection("donations")
                .snapshots()
                .map { snap ->
                    snap.toObjects(Donation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeDonationById(donationId: String): Flow<Donation?> = flow {
        val uid = requireUserId()
        val path = "donations/$donationId"
        emitAll(
            db.collection("donations").document(donationId)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(Donation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    handleFlowError(error, OperationType.GET, path, uid)
                    throw error
                }
        )
    }

    suspend fun getPrivateDetails(donationId: String): Result<DonationPrivateDetails?> {
        requireUserId()
        val docRef = db.collection("donations").document(donationId).collection("private").document("details")
        return try {
            val snap = docRef.get().await()
            val details = if (snap.exists()) {
                snap.toObject(DonationPrivateDetails::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else null
            Result.success(details)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createDonation(
        donation: Donation,
        exactAddress: String,
        exactLat: Double,
        exactLng: Double,
        donorPhone: String,
        contactConsent: Boolean
    ): Result<String> {
        val uid = requireUserId()
        if (!donation.isSafeConfirmed) {
            return Result.failure(IllegalArgumentException("FSSAI safety confirmation is required."))
        }
        if (donation.expiryAtMillis <= System.currentTimeMillis()) {
            return Result.failure(IllegalArgumentException("Cannot post already expired food."))
        }
        if (donation.expiryAtMillis <= donation.preparedAtMillis) {
            return Result.failure(IllegalArgumentException("Expiry time must be after preparation time."))
        }

        val trackingId = donation.trackingId.ifBlank {
            "FBI-2026-${Random.nextInt(1000, 9999)}"
        }
        val otp = Random.nextInt(1000, 9999).toString()
        val qrPayload = "foodbridge://track/$trackingId?otp=$otp"
        val docRef = db.collection("donations").document()
        val privateRef = docRef.collection("private").document("details")

        val approxLat = (exactLat * 100.0).toInt() / 100.0
        val approxLng = (exactLng * 100.0).toInt() / 100.0

        val payload = hashMapOf<String, Any>(
            "donorId" to uid,
            "donorOrgName" to donation.donorOrgName.trim().take(120).ifBlank { donation.donorDisplayName },
            "donorDisplayName" to donation.donorDisplayName.trim().take(80).ifBlank { "Donor" },
            "foodName" to donation.foodName.trim().take(120),
            "category" to donation.category,
            "quantityKg" to donation.quantityKg.coerceAtLeast(0.5),
            "servings" to donation.servings.coerceAtLeast(1L),
            "dietType" to donation.dietType,
            "preparedAtMillis" to donation.preparedAtMillis,
            "expiryAtMillis" to donation.expiryAtMillis,
            "storageType" to donation.storageType,
            "allergens" to donation.allergens.take(10),
            "isSafeConfirmed" to true,
            "areaLabel" to donation.areaLabel.trim().take(120).ifBlank { donation.district },
            "district" to donation.district.trim().take(80),
            "state" to donation.state.trim().take(80),
            "pinCode" to donation.pinCode.trim().take(10),
            "approxLat" to approxLat,
            "approxLng" to approxLng,
            "imageKey" to donation.imageKey.take(120),
            "trackingId" to trackingId,
            "status" to "Donation Posted",
            "isActiveListing" to true,
            "acceptedReceiverId" to "",
            "acceptedReceiverName" to "",
            "assignedVolunteerId" to "",
            "assignedVolunteerName" to "",
            "volunteerLat" to approxLat,
            "volunteerLng" to approxLng,
            "locationSharingEnabled" to false,
            "qrPayload" to qrPayload,
            "isSample" to donation.isSample,
            "estimatedValueInr" to IndiaData.estimateValueInr(donation.servings.toInt(), donation.category).toLong(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (donation.imageBase64.isNotBlank() && donation.imageBase64.length <= 145_000) {
            payload["imageBase64"] = donation.imageBase64
        }

        val privatePayload = mapOf(
            "donorId" to uid,
            "acceptedReceiverId" to "",
            "assignedVolunteerId" to "",
            "donorPhone" to IndiaData.normalizeIndianPhone(donorPhone).take(20),
            "receiverPhone" to "",
            "volunteerPhone" to "",
            "exactAddress" to exactAddress.trim().take(250),
            "exactLat" to exactLat,
            "exactLng" to exactLng,
            "deliveryOtp" to otp,
            "contactConsent" to contactConsent,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.set(payload).await()
            privateRef.set(privatePayload).await()
            userRepository.sendNotification(
                recipientUserId = uid,
                title = "Donation Posted ($trackingId)",
                body = "${donation.foodName} (${donation.servings} servings) is now live in ${donation.district}.",
                type = "donation_posted",
                donationId = docRef.id
            )
            userRepository.logAudit(
                actorName = donation.donorDisplayName,
                action = "DONATION_CREATED",
                targetPath = docRef.path,
                summary = "Posted ${donation.foodName} (${donation.servings} servings) at ${donation.areaLabel}"
            )
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun cancelDonation(donationId: String, actorName: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("donations").document(donationId)
        return try {
            docRef.update(
                mapOf(
                    "status" to "Cancelled",
                    "isActiveListing" to false,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            userRepository.logAudit(actorName, "DONATION_CANCELLED", docRef.path, "Cancelled donation $donationId")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateDonationBasic(
        donationId: String,
        foodName: String,
        servings: Long,
        quantityKg: Double,
        storageType: String
    ): Result<Unit> {
        requireUserId()
        val docRef = db.collection("donations").document(donationId)
        return try {
            docRef.update(
                mapOf(
                    "foodName" to foodName.trim().take(120),
                    "servings" to servings.coerceAtLeast(1L),
                    "quantityKg" to quantityKg.coerceAtLeast(0.5),
                    "storageType" to storageType,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun submitFoodRequest(
        donation: Donation,
        receiverName: String,
        receiverOrgType: String,
        requestedServings: Long,
        receiverPhone: String,
        notes: String,
        urgency: String
    ): Result<String> {
        val uid = requireUserId()
        val reqRef = db.collection("food_requests").document()
        val donRef = db.collection("donations").document(donation.id)
        return try {
            reqRef.set(
                mapOf(
                    "donationId" to donation.id,
                    "donorId" to donation.donorId,
                    "receiverId" to uid,
                    "receiverName" to receiverName.trim().take(120).ifBlank { "Receiver" },
                    "receiverOrgType" to receiverOrgType.take(60),
                    "requestedServings" to requestedServings.coerceIn(1L, donation.servings.coerceAtLeast(1L)),
                    "notes" to "${notes.trim()} | Contact: ${IndiaData.maskPhone(receiverPhone)}".take(300),
                    "urgency" to urgency.takeIf { it in listOf("normal", "emergency") }.orEmpty().ifBlank { "normal" },
                    "status" to "pending",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (donation.status == "Donation Posted") {
                donRef.update(
                    mapOf(
                        "status" to "Request Received",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            userRepository.sendNotification(
                recipientUserId = donation.donorId,
                title = "New Food Request",
                body = "$receiverName requested $requestedServings servings of ${donation.foodName}.",
                type = "request_received",
                donationId = donation.id
            )
            userRepository.logAudit(
                actorName = receiverName,
                action = "FOOD_REQUESTED",
                targetPath = reqRef.path,
                summary = "Requested $requestedServings servings for ${donation.trackingId}"
            )
            Result.success(reqRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, reqRef.path)
            Result.failure(e)
        }
    }

    fun observeRequestsForDonor(donorId: String): Flow<List<FoodRequest>> = flow {
        val path = "food_requests"
        emitAll(
            db.collection("food_requests")
                .whereEqualTo("donorId", donorId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(FoodRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, donorId)
                    throw error
                }
        )
    }

    fun observeRequestsForReceiver(receiverId: String): Flow<List<FoodRequest>> = flow {
        val path = "food_requests"
        emitAll(
            db.collection("food_requests")
                .whereEqualTo("receiverId", receiverId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(FoodRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, receiverId)
                    throw error
                }
        )
    }

    suspend fun acceptFoodRequest(request: FoodRequest, donorName: String): Result<Unit> {
        requireUserId()
        val reqRef = db.collection("food_requests").document(request.id)
        val donRef = db.collection("donations").document(request.donationId)
        val privRef = donRef.collection("private").document("details")
        return try {
            reqRef.update(
                mapOf(
                    "status" to "accepted",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            donRef.update(
                mapOf(
                    "status" to "Accepted",
                    "acceptedReceiverId" to request.receiverId,
                    "acceptedReceiverName" to request.receiverName,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            if (privRef.get().await().exists()) {
                privRef.update(
                    mapOf(
                        "acceptedReceiverId" to request.receiverId,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            userRepository.sendNotification(
                recipientUserId = request.receiverId,
                title = "Food Request Accepted!",
                body = "$donorName approved your request. Pickup coordination is now active.",
                type = "request_accepted",
                donationId = request.donationId
            )
            userRepository.logAudit(donorName, "REQUEST_ACCEPTED", reqRef.path, "Accepted ${request.receiverName}")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, reqRef.path)
            Result.failure(e)
        }
    }

    suspend fun rejectFoodRequest(request: FoodRequest): Result<Unit> {
        requireUserId()
        val reqRef = db.collection("food_requests").document(request.id)
        return try {
            reqRef.update(
                mapOf(
                    "status" to "rejected",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, reqRef.path)
            Result.failure(e)
        }
    }

    suspend fun claimVolunteerTask(donation: Donation, volunteerName: String, volunteerPhone: String): Result<Unit> {
        val uid = requireUserId()
        val donRef = db.collection("donations").document(donation.id)
        val privRef = donRef.collection("private").document("details")
        return try {
            donRef.update(
                mapOf(
                    "assignedVolunteerId" to uid,
                    "assignedVolunteerName" to volunteerName.trim().take(120).ifBlank { "Volunteer" },
                    "status" to "Volunteer Assigned",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            if (privRef.get().await().exists()) {
                privRef.update(
                    mapOf(
                        "assignedVolunteerId" to uid,
                        "volunteerPhone" to IndiaData.normalizeIndianPhone(volunteerPhone).take(20),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            userRepository.sendNotification(
                recipientUserId = donation.donorId,
                title = "Volunteer Assigned",
                body = "$volunteerName accepted pickup for ${donation.foodName} (${donation.trackingId}).",
                type = "volunteer_assigned",
                donationId = donation.id
            )
            if (donation.acceptedReceiverId.isNotBlank()) {
                userRepository.sendNotification(
                    recipientUserId = donation.acceptedReceiverId,
                    title = "Volunteer On The Way",
                    body = "$volunteerName is assigned to deliver ${donation.foodName}.",
                    type = "volunteer_assigned",
                    donationId = donation.id
                )
            }
            userRepository.logAudit(volunteerName, "VOLUNTEER_CLAIMED", donRef.path, "Claimed ${donation.trackingId}")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, donRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateDeliveryStatus(
        donation: Donation,
        newStatus: String,
        actorName: String,
        isCompleted: Boolean = false
    ): Result<Unit> {
        requireUserId()
        val donRef = db.collection("donations").document(donation.id)
        return try {
            val updates = hashMapOf<String, Any>(
                "status" to newStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (isCompleted || newStatus == "Completed") {
                updates["isActiveListing"] = false
            }
            donRef.update(updates).await()

            userRepository.sendNotification(
                recipientUserId = donation.donorId,
                title = "Status: $newStatus",
                body = "${donation.trackingId} (${donation.foodName}) updated to $newStatus.",
                type = "status_update",
                donationId = donation.id
            )
            if (donation.acceptedReceiverId.isNotBlank()) {
                userRepository.sendNotification(
                    recipientUserId = donation.acceptedReceiverId,
                    title = "Status: $newStatus",
                    body = "${donation.trackingId} (${donation.foodName}) updated to $newStatus.",
                    type = "status_update",
                    donationId = donation.id
                )
            }
            userRepository.logAudit(actorName, "DELIVERY_STATUS", donRef.path, "${donation.trackingId} -> $newStatus")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, donRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateVolunteerLocationSharing(
        donationId: String,
        enabled: Boolean,
        lat: Double,
        lng: Double
    ): Result<Unit> {
        requireUserId()
        val donRef = db.collection("donations").document(donationId)
        return try {
            donRef.update(
                mapOf(
                    "locationSharingEnabled" to enabled,
                    "volunteerLat" to lat,
                    "volunteerLng" to lng,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, donRef.path)
            Result.failure(e)
        }
    }

    suspend fun confirmDeliveryWithOtp(
        donation: Donation,
        enteredOtpOrQr: String,
        actorName: String
    ): Result<Unit> {
        val uid = requireUserId()
        val details = getPrivateDetails(donation.id).getOrNull()
            ?: return Result.failure(IllegalStateException("Unable to load delivery verification details."))
        val clean = enteredOtpOrQr.trim()
        val otpMatch = clean == details.deliveryOtp || clean.contains("otp=${details.deliveryOtp}")
        if (!otpMatch) {
            return Result.failure(IllegalArgumentException("Invalid OTP or QR code. Please check with donor/receiver."))
        }
        val result = updateDeliveryStatus(donation, "Completed", actorName, isCompleted = true)
        if (result.isSuccess) {
            runCatching {
                val userDoc = db.collection("users").document(uid)
                val snap = userDoc.get().await()
                if (snap.exists()) {
                    val currentPoints = snap.getLong("points") ?: 0L
                    val currentDeliveries = snap.getLong("deliveriesCompleted") ?: 0L
                    val currentMeals = snap.getLong("mealsShared") ?: 0L
                    userDoc.update(
                        mapOf(
                            "points" to (currentPoints + 50L),
                            "deliveriesCompleted" to (currentDeliveries + 1L),
                            "mealsShared" to (currentMeals + donation.servings),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
        }
        return result
    }

    fun observeActiveEmergencies(): Flow<List<EmergencyRequest>> = flow {
        val uid = requireUserId()
        val path = "emergency_requests"
        emitAll(
            db.collection("emergency_requests")
                .whereEqualTo("isActive", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(EmergencyRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun createEmergencyRequest(request: EmergencyRequest): Result<String> {
        val uid = requireUserId()
        val ref = db.collection("emergency_requests").document()
        return try {
            ref.set(
                mapOf(
                    "requesterId" to uid,
                    "requesterName" to request.requesterName.trim().take(120).ifBlank { "Community Shelter" },
                    "organizationType" to request.organizationType.take(60),
                    "district" to request.district.take(80),
                    "state" to request.state.take(80),
                    "pinCode" to request.pinCode.take(10),
                    "areaLabel" to request.areaLabel.trim().take(120).ifBlank { request.district },
                    "servingsNeeded" to request.servingsNeeded.coerceAtLeast(1L),
                    "dietPreference" to request.dietPreference,
                    "urgencyReason" to request.urgencyReason.trim().take(300),
                    "status" to "open",
                    "fulfilledByOrg" to "",
                    "isActive" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            userRepository.logAudit(
                request.requesterName,
                "EMERGENCY_REQUESTED",
                ref.path,
                "Urgent request for ${request.servingsNeeded} servings in ${request.district}"
            )
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun fulfillEmergencyRequest(emergencyId: String, orgName: String): Result<Unit> {
        requireUserId()
        val ref = db.collection("emergency_requests").document(emergencyId)
        return try {
            ref.update(
                mapOf(
                    "status" to "fulfilled",
                    "isActive" to false,
                    "fulfilledByOrg" to orgName.take(120),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            Result.failure(e)
        }
    }

    fun observeMyComplaints(userId: String): Flow<List<Complaint>> = flow {
        val path = "complaints"
        emitAll(
            db.collection("complaints")
                .whereEqualTo("reporterId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Complaint::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeAllComplaintsAdmin(): Flow<List<Complaint>> = flow {
        val uid = requireUserId()
        val path = "complaints"
        emitAll(
            db.collection("complaints")
                .snapshots()
                .map { snap ->
                    snap.toObjects(Complaint::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun submitComplaint(complaint: Complaint): Result<String> {
        val uid = requireUserId()
        val ref = db.collection("complaints").document()
        return try {
            ref.set(
                mapOf(
                    "reporterId" to uid,
                    "reporterName" to complaint.reporterName.trim().take(100).ifBlank { "Member" },
                    "targetDonationId" to complaint.targetDonationId.take(128),
                    "targetUserId" to complaint.targetUserId.take(128),
                    "category" to complaint.category,
                    "description" to complaint.description.trim().take(500),
                    "severity" to complaint.severity,
                    "status" to "open",
                    "adminNotes" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            userRepository.logAudit(
                complaint.reporterName,
                "COMPLAINT_FILED",
                ref.path,
                "Filed ${complaint.category} report (${complaint.severity})"
            )
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun updateComplaintAdmin(complaintId: String, status: String, adminNotes: String, actorName: String): Result<Unit> {
        requireUserId()
        val ref = db.collection("complaints").document(complaintId)
        return try {
            ref.update(
                mapOf(
                    "status" to status,
                    "adminNotes" to adminNotes.trim().take(400),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            userRepository.logAudit(actorName, "COMPLAINT_UPDATED", ref.path, "Status=$status")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            Result.failure(e)
        }
    }

    fun observePublicReviews(): Flow<List<Review>> = flow {
        val uid = requireUserId()
        val path = "reviews"
        emitAll(
            db.collection("reviews")
                .whereEqualTo("isPublic", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Review::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun submitReview(review: Review): Result<Unit> {
        val uid = requireUserId()
        val ref = db.collection("reviews").document()
        return try {
            ref.set(
                mapOf(
                    "donationId" to review.donationId,
                    "reviewerId" to uid,
                    "reviewerName" to review.reviewerName.trim().take(100).ifBlank { "Member" },
                    "targetUserId" to review.targetUserId.ifBlank { uid },
                    "rating" to review.rating.coerceIn(1L, 5L),
                    "comment" to review.comment.trim().take(400),
                    "isPublic" to true,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun seedSampleIndianDonations(userName: String, userPhone: String): Result<Int> {
        requireUserId()
        val now = System.currentTimeMillis()
        val samples = listOf(
            Triple(
                Donation(
                    donorOrgName = "Udupi Sagar Grand, Indiranagar",
                    donorDisplayName = userName,
                    foodName = "Hot Veg Pulao, Dal Fry & Chapatis",
                    category = "Cooked Meals",
                    quantityKg = 18.0,
                    servings = 65L,
                    dietType = "veg",
                    preparedAtMillis = now - 3_600_000L,
                    expiryAtMillis = now + 14_400_000L,
                    storageType = "Hot Box",
                    allergens = listOf("Dairy"),
                    isSafeConfirmed = true,
                    areaLabel = "Indiranagar 100ft Rd",
                    district = "Bengaluru Urban",
                    state = "Karnataka",
                    pinCode = "560038",
                    approxLat = 12.9784,
                    approxLng = 77.6408,
                    imageKey = "preset_meal",
                    isSample = true
                ),
                "12th Main, HAL 2nd Stage, Indiranagar, Bengaluru",
                12.9784 to 77.6408
            ),
            Triple(
                Donation(
                    donorOrgName = "Siddaganga Annadanam Hall",
                    donorDisplayName = userName,
                    foodName = "Steamed Rice, Sambar & Curd Packets",
                    category = "Event Surplus",
                    quantityKg = 32.0,
                    servings = 110L,
                    dietType = "veg",
                    preparedAtMillis = now - 2_700_000L,
                    expiryAtMillis = now + 12_600_000L,
                    storageType = "Hot Box",
                    allergens = listOf("Dairy", "Mustard"),
                    isSafeConfirmed = true,
                    areaLabel = "BH Road, Tumakuru",
                    district = "Tumakuru",
                    state = "Karnataka",
                    pinCode = "572103",
                    approxLat = 13.3392,
                    approxLng = 77.1140,
                    imageKey = "preset_meal",
                    isSample = true
                ),
                "Opposite SIT Main Gate, BH Road, Tumakuru",
                13.3392 to 77.1140
            ),
            Triple(
                Donation(
                    donorOrgName = "Sahyadri Fresh Mart, Bandra",
                    donorDisplayName = userName,
                    foodName = "Fresh Tomatoes, Bananas & Whole Wheat Flour",
                    category = "Fruits & Vegetables",
                    quantityKg = 25.0,
                    servings = 80L,
                    dietType = "vegan",
                    preparedAtMillis = now - 5_400_000L,
                    expiryAtMillis = now + 86_400_000L,
                    storageType = "Ambient Dry",
                    allergens = listOf("None"),
                    isSafeConfirmed = true,
                    areaLabel = "Bandra West",
                    district = "Mumbai",
                    state = "Maharashtra",
                    pinCode = "400050",
                    approxLat = 19.0596,
                    approxLng = 72.8295,
                    imageKey = "preset_meal",
                    isSample = true
                ),
                "Hill Road Market Lane, Bandra West, Mumbai",
                19.0596 to 72.8295
            ),
            Triple(
                Donation(
                    donorOrgName = "Taj Banquet Surplus Kitchen",
                    donorDisplayName = userName,
                    foodName = "Paneer Makhani, Jeera Rice & Rotis",
                    category = "Cooked Meals",
                    quantityKg = 22.0,
                    servings = 75L,
                    dietType = "veg",
                    preparedAtMillis = now - 1_800_000L,
                    expiryAtMillis = now + 16_200_000L,
                    storageType = "Refrigerated (0-5°C)",
                    allergens = listOf("Dairy", "Nuts"),
                    isSafeConfirmed = true,
                    areaLabel = "Connaught Place",
                    district = "New Delhi",
                    state = "Delhi",
                    pinCode = "110001",
                    approxLat = 28.6315,
                    approxLng = 77.2167,
                    imageKey = "preset_meal",
                    isSample = true
                ),
                "Outer Circle Block G, Connaught Place, New Delhi",
                28.6315 to 77.2167
            ),
            Triple(
                Donation(
                    donorOrgName = "Deccan Heritage Bakery",
                    donorDisplayName = userName,
                    foodName = "Sealed Whole-Grain Bread & Milk Packs",
                    category = "Bakery & Dairy",
                    quantityKg = 14.0,
                    servings = 50L,
                    dietType = "veg",
                    preparedAtMillis = now - 7_200_000L,
                    expiryAtMillis = now + 43_200_000L,
                    storageType = "Refrigerated (0-5°C)",
                    allergens = listOf("Gluten", "Dairy"),
                    isSafeConfirmed = true,
                    areaLabel = "Banjara Hills",
                    district = "Hyderabad",
                    state = "Telangana",
                    pinCode = "500034",
                    approxLat = 17.4126,
                    approxLng = 78.4482,
                    imageKey = "preset_meal",
                    isSample = true
                ),
                "Road No. 12, Banjara Hills, Hyderabad",
                17.4126 to 78.4482
            )
        )

        var count = 0
        for ((donation, address, coords) in samples) {
            val res = createDonation(
                donation = donation,
                exactAddress = address,
                exactLat = coords.first,
                exactLng = coords.second,
                donorPhone = userPhone.ifBlank { "+919876543210" },
                contactConsent = true
            )
            if (res.isSuccess) count++
        }

        createEmergencyRequest(
            EmergencyRequest(
                requesterName = "Asha Kiran Children's Home",
                organizationType = "Orphanage",
                district = "Bengaluru Urban",
                state = "Karnataka",
                pinCode = "560038",
                areaLabel = "Domlur Layout",
                servingsNeeded = 45L,
                dietPreference = "veg",
                urgencyReason = "Evening community dinner shortage for 45 children."
            )
        )
        return Result.success(count)
    }

    private fun handleFlowError(error: Throwable, op: OperationType, path: String, expectedUid: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val ex = firestoreError ?: (error as? Exception)
        if (ex != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(ex, op, path)
        }
    }
}
