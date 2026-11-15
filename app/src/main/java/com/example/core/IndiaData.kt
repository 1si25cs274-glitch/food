package com.example.core

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class CityAnchor(
    val name: String,
    val district: String,
    val state: String,
    val pinCode: String,
    val lat: Double,
    val lng: Double
)

object IndiaData {
    val statesToDistricts: Map<String, List<String>> = linkedMapOf(
        "Karnataka" to listOf("Bengaluru Urban", "Tumakuru", "Mysuru", "Mangaluru", "Hubballi-Dharwad"),
        "Maharashtra" to listOf("Mumbai", "Pune", "Nagpur", "Nashik", "Thane"),
        "Delhi" to listOf("New Delhi", "South Delhi", "Central Delhi", "East Delhi"),
        "Tamil Nadu" to listOf("Chennai", "Coimbatore", "Madurai", "Tiruchirappalli"),
        "Telangana" to listOf("Hyderabad", "Warangal", "Rangareddy"),
        "West Bengal" to listOf("Kolkata", "Howrah", "Darjeeling"),
        "Gujarat" to listOf("Ahmedabad", "Surat", "Vadodara"),
        "Rajasthan" to listOf("Jaipur", "Udaipur", "Jodhpur"),
        "Kerala" to listOf("Ernakulam", "Thiruvananthapuram", "Kozhikode")
    )

    val majorCities: List<CityAnchor> = listOf(
        CityAnchor("Bengaluru", "Bengaluru Urban", "Karnataka", "560038", 12.9716, 77.5946),
        CityAnchor("Tumakuru", "Tumakuru", "Karnataka", "572103", 13.3392, 77.1140),
        CityAnchor("Mumbai", "Mumbai", "Maharashtra", "400001", 19.0760, 72.8777),
        CityAnchor("Delhi NCR", "New Delhi", "Delhi", "110001", 28.6139, 77.2090),
        CityAnchor("Chennai", "Chennai", "Tamil Nadu", "600001", 13.0827, 80.2707),
        CityAnchor("Hyderabad", "Hyderabad", "Telangana", "500001", 17.3850, 78.4867),
        CityAnchor("Pune", "Pune", "Maharashtra", "411001", 18.5204, 73.8567),
        CityAnchor("Kolkata", "Kolkata", "West Bengal", "700001", 22.5726, 88.3639)
    )

    val foodCategories = listOf(
        "Cooked Meals",
        "Raw Grains & Groceries",
        "Bakery & Dairy",
        "Fruits & Vegetables",
        "Packaged Ration",
        "Event Surplus"
    )

    val storageTypes = listOf(
        "Hot Box",
        "Refrigerated (0-5°C)",
        "Ambient Dry",
        "Frozen (-18°C)"
    )

    val allergenOptions = listOf(
        "Dairy",
        "Nuts",
        "Gluten",
        "Soy",
        "Sesame",
        "Mustard",
        "None"
    )

    val trackingStages = listOf(
        "Donation Posted",
        "Request Received",
        "Accepted",
        "Volunteer Assigned",
        "Food Picked Up",
        "Out for Delivery",
        "Delivered",
        "Completed"
    )

    val fssaiSafetyRules = listOf(
        "Cooked meals kept at ambient temperature must be distributed within 4 hours of preparation.",
        "Hot food should be held at 60°C or above in clean, food-grade insulated containers.",
        "Chilled items must stay at 0–5°C in clean cold boxes until handoff.",
        "Never donate stale, reheated leftovers, visibly spoiled items, or damaged cans.",
        "Clearly declare allergens (Dairy, Nuts, Gluten, Soy) before confirmation."
    )

    fun isValidIndianMobile(raw: String): Boolean {
        val digits = raw.filter { it.isDigit() }
        val normalized = if (digits.length == 12 && digits.startsWith("91")) digits.drop(2) else digits
        return normalized.length == 10 && normalized.first() in '6'..'9'
    }

    fun normalizeIndianPhone(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        val ten = when {
            digits.length == 12 && digits.startsWith("91") -> digits.drop(2)
            digits.length == 11 && digits.startsWith("0") -> digits.drop(1)
            else -> digits
        }
        return if (ten.length == 10) "+91$ten" else raw.trim()
    }

    fun maskPhone(phone: String): String {
        val clean = normalizeIndianPhone(phone)
        return if (clean.length >= 13) {
            "${clean.take(5)}•••••${clean.takeLast(2)}"
        } else {
            "+91 9•••••••"
        }
    }

    fun isValidPinCode(pin: String): Boolean {
        val trimmed = pin.trim()
        return trimmed.length == 6 && trimmed.all { it.isDigit() } && trimmed.first() in '1'..'8'
    }

    fun formatInr(amount: Number): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        formatter.maximumFractionDigits = 0
        return formatter.format(amount)
    }

    fun formatDateTime(millis: Long): String {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale("en", "IN"))
        return sdf.format(Date(millis))
    }

    fun remainingTimeLabel(expiryMillis: Long): String {
        val diff = expiryMillis - System.currentTimeMillis()
        if (diff <= 0L) return "Expired"
        val hours = diff / 3_600_000L
        val mins = (diff % 3_600_000L) / 60_000L
        return if (hours > 0) "${hours}h ${mins}m left" else "${mins}m left"
    }

    fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    fun estimateValueInr(servings: Int, category: String): Int {
        val perServing = when (category) {
            "Cooked Meals", "Event Surplus" -> 55
            "Raw Grains & Groceries", "Packaged Ration" -> 45
            "Bakery & Dairy" -> 40
            else -> 35
        }
        return (servings * perServing).coerceAtLeast(50)
    }
}
