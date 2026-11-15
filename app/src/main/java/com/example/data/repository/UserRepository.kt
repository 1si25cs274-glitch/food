package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.IndiaData
import com.example.data.model.AuditLog
import com.example.data.model.NotificationItem
import com.example.data.model.PublicProfile
import com.example.data.model.UserProfile
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

class UserRepository(private val db: FirebaseFirestore) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("User must be signed in.")

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    handleFlowError(error, OperationType.GET, path, userId)
                    throw error
                }
        )
    }

    fun observePublicProfiles(): Flow<List<PublicProfile>> = flow {
        val uid = requireUserId()
        val path = "public_profiles"
        emitAll(
            db.collection("public_profiles")
                .whereEqualTo("isPublic", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PublicProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeAllUsersAdmin(): Flow<List<UserProfile>> = flow {
        val uid = requireUserId()
        val path = "users"
        emitAll(
            db.collection("users")
                .snapshots()
                .map { snap ->
                    snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeNotifications(userId: String): Flow<List<NotificationItem>> = flow {
        val path = "users/$userId/notifications"
        emitAll(
            db.collection("users").document(userId).collection("notifications")
                .snapshots()
                .map { snap ->
                    snap.toObjects(NotificationItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeAuditLogs(): Flow<List<AuditLog>> = flow {
        val uid = requireUserId()
        val path = "audit_logs"
        emitAll(
            db.collection("audit_logs")
                .snapshots()
                .map { snap ->
                    snap.toObjects(AuditLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun saveUserProfile(profile: UserProfile): Result<Unit> {
        val uid = requireUserId()
        val cleanPhone = IndiaData.normalizeIndianPhone(profile.phone)
        val userDoc = db.collection("users").document(uid)
        val pubDoc = db.collection("public_profiles").document(uid)
        return try {
            val existing = userDoc.get().await()
            if (!existing.exists()) {
                val createUserMap = mapOf(
                    "userId" to uid,
                    "displayName" to profile.displayName.trim().take(80).ifBlank { "FoodBridge Member" },
                    "email" to profile.email.trim().take(160),
                    "phone" to cleanPhone.take(20),
                    "accountType" to profile.accountType.takeIf { it in listOf("donor", "receiver", "volunteer") }.orEmpty().ifBlank { "donor" },
                    "organizationName" to profile.organizationName.trim().take(120),
                    "address" to profile.address.trim().take(250),
                    "district" to profile.district.trim().take(80),
                    "state" to profile.state.trim().take(80),
                    "pinCode" to profile.pinCode.trim().take(10),
                    "fssaiOrNgoId" to profile.fssaiOrNgoId.trim().take(60),
                    "preferredLanguage" to profile.preferredLanguage.takeIf { it in listOf("en", "hi", "kn") }.orEmpty().ifBlank { "en" },
                    "sharePhoneWithMatchedParties" to profile.sharePhoneWithMatchedParties,
                    "shareExactLocationOnAccept" to profile.shareExactLocationOnAccept,
                    "whatsappAlertsEnabled" to profile.whatsappAlertsEnabled,
                    "ngoVerified" to false,
                    "suspended" to false,
                    "points" to 0L,
                    "mealsShared" to 0L,
                    "deliveriesCompleted" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userDoc.set(createUserMap).await()

                val createPubMap = mapOf(
                    "userId" to uid,
                    "displayName" to (createUserMap["displayName"] as String),
                    "accountType" to (createUserMap["accountType"] as String),
                    "organizationName" to (createUserMap["organizationName"] as String),
                    "district" to (createUserMap["district"] as String),
                    "state" to (createUserMap["state"] as String),
                    "ngoVerified" to false,
                    "ratingAvg" to 5.0,
                    "ratingCount" to 0L,
                    "mealsShared" to 0L,
                    "deliveriesCompleted" to 0L,
                    "isPublic" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                pubDoc.set(createPubMap).await()
            } else {
                val updateUserMap = mapOf(
                    "displayName" to profile.displayName.trim().take(80).ifBlank { "FoodBridge Member" },
                    "phone" to cleanPhone.take(20),
                    "accountType" to profile.accountType.takeIf { it in listOf("donor", "receiver", "volunteer") }.orEmpty().ifBlank { "donor" },
                    "organizationName" to profile.organizationName.trim().take(120),
                    "address" to profile.address.trim().take(250),
                    "district" to profile.district.trim().take(80),
                    "state" to profile.state.trim().take(80),
                    "pinCode" to profile.pinCode.trim().take(10),
                    "fssaiOrNgoId" to profile.fssaiOrNgoId.trim().take(60),
                    "preferredLanguage" to profile.preferredLanguage.takeIf { it in listOf("en", "hi", "kn") }.orEmpty().ifBlank { "en" },
                    "sharePhoneWithMatchedParties" to profile.sharePhoneWithMatchedParties,
                    "shareExactLocationOnAccept" to profile.shareExactLocationOnAccept,
                    "whatsappAlertsEnabled" to profile.whatsappAlertsEnabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userDoc.update(updateUserMap).await()

                if (pubDoc.get().await().exists()) {
                    pubDoc.update(
                        mapOf(
                            "displayName" to (updateUserMap["displayName"] as String),
                            "accountType" to (updateUserMap["accountType"] as String),
                            "organizationName" to (updateUserMap["organizationName"] as String),
                            "district" to (updateUserMap["district"] as String),
                            "state" to (updateUserMap["state"] as String),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }

            logAudit(
                actorName = profile.displayName.ifBlank { "Member" },
                action = "PROFILE_SAVED",
                targetPath = userDoc.path,
                summary = "Updated ${profile.accountType} registration in ${profile.district}, ${profile.state}"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userDoc.path)
            Result.failure(e)
        }
    }

    suspend fun verifyNgoAdmin(targetUserId: String, verified: Boolean, actorName: String): Result<Unit> {
        requireUserId()
        val userDoc = db.collection("users").document(targetUserId)
        val pubDoc = db.collection("public_profiles").document(targetUserId)
        return try {
            userDoc.update(
                mapOf(
                    "ngoVerified" to verified,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            if (pubDoc.get().await().exists()) {
                pubDoc.update(
                    mapOf(
                        "ngoVerified" to verified,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            sendNotification(
                recipientUserId = targetUserId,
                title = if (verified) "NGO Verification Approved" else "Verification Updated",
                body = if (verified) "Your organization is now verified on FoodBridge India." else "Admin updated your verification status.",
                type = "verification",
                donationId = ""
            )
            logAudit(actorName, "NGO_VERIFICATION", userDoc.path, "Set ngoVerified=$verified for $targetUserId")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userDoc.path)
            Result.failure(e)
        }
    }

    suspend fun setUserSuspendedAdmin(targetUserId: String, suspended: Boolean, actorName: String): Result<Unit> {
        requireUserId()
        val userDoc = db.collection("users").document(targetUserId)
        return try {
            userDoc.update(
                mapOf(
                    "suspended" to suspended,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            logAudit(actorName, "USER_MODERATION", userDoc.path, "Set suspended=$suspended for $targetUserId")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userDoc.path)
            Result.failure(e)
        }
    }

    suspend fun sendNotification(
        recipientUserId: String,
        title: String,
        body: String,
        type: String,
        donationId: String
    ) {
        val senderId = auth.currentUser?.uid ?: return
        val ref = db.collection("users").document(recipientUserId).collection("notifications").document()
        try {
            ref.set(
                mapOf(
                    "senderId" to senderId,
                    "title" to title.take(120),
                    "body" to body.take(300),
                    "type" to type.take(40),
                    "donationId" to donationId.take(128),
                    "isRead" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
        }
    }

    suspend fun markNotificationRead(userId: String, notificationId: String) {
        val ref = db.collection("users").document(userId).collection("notifications").document(notificationId)
        try {
            ref.update("isRead", true).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
        }
    }

    suspend fun logAudit(actorName: String, action: String, targetPath: String, summary: String) {
        val actorId = auth.currentUser?.uid ?: return
        val ref = db.collection("audit_logs").document()
        try {
            ref.set(
                mapOf(
                    "actorId" to actorId,
                    "actorName" to actorName.take(100).ifBlank { "Member" },
                    "action" to action.take(80),
                    "targetPath" to targetPath.take(200),
                    "summary" to summary.take(300),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
        }
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
