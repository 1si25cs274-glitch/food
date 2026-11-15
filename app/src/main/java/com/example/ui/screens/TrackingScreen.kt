package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.IndiaData
import com.example.core.IntentLaunchers
import com.example.core.QrCodeUtils
import com.example.data.model.Complaint
import com.example.data.model.Donation
import com.example.data.model.DonationPrivateDetails
import com.example.data.model.Review
import com.example.data.model.UserProfile
import com.example.ui.components.StatusPill
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.MintContainer
import com.example.ui.theme.OnMintContainer
import com.example.ui.theme.SaffronSecondary

@Composable
fun TrackingScreen(
    profile: UserProfile,
    donations: List<Donation>,
    selectedDonationId: String?,
    privateDetailsMap: Map<String, DonationPrivateDetails>,
    publicReviews: List<Review>,
    onSelectDonation: (String) -> Unit,
    onAdvanceStatus: (Donation, String) -> Unit,
    onOpenOtpDialog: (Donation) -> Unit,
    onSubmitComplaint: (Complaint) -> Unit,
    onSubmitReview: (Review) -> Unit
) {
    val context = LocalContext.current
    var trackingQuery by remember { mutableStateOf("") }
    var showCertificateDialog by remember { mutableStateOf(false) }
    var showComplaintDialog by remember { mutableStateOf(false) }
    var showReviewDialog by remember { mutableStateOf(false) }

    val matchingDonations = remember(donations, trackingQuery) {
        if (trackingQuery.isBlank()) donations
        else donations.filter {
            it.trackingId.contains(trackingQuery, ignoreCase = true) ||
                it.foodName.contains(trackingQuery, ignoreCase = true)
        }
    }

    val activeDonation = remember(matchingDonations, selectedDonationId) {
        matchingDonations.firstOrNull { it.id == selectedDonationId } ?: matchingDonations.firstOrNull()
    }

    LaunchedEffect(activeDonation?.id) {
        val id = activeDonation?.id
        if (!id.isNullOrBlank()) {
            onSelectDonation(id)
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "Live Donation Tracking & QR",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = trackingQuery,
                    onValueChange = { trackingQuery = it },
                    label = { Text("Search by Tracking ID (e.g. FBI-2026-...) or Food") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
            }

            if (matchingDonations.isNotEmpty()) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(matchingDonations.take(10), key = { it.id }) { item ->
                            FilterChip(
                                selected = activeDonation?.id == item.id,
                                onClick = { onSelectDonation(item.id) },
                                label = { Text("${item.trackingId} • ${item.foodName.take(16)}") }
                            )
                        }
                    }
                }
            }

            if (activeDonation == null) {
                item {
                    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.QrCode2, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(40.dp))
                            Text("No donations selected for tracking", fontWeight = FontWeight.Bold)
                            Text(
                                "Post, request, or load sample donations from Home to view live 8-stage tracking, QR codes, and delivery OTP.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                val priv = privateDetailsMap[activeDonation.id]
                val isParticipant = activeDonation.donorId == profile.userId ||
                    activeDonation.acceptedReceiverId == profile.userId ||
                    activeDonation.assignedVolunteerId == profile.userId

                // Summary Card
                item {
                    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = activeDonation.trackingId,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                StatusPill(activeDonation.status)
                            }
                            Text(activeDonation.foodName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "${activeDonation.donorOrgName} • ${activeDonation.servings} meals (${activeDonation.quantityKg} kg) • ${IndiaData.formatInr(activeDonation.estimatedValueInr)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "Prepared: ${IndiaData.formatDateTime(activeDonation.preparedAtMillis)} • Safe until: ${IndiaData.formatDateTime(activeDonation.expiryAtMillis)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (activeDonation.assignedVolunteerName.isNotBlank()) {
                                Text(
                                    "Assigned Volunteer: ${activeDonation.assignedVolunteerName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SaffronSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // 8-Stage Timeline
                item {
                    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("8-Stage Rescue Timeline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            val currentIdx = IndiaData.trackingStages.indexOf(activeDonation.status).let {
                                if (it == -1) 0 else it
                            }
                            IndiaData.trackingStages.forEachIndexed { idx, stage ->
                                val reached = idx <= currentIdx && activeDonation.status != "Cancelled"
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(if (reached) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (reached) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        } else {
                                            Text("${idx + 1}", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = stage,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (idx == currentIdx) FontWeight.Bold else FontWeight.Normal,
                                        color = if (reached) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // QR & Delivery OTP Card
                item {
                    val qrBitmap = remember(activeDonation.qrPayload) {
                        QrCodeUtils.generateQrBitmap(activeDonation.qrPayload.ifBlank { activeDonation.trackingId }, 420)
                    }
                    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("QR & OTP Delivery Verification", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (qrBitmap != null) {
                                Image(
                                    bitmap = qrBitmap,
                                    contentDescription = "Tracking QR Code for ${activeDonation.trackingId}",
                                    modifier = Modifier
                                        .size(170.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color.White)
                                        .padding(8.dp)
                                )
                            }
                            if (priv != null && isParticipant) {
                                Surface(color = MintContainer, shape = RoundedCornerShape(12.dp)) {
                                    Text(
                                        text = "Delivery Handoff OTP: ${priv.deliveryOtp}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = OnMintContainer,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    )
                                }
                            }
                            if (isParticipant && activeDonation.status != "Completed" && activeDonation.status != "Cancelled") {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { onOpenOtpDialog(activeDonation) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("verify_otp_button"),
                                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                                    ) {
                                        Text("Enter OTP / Verify QR")
                                    }
                                    if (activeDonation.status == "Volunteer Assigned") {
                                        OutlinedButton(
                                            onClick = { onAdvanceStatus(activeDonation, "Food Picked Up") },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Mark Picked Up")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Protected Contact & Exact Address (Only when authorized + consent)
                item {
                    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Privacy-Protected Contact (+91)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            if (priv == null || !isParticipant) {
                                Text(
                                    "Exact street address & +91 mobile numbers are masked (${IndiaData.maskPhone("+919876543210")}) until you are matched as Donor, Accepted Receiver, or Assigned Volunteer.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text("Exact Pickup: ${priv.exactAddress}", style = MaterialTheme.typography.bodyMedium)
                                val contactPhone = priv.donorPhone.ifBlank { priv.volunteerPhone }
                                if (priv.contactConsent && contactPhone.isNotBlank()) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = { IntentLaunchers.dialPhone(context, contactPhone) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Call +91")
                                        }
                                        Button(
                                            onClick = {
                                                IntentLaunchers.openWhatsApp(
                                                    context,
                                                    contactPhone,
                                                    "Namaste from FoodBridge India! Regarding donation ${activeDonation.trackingId} (${activeDonation.foodName})."
                                                )
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("WhatsApp")
                                        }
                                    }
                                }
                                OutlinedButton(
                                    onClick = {
                                        IntentLaunchers.openMapsDirections(context, priv.exactLat, priv.exactLng, priv.exactAddress)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Directions, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open Turn-by-Turn Pickup Directions")
                                }
                            }
                        }
                    }
                }

                // Impact Certificate, Review & Safety Report Actions
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showCertificateDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.WorkspacePremium, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Certificate")
                        }
                        OutlinedButton(
                            onClick = { showReviewDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rate")
                        }
                        OutlinedButton(
                            onClick = { showComplaintDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ReportProblem, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Report")
                        }
                    }
                }

                if (publicReviews.isNotEmpty()) {
                    item {
                        Text("Community Reviews", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    items(publicReviews.take(4), key = { it.id }) { rev ->
                        ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("${rev.reviewerName} • ${"★".repeat(rev.rating.toInt())}", fontWeight = FontWeight.Bold)
                                if (rev.comment.isNotBlank()) {
                                    Text(rev.comment, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCertificateDialog && activeDonation != null) {
        val certText = "FoodBridge India Impact Certificate\n" +
            "Awarded to: ${profile.organizationName.ifBlank { profile.displayName }}\n" +
            "Tracking ID: ${activeDonation.trackingId}\n" +
            "Food Rescued: ${activeDonation.foodName} (${activeDonation.servings} meals / ${activeDonation.quantityKg} kg)\n" +
            "Estimated Value Saved: ${IndiaData.formatInr(activeDonation.estimatedValueInr)}\n" +
            "CO2 Prevented: ${String.format("%.1f", activeDonation.quantityKg * 2.5)} kg CO2e\n" +
            "Share Food, Spread Hope. No Food Wasted, No One Hungry."
        AlertDialog(
            onDismissRequest = { showCertificateDialog = false },
            title = { Text("Impact Certificate", fontWeight = FontWeight.Bold) },
            text = {
                Surface(color = MintContainer, shape = RoundedCornerShape(16.dp)) {
                    Text(
                        text = certText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnMintContainer,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            },
            confirmButton = {
                Button(onClick = { IntentLaunchers.shareText(context, "FoodBridge Impact Certificate", certText) }) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share Certificate")
                }
            },
            dismissButton = { TextButton(onClick = { showCertificateDialog = false }) { Text("Close") } }
        )
    }

    if (showComplaintDialog && activeDonation != null) {
        var category by remember { mutableStateOf("hygiene_issue") }
        var severity by remember { mutableStateOf("medium") }
        var desc by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showComplaintDialog = false },
            title = { Text("Report Food Safety / Issue", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("expired_food" to "Expired", "hygiene_issue" to "Hygiene", "suspicious_account" to "Suspicious").forEach { (k, l) ->
                            FilterChip(selected = category == k, onClick = { category = k }, label = { Text(l) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("low", "medium", "high").forEach { sev ->
                            FilterChip(selected = severity == sev, onClick = { severity = sev }, label = { Text(sev.uppercase()) })
                        }
                    }
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Describe the safety concern *") },
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (desc.trim().length >= 5) {
                            onSubmitComplaint(
                                Complaint(
                                    reporterName = profile.displayName,
                                    targetDonationId = activeDonation.id,
                                    targetUserId = activeDonation.donorId,
                                    category = category,
                                    description = desc.trim(),
                                    severity = severity
                                )
                            )
                            showComplaintDialog = false
                        }
                    }
                ) { Text("Submit Report") }
            },
            dismissButton = { TextButton(onClick = { showComplaintDialog = false }) { Text("Cancel") } }
        )
    }

    if (showReviewDialog && activeDonation != null) {
        var rating by remember { mutableIntStateOf(5) }
        var comment by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showReviewDialog = false },
            title = { Text("Rate Food Handoff", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..5).forEach { star ->
                            FilterChip(selected = rating == star, onClick = { rating = star }, label = { Text("$star★") })
                        }
                    }
                    OutlinedTextField(value = comment, onValueChange = { comment = it }, label = { Text("Feedback") }, maxLines = 3)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSubmitReview(
                            Review(
                                donationId = activeDonation.id,
                                reviewerName = profile.displayName,
                                targetUserId = activeDonation.donorId,
                                rating = rating.toLong(),
                                comment = comment.trim()
                            )
                        )
                        showReviewDialog = false
                    }
                ) { Text("Post Review") }
            },
            dismissButton = { TextButton(onClick = { showReviewDialog = false }) { Text("Cancel") } }
        )
    }
}
