package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Scale
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.IndiaData
import com.example.data.model.AuditLog
import com.example.data.model.Complaint
import com.example.data.model.Donation
import com.example.data.model.UserProfile
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MarigoldContainer
import com.example.ui.theme.MintContainer
import com.example.ui.theme.OnMarigoldContainer
import com.example.ui.theme.OnMintContainer
import com.example.ui.theme.OnTealContainer
import com.example.ui.theme.TealContainer
import java.util.Locale

@Composable
fun AdminConsoleSection(
    allUsers: List<UserProfile>,
    allDonations: List<Donation>,
    allComplaints: List<Complaint>,
    auditLogs: List<AuditLog>,
    actorName: String,
    onVerifyNgo: (String, Boolean) -> Unit,
    onSuspendUser: (String, Boolean) -> Unit,
    onUpdateComplaint: (String, String, String) -> Unit,
    onCancelDonation: (String) -> Unit
) {
    var subTab by remember { mutableIntStateOf(0) }
    var search by remember { mutableStateOf("") }

    val totalDonations = allDonations.size
    val totalMeals = allDonations.sumOf { it.servings }
    val completedDeliveries = allDonations.count { it.status == "Completed" || it.status == "Delivered" }
    val totalKg = allDonations.sumOf { it.quantityKg }
    val totalValue = allDonations.sumOf { it.estimatedValueInr }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = OnMintContainer)
                Column {
                    Text("Admin Governance & Safety Console", fontWeight = FontWeight.Bold, color = OnMintContainer)
                    Text(
                        "Manage NGO verification, complaints, suspicious accounts, and audit trails.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnMintContainer
                    )
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiMetricCard(
                title = "Total Donations",
                value = "$totalDonations",
                subtitle = "$completedDeliveries done",
                icon = Icons.Default.Restaurant,
                containerColor = MintContainer,
                contentColor = OnMintContainer,
                modifier = Modifier.weight(1f)
            )
            KpiMetricCard(
                title = "Meals Saved",
                value = "$totalMeals",
                subtitle = "${String.format(Locale.US, "%.1f", totalKg)} kg",
                icon = Icons.Default.Scale,
                containerColor = MarigoldContainer,
                contentColor = OnMarigoldContainer,
                modifier = Modifier.weight(1f)
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiMetricCard(
                title = "Value Saved",
                value = IndiaData.formatInr(totalValue),
                subtitle = "INR",
                icon = Icons.Default.CurrencyRupee,
                containerColor = TealContainer,
                contentColor = OnTealContainer,
                modifier = Modifier.weight(1f)
            )
            KpiMetricCard(
                title = "Open Reports",
                value = "${allComplaints.count { it.status == "open" || it.status == "investigating" }}",
                subtitle = "${allUsers.size} users",
                icon = Icons.Default.ReportProblem,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Users & NGOs", "Donations", "Complaints", "Audit Logs").forEachIndexed { idx, label ->
                FilterChip(
                    selected = subTab == idx,
                    onClick = { subTab = idx },
                    label = { Text(label) }
                )
            }
        }

        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            label = { Text("Search users, NGOs, tracking ID, or district") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        when (subTab) {
            0 -> {
                val filteredUsers = allUsers.filter {
                    search.isBlank() ||
                        it.displayName.contains(search, true) ||
                        it.organizationName.contains(search, true) ||
                        it.district.contains(search, true)
                }
                if (filteredUsers.isEmpty()) {
                    Text("No matching users found.", style = MaterialTheme.typography.bodySmall)
                }
                filteredUsers.forEach { user ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(user.organizationName.ifBlank { user.displayName }, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${user.accountType.uppercase()} • ${user.district}, ${user.state}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (user.fssaiOrNgoId.isNotBlank()) {
                                        Text("ID: ${user.fssaiOrNgoId}", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                if (user.ngoVerified) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Verified", tint = EmeraldPrimary)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { onVerifyNgo(user.userId, !user.ngoVerified) }
                                ) {
                                    Text(if (user.ngoVerified) "Revoke Verify" else "Verify NGO/Donor")
                                }
                                Button(
                                    onClick = { onSuspendUser(user.userId, !user.suspended) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (user.suspended) EmeraldPrimary else MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text(if (user.suspended) "Reinstate" else "Suspend")
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                val filteredDonations = allDonations.filter {
                    search.isBlank() ||
                        it.foodName.contains(search, true) ||
                        it.trackingId.contains(search, true) ||
                        it.district.contains(search, true)
                }
                filteredDonations.forEach { d ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${d.trackingId}: ${d.foodName}", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                StatusPill(d.status)
                            }
                            Text(
                                "${d.donorOrgName} • ${d.servings} meals • ${d.district}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (d.status != "Cancelled" && d.status != "Completed") {
                                OutlinedButton(onClick = { onCancelDonation(d.id) }) {
                                    Text("Force Remove / Cancel Unsafe Listing")
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                if (allComplaints.isEmpty()) {
                    Text("No safety complaints or reports.", style = MaterialTheme.typography.bodySmall)
                }
                allComplaints.forEach { c ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${c.category} (${c.severity.uppercase()})", fontWeight = FontWeight.Bold)
                                StatusPill(c.status)
                            }
                            Text(c.description, style = MaterialTheme.typography.bodySmall)
                            Text("Reporter: ${c.reporterName}", style = MaterialTheme.typography.labelSmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { onUpdateComplaint(c.id, "investigating", "Reviewed by $actorName") }
                                ) {
                                    Text("Investigate")
                                }
                                Button(
                                    onClick = { onUpdateComplaint(c.id, "resolved", "Resolved by $actorName") }
                                ) {
                                    Text("Resolve")
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                if (auditLogs.isEmpty()) {
                    Text("No audit logs yet.", style = MaterialTheme.typography.bodySmall)
                }
                auditLogs.take(25).forEach { log ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${log.action} • ${log.actorName}", fontWeight = FontWeight.SemiBold)
                            Text(log.summary, style = MaterialTheme.typography.bodySmall)
                            Text(log.targetPath, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}
