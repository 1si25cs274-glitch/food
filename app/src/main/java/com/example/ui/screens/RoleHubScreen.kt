package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.core.IndiaData
import com.example.core.IntentLaunchers
import com.example.core.LatLngPoint
import com.example.core.LocationHelper
import com.example.core.LocalizedStrings
import com.example.core.QrCodeUtils
import com.example.data.model.AuditLog
import com.example.data.model.Complaint
import com.example.data.model.Donation
import com.example.data.model.EmergencyRequest
import com.example.data.model.FoodRequest
import com.example.data.model.UserProfile
import com.example.ui.components.DonationCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.MintContainer
import com.example.ui.theme.OnMintContainer
import com.example.ui.theme.SaffronSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleHubScreen(
    initialTab: Int,
    profile: UserProfile,
    isAdmin: Boolean,
    strings: LocalizedStrings,
    activeDonations: List<Donation>,
    myDonorDonations: List<Donation>,
    myReceiverDonations: List<Donation>,
    myVolunteerDonations: List<Donation>,
    donorRequests: List<FoodRequest>,
    myRequests: List<FoodRequest>,
    emergencies: List<EmergencyRequest>,
    allUsersAdmin: List<UserProfile>,
    allDonationsAdmin: List<Donation>,
    allComplaintsAdmin: List<Complaint>,
    auditLogsAdmin: List<AuditLog>,
    userLocation: LatLngPoint,
    isBusy: Boolean,
    onPostDonation: (Donation, String, Double, Double, String, Boolean, () -> Unit) -> Unit,
    onCancelDonation: (String) -> Unit,
    onAcceptRequest: (FoodRequest) -> Unit,
    onRejectRequest: (FoodRequest) -> Unit,
    onOpenRequestDialog: (Donation) -> Unit,
    onPostEmergency: (EmergencyRequest) -> Unit,
    onFulfillEmergency: (String) -> Unit,
    onClaimVolunteerTask: (Donation) -> Unit,
    onAdvanceDeliveryStatus: (Donation, String) -> Unit,
    onToggleVolunteerSharing: (Donation, Boolean) -> Unit,
    onOpenOtpVerify: (Donation) -> Unit,
    onTrackDonation: (String) -> Unit,
    onVerifyNgoAdmin: (String, Boolean) -> Unit,
    onSuspendUserAdmin: (String, Boolean) -> Unit,
    onUpdateComplaintAdmin: (String, String, String) -> Unit
) {
    val tabs = remember(isAdmin) {
        buildList {
            add("Donor")
            add("Receiver")
            add("Volunteer")
            if (isAdmin) add("Admin")
        }
    }
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, tabs.lastIndex)) }
    var showPostDialog by remember { mutableStateOf(false) }
    var showEmergencyDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("hub_tab_$title")
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    item {
                        ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Donor Surplus Studio", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        "Post safe surplus food with FSSAI checks, expiry guards & protected pickup details.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { showPostDialog = true },
                                    modifier = Modifier.testTag("open_post_donation_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Post")
                                }
                            }
                        }
                    }

                    val pendingRequests = donorRequests.filter { it.status == "pending" }
                    if (pendingRequests.isNotEmpty()) {
                        item {
                            Text("Incoming Requests (${pendingRequests.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        items(pendingRequests, key = { it.id }) { req ->
                            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${req.receiverName} (${req.receiverOrgType})", fontWeight = FontWeight.Bold)
                                        StatusPill(req.urgency.uppercase())
                                    }
                                    Text("Requested: ${req.requestedServings} servings", style = MaterialTheme.typography.bodySmall)
                                    if (req.notes.isNotBlank()) {
                                        Text(req.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { onAcceptRequest(req) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Accept")
                                        }
                                        OutlinedButton(
                                            onClick = { onRejectRequest(req) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Decline")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text("My Donations (${myDonorDonations.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    if (myDonorDonations.isEmpty()) {
                        item {
                            Text("You have not posted any donations yet. Tap Post above to share surplus food.", style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        items(myDonorDonations, key = { it.id }) { donation ->
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                DonationCard(
                                    donation = donation,
                                    userLocation = userLocation,
                                    primaryActionLabel = if (donation.status != "Completed" && donation.status != "Cancelled") "Cancel Donation" else null,
                                    onPrimaryAction = if (donation.status != "Completed" && donation.status != "Cancelled") {
                                        { onCancelDonation(donation.id) }
                                    } else null,
                                    onTrackClick = { onTrackDonation(donation.id) }
                                )
                            }
                        }
                    }
                }

                1 -> {
                    item {
                        ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Receiver & NGO Portal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        "Request available food or broadcast an emergency meal need for shelters & orphanages.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { showEmergencyDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = SaffronSecondary),
                                    modifier = Modifier.testTag("open_emergency_dialog_button")
                                ) {
                                    Icon(Icons.Default.Emergency, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Urgent")
                                }
                            }
                        }
                    }

                    if (myRequests.isNotEmpty()) {
                        item {
                            Text("My Food Requests (${myRequests.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        items(myRequests, key = { it.id }) { req ->
                            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${req.requestedServings} servings requested", fontWeight = FontWeight.Bold)
                                        Text(req.notes, style = MaterialTheme.typography.bodySmall)
                                    }
                                    StatusPill(req.status.uppercase())
                                }
                            }
                        }
                    }

                    if (myReceiverDonations.isNotEmpty()) {
                        item {
                            Text("Approved Deliveries For Me", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        items(myReceiverDonations, key = { it.id }) { d ->
                            DonationCard(
                                donation = d,
                                userLocation = userLocation,
                                primaryActionLabel = "Confirm Receipt (OTP)",
                                onPrimaryAction = { onOpenOtpVerify(d) },
                                onTrackClick = { onTrackDonation(d.id) }
                            )
                        }
                    }

                    item {
                        Text("Available Donations Nearby", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    items(activeDonations, key = { it.id }) { donation ->
                        DonationCard(
                            donation = donation,
                            userLocation = userLocation,
                            primaryActionLabel = strings.requestFood,
                            onPrimaryAction = { onOpenRequestDialog(donation) },
                            onTrackClick = { onTrackDonation(donation.id) }
                        )
                    }

                    if (emergencies.isNotEmpty()) {
                        item {
                            Text("Open Emergency Food Requests", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        items(emergencies, key = { it.id }) { em ->
                            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("${em.requesterName} • ${em.servingsNeeded} meals", fontWeight = FontWeight.Bold)
                                    Text(em.urgencyReason, style = MaterialTheme.typography.bodySmall)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${em.areaLabel}, ${em.district}", style = MaterialTheme.typography.labelSmall)
                                        OutlinedButton(onClick = { onFulfillEmergency(em.id) }) {
                                            Text("Mark Fulfilled")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    item {
                        ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_volunteer_delivery),
                                    contentDescription = "Volunteer delivery",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Volunteer Dispatch", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        "Earn +50 Karma points per verified food rescue. Toggle live GPS sharing during active pickups.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    if (myVolunteerDonations.isNotEmpty()) {
                        item {
                            Text("My Active & Completed Tasks (${myVolunteerDonations.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        items(myVolunteerDonations, key = { it.id }) { task ->
                            VolunteerTaskCard(
                                donation = task,
                                onToggleLocationSharing = { onToggleVolunteerSharing(task, it) },
                                onAdvanceStatus = { next -> onAdvanceDeliveryStatus(task, next) },
                                onVerifyOtp = { onOpenOtpVerify(task) },
                                onTrack = { onTrackDonation(task.id) }
                            )
                        }
                    }

                    val openTasks = activeDonations.filter {
                        it.assignedVolunteerId.isBlank() &&
                            it.status in listOf("Donation Posted", "Request Received", "Accepted")
                    }
                    item {
                        Text("Open Pickup Tasks (${openTasks.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    items(openTasks, key = { it.id }) { d ->
                        DonationCard(
                            donation = d,
                            userLocation = userLocation,
                            primaryActionLabel = strings.claimTask,
                            onPrimaryAction = { onClaimVolunteerTask(d) },
                            onTrackClick = { onTrackDonation(d.id) }
                        )
                    }
                }

                3 -> {
                    item {
                        AdminConsoleSection(
                            allUsers = allUsersAdmin,
                            allDonations = allDonationsAdmin,
                            allComplaints = allComplaintsAdmin,
                            auditLogs = auditLogsAdmin,
                            actorName = profile.displayName,
                            onVerifyNgo = onVerifyNgoAdmin,
                            onSuspendUser = onSuspendUserAdmin,
                            onUpdateComplaint = onUpdateComplaintAdmin,
                            onCancelDonation = onCancelDonation
                        )
                    }
                }
            }
        }
    }

    if (showPostDialog) {
        PostDonationDialog(
            profile = profile,
            userLocation = userLocation,
            isBusy = isBusy,
            onDismiss = { showPostDialog = false },
            onSubmit = { donation, exactAddress, lat, lng, phone, consent ->
                onPostDonation(donation, exactAddress, lat, lng, phone, consent) {
                    showPostDialog = false
                }
            }
        )
    }

    if (showEmergencyDialog) {
        PostEmergencyDialog(
            profile = profile,
            onDismiss = { showEmergencyDialog = false },
            onSubmit = { req ->
                onPostEmergency(req)
                showEmergencyDialog = false
            }
        )
    }
}

@Composable
private fun VolunteerTaskCard(
    donation: Donation,
    onToggleLocationSharing: (Boolean) -> Unit,
    onAdvanceStatus: (String) -> Unit,
    onVerifyOtp: () -> Unit,
    onTrack: () -> Unit
) {
    val context = LocalContext.current
    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${donation.trackingId} • ${donation.foodName}", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                StatusPill(donation.status)
            }
            Text("${donation.donorOrgName} -> ${donation.acceptedReceiverName.ifBlank { "Community Receiver" }}", style = MaterialTheme.typography.bodySmall)
            Text("${donation.areaLabel}, ${donation.district} (${donation.pinCode})", style = MaterialTheme.typography.labelSmall)

            if (donation.status != "Completed" && donation.status != "Cancelled") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Share Live GPS on Map", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    Switch(checked = donation.locationSharingEnabled, onCheckedChange = onToggleLocationSharing)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            IntentLaunchers.openMapsDirections(context, donation.approxLat, donation.approxLng, donation.areaLabel)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Navigate")
                    }
                    OutlinedButton(onClick = onTrack, modifier = Modifier.weight(1f)) {
                        Text("Track & QR")
                    }
                }

                val nextStatus = when (donation.status) {
                    "Volunteer Assigned" -> "Food Picked Up"
                    "Food Picked Up" -> "Out for Delivery"
                    "Out for Delivery" -> "Delivered"
                    else -> null
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (nextStatus != null) {
                        Button(
                            onClick = { onAdvanceStatus(nextStatus) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Mark: $nextStatus")
                        }
                    }
                    Button(
                        onClick = onVerifyOtp,
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Complete (OTP)")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostDonationDialog(
    profile: UserProfile,
    userLocation: LatLngPoint,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (Donation, String, Double, Double, String, Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var foodName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(IndiaData.foodCategories.first()) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var quantityKg by remember { mutableStateOf("5") }
    var servings by remember { mutableStateOf("25") }
    var dietType by remember { mutableStateOf("veg") }
    var preparedHoursAgo by remember { mutableStateOf("1") }
    var shelfLifeHours by remember { mutableStateOf("4") }
    var storageType by remember { mutableStateOf(IndiaData.storageTypes.first()) }
    var storageExpanded by remember { mutableStateOf(false) }
    val selectedAllergens = remember { mutableStateListOf<String>() }
    var areaLabel by remember { mutableStateOf(profile.district) }
    var exactAddress by remember { mutableStateOf(profile.address) }
    var pinCode by remember { mutableStateOf(profile.pinCode) }
    var donorPhone by remember { mutableStateOf(profile.phone) }
    var contactConsent by remember { mutableStateOf(true) }
    var safetyConfirmed by remember { mutableStateOf(false) }
    var capturedLat by remember { mutableStateOf(userLocation.lat) }
    var capturedLng by remember { mutableStateOf(userLocation.lng) }
    var gpsCapturedLabel by remember { mutableStateOf("Area center (${String.format("%.3f", capturedLat)}, ${String.format("%.3f", capturedLng)})") }
    var imageBase64 by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val encoded = QrCodeUtils.compressPhotoUriToBase64(context, uri)
            if (encoded != null) {
                imageBase64 = encoded
            } else {
                errorMsg = "Could not compress selected image; using clean food photo fallback."
            }
        }
    }

    val gpsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val ok = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (ok) {
            scope.launch {
                val pt = LocationHelper.fetchCurrentLocation(context)
                if (pt != null) {
                    capturedLat = pt.lat
                    capturedLng = pt.lng
                    gpsCapturedLabel = "GPS captured (${String.format("%.4f", pt.lat)}, ${String.format("%.4f", pt.lng)})"
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Post Surplus Food", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = foodName,
                    onValueChange = { foodName = it },
                    label = { Text("Food Name *") },
                    placeholder = { Text("e.g., Fresh Veg Pulao & Dal") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = !categoryExpanded }) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                        IndiaData.foodCategories.forEach { cat ->
                            DropdownMenuItem(text = { Text(cat) }, onClick = { category = cat; categoryExpanded = false })
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantityKg,
                        onValueChange = { quantityKg = it },
                        label = { Text("Qty (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = servings,
                        onValueChange = { servings = it.filter { c -> c.isDigit() } },
                        label = { Text("Servings") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("veg" to "Veg", "vegan" to "Vegan", "non_veg" to "Non-Veg").forEach { (k, lbl) ->
                        FilterChip(selected = dietType == k, onClick = { dietType = k }, label = { Text(lbl) })
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = preparedHoursAgo,
                        onValueChange = { preparedHoursAgo = it.filter { c -> c.isDigit() } },
                        label = { Text("Prepared (hrs ago)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = shelfLifeHours,
                        onValueChange = { shelfLifeHours = it.filter { c -> c.isDigit() } },
                        label = { Text("Safe for (hrs)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                ExposedDropdownMenuBox(expanded = storageExpanded, onExpandedChange = { storageExpanded = !storageExpanded }) {
                    OutlinedTextField(
                        value = storageType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Storage Conditions") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = storageExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = storageExpanded, onDismissRequest = { storageExpanded = false }) {
                        IndiaData.storageTypes.forEach { st ->
                            DropdownMenuItem(text = { Text(st) }, onClick = { storageType = st; storageExpanded = false })
                        }
                    }
                }

                Text("Allergens (Tap all that apply)", style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IndiaData.allergenOptions.take(4).forEach { alg ->
                        FilterChip(
                            selected = selectedAllergens.contains(alg),
                            onClick = {
                                if (selectedAllergens.contains(alg)) selectedAllergens.remove(alg) else selectedAllergens.add(alg)
                            },
                            label = { Text(alg) }
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (imageBase64.isBlank()) "Pick Food Photo (Zero-Permission)" else "Food Photo Attached")
                }

                OutlinedTextField(
                    value = areaLabel,
                    onValueChange = { areaLabel = it },
                    label = { Text("Locality / Area (Publicly visible) *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = exactAddress,
                    onValueChange = { exactAddress = it },
                    label = { Text("Exact Pickup Address (Private until match) *") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pinCode,
                        onValueChange = { pinCode = it.filter { c -> c.isDigit() }.take(6) },
                        label = { Text("PIN Code *") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = donorPhone,
                        onValueChange = { donorPhone = it },
                        label = { Text("Phone (+91) *") },
                        modifier = Modifier.weight(1.4f),
                        singleLine = true
                    )
                }

                OutlinedButton(
                    onClick = {
                        val hasLoc = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasLoc) {
                            scope.launch {
                                val pt = LocationHelper.fetchCurrentLocation(context)
                                if (pt != null) {
                                    capturedLat = pt.lat
                                    capturedLng = pt.lng
                                    gpsCapturedLabel = "GPS captured (${String.format("%.4f", pt.lat)}, ${String.format("%.4f", pt.lng)})"
                                }
                            }
                        } else {
                            gpsLauncher.launch(
                                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(gpsCapturedLabel)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = contactConsent, onCheckedChange = { contactConsent = it })
                    Text("Allow matched NGO/volunteer to call or WhatsApp me", style = MaterialTheme.typography.bodySmall)
                }

                Surface(color = MintContainer, shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = safetyConfirmed, onCheckedChange = { safetyConfirmed = it })
                        Text(
                            "FSSAI Safety Declaration: Food is fresh, unexpired, hygienically stored, and safe for consumption.",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnMintContainer
                        )
                    }
                }

                if (errorMsg != null) {
                    Text(errorMsg.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isBusy,
                onClick = {
                    val kg = quantityKg.toDoubleOrNull() ?: 0.0
                    val srv = servings.toLongOrNull() ?: 0L
                    val prepHrs = preparedHoursAgo.toLongOrNull() ?: 1L
                    val safeHrs = shelfLifeHours.toLongOrNull() ?: 0L
                    val now = System.currentTimeMillis()
                    when {
                        foodName.trim().length < 2 -> errorMsg = "Enter food name."
                        kg <= 0.0 || srv <= 0L -> errorMsg = "Enter valid quantity & servings."
                        safeHrs <= 0L -> errorMsg = "Safe shelf life must be at least 1 hour (expired food prohibited)."
                        !IndiaData.isValidPinCode(pinCode) -> errorMsg = "Enter valid 6-digit Indian PIN code."
                        !IndiaData.isValidIndianMobile(donorPhone) -> errorMsg = "Enter valid +91 mobile number."
                        !safetyConfirmed -> errorMsg = "Mandatory FSSAI safety confirmation required."
                        else -> {
                            onSubmit(
                                Donation(
                                    donorOrgName = profile.organizationName.ifBlank { profile.displayName },
                                    donorDisplayName = profile.displayName,
                                    foodName = foodName.trim(),
                                    category = category,
                                    quantityKg = kg,
                                    servings = srv,
                                    dietType = dietType,
                                    preparedAtMillis = now - prepHrs * 3_600_000L,
                                    expiryAtMillis = now + safeHrs * 3_600_000L,
                                    storageType = storageType,
                                    allergens = selectedAllergens.toList(),
                                    isSafeConfirmed = true,
                                    areaLabel = areaLabel.trim().ifBlank { profile.district },
                                    district = profile.district,
                                    state = profile.state,
                                    pinCode = pinCode,
                                    approxLat = capturedLat,
                                    approxLng = capturedLng,
                                    imageKey = "preset_meal",
                                    imageBase64 = imageBase64
                                ),
                                exactAddress.ifBlank { areaLabel },
                                capturedLat,
                                capturedLng,
                                donorPhone,
                                contactConsent
                            )
                        }
                    }
                }
            ) {
                Text(if (isBusy) "Posting..." else "Publish Donation")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun PostEmergencyDialog(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onSubmit: (EmergencyRequest) -> Unit
) {
    var orgName by remember { mutableStateOf(profile.organizationName.ifBlank { profile.displayName }) }
    var orgType by remember { mutableStateOf("Orphanage") }
    var servings by remember { mutableStateOf("50") }
    var area by remember { mutableStateOf(profile.district) }
    var reason by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Emergency Food Request", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = orgName, onValueChange = { orgName = it }, label = { Text("Shelter / NGO Name *") }, singleLine = true)
                OutlinedTextField(value = orgType, onValueChange = { orgType = it }, label = { Text("Organization Type") }, singleLine = true)
                OutlinedTextField(
                    value = servings,
                    onValueChange = { servings = it.filter { c -> c.isDigit() } },
                    label = { Text("Meals Needed *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(value = area, onValueChange = { area = it }, label = { Text("Area / Locality *") }, singleLine = true)
                OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Urgency Details *") }, maxLines = 3)
                if (err != null) Text(err.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val count = servings.toLongOrNull() ?: 0L
                    if (orgName.isBlank() || count <= 0 || reason.trim().length < 3) {
                        err = "Please enter organization name, meal count, and urgency details."
                    } else {
                        onSubmit(
                            EmergencyRequest(
                                requesterName = orgName.trim(),
                                organizationType = orgType.trim(),
                                district = profile.district,
                                state = profile.state,
                                pinCode = profile.pinCode,
                                areaLabel = area.trim().ifBlank { profile.district },
                                servingsNeeded = count,
                                dietPreference = "veg",
                                urgencyReason = reason.trim()
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SaffronSecondary)
            ) {
                Text("Broadcast Alert")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
