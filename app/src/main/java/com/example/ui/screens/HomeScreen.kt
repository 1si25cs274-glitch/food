package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Co2
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.IndiaData
import com.example.core.LatLngPoint
import com.example.core.LocalizedStrings
import com.example.data.model.Donation
import com.example.data.model.EmergencyRequest
import com.example.data.model.UserProfile
import com.example.ui.components.DonationCard
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.MonthlyImpactBarChart
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.MarigoldContainer
import com.example.ui.theme.MintContainer
import com.example.ui.theme.OnMarigoldContainer
import com.example.ui.theme.OnMintContainer
import com.example.ui.theme.OnTealContainer
import com.example.ui.theme.SaffronSecondary
import com.example.ui.theme.TealContainer
import java.util.Locale

@Composable
fun HomeScreen(
    profile: UserProfile,
    strings: LocalizedStrings,
    activeDonations: List<Donation>,
    allTrackedDonations: List<Donation>,
    emergencies: List<EmergencyRequest>,
    userLocation: LatLngPoint,
    isBusy: Boolean,
    onNavigateToHub: (Int) -> Unit,
    onNavigateToMap: () -> Unit,
    onTrackDonation: (String) -> Unit,
    onRequestFood: (Donation) -> Unit,
    onFulfillEmergency: (String) -> Unit,
    onSeedSampleData: () -> Unit
) {
    val totalMeals = allTrackedDonations.sumOf { it.servings } + profile.mealsShared
    val totalKg = allTrackedDonations.sumOf { it.quantityKg }
    val totalValueInr = allTrackedDonations.sumOf { it.estimatedValueInr }
    val co2PreventedKg = totalKg * 2.5

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_foodbridge),
                        contentDescription = "FoodBridge India banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        ForestGreen.copy(alpha = 0.92f),
                                        ForestGreen.copy(alpha = 0.55f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = SaffronSecondary
                            ) {
                                Text(
                                    text = profile.accountType.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            if (profile.ngoVerified) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Verified NGO",
                                    tint = Color(0xFF4ADE80),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Text(
                            text = "Namaste, ${profile.displayName.substringBefore(" ")}!",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.tagline,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.92f),
                            maxLines = 2
                        )
                        Text(
                            text = "${profile.district}, ${profile.state} • ${profile.points} Karma pts",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFBBF7D0)
                        )
                    }
                }
            }

            // Quick Actions
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Quick Actions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickActionTile(
                            title = strings.postDonation,
                            icon = Icons.Default.AddCircle,
                            containerColor = MintContainer,
                            contentColor = OnMintContainer,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_post_donation"),
                            onClick = { onNavigateToHub(0) }
                        )
                        QuickActionTile(
                            title = strings.browseFood,
                            icon = Icons.Default.Map,
                            containerColor = MarigoldContainer,
                            contentColor = OnMarigoldContainer,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_find_food"),
                            onClick = onNavigateToMap
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickActionTile(
                            title = strings.volunteerPickups,
                            icon = Icons.Default.LocalShipping,
                            containerColor = TealContainer,
                            contentColor = OnTealContainer,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_volunteer_pickups"),
                            onClick = { onNavigateToHub(2) }
                        )
                        QuickActionTile(
                            title = strings.emergencyHelp,
                            icon = Icons.Default.Emergency,
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_emergency"),
                            onClick = { onNavigateToHub(1) }
                        )
                    }
                }
            }

            // KPI Metrics Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiMetricCard(
                            title = strings.mealsSaved,
                            value = totalMeals.toString(),
                            subtitle = "LIVE",
                            icon = Icons.Default.Restaurant,
                            containerColor = MintContainer,
                            contentColor = OnMintContainer,
                            modifier = Modifier.weight(1f)
                        )
                        KpiMetricCard(
                            title = strings.wasteReduced,
                            value = "${String.format(Locale.US, "%.1f", totalKg)} kg",
                            subtitle = "FSSAI",
                            icon = Icons.Default.Scale,
                            containerColor = MarigoldContainer,
                            contentColor = OnMarigoldContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiMetricCard(
                            title = strings.activeListings,
                            value = activeDonations.size.toString(),
                            subtitle = "INDIA",
                            icon = Icons.Default.Inventory2,
                            containerColor = TealContainer,
                            contentColor = OnTealContainer,
                            modifier = Modifier.weight(1f)
                        )
                        KpiMetricCard(
                            title = strings.valueSaved,
                            value = IndiaData.formatInr(totalValueInr),
                            subtitle = "INR ₹",
                            icon = Icons.Default.CurrencyRupee,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Monthly Impact Chart
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Monthly Rescue Impact",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Meals redistributed across Indian cities",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MintContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Co2,
                                        contentDescription = null,
                                        tint = OnMintContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${String.format(Locale.US, "%.0f", co2PreventedKg)} kg CO₂",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = OnMintContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        val currentMonthMeals = totalMeals.toInt().coerceAtLeast(25)
                        MonthlyImpactBarChart(
                            monthlyMeals = listOf(
                                "Jul" to 120,
                                "Aug" to 185,
                                "Sep" to 210,
                                "Oct" to 265,
                                "Nov" to currentMonthMeals
                            )
                        )
                    }
                }
            }

            // Active Emergency Requests
            if (emergencies.isNotEmpty()) {
                item {
                    Text(
                        text = "Urgent Community Food Alerts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                items(emergencies.take(3), key = { it.id }) { emergency ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${emergency.requesterName} (${emergency.organizationType})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Text(
                                        text = "${emergency.servingsNeeded} meals needed",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Text(
                                text = emergency.urgencyReason,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${emergency.areaLabel}, ${emergency.district} (${emergency.pinCode})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                FilledTonalButton(onClick = { onFulfillEmergency(emergency.id) }) {
                                    Text("Fulfill Alert")
                                }
                            }
                        }
                    }
                }
            }

            // Sample Data Seeder / Nearby Spotlight
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Fresh Donations Nearby",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedButton(
                        onClick = onSeedSampleData,
                        enabled = !isBusy,
                        modifier = Modifier.testTag("seed_sample_data_button")
                    ) {
                        Text(if (isBusy) "Loading..." else "Seed Multi-City Demo")
                    }
                }
            }

            if (activeDonations.isEmpty()) {
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.Restaurant,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "No active donations yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Post a surplus meal or tap 'Seed Multi-City Demo' to test live listings across Bengaluru, Tumakuru, Mumbai, Delhi, and Hyderabad.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = onSeedSampleData,
                                enabled = !isBusy,
                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                            ) {
                                Text("Load Indian Cities Sample Data")
                            }
                        }
                    }
                }
            } else {
                items(activeDonations.take(5), key = { it.id }) { donation ->
                    DonationCard(
                        donation = donation,
                        userLocation = userLocation,
                        primaryActionLabel = strings.requestFood,
                        onPrimaryAction = { onRequestFood(donation) },
                        onTrackClick = { onTrackDonation(donation.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    title: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        color = containerColor,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, contentDescription = title, tint = contentColor, modifier = Modifier.size(24.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
