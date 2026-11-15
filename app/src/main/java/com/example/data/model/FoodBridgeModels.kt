package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val phone: String = "",
    val accountType: String = "donor", // donor, receiver, volunteer
    val organizationName: String = "",
    val address: String = "",
    val district: String = "Bengaluru Urban",
    val state: String = "Karnataka",
    val pinCode: String = "560038",
    val fssaiOrNgoId: String = "",
    val preferredLanguage: String = "en",
    val sharePhoneWithMatchedParties: Boolean = true,
    val shareExactLocationOnAccept: Boolean = true,
    val whatsappAlertsEnabled: Boolean = true,
    val ngoVerified: Boolean = false,
    val suspended: Boolean = false,
    val points: Long = 0L,
    val mealsShared: Long = 0L,
    val deliveriesCompleted: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PublicProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val accountType: String = "donor",
    val organizationName: String = "",
    val district: String = "Bengaluru Urban",
    val state: String = "Karnataka",
    val ngoVerified: Boolean = false,
    val ratingAvg: Double = 5.0,
    val ratingCount: Long = 0L,
    val mealsShared: Long = 0L,
    val deliveriesCompleted: Long = 0L,
    val isPublic: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Donation(
    @DocumentId val id: String = "",
    val donorId: String = "",
    val donorOrgName: String = "",
    val donorDisplayName: String = "",
    val foodName: String = "",
    val category: String = "Cooked Meals",
    val quantityKg: Double = 1.0,
    val servings: Long = 10L,
    val dietType: String = "veg",
    val preparedAtMillis: Long = 0L,
    val expiryAtMillis: Long = 0L,
    val storageType: String = "Hot Box",
    val allergens: List<String> = emptyList(),
    val isSafeConfirmed: Boolean = true,
    val areaLabel: String = "",
    val district: String = "Bengaluru Urban",
    val state: String = "Karnataka",
    val pinCode: String = "560038",
    val approxLat: Double = 12.9716,
    val approxLng: Double = 77.5946,
    val imageKey: String = "preset_meal",
    val imageBase64: String = "",
    val trackingId: String = "",
    val status: String = "Donation Posted",
    val isActiveListing: Boolean = true,
    val acceptedReceiverId: String = "",
    val acceptedReceiverName: String = "",
    val assignedVolunteerId: String = "",
    val assignedVolunteerName: String = "",
    val volunteerLat: Double = 12.9716,
    val volunteerLng: Double = 77.5946,
    val locationSharingEnabled: Boolean = false,
    val qrPayload: String = "",
    val isSample: Boolean = false,
    val estimatedValueInr: Long = 500L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DonationPrivateDetails(
    @DocumentId val id: String = "",
    val donorId: String = "",
    val acceptedReceiverId: String = "",
    val assignedVolunteerId: String = "",
    val donorPhone: String = "",
    val receiverPhone: String = "",
    val volunteerPhone: String = "",
    val exactAddress: String = "",
    val exactLat: Double = 12.9716,
    val exactLng: Double = 77.5946,
    val deliveryOtp: String = "",
    val contactConsent: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FoodRequest(
    @DocumentId val id: String = "",
    val donationId: String = "",
    val donorId: String = "",
    val receiverId: String = "",
    val receiverName: String = "",
    val receiverOrgType: String = "NGO",
    val requestedServings: Long = 10L,
    val notes: String = "",
    val urgency: String = "normal",
    val status: String = "pending",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class EmergencyRequest(
    @DocumentId val id: String = "",
    val requesterId: String = "",
    val requesterName: String = "",
    val organizationType: String = "Shelter",
    val district: String = "Bengaluru Urban",
    val state: String = "Karnataka",
    val pinCode: String = "560038",
    val areaLabel: String = "",
    val servingsNeeded: Long = 50L,
    val dietPreference: String = "veg",
    val urgencyReason: String = "",
    val status: String = "open",
    val fulfilledByOrg: String = "",
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Complaint(
    @DocumentId val id: String = "",
    val reporterId: String = "",
    val reporterName: String = "",
    val targetDonationId: String = "",
    val targetUserId: String = "",
    val category: String = "hygiene_issue",
    val description: String = "",
    val severity: String = "medium",
    val status: String = "open",
    val adminNotes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Review(
    @DocumentId val id: String = "",
    val donationId: String = "",
    val reviewerId: String = "",
    val reviewerName: String = "",
    val targetUserId: String = "",
    val rating: Long = 5L,
    val comment: String = "",
    val isPublic: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class NotificationItem(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "alert",
    val donationId: String = "",
    val isRead: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AuditLog(
    @DocumentId val id: String = "",
    val actorId: String = "",
    val actorName: String = "",
    val action: String = "",
    val targetPath: String = "",
    val summary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
