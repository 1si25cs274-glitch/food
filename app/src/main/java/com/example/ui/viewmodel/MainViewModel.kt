package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.AppLanguage
import com.example.core.IndiaData
import com.example.core.LanguagePreferences
import com.example.core.LatLngPoint
import com.example.core.LocalNotifier
import com.example.data.model.AuditLog
import com.example.data.model.Complaint
import com.example.data.model.Donation
import com.example.data.model.DonationPrivateDetails
import com.example.data.model.EmergencyRequest
import com.example.data.model.FoodRequest
import com.example.data.model.NotificationItem
import com.example.data.model.PublicProfile
import com.example.data.model.Review
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.repository.DonationRepository
import com.example.data.repository.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainViewModel(
    private val userRepository: UserRepository,
    private val donationRepository: DonationRepository,
    private val languagePreferences: LanguagePreferences,
    private val db: FirebaseFirestore,
    val currentUserId: String,
    val currentUserEmail: String,
    val currentUserDisplayName: String,
    val isEmailVerified: Boolean
) : ViewModel() {

    private val _isAdmin = MutableStateFlow(
        isEmailVerified && currentUserEmail.equals("1si25cs274@sit.ac.in", ignoreCase = true)
    )
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>()
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    private val _userLocation = MutableStateFlow(LatLngPoint(12.9716, 77.5946))
    val userLocation: StateFlow<LatLngPoint> = _userLocation.asStateFlow()

    private val _selectedCity = MutableStateFlow(IndiaData.majorCities.first())
    val selectedCity = _selectedCity.asStateFlow()

    private val _selectedTrackingDonationId = MutableStateFlow<String?>(null)
    val selectedTrackingDonationId: StateFlow<String?> = _selectedTrackingDonationId.asStateFlow()

    private val _privateDetailsMap = MutableStateFlow<Map<String, DonationPrivateDetails>>(emptyMap())
    val privateDetailsMap: StateFlow<Map<String, DonationPrivateDetails>> = _privateDetailsMap.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    val language: StateFlow<AppLanguage> = languagePreferences.languageFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), AppLanguage.EN)

    val userProfileState: StateFlow<UiState<UserProfile?>> = userRepository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { err ->
            val root = unwrapError(err, "userProfile")
            emit(UiState.Error(root.message ?: "Failed to load profile"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), UiState.Loading)

    val activeDonations: StateFlow<List<Donation>> = donationRepository.observeActiveDonations()
        .safeListState(emptyList(), "activeDonations")

    val myDonorDonations: StateFlow<List<Donation>> = donationRepository.observeDonorDonations(currentUserId)
        .safeListState(emptyList(), "myDonorDonations")

    val myReceiverDonations: StateFlow<List<Donation>> = donationRepository.observeReceiverDonations(currentUserId)
        .safeListState(emptyList(), "myReceiverDonations")

    val myVolunteerDonations: StateFlow<List<Donation>> = donationRepository.observeVolunteerDonations(currentUserId)
        .safeListState(emptyList(), "myVolunteerDonations")

    val donorRequests: StateFlow<List<FoodRequest>> = donationRepository.observeRequestsForDonor(currentUserId)
        .safeListState(emptyList(), "donorRequests")

    val myRequests: StateFlow<List<FoodRequest>> = donationRepository.observeRequestsForReceiver(currentUserId)
        .safeListState(emptyList(), "myRequests")

    val emergencies: StateFlow<List<EmergencyRequest>> = donationRepository.observeActiveEmergencies()
        .safeListState(emptyList(), "emergencies")

    val notifications: StateFlow<List<NotificationItem>> = userRepository.observeNotifications(currentUserId)
        .safeListState(emptyList(), "notifications")

    val publicProfiles: StateFlow<List<PublicProfile>> = userRepository.observePublicProfiles()
        .safeListState(emptyList(), "publicProfiles")

    val publicReviews: StateFlow<List<Review>> = donationRepository.observePublicReviews()
        .safeListState(emptyList(), "publicReviews")

    val myComplaints: StateFlow<List<Complaint>> = donationRepository.observeMyComplaints(currentUserId)
        .safeListState(emptyList(), "myComplaints")

    val allUsersAdmin: StateFlow<List<UserProfile>> =
        (if (_isAdmin.value) userRepository.observeAllUsersAdmin() else flowOf(emptyList()))
            .safeListState(emptyList(), "allUsersAdmin")

    val allDonationsAdmin: StateFlow<List<Donation>> =
        (if (_isAdmin.value) donationRepository.observeAllDonationsAdmin() else flowOf(emptyList()))
            .safeListState(emptyList(), "allDonationsAdmin")

    val allComplaintsAdmin: StateFlow<List<Complaint>> =
        (if (_isAdmin.value) donationRepository.observeAllComplaintsAdmin() else flowOf(emptyList()))
            .safeListState(emptyList(), "allComplaintsAdmin")

    val auditLogsAdmin: StateFlow<List<AuditLog>> =
        (if (_isAdmin.value) userRepository.observeAuditLogs() else flowOf(emptyList()))
            .safeListState(emptyList(), "auditLogsAdmin")

    init {
        viewModelScope.launch {
            runCatching {
                val adminSnap = db.collection("admins").document(currentUserId).get().await()
                if (adminSnap.exists()) {
                    _isAdmin.value = true
                }
            }
        }
    }

    fun setLanguage(lang: AppLanguage) {
        viewModelScope.launch {
            languagePreferences.setLanguage(lang)
        }
    }

    fun selectCity(cityName: String) {
        val anchor = IndiaData.majorCities.firstOrNull { it.name == cityName } ?: return
        _selectedCity.value = anchor
        _userLocation.value = LatLngPoint(anchor.lat, anchor.lng)
    }

    fun updateUserGpsLocation(point: LatLngPoint) {
        _userLocation.value = point
    }

    fun selectTrackingDonation(donationId: String?) {
        _selectedTrackingDonationId.value = donationId
        if (!donationId.isNullOrBlank()) {
            loadPrivateDetails(donationId)
        }
    }

    fun loadPrivateDetails(donationId: String) {
        viewModelScope.launch {
            val res = donationRepository.getPrivateDetails(donationId)
            val details = res.getOrNull()
            if (details != null) {
                _privateDetailsMap.value = _privateDetailsMap.value + (donationId to details)
            }
        }
    }

    fun saveUserProfile(profile: UserProfile, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = userRepository.saveUserProfile(profile)
            _isBusy.value = false
            if (res.isSuccess) {
                _snackbarEvents.emit("Profile saved successfully")
                onSuccess()
            } else {
                _snackbarEvents.emit(res.exceptionOrNull()?.localizedMessage ?: "Failed to save profile")
            }
        }
    }

    fun postDonation(
        context: Context,
        donation: Donation,
        exactAddress: String,
        exactLat: Double,
        exactLng: Double,
        donorPhone: String,
        contactConsent: Boolean,
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = donationRepository.createDonation(
                donation = donation,
                exactAddress = exactAddress,
                exactLat = exactLat,
                exactLng = exactLng,
                donorPhone = donorPhone,
                contactConsent = contactConsent
            )
            _isBusy.value = false
            if (res.isSuccess) {
                val id = res.getOrThrow()
                LocalNotifier.showAlert(
                    context,
                    id.hashCode(),
                    "FoodBridge Donation Posted",
                    "${donation.foodName} (${donation.servings} servings) is now visible to nearby NGOs."
                )
                _snackbarEvents.emit("Donation posted! Safety & tracking ID generated.")
                onSuccess(id)
            } else {
                _snackbarEvents.emit(res.exceptionOrNull()?.localizedMessage ?: "Could not post donation")
            }
        }
    }

    fun updateDonationBasic(donationId: String, foodName: String, servings: Long, quantityKg: Double, storageType: String) {
        viewModelScope.launch {
            val res = donationRepository.updateDonationBasic(donationId, foodName, servings, quantityKg, storageType)
            if (res.isSuccess) {
                _snackbarEvents.emit("Donation updated")
            } else {
                _snackbarEvents.emit(res.exceptionOrNull()?.localizedMessage ?: "Update failed")
            }
        }
    }

    fun cancelDonation(donationId: String, actorName: String) {
        viewModelScope.launch {
            val res = donationRepository.cancelDonation(donationId, actorName)
            if (res.isSuccess) {
                _snackbarEvents.emit("Donation cancelled")
            } else {
                _snackbarEvents.emit(res.exceptionOrNull()?.localizedMessage ?: "Cancel failed")
            }
        }
    }

    fun requestFood(
        context: Context,
        donation: Donation,
        receiverName: String,
        receiverOrgType: String,
        requestedServings: Long,
        receiverPhone: String,
        notes: String,
        urgency: String
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = donationRepository.submitFoodRequest(
                donation = donation,
                receiverName = receiverName,
                receiverOrgType = receiverOrgType,
                requestedServings = requestedServings,
                receiverPhone = receiverPhone,
                notes = notes,
                urgency = urgency
            )
            _isBusy.value = false
            if (res.isSuccess) {
                LocalNotifier.showAlert(
                    context,
                    donation.id.hashCode(),
                    "Request Sent",
                    "Requested $requestedServings servings of ${donation.foodName}."
                )
                _snackbarEvents.emit("Food request submitted to donor!")
            } else {
                _snackbarEvents.emit(res.exceptionOrNull()?.localizedMessage ?: "Request failed")
            }
        }
    }

    fun acceptRequest(context: Context, request: FoodRequest, donorName: String) {
        viewModelScope.launch {
            val res = donationRepository.acceptFoodRequest(request, donorName)
            if (res.isSuccess) {
                LocalNotifier.showAlert(
                    context,
                    request.id.hashCode(),
                    "Request Accepted",
                    "Approved ${request.receiverName} for ${request.requestedServings} servings."
                )
                _snackbarEvents.emit("Request accepted! Receiver & volunteers notified.")
            } else {
                _snackbarEvents.emit(res.exceptionOrNull()?.localizedMessage ?: "Could not accept request")
            }
        }
    }

    fun rejectRequest(request: FoodRequest) {
        viewModelScope.launch {
            val res = donationRepository.rejectFoodRequest(request)
            if (res.isSuccess) _snackbarEvents.emit("Request declined")
        }
    }

    fun claimVolunteerTask(context: Context, donation: Donation, volunteerName: String, volunteerPhone: String) {
        viewModelScope.launch {
            val res = donationRepository.claimVolunteerTask(donation, volunteerName, volunteerPhone)
            if (res.isSuccess) {
                LocalNotifier.showAlert(
                    context,
                    donation.id.hashCode(),
                    "Pickup Task Claimed",
                    "Navigate to ${donation.areaLabel} for ${donation.trackingId}."
                )
                _snackbarEvents.emit("You are assigned to ${donation.trackingId}!")
                loadPrivateDetails(donation.id)
            } else {
                _snackbarEvents.emit(res.exceptionOrNull()?.localizedMessage ?: "Unable to claim task")
            }
        }
    }

    fun advanceDeliveryStatus(context: Context, donation: Donation, nextStatus: String, actorName: String) {
        viewModelScope.launch {
            val res = donationRepository.updateDeliveryStatus(donation, nextStatus, actorName)
            if (res.isSuccess) {
                LocalNotifier.showAlert(
                    context,
                    donation.id.hashCode() + nextStatus.hashCode(),
                    "Delivery Update: $nextStatus",
                    "${donation.trackingId} is now $nextStatus."
                )
                _snackbarEvents.emit("Updated status to $nextStatus")
            } else {
                _snackbarEvents.emit(res.exceptionOrNull()?.localizedMessage ?: "Status update failed")
            }
        }
    }

    fun toggleVolunteerLocationSharing(donation: Donation, enabled: Boolean) {
        viewModelScope.launch {
            val point = _userLocation.value
            val res = donationRepository.updateVolunteerLocationSharing(donation.id, enabled, point.lat, point.lng)
            if (res.isSuccess) {
                _snackbarEvents.emit(if (enabled) "Live volunteer GPS sharing enabled" else "Live GPS sharing paused")
            }
        }
    }

    fun confirmDeliveryOtp(context: Context, donation: Donation, otpOrQr: String, actorName: String, onVerified: () -> Unit) {
        viewModelScope.launch {
            val res = donationRepository.confirmDeliveryWithOtp(donation, otpOrQr, actorName)
            if (res.isSuccess) {
                LocalNotifier.showAlert(
                    context,
                    donation.id.hashCode(),
                    "Delivery Completed!",
                    "${donation.trackingId} verified (+50 Karma Points earned)."
                )
                _snackbarEvents.emit("Delivery verified & completed! +50 Karma Points")
                onVerified()
            } else {
                _snackbarEvents.emit(res.exceptionOrNull()?.localizedMessage ?: "OTP verification failed")
            }
        }
    }

    fun postEmergencyRequest(request: EmergencyRequest) {
        viewModelScope.launch {
            val res = donationRepository.createEmergencyRequest(request)
            if (res.isSuccess) {
                _snackbarEvents.emit("Emergency food alert broadcasted!")
            } else {
                _snackbarEvents.emit(res.exceptionOrNull()?.localizedMessage ?: "Failed to post emergency")
            }
        }
    }

    fun fulfillEmergency(emergencyId: String, orgName: String) {
        viewModelScope.launch {
            val res = donationRepository.fulfillEmergencyRequest(emergencyId, orgName)
            if (res.isSuccess) {
                _snackbarEvents.emit("Marked emergency request fulfilled! Thank you.")
            }
        }
    }

    fun submitSafetyComplaint(complaint: Complaint) {
        viewModelScope.launch {
            val res = donationRepository.submitComplaint(complaint)
            if (res.isSuccess) {
                _snackbarEvents.emit("Safety report submitted to Admin moderation queue.")
            } else {
                _snackbarEvents.emit(res.exceptionOrNull()?.localizedMessage ?: "Could not submit report")
            }
        }
    }

    fun submitReview(review: Review) {
        viewModelScope.launch {
            val res = donationRepository.submitReview(review)
            if (res.isSuccess) {
                _snackbarEvents.emit("Thank you for your feedback!")
            } else {
                _snackbarEvents.emit(res.exceptionOrNull()?.localizedMessage ?: "Review failed")
            }
        }
    }

    fun markNotificationRead(notificationId: String) {
        viewModelScope.launch {
            userRepository.markNotificationRead(currentUserId, notificationId)
        }
    }

    fun verifyNgoAdmin(targetUserId: String, verified: Boolean, actorName: String) {
        viewModelScope.launch {
            val res = userRepository.verifyNgoAdmin(targetUserId, verified, actorName)
            if (res.isSuccess) _snackbarEvents.emit("NGO verification updated")
        }
    }

    fun suspendUserAdmin(targetUserId: String, suspended: Boolean, actorName: String) {
        viewModelScope.launch {
            val res = userRepository.setUserSuspendedAdmin(targetUserId, suspended, actorName)
            if (res.isSuccess) _snackbarEvents.emit(if (suspended) "User suspended" else "User reinstated")
        }
    }

    fun updateComplaintAdmin(complaintId: String, status: String, notes: String, actorName: String) {
        viewModelScope.launch {
            val res = donationRepository.updateComplaintAdmin(complaintId, status, notes, actorName)
            if (res.isSuccess) _snackbarEvents.emit("Complaint marked $status")
        }
    }

    fun seedSampleData(userName: String, userPhone: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = donationRepository.seedSampleIndianDonations(userName, userPhone)
            _isBusy.value = false
            if (res.isSuccess) {
                _snackbarEvents.emit("Loaded ${res.getOrThrow()} sample Indian city donations & emergency alert!")
            } else {
                _snackbarEvents.emit(res.exceptionOrNull()?.localizedMessage ?: "Sample seeding failed")
            }
        }
    }

    private fun <T> Flow<List<T>>.safeListState(initial: List<T>, label: String): StateFlow<List<T>> =
        this.catch { err ->
            unwrapError(err, label)
            emit(initial)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), initial)

    private fun unwrapError(error: Throwable, label: String): Throwable {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val root = firestoreError ?: error
        Log.w(TAG, "Stream error [$label]: ${root.message}", root)
        return root
    }

    private companion object {
        const val TAG = "MainViewModel"
        const val STOP_TIMEOUT = 5000L
    }
}
