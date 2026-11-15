package com.example.ui

import android.Manifest
import android.app.Application
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.core.AppLanguage
import com.example.core.AppStrings
import com.example.core.IndiaData
import com.example.core.LanguagePreferences
import com.example.data.model.Donation
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.repository.DonationRepository
import com.example.data.repository.UserRepository
import com.example.ui.auth.GoogleAuthHelper
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MapExploreScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RoleHubScreen
import com.example.ui.screens.TrackingScreen
import com.example.ui.viewmodel.MainViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

const val ROUTE_HOME = "home"
const val ROUTE_MAP = "map"
const val ROUTE_HUB = "hub"
const val ROUTE_TRACK = "track"
const val ROUTE_PROFILE = "profile"

private data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser) }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun FoodBridgeApp() {
    val context = LocalContext.current
    val languagePreferences = remember(context) { LanguagePreferences(context.applicationContext) }
    val selectedLang by languagePreferences.languageFlow.collectAsStateWithLifecycle(initialValue = AppLanguage.EN)
    val scope = rememberCoroutineScope()

    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(
            selectedLanguage = selectedLang,
            onLanguageSelected = { lang ->
                scope.launch { languagePreferences.setLanguage(lang) }
            },
            onAuthSuccess = {}
        )
    } else {
        key(user.uid) {
            AuthenticatedFoodBridgeShell(
                user = user,
                languagePreferences = languagePreferences
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedFoodBridgeShell(
    user: FirebaseUser,
    languagePreferences: LanguagePreferences
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: MainViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as Application
                val dbId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                val userRepo = UserRepository(db)
                val donationRepo = DonationRepository(db, userRepo)
                MainViewModel(
                    userRepository = userRepo,
                    donationRepository = donationRepo,
                    languagePreferences = languagePreferences,
                    db = db,
                    currentUserId = user.uid,
                    currentUserEmail = user.email.orEmpty(),
                    currentUserDisplayName = user.displayName.orEmpty(),
                    isEmailVerified = user.isEmailVerified
                )
            }
        }
    )

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {}

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val profileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val strings = AppStrings.get(language)

    when (val state = profileState) {
        UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is UiState.Error -> {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Unable to load profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(state.message, style = MaterialTheme.typography.bodySmall)
                    Button(onClick = { GoogleAuthHelper.signOut(credentialManager, scope) }) {
                        Text("Sign Out & Retry")
                    }
                }
            }
        }

        is UiState.Success -> {
            val profile = state.data
            if (profile == null) {
                OnboardingScreen(
                    uid = user.uid,
                    email = user.email.orEmpty(),
                    initialDisplayName = user.displayName.orEmpty(),
                    selectedLanguage = language,
                    isBusy = isBusy,
                    onSaveProfile = { newProfile -> viewModel.saveUserProfile(newProfile) }
                )
            } else {
                MainAuthenticatedScaffold(
                    profile = profile,
                    isAdmin = isAdmin,
                    language = language,
                    strings = strings,
                    viewModel = viewModel,
                    snackbarHostState = snackbarHostState,
                    onSignOut = { GoogleAuthHelper.signOut(credentialManager, scope) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainAuthenticatedScaffold(
    profile: UserProfile,
    isAdmin: Boolean,
    language: AppLanguage,
    strings: com.example.core.LocalizedStrings,
    viewModel: MainViewModel,
    snackbarHostState: SnackbarHostState,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route ?: ROUTE_HOME

    val activeDonations by viewModel.activeDonations.collectAsStateWithLifecycle()
    val myDonorDonations by viewModel.myDonorDonations.collectAsStateWithLifecycle()
    val myReceiverDonations by viewModel.myReceiverDonations.collectAsStateWithLifecycle()
    val myVolunteerDonations by viewModel.myVolunteerDonations.collectAsStateWithLifecycle()
    val donorRequests by viewModel.donorRequests.collectAsStateWithLifecycle()
    val myRequests by viewModel.myRequests.collectAsStateWithLifecycle()
    val emergencies by viewModel.emergencies.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val publicReviews by viewModel.publicReviews.collectAsStateWithLifecycle()
    val allUsersAdmin by viewModel.allUsersAdmin.collectAsStateWithLifecycle()
    val allDonationsAdmin by viewModel.allDonationsAdmin.collectAsStateWithLifecycle()
    val allComplaintsAdmin by viewModel.allComplaintsAdmin.collectAsStateWithLifecycle()
    val auditLogsAdmin by viewModel.auditLogsAdmin.collectAsStateWithLifecycle()
    val userLocation by viewModel.userLocation.collectAsStateWithLifecycle()
    val selectedCity by viewModel.selectedCity.collectAsStateWithLifecycle()
    val selectedTrackingId by viewModel.selectedTrackingDonationId.collectAsStateWithLifecycle()
    val privateDetailsMap by viewModel.privateDetailsMap.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    var hubInitialTab by remember { mutableIntStateOf(0) }
    var requestDialogDonation by remember { mutableStateOf<Donation?>(null) }
    var otpDialogDonation by remember { mutableStateOf<Donation?>(null) }
    var showNotificationsDialog by remember { mutableStateOf(false) }

    val allCombinedDonations = remember(activeDonations, myDonorDonations, myReceiverDonations, myVolunteerDonations) {
        (activeDonations + myDonorDonations + myReceiverDonations + myVolunteerDonations)
            .distinctBy { it.id }
    }

    val navItems = remember(strings) {
        listOf(
            NavItem(ROUTE_HOME, strings.navHome, Icons.Default.Home),
            NavItem(ROUTE_MAP, strings.navMap, Icons.Default.Map),
            NavItem(ROUTE_HUB, strings.navHub, Icons.Default.Hub),
            NavItem(ROUTE_TRACK, strings.navTrack, Icons.Default.QrCode2),
            NavItem(ROUTE_PROFILE, strings.navProfile, Icons.Default.Person)
        )
    }

    val unreadCount = notifications.count { !it.isRead }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(strings.appTitle, fontWeight = FontWeight.Bold)
                            Text(
                                "${selectedCity.name} • ${strings.fssaiBanner}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showNotificationsDialog = true },
                            modifier = Modifier.testTag("notifications_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (unreadCount > 0) {
                                        Badge { Text(unreadCount.toString()) }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        navItems.forEach { item ->
                            NavigationBarItem(
                                selected = currentRoute == item.route,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(ROUTE_HOME) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag("nav_${item.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        navItems.forEach { item ->
                            NavigationRailItem(
                                selected = currentRoute == item.route,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(ROUTE_HOME) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label) }
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_HOME,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(ROUTE_HOME) {
                        HomeScreen(
                            profile = profile,
                            strings = strings,
                            activeDonations = activeDonations,
                            allTrackedDonations = allCombinedDonations,
                            emergencies = emergencies,
                            userLocation = userLocation,
                            isBusy = isBusy,
                            onNavigateToHub = { tab ->
                                hubInitialTab = tab
                                navController.navigate(ROUTE_HUB) {
                                    popUpTo(ROUTE_HOME) { saveState = true }
                                    launchSingleTop = true
                                }
                            },
                            onNavigateToMap = {
                                navController.navigate(ROUTE_MAP) {
                                    popUpTo(ROUTE_HOME) { saveState = true }
                                    launchSingleTop = true
                                }
                            },
                            onTrackDonation = { id ->
                                viewModel.selectTrackingDonation(id)
                                navController.navigate(ROUTE_TRACK) {
                                    popUpTo(ROUTE_HOME) { saveState = true }
                                    launchSingleTop = true
                                }
                            },
                            onRequestFood = { d -> requestDialogDonation = d },
                            onFulfillEmergency = { emId ->
                                viewModel.fulfillEmergency(emId, profile.organizationName.ifBlank { profile.displayName })
                            },
                            onSeedSampleData = {
                                viewModel.seedSampleData(profile.displayName, profile.phone)
                            }
                        )
                    }

                    composable(ROUTE_MAP) {
                        MapExploreScreen(
                            donations = activeDonations,
                            userLocation = userLocation,
                            selectedCity = selectedCity,
                            strings = strings,
                            onSelectCity = viewModel::selectCity,
                            onUpdateGps = viewModel::updateUserGpsLocation,
                            onRequestFood = { d -> requestDialogDonation = d },
                            onTrackDonation = { id ->
                                viewModel.selectTrackingDonation(id)
                                navController.navigate(ROUTE_TRACK) {
                                    popUpTo(ROUTE_HOME) { saveState = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable(ROUTE_HUB) {
                        RoleHubScreen(
                            initialTab = hubInitialTab,
                            profile = profile,
                            isAdmin = isAdmin,
                            strings = strings,
                            activeDonations = activeDonations,
                            myDonorDonations = myDonorDonations,
                            myReceiverDonations = myReceiverDonations,
                            myVolunteerDonations = myVolunteerDonations,
                            donorRequests = donorRequests,
                            myRequests = myRequests,
                            emergencies = emergencies,
                            allUsersAdmin = allUsersAdmin,
                            allDonationsAdmin = allDonationsAdmin,
                            allComplaintsAdmin = allComplaintsAdmin,
                            auditLogsAdmin = auditLogsAdmin,
                            userLocation = userLocation,
                            isBusy = isBusy,
                            onPostDonation = { d, addr, lat, lng, phone, consent, onDone ->
                                viewModel.postDonation(context, d, addr, lat, lng, phone, consent) { onDone() }
                            },
                            onCancelDonation = { id -> viewModel.cancelDonation(id, profile.displayName) },
                            onAcceptRequest = { req -> viewModel.acceptRequest(context, req, profile.displayName) },
                            onRejectRequest = viewModel::rejectRequest,
                            onOpenRequestDialog = { d -> requestDialogDonation = d },
                            onPostEmergency = viewModel::postEmergencyRequest,
                            onFulfillEmergency = { id -> viewModel.fulfillEmergency(id, profile.organizationName) },
                            onClaimVolunteerTask = { d ->
                                viewModel.claimVolunteerTask(context, d, profile.displayName, profile.phone)
                            },
                            onAdvanceDeliveryStatus = { d, status ->
                                viewModel.advanceDeliveryStatus(context, d, status, profile.displayName)
                            },
                            onToggleVolunteerSharing = viewModel::toggleVolunteerLocationSharing,
                            onOpenOtpVerify = { d ->
                                viewModel.loadPrivateDetails(d.id)
                                otpDialogDonation = d
                            },
                            onTrackDonation = { id ->
                                viewModel.selectTrackingDonation(id)
                                navController.navigate(ROUTE_TRACK) {
                                    popUpTo(ROUTE_HOME) { saveState = true }
                                    launchSingleTop = true
                                }
                            },
                            onVerifyNgoAdmin = { uid, verified ->
                                viewModel.verifyNgoAdmin(uid, verified, profile.displayName)
                            },
                            onSuspendUserAdmin = { uid, suspended ->
                                viewModel.suspendUserAdmin(uid, suspended, profile.displayName)
                            },
                            onUpdateComplaintAdmin = { cid, status, notes ->
                                viewModel.updateComplaintAdmin(cid, status, notes, profile.displayName)
                            }
                        )
                    }

                    composable(ROUTE_TRACK) {
                        TrackingScreen(
                            profile = profile,
                            donations = allCombinedDonations,
                            selectedDonationId = selectedTrackingId,
                            privateDetailsMap = privateDetailsMap,
                            publicReviews = publicReviews,
                            onSelectDonation = viewModel::selectTrackingDonation,
                            onAdvanceStatus = { d, st -> viewModel.advanceDeliveryStatus(context, d, st, profile.displayName) },
                            onOpenOtpDialog = { d ->
                                viewModel.loadPrivateDetails(d.id)
                                otpDialogDonation = d
                            },
                            onSubmitComplaint = viewModel::submitSafetyComplaint,
                            onSubmitReview = viewModel::submitReview
                        )
                    }

                    composable(ROUTE_PROFILE) {
                        ProfileScreen(
                            profile = profile,
                            isAdmin = isAdmin,
                            selectedLanguage = language,
                            onLanguageSelected = viewModel::setLanguage,
                            onUpdatePrivacy = { updated -> viewModel.saveUserProfile(updated) },
                            onSignOut = onSignOut
                        )
                    }
                }
            }
        }
    }

    // Shared Request Food Dialog
    val targetRequestDonation = requestDialogDonation
    if (targetRequestDonation != null) {
        var reqServings by remember { mutableStateOf(targetRequestDonation.servings.coerceAtMost(25L).toString()) }
        var orgType by remember { mutableStateOf("NGO") }
        var phone by remember { mutableStateOf(profile.phone) }
        var urgency by remember { mutableStateOf("normal") }
        var notes by remember { mutableStateOf("") }
        var err by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { requestDialogDonation = null },
            title = { Text("Request ${targetRequestDonation.foodName}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Available: ${targetRequestDonation.servings} meals from ${targetRequestDonation.donorOrgName}", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = reqServings,
                        onValueChange = { reqServings = it.filter { c -> c.isDigit() } },
                        label = { Text("Servings Required *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(value = orgType, onValueChange = { orgType = it }, label = { Text("NGO / Shelter / Individual") }, singleLine = true)
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Receiver Mobile (+91) *") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = urgency == "normal", onClick = { urgency = "normal" }, label = { Text("Standard") })
                        FilterChip(selected = urgency == "emergency", onClick = { urgency = "emergency" }, label = { Text("Emergency") })
                    }
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Pickup / Dietary Notes") }, maxLines = 2)
                    if (err != null) Text(err.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val srv = reqServings.toLongOrNull() ?: 0L
                        when {
                            srv <= 0L || srv > targetRequestDonation.servings -> err = "Enter servings between 1 and ${targetRequestDonation.servings}."
                            !IndiaData.isValidIndianMobile(phone) -> err = "Enter a valid +91 mobile number."
                            else -> {
                                viewModel.requestFood(
                                    context = context,
                                    donation = targetRequestDonation,
                                    receiverName = profile.organizationName.ifBlank { profile.displayName },
                                    receiverOrgType = orgType,
                                    requestedServings = srv,
                                    receiverPhone = phone,
                                    notes = notes,
                                    urgency = urgency
                                )
                                requestDialogDonation = null
                            }
                        }
                    }
                ) { Text("Submit Request") }
            },
            dismissButton = { TextButton(onClick = { requestDialogDonation = null }) { Text("Cancel") } }
        )
    }

    // Shared OTP / QR Verification Dialog
    val targetOtpDonation = otpDialogDonation
    if (targetOtpDonation != null) {
        var otpCode by remember { mutableStateOf("") }
        val knownOtp = privateDetailsMap[targetOtpDonation.id]?.deliveryOtp
        AlertDialog(
            onDismissRequest = { otpDialogDonation = null },
            title = { Text("Confirm Delivery (${targetOtpDonation.trackingId})", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter the 4-digit Delivery OTP or scan QR payload to complete handoff.", style = MaterialTheme.typography.bodySmall)
                    if (!knownOtp.isNullOrBlank()) {
                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(10.dp)) {
                            Text("Participant OTP Reference: $knownOtp", modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = { otpCode = it },
                        label = { Text("4-digit OTP or QR Payload *") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.confirmDeliveryOtp(context, targetOtpDonation, otpCode, profile.displayName) {
                            otpDialogDonation = null
                        }
                    }
                ) { Text("Verify & Complete") }
            },
            dismissButton = { TextButton(onClick = { otpDialogDonation = null }) { Text("Cancel") } }
        )
    }

    // Notifications Dialog
    if (showNotificationsDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationsDialog = false },
            title = { Text("Notifications (${notifications.size})", fontWeight = FontWeight.Bold) },
            text = {
                if (notifications.isEmpty()) {
                    Text("No alerts yet. Requests, assignments, and delivery updates will appear here.")
                } else {
                    LazyColumn(
                        modifier = Modifier.height(320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(notifications, key = { it.id }) { n ->
                            ElevatedCard(
                                onClick = {
                                    viewModel.markNotificationRead(n.id)
                                    if (n.donationId.isNotBlank()) {
                                        viewModel.selectTrackingDonation(n.donationId)
                                        showNotificationsDialog = false
                                        navController.navigate(ROUTE_TRACK)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = if (n.isRead) n.title else "• ${n.title}",
                                        fontWeight = if (n.isRead) FontWeight.Normal else FontWeight.Bold
                                    )
                                    Text(n.body, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotificationsDialog = false }) { Text("Close") }
            }
        )
    }
}
